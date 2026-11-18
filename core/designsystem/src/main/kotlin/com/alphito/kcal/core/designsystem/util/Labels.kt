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

@Composable
@ReadOnlyComposable
fun kcalPriceLabel(kcal: Int, pricePkr: Int): String =
    stringResource(R.string.ds_kcal_price, KcalFormat.int(kcal, appLocale()), KcalFormat.int(pricePkr, appLocale()))

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

private val foodNames = mapOf(
    "anda_paratha" to (R.string.ds_food_anda_paratha to R.string.ds_food_anda_paratha_portion),
    "lassi" to (R.string.ds_food_lassi to R.string.ds_food_lassi_portion),
    "daal_chawal" to (R.string.ds_food_daal_chawal to R.string.ds_food_daal_chawal_portion),
    "chickpea_curry_rice" to (R.string.ds_food_chickpea_curry_rice to R.string.ds_food_chickpea_curry_rice_portion),
    "vegetable_pulao" to (R.string.ds_food_vegetable_pulao to R.string.ds_food_vegetable_pulao_portion),
    "chicken_karahi_roti" to (R.string.ds_food_chicken_karahi_roti to R.string.ds_food_chicken_karahi_roti_portion),
    "mixed_veg_roti" to (R.string.ds_food_mixed_veg_roti to R.string.ds_food_mixed_veg_roti_portion),
    "grilled_chicken" to (R.string.ds_food_grilled_chicken to R.string.ds_food_grilled_chicken_portion),
    "salad" to (R.string.ds_food_salad to R.string.ds_food_salad_portion),
    "roti" to (R.string.ds_food_roti to R.string.ds_food_roti_portion),
    "fruit_chaat" to (R.string.ds_food_fruit_chaat to R.string.ds_food_fruit_chaat_portion),
)

/** Localized dish name (catalogue foods are resources; unknown/remote foods fall back to their own name). */
@Composable
@ReadOnlyComposable
fun foodName(food: Food): String = foodNames[food.id]?.let { stringResource(it.first) } ?: food.name

@Composable
@ReadOnlyComposable
fun foodPortion(food: Food): String = foodNames[food.id]?.let { stringResource(it.second) } ?: food.portion

/** "Daal chawal + Salad" in the current language. */
@Composable
@ReadOnlyComposable
fun mealTitle(slot: MealSlot): String = slot.items.map { foodName(it.food) }.joinToString(" + ")

private val regionNames = mapOf(
    "Pakistan — Lahore" to R.string.ds_region_lahore,
    "Pakistan — Karachi" to R.string.ds_region_karachi,
    "Pakistan — Islamabad" to R.string.ds_region_islamabad,
    "Pakistan — Rawalpindi" to R.string.ds_region_rawalpindi,
    "Pakistan — Faisalabad" to R.string.ds_region_faisalabad,
    "Pakistan — Peshawar" to R.string.ds_region_peshawar,
)

/** Regions are stored by a stable key (their English name) and displayed localized. */
@Composable
@ReadOnlyComposable
fun regionLabel(region: String): String = regionNames[region]?.let { stringResource(it) } ?: region

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
