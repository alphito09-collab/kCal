package com.alphito.kcal.feature.healthlog.presentation.mvi

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.DairyLog
import com.alphito.kcal.core.model.DairyReaction
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IbsLog
import com.alphito.kcal.core.model.IbsSymptom
import com.alphito.kcal.core.model.Severity
import com.alphito.kcal.core.model.SugarContext
import com.alphito.kcal.core.model.SugarReading
import com.alphito.kcal.feature.healthlog.domain.HealthLogRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Immutable
data class SugarUi(
    val input: String = "",
    val context: SugarContext = SugarContext.Fasting,
    val week: ImmutableList<SugarReading?> = persistentListOf(),
    val selected: Int = 6,
) {
    private val logged get() = week.filterNotNull()
    val loggedCount: Int get() = logged.size
    val inRangeCount: Int get() = logged.count { it.inNormalRange }
    val average: Int? get() = logged.takeIf { it.isNotEmpty() }?.map { it.mgDl }?.average()?.roundToInt()
}

@Immutable
data class DairyUi(
    val hadDairy: Boolean = true,
    val reactions: ImmutableSet<DairyReaction> = persistentSetOf(),
    val history: ImmutableList<DairyLog> = persistentListOf(),
    val daysSinceReaction: Int? = null,
    val reactionsLast30Days: Int = 0,
)

@Immutable
data class IbsUi(
    val symptoms: ImmutableSet<IbsSymptom> = persistentSetOf(),
    val severity: Severity = Severity.Mild,
    val week: ImmutableList<IbsLog?> = persistentListOf(),
    val selected: Int = 6,
    /** Foods eaten only on symptom days this week – candidates to try swapping. */
    val suspectFoods: ImmutableList<Food> = persistentListOf(),
) {
    val symptomFreeDays: Int get() = week.count { it != null && it.symptoms.isEmpty() }
    val loggedDays: Int get() = week.count { it != null }
}

data class HealthLogState(
    val tabs: ImmutableList<HealthCondition> = persistentListOf(),
    val active: HealthCondition = HealthCondition.Diabetes,
    val days: ImmutableList<LocalDate> = persistentListOf(),
    /** Which day of the last 7 the form logs (6 = today). */
    val logIndex: Int = 6,
    val sugar: SugarUi = SugarUi(),
    val dairy: DairyUi = DairyUi(),
    val ibs: IbsUi = IbsUi(),
    val justSaved: Boolean = false,
    val invalidInput: Boolean = false,
) : UiState {
    val logDay: LocalDate? get() = days.getOrNull(logIndex)
}

sealed interface HealthLogIntent : UiIntent {
    data class SelectTab(val condition: HealthCondition) : HealthLogIntent
    data class SelectLogDay(val index: Int) : HealthLogIntent
    data class SugarInput(val value: String) : HealthLogIntent
    data class SugarContextSelected(val context: SugarContext) : HealthLogIntent
    data class SelectSugarDay(val index: Int) : HealthLogIntent
    data class HadDairy(val value: Boolean) : HealthLogIntent
    data class ToggleReaction(val reaction: DairyReaction) : HealthLogIntent
    data class ToggleSymptom(val symptom: IbsSymptom) : HealthLogIntent
    data class SelectSeverity(val severity: Severity) : HealthLogIntent
    data class SelectIbsDay(val index: Int) : HealthLogIntent
    data object Save : HealthLogIntent
    data object Back : HealthLogIntent
}

sealed interface HealthLogEffect : UiEffect { data object Close : HealthLogEffect }

