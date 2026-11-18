package com.alphito.kcal.core.data.catalog

import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.MealType
import kotlinx.collections.immutable.persistentSetOf

/**
 * Built-in regional catalogue (Pakistan / PKR) taken from the Figma screens. Seeded into Room
 * on first use; a remote catalogue can later replace this without touching the UI.
 */
internal object FoodCatalog {
    private val lunchDinner = persistentSetOf(MealType.Lunch, MealType.Dinner)

    val foods: List<Food> = listOf(
        Food("anda_paratha", "Anda paratha", "Paratha 80g + Egg 50g", 250, 90, FoodRole.BreakfastMain,
            persistentSetOf(MealType.Breakfast), "anda_paratha", persistentSetOf()),
        Food("lassi", "Lassi", "Yogurt Drink 200ml", 130, 30, FoodRole.Drink,
            persistentSetOf(MealType.Breakfast), "lassi", persistentSetOf(FoodTag.Dairy, FoodTag.Vegetarian)),
        Food("daal_chawal", "Daal chawal", "Rice 150g + Daal 120g", 420, 110, FoodRole.Main,
            lunchDinner, "daal_chawal", persistentSetOf(FoodTag.Vegetarian)),
        Food("chickpea_curry_rice", "Chickpea curry + rice", "Chana 150g + Rice 120g", 410, 100, FoodRole.Main,
            lunchDinner, "chickpea_curry_rice", persistentSetOf(FoodTag.Vegetarian, FoodTag.HighFodmap)),
        Food("vegetable_pulao", "Vegetable pulao", "Pulao 250g", 390, 95, FoodRole.Main,
            lunchDinner, "vegetable_pulao", persistentSetOf(FoodTag.Vegetarian)),
        Food("chicken_karahi_roti", "Chicken karahi + roti", "Karahi 180g + Roti 1 pc", 480, 180, FoodRole.Main,
            lunchDinner, "chicken_karahi_roti", persistentSetOf(FoodTag.Meat, FoodTag.Spicy)),
        Food("mixed_veg_roti", "Mixed veg + roti", "Mixed Veg 180g + Roti 1 pc", 350, 85, FoodRole.Main,
            lunchDinner, "mixed_veg_roti", persistentSetOf(FoodTag.Vegetarian)),
        Food("grilled_chicken", "Grilled chicken", "Chicken 150g", 380, 160, FoodRole.Protein,
            lunchDinner, "grilled_chicken", persistentSetOf(FoodTag.Meat)),
        Food("salad", "Salad", "Mixed Veg 150g", 100, 40, FoodRole.Side,
            persistentSetOf(MealType.Lunch, MealType.Snack), "salad", persistentSetOf(FoodTag.Vegetarian)),
        Food("roti", "Roti", "Roti 120g (2 pcs)", 230, 60, FoodRole.Side,
            persistentSetOf(MealType.Dinner), "roti", persistentSetOf(FoodTag.Vegetarian)),
        Food("fruit_chaat", "Fruit chaat", "Mixed Fruit 200g", 180, 60, FoodRole.Snack,
            persistentSetOf(MealType.Snack), "fruit_chaat", persistentSetOf(FoodTag.Vegetarian, FoodTag.HighSugar)),
    )
}
