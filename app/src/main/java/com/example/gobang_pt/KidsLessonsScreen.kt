package com.example.gobang_pt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gobang_pt.ui.theme.GobangptTheme

private sealed interface KidsFeedback {
    data class Correct(val step: KidsStep, val stepIndex: Int) : KidsFeedback
    data class Wrong(val message: String) : KidsFeedback
}

@Composable
internal fun KidsLessonsScreen(
    lessons: List<KidsLesson>,
    selectedLessonId: String?,
    progress: KidsProgressStore,
    onSelectLesson: (KidsLesson) -> Unit,
    onBackToGame: () -> Unit,
    onBackToList: () -> Unit,
    onNextLesson: (KidsLesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    var completedIds by remember { mutableStateOf(progress.completedLessonIds()) }
    val selectedLesson = lessons.firstOrNull { it.id == selectedLessonId }
    val allLessonsCompleted = lessons.isNotEmpty() && lessons.all { it.id in completedIds }

    Box(modifier = modifier.fillMaxSize().testTag("kids_lessons_screen")) {
        if (selectedLesson == null) {
            KidsLessonHome(
                lessons = lessons,
                completedIds = completedIds,
                onSelectLesson = onSelectLesson,
                onBackToGame = onBackToGame,
            )
        } else {
            val nextLesson = lessons.getOrNull(lessons.indexOf(selectedLesson) + 1)
            key(selectedLesson.id) {
                KidsLessonPlayer(
                    lesson = selectedLesson,
                    nextLesson = nextLesson,
                    allLessonsCompleted = allLessonsCompleted,
                    onCompleted = { lessonId ->
                        if (lessonId !in completedIds) {
                            progress.markCompleted(lessonId)
                            completedIds = completedIds + lessonId
                        }
                    },
                    onBackToList = onBackToList,
                    onNextLesson = onNextLesson,
                )
            }
        }
    }
}

@Composable
private fun KidsLessonHome(
    lessons: List<KidsLesson>,
    completedIds: Set<String>,
    onSelectLesson: (KidsLesson) -> Unit,
    onBackToGame: () -> Unit,
) {
    val completedCount = lessons.count { it.id in completedIds }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                TextButton(
                    onClick = onBackToGame,
                    modifier = Modifier.testTag("kids_back_to_game_button"),
                ) {
                    Text(stringResource(R.string.kids_back_to_game))
                }
            }
            Text(
                text = stringResource(R.string.kids_learning_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("kids_lessons_title"),
            )
            Text(
                text = stringResource(R.string.kids_learning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.kids_progress, completedCount, lessons.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp).testTag("kids_progress"),
            )
            Text(
                text = stringResource(R.string.kids_how_to_play),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            ThinkingCard(Modifier.padding(top = 16.dp))

            KidsStage.entries.forEach { stage ->
                val stageLessons = lessons.filter { it.stage == stage }.sortedBy { it.order }
                if (stageLessons.isNotEmpty()) {
                    Text(
                        text = stringResource(stage.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp),
                    )
                    stageLessons.forEach { lesson ->
                        KidsLessonCard(
                            lesson = lesson,
                            completed = lesson.id in completedIds,
                            onClick = { onSelectLesson(lesson) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ThinkingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.kids_thinking_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.kids_thinking_one),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.kids_thinking_two),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = stringResource(R.string.kids_thinking_three),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun KidsLessonCard(
    lesson: KidsLesson,
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("kids_lesson_${lesson.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.kids_lesson_index, lesson.order),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.kids_lesson_pattern, lesson.patternName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = lesson.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(
                    if (completed) R.string.kids_lesson_completed else R.string.kids_lesson_start,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun KidsLessonPlayer(
    lesson: KidsLesson,
    nextLesson: KidsLesson?,
    allLessonsCompleted: Boolean,
    onCompleted: (String) -> Unit,
    onBackToList: () -> Unit,
    onNextLesson: (KidsLesson) -> Unit,
) {
    val sessionSaver = remember(lesson.id) {
        Saver<KidsLessonSession, String>(
            save = { session -> session.encodeMoves() },
            restore = { encoded -> KidsLessonSession.restore(lesson, encoded) },
        )
    }
    var session by rememberSaveable(lesson.id, stateSaver = sessionSaver) {
        mutableStateOf(KidsLessonSession(lesson))
    }
    var feedback by remember(lesson.id) { mutableStateOf<KidsFeedback?>(null) }
    var hintVisible by remember(lesson.id) { mutableStateOf(false) }
    var wrongAttempts by remember(lesson.id) { mutableStateOf(0) }

    LaunchedEffect(session.isCompleted, lesson.id) {
        if (session.isCompleted) onCompleted(lesson.id)
    }

    val displayGame = remember(session) { session.toGobangGame() }
    val currentPosition = remember(session) { session.position }
    val occupiedMessage = stringResource(R.string.kids_occupied_message)
    val waitingForContinue = feedback is KidsFeedback.Correct
    val activeStep = (feedback as? KidsFeedback.Correct)?.step ?: session.currentStep ?: lesson.steps.last()
    val displayedStepNumber = (feedback as? KidsFeedback.Correct)?.stepIndex ?: session.stepIndex
    val hintPositions = if (hintVisible && session.currentStep != null && !waitingForContinue) {
        session.currentStep!!.answers
    } else {
        emptySet()
    }

    val onPositionClick: (Position) -> Unit = { position ->
        if (!session.isCompleted && !waitingForContinue) {
            if (currentPosition.stoneAt(position) != null) {
                feedback = KidsFeedback.Wrong(occupiedMessage)
            } else {
                when (val result = session.submit(position)) {
                    is KidsSubmitResult.Correct -> {
                        session = result.session
                        feedback = KidsFeedback.Correct(result.step, result.stepIndex)
                        hintVisible = false
                        wrongAttempts = 0
                    }
                    is KidsSubmitResult.Wrong -> {
                        wrongAttempts += 1
                        feedback = KidsFeedback.Wrong(result.step.wrongFeedback)
                        if (wrongAttempts >= 2) hintVisible = true
                    }
                    KidsSubmitResult.Completed -> Unit
                }
            }
        }
    }

    val onReplay = {
        session = KidsLessonSession(lesson)
        feedback = null
        hintVisible = false
        wrongAttempts = 0
    }
    val onContinue = { feedback = null }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp && maxWidth > maxHeight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        KidsLessonBoard(
                            game = displayGame,
                            isInteractive = !session.isCompleted && !waitingForContinue,
                            showLastMove = session.lastWhiteReply != null,
                            highlightedPositions = setOfNotNull(session.lastChildMove),
                            hintPositions = hintPositions,
                            completed = session.isCompleted,
                            onPositionClick = onPositionClick,
                            modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                        )
                    }
                    if (session.moves.isNotEmpty()) {
                        KidsBoardLegend(hintVisible, Modifier.padding(top = 8.dp))
                    }
                }
                Column(
                    modifier = Modifier.width(360.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KidsPlayerHeader(
                        lesson = lesson,
                        stepNumber = displayedStepNumber,
                        completed = session.isCompleted,
                        target = activeStep.target,
                        onBackToList = onBackToList,
                    )
                    KidsPromptCard(
                        lesson = lesson,
                        step = activeStep,
                        completed = session.isCompleted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (session.isCompleted) {
                        KidsCompletionCard(
                            lesson = lesson,
                            nextLesson = nextLesson,
                            allLessonsCompleted = allLessonsCompleted,
                            onReplay = onReplay,
                            onNextLesson = onNextLesson,
                            onBackToList = onBackToList,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    } else {
                        KidsFeedbackCard(
                            session = session,
                            feedback = feedback,
                            hintVisible = hintVisible,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        KidsActions(
                            feedback = feedback,
                            onHint = { hintVisible = true },
                            onContinue = onContinue,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    KidsThinkingCard(lesson, Modifier.padding(top = 12.dp, bottom = 12.dp))
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KidsPlayerHeader(
                        lesson = lesson,
                        stepNumber = displayedStepNumber,
                        completed = session.isCompleted,
                        target = activeStep.target,
                        onBackToList = onBackToList,
                    )
                    KidsPromptCard(
                        lesson = lesson,
                        step = activeStep,
                        completed = session.isCompleted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    KidsLessonBoard(
                        game = displayGame,
                        isInteractive = !session.isCompleted && !waitingForContinue,
                        showLastMove = session.lastWhiteReply != null,
                        highlightedPositions = setOfNotNull(session.lastChildMove),
                        hintPositions = hintPositions,
                        completed = session.isCompleted,
                        onPositionClick = onPositionClick,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (session.moves.isNotEmpty()) {
                        KidsBoardLegend(hintVisible, Modifier.padding(top = 8.dp))
                    }
                    if (session.isCompleted) {
                        KidsCompletionCard(
                            lesson = lesson,
                            nextLesson = nextLesson,
                            allLessonsCompleted = allLessonsCompleted,
                            onReplay = onReplay,
                            onNextLesson = onNextLesson,
                            onBackToList = onBackToList,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    } else {
                        KidsFeedbackCard(
                            session = session,
                            feedback = feedback,
                            hintVisible = hintVisible,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        KidsActions(
                            feedback = feedback,
                            onHint = { hintVisible = true },
                            onContinue = onContinue,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    KidsThinkingCard(lesson, Modifier.padding(top = 12.dp, bottom = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun KidsPlayerHeader(
    lesson: KidsLesson,
    stepNumber: Int,
    completed: Boolean,
    target: KidsTarget,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            TextButton(
                onClick = onBackToList,
                modifier = Modifier.testTag("kids_lesson_back_to_list_button"),
            ) {
                Text(stringResource(R.string.kids_back_to_list))
            }
        }
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("kids_lesson_title"),
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.kids_lesson_pattern, lesson.patternName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            TargetChip(target)
        }
        if (!completed) {
            Text(
                text = stringResource(R.string.kids_lesson_step_progress, stepNumber + 1, lesson.steps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).testTag("kids_lesson_step_progress"),
            )
        }
    }
}

@Composable
private fun TargetChip(target: KidsTarget, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.kids_lesson_target, stringResource(target.labelRes)),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun KidsPromptCard(
    lesson: KidsLesson,
    step: KidsStep,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth().testTag("kids_lesson_prompt_card")) {
        Column(Modifier.padding(16.dp)) {
            if (!completed) {
                Text(
                    text = step.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("kids_lesson_prompt"),
                )
            }
            Text(
                text = stringResource(R.string.kids_lesson_rule_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = if (completed) 0.dp else 10.dp),
            )
            Text(
                text = lesson.rule,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun KidsFeedbackCard(
    session: KidsLessonSession,
    feedback: KidsFeedback?,
    hintVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentStep = session.currentStep

    Column(modifier.fillMaxWidth()) {
        when (feedback) {
            is KidsFeedback.Correct -> {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("kids_lesson_correct_feedback"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.kids_correct_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = feedback.step.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        if (feedback.step.whiteReply != null) {
                            Text(
                                text = stringResource(
                                    R.string.kids_white_reply,
                                    feedback.step.whiteReplyText,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
            is KidsFeedback.Wrong -> {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("kids_lesson_wrong_feedback"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.kids_wrong_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = feedback.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            null -> Unit
        }
        if (hintVisible && currentStep != null && feedback !is KidsFeedback.Correct) {
            Card(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .testTag("kids_lesson_hint_text"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kids_hint_prefix, currentStep.hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun KidsActions(
    feedback: KidsFeedback?,
    onHint: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (feedback is KidsFeedback.Correct) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("kids_lesson_continue_button"),
            ) {
                Text(stringResource(R.string.kids_continue))
            }
        } else {
            OutlinedButton(
                onClick = onHint,
                modifier = Modifier.fillMaxWidth().testTag("kids_lesson_hint_button"),
            ) {
                Text(stringResource(R.string.kids_hint_button))
            }
        }
    }
}

@Composable
private fun KidsCompletionCard(
    lesson: KidsLesson,
    nextLesson: KidsLesson?,
    allLessonsCompleted: Boolean,
    onReplay: () -> Unit,
    onNextLesson: (KidsLesson) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("kids_completion_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    if (nextLesson == null && allLessonsCompleted) {
                        R.string.kids_all_completed
                    } else {
                        R.string.kids_completed_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.kids_completed_pattern, lesson.patternName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.kids_completed_star),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = lesson.steps.last().explanation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = lesson.completion,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onReplay,
                    modifier = Modifier.weight(1f).testTag("kids_lesson_replay_button"),
                ) {
                    Text(stringResource(R.string.kids_replay))
                }
                if (nextLesson != null) {
                    Button(
                        onClick = { onNextLesson(nextLesson) },
                        modifier = Modifier.weight(1f).testTag("kids_lesson_next_button"),
                    ) {
                        Text(stringResource(R.string.kids_next_lesson))
                    }
                }
            }
            OutlinedButton(
                onClick = onBackToList,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("kids_completion_back_button"),
            ) {
                Text(stringResource(R.string.kids_back_to_list))
            }
        }
    }
}

@Composable
private fun KidsThinkingCard(lesson: KidsLesson, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.kids_lesson_thinking_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = lesson.thinkingTip,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun KidsLessonBoard(
    game: GobangGame,
    isInteractive: Boolean,
    showLastMove: Boolean,
    highlightedPositions: Set<Position>,
    hintPositions: Set<Position>,
    completed: Boolean,
    onPositionClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
) {
    GobangBoard(
        game = game,
        onPositionClick = onPositionClick,
        isInteractive = isInteractive,
        showLastMove = showLastMove,
        highlightedPositions = highlightedPositions,
        hintPositions = hintPositions,
        boardDescriptionResource = if (completed) {
            R.string.kids_board_description_completed
        } else {
            R.string.kids_board_description
        },
        testTag = "kids_lesson_board",
        modifier = modifier,
    )
}

@Composable
private fun KidsBoardLegend(hintVisible: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            if (hintVisible) R.string.kids_board_legend_hint else R.string.kids_board_legend,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.testTag("kids_board_legend"),
    )
}

@Preview(name = "Kids lessons", showBackground = true)
@Composable
private fun KidsLessonsHomePreview() {
    GobangptTheme {
        KidsLessonsScreen(
            lessons = kidsLessons,
            selectedLessonId = null,
            progress = InMemoryKidsProgress(),
            onSelectLesson = {},
            onBackToGame = {},
            onBackToList = {},
            onNextLesson = {},
        )
    }
}

@Preview(name = "Kids lesson player", showBackground = true)
@Composable
private fun KidsLessonPlayerPreview() {
    GobangptTheme {
        KidsLessonsScreen(
            lessons = kidsLessons,
            selectedLessonId = "make_open_four",
            progress = InMemoryKidsProgress(),
            onSelectLesson = {},
            onBackToGame = {},
            onBackToList = {},
            onNextLesson = {},
        )
    }
}
