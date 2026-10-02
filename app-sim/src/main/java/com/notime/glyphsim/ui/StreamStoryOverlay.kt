package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.matrix.PlayGoals
import com.notime.glyphsim.matrix.PlayQuests
import com.notime.glyphsim.matrix.ReminderAnimations
import com.notime.glyphsim.matrix.SimulatedMatrixView

/** Quiet, symbolic continuity: every dot is derived from a persisted story fact. */
@Composable
internal fun StreamStoryOverlay(
    goals: PlayGoals.Progress?, quest: PlayQuests.Progress?, agent: AgentState?, day: Long,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        goals?.let { progress ->
            val pattern = when (PlayGoals.currentProject(progress)) {
                PlayGoals.Project.BIRDHOUSE -> listOf("    #    ", "   ###   ", "  #####  ", " ####### ", "  #####  ", "  ## ##  ", "  #####  ", "    #    ", "    #    ")
                PlayGoals.Project.HERBS -> listOf("    #    ", "  # # #  ", "   ###   ", "    #    ", "  #####  ", "  #####  ", "   ###   ")
                PlayGoals.Project.KITE -> listOf("    #    ", "   ###   ", "  #####  ", "   ###   ", "    #    ", "     #   ", "    #    ", "     #   ")
            }
            val frame = IntArray(13 * 13)
            pattern.forEachIndexed { y, line -> line.forEachIndexed { x, point ->
                if (point == '#') frame[(y + 2) * 13 + x + 2] = 4095
            } }
            val total = PlayGoals.sessionCount(PlayGoals.currentProject(progress))
            val title = stringResource(when (PlayGoals.currentProject(progress)) {
                PlayGoals.Project.BIRDHOUSE -> R.string.stream_story_birdhouse
                PlayGoals.Project.HERBS -> R.string.stream_story_herbs
                PlayGoals.Project.KITE -> R.string.stream_story_kite
            })
            StoryToken(frame, AnimationType.CREATIVITY, progress.session.coerceIn(0, total), total, title)
        }
        quest?.let { progress ->
            val plan = PlayQuests.planFor(progress)
            if (plan.kind == PlayQuests.DayKind.TRAVEL) {
                val type = when (progress.quest) {
                    PlayQuests.Quest.TREASURE -> AnimationType.CREATIVITY
                    PlayQuests.Quest.MAGIC -> AnimationType.FOCUS
                    PlayQuests.Quest.EXPEDITION -> AnimationType.MOVE
                    PlayQuests.Quest.DRAGON_EGG -> AnimationType.LOVE
                }
                StoryToken(ReminderAnimations.framesFor(type).first(), type, progress.stepsDone, plan.steps.size)
            } else goals?.let { goal ->
                val type = when (PlayGoals.intentionFor(day)) {
                    PlayGoals.Intention.MUSHROOMS -> AnimationType.MOVE
                    PlayGoals.Intention.FISHING -> AnimationType.FOCUS
                    PlayGoals.Intention.SUNSET, PlayGoals.Intention.STARS -> AnimationType.MINDFULNESS
                }
                StoryToken(ReminderAnimations.framesFor(type).first(), type, if (goal.intentionDay == day) 1 else 0, 1)
            }
        }
        agent?.relationships?.values?.maxByOrNull { it.closeness }?.takeIf { it.interactions > 0 }?.let {
            StoryToken(ReminderAnimations.framesFor(AnimationType.LOVE).first(), AnimationType.LOVE,
                (it.closeness.coerceIn(0.0, 1.0) * 5).toInt(), 5)
        }
        agent?.learnedPreferences?.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let {
            val type = when (it.key) {
                GoalKind.GET_FOOD -> AnimationType.DRINK
                GoalKind.REST -> AnimationType.REST
                GoalKind.DEVELOP -> AnimationType.BOOK
                GoalKind.CONNECT_WITH -> AnimationType.LOVE
                GoalKind.EARN_MONEY -> AnimationType.WORK
                else -> AnimationType.MOVE
            }
            StoryToken(ReminderAnimations.framesFor(type).first(), type, 1, 1)
        }
    }
}

@Composable
private fun StoryToken(frame: IntArray, topic: AnimationType, done: Int, total: Int, title: String? = null) {
    Column(Modifier.width(40.dp).background(Color(0xBB111821), RoundedCornerShape(8.dp)).padding(4.dp)) {
        SimulatedMatrixView(frame = frame, showPuck = false,
            contentDescription = stringResource(R.string.stream_story_progress, title ?: stringResource(topic.labelRes), done, total),
            modifier = Modifier.size(32.dp))
        Canvas(Modifier.fillMaxWidth().height(7.dp)) {
            val count = total.coerceIn(1, 6)
            repeat(count) { i ->
                drawCircle(if (i < done) Color(0xFF7FD1A6) else Color(0xFF3D4855),
                    radius = 1.4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * (i + .5f) / count, size.height / 2))
            }
        }
    }
}
