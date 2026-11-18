package com.alphito.kcal.core.model

/** Supported UI languages, in the order shown on the Language screen. `tag` is a BCP-47 code. */
enum class AppLanguage(val tag: String, val nativeName: String, val englishName: String, val rtl: Boolean = false) {
    English("en", "English", "English"),
    Arabic("ar", "العربية", "Arabic", rtl = true),
    German("de", "Deutsch", "German"),
    Spanish("es", "Español", "Spanish"),
    French("fr", "Français", "French"),
    Hindi("hi", "हिन्दी", "Hindi"),
    Indonesian("id", "Bahasa Indonesia", "Indonesian"),
    Portuguese("pt", "Português", "Portuguese"),
    Russian("ru", "Русский", "Russian"),
    Turkish("tr", "Türkçe", "Turkish"),
    Urdu("ur", "اردو", "Urdu", rtl = true),
    Chinese("zh", "中文", "Chinese"),
    ;

    /** Short code shown on the badge ("EN", "AR" …). Indonesian keeps the familiar "IN". */
    val code: String get() = if (this == Indonesian) "IN" else tag.uppercase()

    companion object {
        // "in" is Android's legacy code for Indonesian – accept both.
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: if (tag == "in") Indonesian else English
    }
}

enum class PremiumPlan { Weekly, Monthly }

data class SubscriptionState(
    val activePlan: PremiumPlan? = null,
    val trialDaysLeft: Int? = null,
) {
    val isPremium: Boolean get() = activePlan != null
}

data class AppPreferences(
    val onboardingComplete: Boolean = false,
    /** First-session intro (Premium → Language) finished; afterwards launch skips it. */
    val introSeen: Boolean = false,
    /** Height entry unit chosen on "Tell us about you" (stored value is always cm). */
    val heightInFeet: Boolean = false,
    /** Weight entry/display unit (stored value is always kg). */
    val weightInLb: Boolean = false,
    val language: AppLanguage = AppLanguage.English,
    val activityTrackingEnabled: Boolean = false,
    val waterGoalMl: Int = WaterDay.DEFAULT_GOAL_ML,
    val freeTrialPromptSeen: Boolean = false,
    val subscription: SubscriptionState = SubscriptionState(),
    val lastRating: Int = 0,
)
