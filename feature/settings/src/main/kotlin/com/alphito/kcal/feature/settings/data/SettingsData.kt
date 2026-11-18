package com.alphito.kcal.feature.settings.data

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.WeightRepository
import com.alphito.kcal.feature.settings.domain.DataExporter
import com.alphito.kcal.feature.settings.domain.PasswordResult
import com.alphito.kcal.feature.settings.domain.PasswordService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class LocalOnlyPasswordService : PasswordService {
    override suspend fun change(newPassword: String): PasswordResult = PasswordResult.Unavailable
}

internal class JsonDataExporter(
    private val users: UserRepository,
    private val weights: WeightRepository,
    private val steps: StepsRepository,
    private val mealPlans: MealPlanRepository,
    private val preferences: PreferencesRepository,
    private val clock: AppClock,
    private val dispatchers: DispatcherProvider,
) : DataExporter {
    override suspend fun exportJson(): String = withContext(dispatchers.default) {
        val today = clock.today()
        val profile = users.current()
        val root = JSONObject()
        root.put("exportedOn", today.toString())
        profile?.let {
            root.put("profile", JSONObject().apply {
                put("name", it.name); put("age", it.age); put("gender", it.gender.name)
                put("heightCm", it.heightCm); put("weightKg", it.weightKg); put("goal", it.goal.name)
                put("region", it.region); put("budget", it.budget.name)
                put("conditions", JSONArray(it.conditions.map { c -> c.name }))
            })
        }
        root.put("weights", JSONArray(weights.entries.first().map { JSONObject().put("date", it.date.toString()).put("kg", it.weightKg) }))
        root.put("steps", JSONArray(steps.observeRange(today.minusMonths(6), today).first().map { JSONObject().put("date", it.date.toString()).put("steps", it.steps) }))
        root.put("mealPlan", JSONArray(mealPlans.observeDays(today.minusDays(6), today.plusDays(6)).first().map { day ->
            JSONObject().put("date", day.date.toString()).put("meals", JSONArray(day.slots.filter { it.items.isNotEmpty() }.map { slot ->
                JSONObject().put("type", slot.mealType.name).put("checked", slot.checked).put("items", JSONArray(slot.items.map { it.food.name }))
            }))
        }))
        root.put("language", preferences.snapshot().language.tag)
        root.toString(2)
    }
}
