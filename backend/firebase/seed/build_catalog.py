#!/usr/bin/env python3
"""
Builds the kCal food catalogue from the editable tables in ./catalog/.

    python3 backend/firebase/seed/build_catalog.py

Inputs (pipe-separated, '#' comments):
  catalog/ingredients.psv   nutrition per 100 g + flags + 12-language names
  catalog/foods_*.psv       dishes = ingredients with grams + 12-language names
  catalog/VERSION           integer; bump it whenever the data changes

Outputs (identical content):
  core/data/src/main/assets/catalog.json   bundled with the app (offline first run)
  backend/firebase/seed/catalog.json       uploaded by seed.mjs (remote updates)

Nutrition, glycaemic level and condition tags are *derived* from the ingredients, so a dish can
never claim to be lactose-free while containing milk, and the rules live in one place:
  HighFodmap  fodmap ingredients >= 90 g, or onion/garlic (fodmap2) >= 15 g      (Monash low-FODMAP)
  Dairy       high-lactose dairy (milk) >= 150 g  (~7 g lactose; <=12 g/sitting is usually tolerated)
  HighSugar   added/concentrated sugar >= 15 g; AddedSugar >= 5 g (diabetes excludes both)
  Spicy       chilli >= 3 g
  Gi High     >= 75 % of carbohydrate from high-GI ingredients; Low when >= 60 % from low-GI
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
SRC = pathlib.Path(__file__).resolve().parent / "catalog"
LANGS = ["en", "ar", "de", "es", "fr", "hi", "id", "pt", "ru", "tr", "ur", "zh"]
ROLES = {"BreakfastMain", "Drink", "Main", "Protein", "Side", "Snack"}
MEALS = {"B": "Breakfast", "L": "Lunch", "S": "Snack", "D": "Dinner"}
CLUSTERS = {
    "global", "south_asia", "middle_east", "north_africa", "turkiye_caucasus", "europe", "cis", "east_asia",
    "southeast_asia", "latin_america", "north_america_oceania", "sub_saharan_africa",
}
CATEGORIES = {
    "grain", "tuber", "legume", "plant_protein", "meat", "fish", "egg", "dairy", "fat", "vegetable",
    "fruit", "nuts", "sweet", "drink", "condiment",
}


def rows(path):
    for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line.strip() and not line.startswith("#"):
            yield n, [c.strip() for c in line.split("|")]


def fail(msg):
    sys.exit(f"catalog error: {msg}")


def load_ingredients():
    out = {}
    for n, c in rows(SRC / "ingredients.psv"):
        if len(c) != 9 + len(LANGS):
            fail(f"ingredients.psv:{n} has {len(c)} columns")
        iid, cat, flags, kcal, p, carb, fat, fib, gi, *names = c
        if iid in out:
            fail(f"duplicate ingredient {iid}")
        if cat not in CATEGORIES:
            fail(f"ingredients.psv:{n} unknown category {cat}")
        out[iid] = {
            "id": iid, "category": cat, "flags": set(filter(None, flags.split(","))),
            "kcal": float(kcal), "protein": float(p), "carbs": float(carb), "fat": float(fat), "fiber": float(fib),
            "gi": gi, "names": dict(zip(LANGS, names)),
        }
    return out


def derive(parts, ing):
    tot = dict(kcal=0.0, protein=0.0, carbs=0.0, fat=0.0, fiber=0.0)
    fodmap = fodmap2 = lactose = sugar = chili = 0.0
    carbs_by_gi = {"L": 0.0, "M": 0.0, "H": 0.0}
    has = set()
    for iid, g in parts:
        i = ing[iid]
        for k in tot:
            tot[k] += i[k] * g / 100
        carbs = i["carbs"] * g / 100
        # Non-starchy vegetables/fruit acids carry few, slowly absorbed carbs: count them as low-GI.
        carbs_by_gi[i["gi"] if i["gi"] in carbs_by_gi else "L"] += carbs
        f = i["flags"]
        fodmap += g if "fodmap" in f else 0
        fodmap2 += g if "fodmap2" in f else 0
        lactose += g if "lactose" in f else 0
        sugar += g if "sugar" in f else 0
        chili += g if "spicy" in f else 0
        if "gluten" in f:
            has.add("Gluten")
        has.add(i["category"])
    carb_total = sum(carbs_by_gi.values())
    if carb_total < 10:
        gi = "Low"
    elif carbs_by_gi["H"] / carb_total >= 0.75:
        gi = "High"
    elif carbs_by_gi["L"] / carb_total >= 0.6:
        gi = "Low"
    else:
        gi = "Medium"
    tags = []
    if lactose >= 150:
        tags.append("Dairy")
    if sugar >= 15:
        tags.append("HighSugar")
    if sugar >= 5:
        # Any meaningful added sugar (sweet tea, lassi, honey) – excluded for diabetes.
        tags.append("AddedSugar")
    if chili >= 3:
        tags.append("Spicy")
    if fodmap >= 90 or fodmap2 >= 15:
        tags.append("HighFodmap")
    if "meat" in has:
        tags.append("Meat")
    if "fish" in has:
        tags.append("Fish")
    if "egg" in has:
        tags.append("Egg")
    if "nuts" in has:
        tags.append("Nuts")
    if "Gluten" in has:
        tags.append("Gluten")
    if not ({"meat", "fish"} & has):
        tags.append("Vegetarian")
    return {k: round(v, 1) for k, v in tot.items()}, gi, tags


def load_foods(ing):
    foods, seen = [], set()
    for path in sorted(SRC.glob("foods_*.psv")):
        for n, c in rows(path):
            where = f"{path.name}:{n}"
            if len(c) != 8 + len(LANGS):
                fail(f"{where} has {len(c)} columns")
            fid, regions, role, meals, cost, emoji, image, parts, *names = c
            if fid in seen:
                fail(f"{where} duplicate food {fid}")
            seen.add(fid)
            if role not in ROLES:
                fail(f"{where} bad role {role}")
            region_list = [r for r in regions.split(",") if r]
            for r in region_list:
                if r not in CLUSTERS and not re.fullmatch(r"[A-Z]{2}", r):
                    fail(f"{where} bad region {r}")
            meal_list = []
            for m in meals.split(","):
                if m not in MEALS:
                    fail(f"{where} bad meal {m}")
                meal_list.append(MEALS[m])
            if cost not in {"1", "2", "3"}:
                fail(f"{where} bad cost {cost}")
            parsed = []
            for p in parts.split(","):
                iid, grams = p.split(":")
                if iid not in ing:
                    fail(f"{where} unknown ingredient {iid}")
                parsed.append((iid, float(grams)))
            if any(not x for x in names):
                fail(f"{where} missing a translation")
            nutrition, gi, tags = derive(parsed, ing)
            foods.append({
                "id": fid,
                "names": dict(zip(LANGS, names)),
                "role": role,
                "mealTypes": meal_list,
                "cost": int(cost),
                "emoji": emoji,
                "imageKey": "" if image == "-" else image,
                "regions": [] if region_list == ["global"] else region_list,
                "kcal": round(nutrition["kcal"]),
                "protein": nutrition["protein"],
                "carbs": nutrition["carbs"],
                "fat": nutrition["fat"],
                "fiber": nutrition["fiber"],
                "gi": gi,
                "tags": tags,
                "ingredients": [
                    {"id": iid, "grams": round(g), "category": ing[iid]["category"], "names": ing[iid]["names"]}
                    for iid, g in parsed
                ],
                "active": True,
            })
    return foods


def main():
    ing = load_ingredients()
    foods = load_foods(ing)
    version = int((SRC / "VERSION").read_text().strip())
    catalog = {"version": version, "foods": foods}
    text = json.dumps(catalog, ensure_ascii=False, separators=(",", ":"))
    for target in (ROOT / "core/data/src/main/assets/catalog.json", SRC.parent / "catalog.json"):
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")
    by_region = {}
    for f in foods:
        for r in f["regions"] or ["global"]:
            by_region[r] = by_region.get(r, 0) + 1
    print(f"catalog v{version}: {len(ing)} ingredients, {len(foods)} foods, {len(text) // 1024} KB")
    print("  " + ", ".join(f"{k}={v}" for k, v in sorted(by_region.items())))


if __name__ == "__main__":
    main()
