package com.alphito.kcal.feature.healthlog.presentation.mvi

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.DairyLog
import com.alphito.kcal.core.model.DairyReaction
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IbsLog
import com.alphito.kcal.core.model.IbsSymptom
import com.alphito.kcal.core.model.Severity
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

@Immutable
data class SugarUi(
    val input: String = "",
    val week: ImmutableList<SugarReading?> = persistentListOf(),
    val selected: Int = 6,
)

@Immutable
data class DairyUi(
    val hadDairy: Boolean = true,
    val reactions: ImmutableSet<DairyReaction> = persistentSetOf(),
    val history: ImmutableList<DairyLog> = persistentListOf(),
    val daysSinceReaction: Int? = null,
)

@Immutable
data class IbsUi(
    val symptoms: ImmutableSet<IbsSymptom> = persistentSetOf(),
    val severity: Severity = Severity.Mild,
    val week: ImmutableList<IbsLog?> = persistentListOf(),
    val selected: Int = 6,
)

data class HealthLogState(
    val tabs: ImmutableList<HealthCondition> = persistentListOf(),
    val active: HealthCondition = HealthCondition.Diabetes,
    val days: ImmutableList<LocalDate> = persistentListOf(),
    val sugar: SugarUi = SugarUi(),
    val dairy: DairyUi = DairyUi(),
    val ibs: IbsUi = IbsUi(),
    val justSaved: Boolean = false,
) : UiState

sealed interface HealthLogIntent : UiIntent {
    data class SelectTab(val condition: HealthCondition) : HealthLogIntent
    data class SugarInput(val value: String) : HealthLogIntent
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
    private val clock: AppClock,
) : MviViewModel<HealthLogState, HealthLogIntent, HealthLogEffect>(HealthLogState()) {

    private val today = clock.today()
    private val week = (6 downTo 0).map { today.minusDays(it.toLong()) }

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
            reduce {
                copy(sugar = sugar.copy(
                    week = week.map { byDay[it] }.toImmutableList(),
                    input = sugar.input.ifEmpty { byDay[today]?.mgDl?.toString().orEmpty() },
                ))
            }
        }.catch { onError(it) }.launchIn(viewModelScope)
        repository.dairy(limit = 30).onEach { logs ->
            val lastReaction = logs.firstOrNull { it.reactions.isNotEmpty() }?.date
            reduce {
                copy(dairy = dairy.copy(
                    history = logs.take(4).toImmutableList(),
                    daysSinceReaction = lastReaction?.let { ChronoUnit.DAYS.between(it, today).toInt() },
                ))
            }
        }.catch { onError(it) }.launchIn(viewModelScope)
        repository.ibs(week.first(), today).onEach { logs ->
            val byDay = logs.associateBy { it.date }
            reduce { copy(ibs = ibs.copy(week = week.map { byDay[it] }.toImmutableList())) }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: HealthLogIntent) {
        when (intent) {
            is HealthLogIntent.SelectTab -> reduce { copy(active = intent.condition, justSaved = false) }
            is HealthLogIntent.SugarInput -> reduce { copy(sugar = sugar.copy(input = intent.value.filter(Char::isDigit).take(3)), justSaved = false) }
            is HealthLogIntent.SelectSugarDay -> reduce { copy(sugar = sugar.copy(selected = intent.index)) }
            is HealthLogIntent.HadDairy -> reduce { copy(dairy = dairy.copy(hadDairy = intent.value, reactions = if (intent.value) dairy.reactions else persistentSetOf())) }
            is HealthLogIntent.ToggleReaction -> reduce {
                val set = dairy.reactions.toPersistentSet()
                copy(dairy = dairy.copy(reactions = if (intent.reaction in set) set.remove(intent.reaction) else set.add(intent.reaction)))
            }
            is HealthLogIntent.ToggleSymptom -> reduce {
                val set = ibs.symptoms.toPersistentSet()
                copy(ibs = ibs.copy(symptoms = if (intent.symptom in set) set.remove(intent.symptom) else set.add(intent.symptom)))
            }
            is HealthLogIntent.SelectSeverity -> reduce { copy(ibs = ibs.copy(severity = intent.severity)) }
            is HealthLogIntent.SelectIbsDay -> reduce { copy(ibs = ibs.copy(selected = intent.index)) }
            HealthLogIntent.Save -> save()
            HealthLogIntent.Back -> sendEffect(HealthLogEffect.Close)
        }
    }

    private fun save() = launchSafe {
        val s = currentState
        when (s.active) {
            HealthCondition.Diabetes -> {
                val mg = s.sugar.input.toIntOrNull()?.takeIf { it in 20..600 } ?: return@launchSafe
                repository.saveSugar(SugarReading(today, mg))
            }
            HealthCondition.LactoseIntolerant -> repository.saveDairy(DairyLog(today, s.dairy.hadDairy, s.dairy.reactions, ""))
            HealthCondition.Ibs -> repository.saveIbs(IbsLog(today, s.ibs.symptoms, s.ibs.severity))
        }
        reduce { copy(justSaved = true) }
        delay(1_600)
        reduce { copy(justSaved = false) }
    }
}