class HealthLogViewModel(
    private val repository: HealthLogRepository,
    users: UserRepository,
    private val mealPlans: MealPlanRepository,
    clock: AppClock,
) : MviViewModel<HealthLogState, HealthLogIntent, HealthLogEffect>(HealthLogState()) {

    private val today = clock.today()
    private val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
    private var dairyByDay: Map<LocalDate, DairyLog> = emptyMap()

    init {
        reduce { copy(days = week.toImmutableList()) }
        launchSafe {
            val conditions = users.current()?.conditions.orEmpty()
            // One condition → just that log. None or several → tabs for each.
            val tabs = (conditions.takeIf { it.isNotEmpty() } ?: HealthCondition.entries).sortedBy { it.ordinal }
            reduce { copy(tabs = tabs.toImmutableList(), active = tabs.first()) }
        }
        repository.sugar(week.first(), today).onEach { readings ->
            val byDay = readings.associateBy { it.date }
            reduce { copy(sugar = sugar.copy(week = week.map { byDay[it] }.toImmutableList())) }
            loadForm(onlyIfEmpty = true)
        }.catch { onError(it) }.launchIn(viewModelScope)
        repository.dairy(limit = 30).onEach { logs ->
            dairyByDay = logs.associateBy { it.date }
            val lastReaction = logs.firstOrNull { it.reactions.isNotEmpty() }?.date
            reduce {
                copy(dairy = dairy.copy(
                    history = logs.take(4).toImmutableList(),
                    daysSinceReaction = lastReaction?.let { ChronoUnit.DAYS.between(it, today).toInt() },
                    reactionsLast30Days = logs.count { it.reactions.isNotEmpty() && !it.date.isBefore(today.minusDays(30)) },
                ))
            }
            loadForm(onlyIfEmpty = true)
        }.catch { onError(it) }.launchIn(viewModelScope)
        repository.ibs(week.first(), today).onEach { logs ->
            val byDay = logs.associateBy { it.date }
            reduce { copy(ibs = ibs.copy(week = week.map { byDay[it] }.toImmutableList())) }
            loadForm(onlyIfEmpty = true)
            findSuspectFoods(logs)
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: HealthLogIntent) {
        when (intent) {
            is HealthLogIntent.SelectTab -> reduce { copy(active = intent.condition, justSaved = false, invalidInput = false) }
            is HealthLogIntent.SelectLogDay -> {
                reduce { copy(logIndex = intent.index.coerceIn(0, 6), justSaved = false, invalidInput = false) }
                loadForm(onlyIfEmpty = false)
            }
            is HealthLogIntent.SugarInput -> reduce {
                copy(sugar = sugar.copy(input = intent.value.filter(Char::isDigit).take(3)), justSaved = false, invalidInput = false)
            }
            is HealthLogIntent.SugarContextSelected -> reduce { copy(sugar = sugar.copy(context = intent.context), justSaved = false) }
            is HealthLogIntent.SelectSugarDay -> reduce { copy(sugar = sugar.copy(selected = intent.index)) }
            is HealthLogIntent.HadDairy -> reduce {
                copy(dairy = dairy.copy(hadDairy = intent.value, reactions = if (intent.value) dairy.reactions else persistentSetOf()), justSaved = false)
            }
            is HealthLogIntent.ToggleReaction -> reduce {
                val set = dairy.reactions.toPersistentSet()
                copy(dairy = dairy.copy(reactions = if (intent.reaction in set) set.remove(intent.reaction) else set.add(intent.reaction)), justSaved = false)
            }
            is HealthLogIntent.ToggleSymptom -> reduce {
                val set = ibs.symptoms.toPersistentSet()
                copy(ibs = ibs.copy(symptoms = if (intent.symptom in set) set.remove(intent.symptom) else set.add(intent.symptom)), justSaved = false)
            }
            is HealthLogIntent.SelectSeverity -> reduce { copy(ibs = ibs.copy(severity = intent.severity), justSaved = false) }
            is HealthLogIntent.SelectIbsDay -> reduce { copy(ibs = ibs.copy(selected = intent.index)) }
            HealthLogIntent.Save -> save()
            HealthLogIntent.Back -> sendEffect(HealthLogEffect.Close)
        }
    }

    /** Fills the form with what is already logged for the selected day (so editing never loses data). */
    private fun loadForm(onlyIfEmpty: Boolean) = reduce {
        val i = logIndex
        val day = days.getOrNull(i) ?: return@reduce this
        val reading = sugar.week.getOrNull(i)
        val ibsLog = ibs.week.getOrNull(i)
        val dairyLog = dairyByDay[day]
        copy(
            sugar = if (onlyIfEmpty && sugar.input.isNotEmpty()) sugar
            else sugar.copy(input = reading?.mgDl?.toString().orEmpty(), context = reading?.context ?: sugar.context),
            ibs = if (onlyIfEmpty && ibs.symptoms.isNotEmpty()) ibs
            else ibs.copy(symptoms = ibsLog?.symptoms?.toPersistentSet() ?: persistentSetOf(), severity = ibsLog?.severity ?: Severity.Mild),
            dairy = if (onlyIfEmpty && dairy.reactions.isNotEmpty()) dairy
            else dairy.copy(hadDairy = dairyLog?.hadDairy ?: true, reactions = dairyLog?.reactions?.toPersistentSet() ?: persistentSetOf()),
        )
    }

    private fun findSuspectFoods(logs: List<IbsLog>) = launchSafe {
        val symptomDays = logs.filter { it.symptoms.isNotEmpty() }.map { it.date }
        val clearDays = logs.filter { it.symptoms.isEmpty() }.map { it.date }
        if (symptomDays.isEmpty()) {
            reduce { copy(ibs = ibs.copy(suspectFoods = persistentListOf())) }
            return@launchSafe
        }
        val onClearDays = mealPlans.foodsEatenOn(clearDays).map { it.id }.toSet()
        val suspects = mealPlans.foodsEatenOn(symptomDays).filter { it.id !in onClearDays }.take(MAX_SUSPECTS)
        reduce { copy(ibs = ibs.copy(suspectFoods = suspects.toImmutableList())) }
    }

    private fun save() = launchSafe {
        val s = currentState
        val day = s.logDay ?: return@launchSafe
        when (s.active) {
            HealthCondition.Diabetes -> {
                val mg = s.sugar.input.toIntOrNull()?.takeIf { it in 20..600 }
                if (mg == null) {
                    reduce { copy(invalidInput = true) }
                    return@launchSafe
                }
                repository.saveSugar(SugarReading(day, mg, s.sugar.context))
            }
            HealthCondition.LactoseIntolerant -> repository.saveDairy(DairyLog(day, s.dairy.hadDairy, s.dairy.reactions, ""))
            HealthCondition.Ibs -> repository.saveIbs(IbsLog(day, s.ibs.symptoms, s.ibs.severity))
        }
        reduce { copy(justSaved = true) }
        delay(1_600)
        reduce { copy(justSaved = false) }
    }

    private companion object { const val MAX_SUSPECTS = 4 }
}
