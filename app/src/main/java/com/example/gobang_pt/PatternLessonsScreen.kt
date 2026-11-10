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
import androidx.compose.runtime.remember
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
internal fun PatternLessonList(
    onSelectLesson: (PatternLesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        patternLessons.forEach { lesson ->
            Card(
                onClick = { onSelectLesson(lesson) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("pattern_lesson_${lesson.id}"),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(lesson.level.labelResource),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = lesson.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.pattern_lesson_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PatternLessonScreen(
    lesson: PatternLesson,
    isPractice: Boolean,
    foundAnswers: List<Position>,
    showError: Boolean,
    onStartPractice: () -> Unit,
    onShowExample: () -> Unit,
    onPositionClick: (Position) -> Unit,
    onResetPractice: () -> Unit,
    onNextLesson: (() -> Unit)?,
    onBackToList: () -> Unit,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completed = foundAnswers.toSet() == lesson.exercise.answers
    val displayedPosition = remember(lesson, isPractice, foundAnswers) {
        if (isPractice) {
            lesson.exercise.previewPosition(foundAnswers)
        } else {
            lesson.example.position
        }
    }
    val displayedGame = remember(displayedPosition) { displayedPosition.toGobangGame() }
    val highlights = if (isPractice) foundAnswers.toSet() else lesson.example.highlights
    val highlightColor = if (isPractice) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary

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
                    PatternBoard(
                        game = displayedGame,
                        isPractice = isPractice,
                        completed = completed,
                        highlights = highlights,
                        highlightColor = highlightColor,
                        onPositionClick = onPositionClick,
                        modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PatternLessonHeader(lesson, onBackToGame)
                    if (isPractice) {
                        PatternExercisePrompt(lesson, foundAnswers.size)
                    }
                    PatternLessonGuidance(
                        lesson = lesson,
                        isPractice = isPractice,
                        showError = showError,
                        completed = completed,
                    )
                    PatternLessonActions(
                        isPractice = isPractice,
                        completed = completed,
                        onStartPractice = onStartPractice,
                        onShowExample = onShowExample,
                        onResetPractice = onResetPractice,
                        onNextLesson = onNextLesson,
                    )
                    PatternBackToList(onBackToList)
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
                    PatternLessonHeader(lesson, onBackToGame)
                    if (isPractice) {
                        PatternExercisePrompt(lesson, foundAnswers.size)
                    }
                    PatternBoard(
                        game = displayedGame,
                        isPractice = isPractice,
                        completed = completed,
                        highlights = highlights,
                        highlightColor = highlightColor,
                        onPositionClick = onPositionClick,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    PatternLessonGuidance(
                        lesson = lesson,
                        isPractice = isPractice,
                        showError = showError,
                        completed = completed,
                    )
                    PatternLessonActions(
                        isPractice = isPractice,
                        completed = completed,
                        onStartPractice = onStartPractice,
                        onShowExample = onShowExample,
                        onResetPractice = onResetPractice,
                        onNextLesson = onNextLesson,
                    )
                    PatternBackToList(onBackToList)
                }
            }
        }
    }
}

@Composable
private fun PatternBoard(
    game: GobangGame,
    isPractice: Boolean,
    completed: Boolean,
    highlights: Set<Position>,
    highlightColor: Color,
    onPositionClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
) {
    GobangBoard(
        game = game,
        onPositionClick = onPositionClick,
        isInteractive = isPractice && !completed,
        showLastMove = false,
        highlightedPositions = highlights,
        highlightColor = highlightColor,
        boardDescriptionResource = if (isPractice) {
            R.string.pattern_practice_board_description
        } else {
            R.string.pattern_example_board_description
        },
        modifier = modifier,
    )
}

@Composable
private fun PatternLessonHeader(
    lesson: PatternLesson,
    onBackToGame: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            TextButton(
                onClick = onBackToGame,
                modifier = Modifier.testTag("pattern_back_to_game_button"),
            ) {
                Text(stringResource(R.string.back_to_game))
            }
        }
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("pattern_lesson_title"),
        )
        Text(
            text = stringResource(lesson.level.labelResource),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = lesson.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PatternExercisePrompt(
    lesson: PatternLesson,
    foundCount: Int,
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.pattern_practice),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = lesson.exercise.task,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp).testTag("pattern_exercise_task"),
        )
        Text(
            text = stringResource(
                R.string.pattern_answer_progress,
                foundCount,
                lesson.exercise.answers.size,
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp).testTag("pattern_answer_progress"),
        )
    }
}

@Composable
private fun PatternLessonGuidance(
    lesson: PatternLesson,
    isPractice: Boolean,
    showError: Boolean,
    completed: Boolean,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        if (isPractice) {
            if (showError && !completed) {
                Text(
                    text = stringResource(R.string.pattern_answer_wrong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("pattern_wrong_answer"),
                )
            }
            if (completed) {
                Text(
                    text = stringResource(R.string.pattern_practice_complete),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("pattern_practice_complete"),
                )
                Text(
                    text = lesson.exercise.successExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.pattern_hint, lesson.exercise.hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = if (showError) 8.dp else 0.dp),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.pattern_example),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = lesson.explanation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.pattern_highlight_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.pattern_defense_tip),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = lesson.defenseTip,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PatternLessonActions(
    isPractice: Boolean,
    completed: Boolean,
    onStartPractice: () -> Unit,
    onShowExample: () -> Unit,
    onResetPractice: () -> Unit,
    onNextLesson: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isPractice) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onResetPractice,
                    modifier = Modifier.testTag("pattern_reset_button"),
                ) {
                    Text(stringResource(R.string.pattern_reset))
                }
                if (onNextLesson != null) {
                    Button(
                        onClick = onNextLesson,
                        enabled = completed,
                        modifier = Modifier.testTag("pattern_next_lesson_button"),
                    ) {
                        Text(stringResource(R.string.pattern_next_lesson))
                    }
                }
            }
            TextButton(
                onClick = onShowExample,
                modifier = Modifier.testTag("pattern_show_example_button"),
            ) {
                Text(stringResource(R.string.pattern_show_example))
            }
        } else {
            Button(
                onClick = onStartPractice,
                modifier = Modifier.testTag("pattern_start_practice_button"),
            ) {
                Text(stringResource(R.string.pattern_start_practice))
            }
        }
    }
}

@Composable
private fun PatternBackToList(onBackToList: () -> Unit) {
    TextButton(
        onClick = onBackToList,
        modifier = Modifier.testTag("pattern_back_to_list_button"),
    ) {
        Text(stringResource(R.string.back_to_pattern_lessons))
    }
}

private val PatternLevel.labelResource: Int
    get() = when (this) {
        PatternLevel.BASIC -> R.string.pattern_level_basic
        PatternLevel.COMBINATION -> R.string.pattern_level_combination
    }

@Preview(name = "Pattern lessons", showBackground = true)
@Composable
private fun PatternLessonListPreview() {
    GobangptTheme {
        PatternLessonList(onSelectLesson = {})
    }
}

@Preview(name = "Pattern example", showBackground = true)
@Composable
private fun PatternLessonPreview() {
    GobangptTheme {
        PatternLessonScreen(
            lesson = patternLessons.first(),
            isPractice = false,
            foundAnswers = emptyList(),
            showError = false,
            onStartPractice = {},
            onShowExample = {},
            onPositionClick = {},
            onResetPractice = {},
            onNextLesson = {},
            onBackToList = {},
            onBackToGame = {},
        )
    }
}
