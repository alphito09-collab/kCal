package com.alphito.kcal.core.designsystem.util

import androidx.annotation.DrawableRes
import com.alphito.kcal.core.designsystem.R
import com.alphito.kcal.core.model.AppLanguage

/** Maps catalogue image keys (stored in Room) to bundled, pre-sized WebP thumbnails. */
object FoodImages {
    private val map: Map<String, Int> = mapOf(
        "anda_paratha" to R.drawable.food_anda_paratha,
        "lassi" to R.drawable.food_lassi,
        "daal_chawal" to R.drawable.food_daal_chawal,
        "daal_chawal_salad" to R.drawable.food_next_meal_card,
        "salad" to R.drawable.food_salad,
        "fruit_chaat" to R.drawable.food_fruit_chaat,
        "grilled_chicken" to R.drawable.food_grilled_chicken,
        "roti" to R.drawable.food_roti,
        "chickpea_curry_rice" to R.drawable.food_chickpea_curry_rice,
        "vegetable_pulao" to R.drawable.food_vegetable_pulao,
        "chicken_karahi_roti" to R.drawable.food_chicken_karahi_roti,
        "mixed_veg_roti" to R.drawable.food_mixed_veg_roti,
    )

    @DrawableRes
    fun resFor(key: String?): Int = map[key] ?: R.drawable.food_daal_chawal
}

object OnboardingImages {
    @DrawableRes val NoManualLogging = R.drawable.img_onboarding_02
    @DrawableRes val BuiltForRegion = R.drawable.img_onboarding_03
    @DrawableRes val FitsBudget = R.drawable.img_onboarding_04
}

object Flags {
    @DrawableRes
    fun resFor(language: AppLanguage): Int = when (language) {
        AppLanguage.English -> R.drawable.flag_en
        AppLanguage.Arabic -> R.drawable.flag_ar
        AppLanguage.German -> R.drawable.flag_de
        AppLanguage.Spanish -> R.drawable.flag_es
        AppLanguage.French -> R.drawable.flag_fr
        AppLanguage.Hindi -> R.drawable.flag_hi
        AppLanguage.Indonesian -> R.drawable.flag_id
        AppLanguage.Portuguese -> R.drawable.flag_pt
        AppLanguage.Russian -> R.drawable.flag_ru
        AppLanguage.Turkish -> R.drawable.flag_tr
        AppLanguage.Urdu -> R.drawable.flag_ur
        AppLanguage.Chinese -> R.drawable.flag_zh
    }
}
