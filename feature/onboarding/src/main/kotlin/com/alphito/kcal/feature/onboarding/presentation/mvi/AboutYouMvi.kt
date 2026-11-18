package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.HeightUnits
import com.alphito.kcal.core.model.WeightUnits
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.feature.onboarding.domain.ProfileRules

data class AboutYouState(
    val name: String = "",
    val age: String = "",
    val gender: Gender = Gender.Female,
    val height: String = "",
    val heightInFeet: Boolean = false,
    val feet: String = "",
    val inches: String = "",
    val weight: String = "",
    val weightInLb: Boolean = false,
    val isEdit: Boolean = false,
    val showError: Boolean = false,
    val loaded: Boolean = false,
) : UiState {
    /** Height in cm regardless of the entry unit (null while incomplete). */
    val heightCm: Int?
        get() = if (heightInFeet) {
            val ft = feet.toIntOrNull(); val inch = inches.toIntOrNull() ?: 0
            if (ft == null || inch > 11) null else HeightUnits.toCm(ft, inch)
        } else height.toIntOrNull()

    /** Weight in kg regardless of the entry unit. */
    val weightKg: Double?
        get() = weight.toDoubleOrNull()?.let { WeightUnits.toKg(it, weightInLb) }

    val canContinue: Boolean
        get() = ProfileRules.isValid(name, age.toIntOrNull(), heightCm, weightKg)
}

sealed interface AboutYouIntent : UiIntent {
    data class Name(val value: String) : AboutYouIntent
    data class Age(val value: String) : AboutYouIntent
    data class GenderSelected(val value: Gender) : AboutYouIntent
    data class Height(val value: String) : AboutYouIntent
    data class Feet(val value: String) : AboutYouIntent
    data class Inches(val value: String) : AboutYouIntent
    data class HeightUnit(val feet: Boolean) : AboutYouIntent
    data class WeightUnit(val lb: Boolean) : AboutYouIntent
    data class Weight(val value: String) : AboutYouIntent
    data object Submit : AboutYouIntent
    data object Back : AboutYouIntent
}

sealed interface AboutYouEffect : UiEffect {
    data object Next : AboutYouEffect
    data object Close : AboutYouEffect
}

class AboutYouViewModel(
    private val isEdit: Boolean,
    private val users: UserRepository,
    private val preferences: PreferencesRepository,
) : MviViewModel<AboutYouState, AboutYouIntent, AboutYouEffect>(AboutYouState(isEdit = isEdit)) {

    init {
        launchSafe {
            val existing = users.current()
            val prefs = preferences.snapshot()
            val inFeet = prefs.heightInFeet
            val inLb = prefs.weightInLb
            reduce {
                val base = copy(heightInFeet = inFeet, weightInLb = inLb, loaded = true)
                if (existing == null) base else {
                    val fi = HeightUnits.toFeetInches(existing.heightCm)
                    base.copy(
                        name = existing.name, age = existing.age.toString(), gender = existing.gender,
                        height = existing.heightCm.toString(), feet = fi.feet.toString(), inches = fi.inches.toString(),
                        weight = WeightUnits.display(existing.weightKg, inLb).toString(),
                    )
                }
            }
        }
    }

    override fun onIntent(intent: AboutYouIntent) {
        when (intent) {
            is AboutYouIntent.Name -> reduce { copy(name = intent.value.take(40), showError = false) }
            is AboutYouIntent.Age -> reduce { copy(age = intent.value.digitsOnly(3), showError = false) }
            is AboutYouIntent.GenderSelected -> reduce { copy(gender = intent.value) }
            is AboutYouIntent.Height -> reduce { copy(height = intent.value.digitsOnly(3), showError = false) }
            is AboutYouIntent.Feet -> reduce { copy(feet = intent.value.digitsOnly(1), showError = false) }
            is AboutYouIntent.Inches -> reduce { copy(inches = intent.value.digitsOnly(2), showError = false) }
            is AboutYouIntent.HeightUnit -> reduce {
                // Convert whatever was typed so switching units never loses the value.
                if (intent.feet == heightInFeet) this
                else if (intent.feet) {
                    val fi = height.toIntOrNull()?.let(HeightUnits::toFeetInches)
                    copy(heightInFeet = true, feet = fi?.feet?.toString().orEmpty(), inches = fi?.inches?.toString().orEmpty())
                } else copy(heightInFeet = false, height = heightCm?.toString().orEmpty())
            }
            is AboutYouIntent.WeightUnit -> reduce {
                if (intent.lb == weightInLb) this
                else copy(weightInLb = intent.lb, weight = weightKg?.let { WeightUnits.display(it, intent.lb).toString() }.orEmpty())
            }
            is AboutYouIntent.Weight -> reduce { copy(weight = intent.value.decimalOnly(), showError = false) }
            AboutYouIntent.Back -> sendEffect(AboutYouEffect.Close)
            AboutYouIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canContinue) {
            reduce { copy(showError = true) }; return
        }
        launchSafe {
            val existing = users.current()
            val base = existing ?: UserProfile(name = s.name, age = 0, gender = s.gender, heightCm = 0, weightKg = 0.0)
            users.save(
                base.copy(
                    name = s.name.trim(), age = s.age.toInt(), gender = s.gender,
                    heightCm = s.heightCm ?: return@launchSafe, weightKg = s.weightKg ?: return@launchSafe,
                ),
            )
            preferences.setHeightInFeet(s.heightInFeet)
            preferences.setWeightInLb(s.weightInLb)
            sendEffect(if (isEdit) AboutYouEffect.Close else AboutYouEffect.Next)
        }
    }
}

internal fun String.digitsOnly(max: Int) = filter(Char::isDigit).take(max)

internal fun String.decimalOnly(): String {
    val cleaned = replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot == -1) cleaned.take(3) else cleaned.substring(0, dot).take(3) + "." + cleaned.substring(dot + 1).replace(".", "").take(1)
}
