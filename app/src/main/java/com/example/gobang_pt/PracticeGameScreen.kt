package com.example.gobang_pt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gobang_pt.ui.theme.GobangptTheme

@Composable
internal fun PracticeGameScreen(
    scenario: PracticeScenario,
    game: GobangGame,
    analysis: PracticeAnalysis,
    isAiThinking: Boolean,
    showHint: Boolean,
    recommendation: PracticeRecommendation?,
    onPositionClick: (Position) -> Unit,
    onToggleHint: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onNextScenario: (() -> Unit)?,
    onBackToLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completed = game.winner == Stone.BLACK
    val interactive = !game.isOver && !isAiThinking && game.currentPlayer == Stone.BLACK
    val highlights = if (showHint && recommendation != null && !game.isOver) {
        setOf(recommendation.move)
    } else {
        emptySet()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp && maxWidth > maxHeight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    GobangBoard(
                        game = game,
                        onPositionClick = onPositionClick,
                        isInteractive = interactive,
                        highlightedPositions = highlights,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        boardDescriptionResource = R.string.practice_board_description,
                        modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PracticeHeader(scenario, onBackToLayout)
                    PracticeStatus(game, isAiThinking)
                    PracticeCoachSummary(game, analysis, showHint, recommendation)
                    PracticeGoalList(scenario, analysis)
                    PracticeActions(
                        game = game,
                        isAiThinking = isAiThinking,
                        showHint = showHint,
                        onToggleHint = onToggleHint,
                        onUndo = onUndo,
                        onRestart = onRestart,
                        onNextScenario = onNextScenario,
                    )
                    MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PracticeHeader(scenario, onBackToLayout)
                    PracticeStatus(game, isAiThinking)
                    PracticeCoachSummary(game, analysis, showHint, recommendation)
                    GobangBoard(
                        game = game,
                        onPositionClick = onPositionClick,
                        isInteractive = interactive,
                        highlightedPositions = highlights,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        boardDescriptionResource = R.string.practice_board_description,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    PracticeGoalList(scenario, analysis)
                    PracticeActions(
                        game = game,
                        isAiThinking = isAiThinking,
                        showHint = showHint,
                        onToggleHint = onToggleHint,
                        onUndo = onUndo,
                        onRestart = onRestart,
                        onNextScenario = onNextScenario,
                    )
                    MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PracticeHeader(
    scenario: PracticeScenario,
    onBackToLayout: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            TextButton(
                onClick = onBackToLayout,
                modifier = Modifier.testTag("practice_back_to_layout_button"),
            ) {
                Text(stringResource(R.string.practice_back_to_lesson))
            }
        }
        Text(
            text = scenario.layoutLesson.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("practice_title"),
        )
        Text(
            text = stringResource(R.string.practice_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PracticeStatus(
    game: GobangGame,
    isAiThinking: Boolean,
) {
    Text(
        text = when {
            game.winner == Stone.BLACK -> stringResource(R.string.practice_victory)
            game.winner == Stone.WHITE -> stringResource(R.string.practice_defeat)
            game.isDraw -> stringResource(R.string.practice_draw)
            isAiThinking -> stringResource(R.string.computer_thinking)
            game.currentPlayer == Stone.BLACK -> stringResource(R.string.player_turn)
            else -> stringResource(R.string.computer_turn)
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (game.winner == Stone.BLACK) {
            Color(0xFF2E7D32)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.padding(top = 12.dp).testTag("practice_status"),
    )
}

@Composable
private fun PracticeCoachSummary(
    game: GobangGame,
    analysis: PracticeAnalysis,
    showHint: Boolean,
    recommendation: PracticeRecommendation?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("practice_coach"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (analysis.currentGoal == null) {
                    stringResource(R.string.practice_all_goals)
                } else {
                    stringResource(R.string.practice_current_goal, analysis.currentGoal.label)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("practice_current_goal"),
            )
            analysis.currentGoal?.let { goal ->
                Text(
                    text = goal.guidance,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (!game.isOver && analysis.latestAchievedGoal != null) {
                Text(
                    text = stringResource(
                        R.string.practice_formed_goal,
                        analysis.latestAchievedGoal.label,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(top = 8.dp).testTag("practice_latest_goal"),
                )
            }
            if (showHint && recommendation != null && !game.isOver) {
                Text(
                    text = recommendation.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp).testTag("practice_hint_text"),
                )
            } else if (!game.isOver) {
                Text(
                    text = stringResource(R.string.practice_free_move_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PracticeGoalList(
    scenario: PracticeScenario,
    analysis: PracticeAnalysis,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("practice_goal_list"),
    ) {
        Text(
            text = stringResource(R.string.practice_goal_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        scenario.goals.forEach { goal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = goal.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (goal == analysis.currentGoal) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = stringResource(
                        when {
                            goal in analysis.achievedGoals -> R.string.practice_goal_done
                            goal == analysis.currentGoal -> R.string.practice_goal_current
                            else -> R.string.practice_goal_pending
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (goal in analysis.achievedGoals) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.testTag("practice_goal_${goal.name}"),
                )
            }
        }
    }
}

@Composable
private fun PracticeActions(
    game: GobangGame,
    isAiThinking: Boolean,
    showHint: Boolean,
    onToggleHint: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onNextScenario: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onToggleHint,
                enabled = !game.isOver && !isAiThinking && game.currentPlayer == Stone.BLACK,
                modifier = Modifier.testTag("practice_hint_button"),
            ) {
                Text(
                    stringResource(
                        if (showHint) R.string.practice_hide_hint else R.string.practice_show_hint,
                    ),
                )
            }
            OutlinedButton(
                onClick = onUndo,
                enabled = game.moves.isNotEmpty() && !isAiThinking,
                modifier = Modifier.testTag("practice_undo_button"),
            ) {
                Text(stringResource(R.string.practice_undo))
            }
        }
        OutlinedButton(
            onClick = onRestart,
            enabled = !isAiThinking,
            modifier = Modifier.padding(top = 8.dp).testTag("practice_restart_button"),
        ) {
            Text(stringResource(R.string.practice_restart))
        }
        if (game.winner == Stone.BLACK && onNextScenario != null) {
            Button(
                onClick = onNextScenario,
                modifier = Modifier.padding(top = 8.dp).testTag("practice_next_button"),
            ) {
                Text(stringResource(R.string.practice_next))
            }
        }
    }
}

@Preview(name = "Practice game", showBackground = true)
@Composable
private fun PracticeGamePreview() {
    GobangptTheme {
        val scenario = practiceScenarios.first()
        val game = scenario.newGame().play(patternPoint("h8"))
        PracticeGameScreen(
            scenario = scenario,
            game = game,
            analysis = PracticeCoach.analyze(scenario, game),
            isAiThinking = false,
            showHint = true,
            recommendation = PracticeCoach.recommend(scenario, game),
            onPositionClick = {},
            onToggleHint = {},
            onUndo = {},
            onRestart = {},
            onNextScenario = {},
            onBackToLayout = {},
        )
    }
}
