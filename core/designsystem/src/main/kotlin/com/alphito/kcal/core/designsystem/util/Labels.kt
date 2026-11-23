package com.alphito.kcal.core.designsystem.util

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.alphito.kcal.core.designsystem.R
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.WeightUnits
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Every user-visible label is resolved from string resources here, against the *current*
 * configuration, so changing the app language re-renders text, units, dates and food names
 * in place.
 */

/** The UI locale (reads LocalConfiguration, so callers recompose when the language changes). */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

@Composable
@ReadOnlyComposable
fun MealType.label(): String = stringResource(
    when (this) {
        MealType.Breakfast -> R.string.ds_meal_breakfast
        MealType.Lunch -> R.string.ds_meal_lunch
        MealType.Snack -> R.string.ds_meal_snack
        MealType.Dinner -> R.string.ds_meal_dinner
    },
)

@Composable
@ReadOnlyComposable
fun kcalUnit(): String = stringResource(R.string.ds_unit_kcal)

@Composable
@ReadOnlyComposable
fun kcalPerDayUnit(): String = stringResource(R.string.ds_unit_kcal_day)

@Composable
@ReadOnlyComposable
fun unitLabel(unit: KcalUnit): String = stringResource(unit.res)

enum class KcalUnit(internal val res: Int) {
    Kg(R.string.ds_unit_kg), Lb(R.string.ds_unit_lb), Cm(R.string.ds_unit_cm), Ft(R.string.ds_unit_ft),
    In(R.string.ds_unit_in), Ml(R.string.ds_unit_ml), MgDl(R.string.ds_unit_mgdl),
}

@Composable
@ReadOnlyComposable
fun weightUnitLabel(inLb: Boolean): String = unitLabel(if (inLb) KcalUnit.Lb else KcalUnit.Kg)

@Composable
@ReadOnlyComposable
fun valueWithUnit(value: String, unit: String): String = stringResource(R.string.ds_value_unit, value, unit)

@Composable
@ReadOnlyComposable
fun kcalLabel(kcal: Int): String = stringResource(R.string.ds_kcal, KcalFormat.int(kcal, appLocale()))

/** "250 kcal · Budget-friendly" – cost is a local-market tier (1–3), not a currency amount. */
@Composable
@ReadOnlyComposable
fun kcalCostLabel(kcal: Int, cost: Int): String =
    stringResource(R.string.ds_kcal_cost, KcalFormat.int(kcal, appLocale()), costLabel(cost))

@Composable
@ReadOnlyComposable
fun costLabel(cost: Int): String = stringResource(
    when (cost) {
        1 -> R.string.ds_cost_budget
        2 -> R.string.ds_cost_moderate
        else -> R.string.ds_cost_premium
    },
)

/** "150 g" with locale digits. */
@Composable
@ReadOnlyComposable
fun gramsText(grams: Int): String = valueWithUnit(KcalFormat.int(grams, appLocale()), stringResource(R.string.ds_unit_g))

/** "68.4 kg" / "150.8 lb" from a stored kg value. */
@Composable
@ReadOnlyComposable
fun weightText(kg: Double, inLb: Boolean): String =
    valueWithUnit(KcalFormat.decimal1(WeightUnits.display(kg, inLb), appLocale()), weightUnitLabel(inLb))

/** "−0.6 kg" / "−1.3 lb" for a kg delta. */
@Composable
@ReadOnlyComposable
fun weightChangeText(deltaKg: Double, inLb: Boolean): String =
    valueWithUnit(KcalFormat.signedDecimal1(if (inLb) WeightUnits.toLb(deltaKg) else deltaKg), weightUnitLabel(inLb))

@Composable
@ReadOnlyComposable
fun fractionText(value: Int, total: Int): String = stringResource(R.string.ds_fraction, value, total)

@Composable
@ReadOnlyComposable
fun rangeText(from: String, to: String): String = stringResource(R.string.ds_range, from, to)

/** Localized dish name from the catalogue (all 12 app languages; English as fallback). */
@Composable
@ReadOnlyComposable
fun foodName(food: Food): String = food.localizedName(appLanguage())

/** "Rice 190 g · Lentils 150 g · Tomato 40 g" – the three biggest ingredients, scaled to the portion. */
@Composable
@ReadOnlyComposable
fun portionText(food: Food, servings: Double = 1.0): String {
    val language = appLanguage()
    val unit = stringResource(R.string.ds_unit_g)
    val locale = appLocale()
    return food.ingredients.sortedByDescending { it.grams }.take(3).joinToString(" · ") {
        it.localizedName(language) + " " + KcalFormat.int((it.grams * servings).roundToInt(), locale) + " " + unit
    }
}

/** BCP-47 language of the UI ("id" for Indonesian even on JDKs that still report "in"). */
@Composable
@ReadOnlyComposable
fun appLanguage(): String = appLocale().language.let { if (it == "in") "id" else it }

/** "Daal chawal + Salad" in the current language. */
@Composable
@ReadOnlyComposable
fun mealTitle(slot: MealSlot): String = slot.items.map { foodName(it.food) }.joinToString(" + ")

/** Country name in the UI language ("Pakistan", "باكستان", "巴基斯坦") from an ISO code. */
@Composable
@ReadOnlyComposable
fun regionLabel(countryCode: String): String =
    Locale.Builder().setRegion(countryCode).build().getDisplayCountry(appLocale()).ifBlank { countryCode }

/**
 * Locale-correct date from a CLDR skeleton ("EEEdMMM", "MMMd", "MMMMyyyy"): field order,
 * month/day names and numerals all follow the UI language (e.g. "Wed, 11 Aug" / "ب، ١١ أغسطس").
 */
@Composable
@ReadOnlyComposable
fun localizedDate(date: LocalDate, skeleton: String): String {
    val locale = appLocale()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
}

/** Name of a language in the *current* UI language (e.g. "German" / "Allemand" / "ألمانية"). */
@Composable
@ReadOnlyComposable
fun languageDisplayName(tag: String): String {
    val locale = appLocale()
    return Locale.forLanguageTag(tag).getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
