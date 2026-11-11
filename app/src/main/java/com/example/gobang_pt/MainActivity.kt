package com.example.gobang_pt

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gobang_pt.ui.theme.GobangptTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class GameMode {
    LOCAL,
    VS_AI,
}

private val BoardColor = Color(0xFFD8A75D)
private val GridColor = Color(0xFF4A321D)
private val LastMoveColor = Color(0xFFD64545)
private val WinningLineColor = Color(0xFF2E7D32)

private val gameSaver = Saver<GobangGame, String>(
    save = { game ->
        game.moves.joinToString(";") { move ->
            "${move.position.row},${move.position.column}"
        }
    },
    restore = { savedMoves ->
        savedMoves.split(';').filter { it.isNotBlank() }.fold(GobangGame()) { game, move ->
            val (row, column) = move.split(',').map(String::toInt)
            game.play(Position(row, column))
        }
    },
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GobangptTheme {
                GobangScreen()
            }
        }
    }
}

@Composable
fun GobangScreen() {
    var game by rememberSaveable(stateSaver = gameSaver) { mutableStateOf(GobangGame()) }
    var gameMode by rememberSaveable { mutableStateOf(GameMode.LOCAL) }
    var aiDifficulty by rememberSaveable { mutableStateOf(AiDifficulty.EASY) }
    var isAiThinking by remember { mutableStateOf(false) }
    var showRestartConfirmation by remember { mutableStateOf(false) }
    var showSettingsConfirmation by remember { mutableStateOf(false) }
    var requestedGameMode by remember { mutableStateOf<GameMode?>(null) }
    var requestedDifficulty by remember { mutableStateOf<AiDifficulty?>(null) }
    var isReviewing by rememberSaveable { mutableStateOf(false) }
    var reviewMoveCount by rememberSaveable { mutableStateOf(0) }
    var showLearningCenter by rememberSaveable { mutableStateOf(false) }
    var learningCategory by rememberSaveable { mutableStateOf(LearningCategory.CLASSIC) }
    var selectedClassicGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var classicReviewMoveCount by rememberSaveable { mutableStateOf(0) }
    var selectedPatternLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var patternPlaybackFrameIndex by rememberSaveable { mutableStateOf(0) }
    var selectedLayoutLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var layoutPlaybackFrameIndex by rememberSaveable { mutableStateOf(0) }
    var selectedPracticeLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceMoves by rememberSaveable { mutableStateOf("") }
    var showPracticeHint by rememberSaveable { mutableStateOf(false) }
    var isPracticeThinking by remember { mutableStateOf(false) }
    val currentReviewMoveCount = reviewMoveCount.coerceIn(0, game.moves.size)
    val displayedGame = if (isReviewing) game.replayTo(currentReviewMoveCount) else game
    val selectedClassicGame = classicGames.firstOrNull { it.id == selectedClassicGameId }
    val selectedPatternLesson = patternLessons.firstOrNull { it.id == selectedPatternLessonId }
    val selectedLayoutLesson = layoutLessons.firstOrNull { it.id == selectedLayoutLessonId }
    val selectedPractice = practiceScenarios.firstOrNull { it.id == selectedPracticeLessonId }
    val practiceGame = selectedPractice?.replay(practiceMoves)
    val resetPatternPlayback = { patternPlaybackFrameIndex = 0 }
    val resetLayoutLesson = { layoutPlaybackFrameIndex = 0 }
    val resetPractice = {
        practiceMoves = ""
        showPracticeHint = false
    }
    val onBackToGame = {
        showLearningCenter = false
        selectedClassicGameId = null
        selectedPatternLessonId = null
        selectedLayoutLessonId = null
        selectedPracticeLessonId = null
        resetPatternPlayback()
        resetLayoutLesson()
        resetPractice()
    }

    BackHandler(enabled = showLearningCenter) {
        when {
            selectedPractice != null -> {
                selectedPracticeLessonId = null
                resetPractice()
            }
            selectedClassicGame != null -> selectedClassicGameId = null
            selectedPatternLesson != null -> {
                selectedPatternLessonId = null
                resetPatternPlayback()
            }
            selectedLayoutLesson != null -> {
                selectedLayoutLessonId = null
                resetLayoutLesson()
            }
            else -> onBackToGame()
        }
    }

    LaunchedEffect(game, gameMode, aiDifficulty, isReviewing, showLearningCenter) {
        isAiThinking = false
        if (gameMode == GameMode.VS_AI && !isReviewing && !showLearningCenter &&
            !game.isOver && game.currentPlayer == Stone.WHITE
        ) {
            isAiThinking = true
            val thinkingTimeMillis = 1000L
            val thinkingStartedAt = SystemClock.elapsedRealtime()
            val aiMove = withContext(Dispatchers.Default) {
                GobangAi.chooseMove(game, aiDifficulty)
            }
            val remainingThinkingTime = thinkingTimeMillis - (SystemClock.elapsedRealtime() - thinkingStartedAt)
            if (remainingThinkingTime > 0) delay(remainingThinkingTime)
            if (aiMove != null) game = game.play(aiMove)
            isAiThinking = false
        }
    }

    LaunchedEffect(selectedPracticeLessonId, practiceGame, showLearningCenter) {
        if (showLearningCenter && selectedPractice != null && practiceGame != null &&
            practiceGame.currentPlayer == Stone.WHITE && !practiceGame.isOver
        ) {
            isPracticeThinking = true
            val thinkingStartedAt = SystemClock.elapsedRealtime()
            val aiMove = withContext(Dispatchers.Default) {
                selectedPractice.chooseWhiteMove(practiceGame)
            }
            val remainingThinkingTime = 500L - (SystemClock.elapsedRealtime() - thinkingStartedAt)
            if (remainingThinkingTime > 0) delay(remainingThinkingTime)
            if (aiMove != null) practiceMoves = selectedPractice.encode(practiceGame.play(aiMove))
            isPracticeThinking = false
        } else {
            isPracticeThinking = false
        }
    }

    val onPositionClick: (Position) -> Unit = { position ->
        val canPlay = !isReviewing && !isAiThinking &&
            (gameMode == GameMode.LOCAL || game.currentPlayer == Stone.BLACK)
        if (canPlay) game = game.play(position)
    }
    val onUndo = {
        if (gameMode == GameMode.LOCAL) {
            game = game.undo()
        } else {
            val movesToUndo = if (game.moves.lastOrNull()?.stone == Stone.BLACK) 1 else 2
            game = game.replayTo((game.moves.size - movesToUndo).coerceAtLeast(0))
        }
    }
    val onRestart = {
        if (game.moves.isEmpty()) game = GobangGame() else showRestartConfirmation = true
    }
    val onStartReview = {
        reviewMoveCount = 0
        isReviewing = true
    }
    val onGameModeChange: (GameMode) -> Unit = { mode ->
        if (mode != gameMode) {
            if (game.moves.isEmpty()) {
                gameMode = mode
            } else {
                requestedGameMode = mode
                showSettingsConfirmation = true
            }
        }
    }
    val onDifficultyChange: (AiDifficulty) -> Unit = { difficulty ->
        if (difficulty != aiDifficulty) {
            if (game.moves.isEmpty()) {
                aiDifficulty = difficulty
            } else {
                requestedDifficulty = difficulty
                showSettingsConfirmation = true
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (showLearningCenter && selectedClassicGame != null) {
                ClassicGameReviewScreen(
                    classicGame = selectedClassicGame,
                    moveCount = classicReviewMoveCount,
                    onPreviousMove = {
                        classicReviewMoveCount = (classicReviewMoveCount - 1)
                            .coerceAtLeast(0)
                    },
                    onNextMove = {
                        classicReviewMoveCount = (classicReviewMoveCount + 1)
                            .coerceAtMost(selectedClassicGame.moves.size)
                    },
                    onBackToList = { selectedClassicGameId = null },
                    onBackToGame = onBackToGame,
                )
            } else if (showLearningCenter && selectedPatternLesson != null) {
                val playback = selectedPatternLesson.toPlayback()
                LessonPlaybackScreen(
                    playback = playback,
                    frameIndex = patternPlaybackFrameIndex,
                    onPreviousFrame = {
                        patternPlaybackFrameIndex = (patternPlaybackFrameIndex - 1).coerceAtLeast(0)
                    },
                    onNextFrame = {
                        patternPlaybackFrameIndex = (patternPlaybackFrameIndex + 1)
                            .coerceAtMost(playback.frames.lastIndex)
                    },
                    onBackToList = {
                        selectedPatternLessonId = null
                        resetPatternPlayback()
                    },
                    onBackToGame = onBackToGame,
                )
            } else if (showLearningCenter && selectedPractice != null && practiceGame != null) {
                PracticeGameScreen(
                    scenario = selectedPractice,
                    game = practiceGame,
                    analysis = PracticeCoach.analyze(selectedPractice, practiceGame),
                    isAiThinking = isPracticeThinking,
                    showHint = showPracticeHint,
                    recommendation = if (showPracticeHint) {
                        PracticeCoach.recommend(selectedPractice, practiceGame)
                    } else {
                        null
                    },
                    onPositionClick = { position ->
                        if (!isPracticeThinking && practiceGame.currentPlayer == Stone.BLACK && !practiceGame.isOver) {
                            val updated = practiceGame.play(position)
                            if (updated !== practiceGame) {
                                practiceMoves = selectedPractice.encode(updated)
                                showPracticeHint = false
                            }
                        }
                    },
                    onToggleHint = { showPracticeHint = !showPracticeHint },
                    onUndo = {
                        if (!isPracticeThinking && practiceGame.moves.isNotEmpty()) {
                            practiceMoves = selectedPractice.encode(selectedPractice.undo(practiceGame))
                            showPracticeHint = false
                        }
                    },
                    onRestart = resetPractice,
                    onNextScenario = practiceScenarios.indexOf(selectedPractice)
                        .takeIf { it in 0 until practiceScenarios.lastIndex }
                        ?.let { index ->
                            {
                                val nextScenario = practiceScenarios[index + 1]
                                selectedLayoutLessonId = nextScenario.id
                                selectedPracticeLessonId = nextScenario.id
                                resetLayoutLesson()
                                resetPractice()
                            }
                        },
                    onBackToLayout = {
                        selectedPracticeLessonId = null
                        resetPractice()
                    },
                )
            } else if (showLearningCenter && selectedLayoutLesson != null) {
                val playback = selectedLayoutLesson.toPlayback()
                LessonPlaybackScreen(
                    playback = playback,
                    frameIndex = layoutPlaybackFrameIndex,
                    onPreviousFrame = {
                        layoutPlaybackFrameIndex = (layoutPlaybackFrameIndex - 1).coerceAtLeast(0)
                    },
                    onNextFrame = {
                        layoutPlaybackFrameIndex = (layoutPlaybackFrameIndex + 1)
                            .coerceAtMost(playback.frames.lastIndex)
                    },
                    onBackToList = {
                        selectedLayoutLessonId = null
                        resetLayoutLesson()
                    },
                    onBackToGame = onBackToGame,
                    onStartPractice = {
                        selectedPracticeLessonId = selectedLayoutLesson.id
                        resetPractice()
                    },
                )
            } else if (showLearningCenter) {
                LearningHomeScreen(
                    selectedCategory = learningCategory,
                    onCategoryChange = { learningCategory = it },
                    onSelectGame = { classicGame ->
                        selectedClassicGameId = classicGame.id
                        classicReviewMoveCount = 0
                    },
                    onSelectLesson = { lesson ->
                        selectedPatternLessonId = lesson.id
                        resetPatternPlayback()
                    },
                    onSelectLayoutLesson = { lesson ->
                        selectedLayoutLessonId = lesson.id
                        resetLayoutLesson()
                    },
                    onBackToGame = onBackToGame,
                )
            } else if (maxWidth >= 700.dp && maxWidth > maxHeight) {
                ExpandedGobangLayout(
                    game = displayedGame,
                    gameMode = gameMode,
                    aiDifficulty = aiDifficulty,
                    isAiThinking = isAiThinking,
                    totalMoveCount = game.moves.size,
                    isReviewing = isReviewing,
                    reviewMoveCount = currentReviewMoveCount,
                    onPositionClick = onPositionClick,
                    onUndo = onUndo,
                    onRestart = onRestart,
                    onStartReview = onStartReview,
                    onOpenLearning = {
                        showLearningCenter = true
                        selectedClassicGameId = null
                        selectedPatternLessonId = null
                        selectedLayoutLessonId = null
                        selectedPracticeLessonId = null
                        resetPatternPlayback()
                        resetLayoutLesson()
                        resetPractice()
                    },
                    onGameModeChange = onGameModeChange,
                    onDifficultyChange = onDifficultyChange,
                    onPreviousMove = { reviewMoveCount = currentReviewMoveCount - 1 },
                    onNextMove = { reviewMoveCount = currentReviewMoveCount + 1 },
                    onExitReview = { isReviewing = false },
                )
            } else {
                CompactGobangLayout(
                    game = displayedGame,
                    gameMode = gameMode,
                    aiDifficulty = aiDifficulty,
                    isAiThinking = isAiThinking,
                    totalMoveCount = game.moves.size,
                    isReviewing = isReviewing,
                    reviewMoveCount = currentReviewMoveCount,
                    onPositionClick = onPositionClick,
                    onUndo = onUndo,
                    onRestart = onRestart,
                    onStartReview = onStartReview,
                    onOpenLearning = {
                        showLearningCenter = true
                        selectedClassicGameId = null
                        selectedPatternLessonId = null
                        selectedLayoutLessonId = null
                        selectedPracticeLessonId = null
                        resetPatternPlayback()
                        resetLayoutLesson()
                        resetPractice()
                    },
                    onGameModeChange = onGameModeChange,
                    onDifficultyChange = onDifficultyChange,
                    onPreviousMove = { reviewMoveCount = currentReviewMoveCount - 1 },
                    onNextMove = { reviewMoveCount = currentReviewMoveCount + 1 },
                    onExitReview = { isReviewing = false },
                )
            }
        }
    }

    if (showRestartConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmation = false },
            title = { Text(stringResource(R.string.restart)) },
            text = { Text(stringResource(R.string.restart_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        game = GobangGame()
                        showRestartConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_restart_button"),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showSettingsConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showSettingsConfirmation = false
                requestedGameMode = null
                requestedDifficulty = null
            },
            title = { Text(stringResource(R.string.change_settings)) },
            text = { Text(stringResource(R.string.change_settings_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        gameMode = requestedGameMode ?: gameMode
                        aiDifficulty = requestedDifficulty ?: aiDifficulty
                        game = GobangGame()
                        requestedGameMode = null
                        requestedDifficulty = null
                        showSettingsConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_settings_button"),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsConfirmation = false
                        requestedGameMode = null
                        requestedDifficulty = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CompactGobangLayout(
    game: GobangGame,
    gameMode: GameMode,
    aiDifficulty: AiDifficulty,
    isAiThinking: Boolean,
    totalMoveCount: Int,
    isReviewing: Boolean,
    reviewMoveCount: Int,
    onPositionClick: (Position) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLearning: () -> Unit,
    onGameModeChange: (GameMode) -> Unit,
    onDifficultyChange: (AiDifficulty) -> Unit,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onExitReview: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameHeader(gameMode)
            GameSettings(
                gameMode = gameMode,
                aiDifficulty = aiDifficulty,
                enabled = !isAiThinking && !isReviewing,
                onGameModeChange = onGameModeChange,
                onDifficultyChange = onDifficultyChange,
                modifier = Modifier.padding(top = 12.dp),
            )
            PlayerCards(Modifier.padding(top = 12.dp), game, gameMode)
            GameStatus(game, gameMode, isAiThinking, Modifier.padding(vertical = 12.dp))
            if (isReviewing) {
                ReviewStatus(game, reviewMoveCount, totalMoveCount, Modifier.padding(bottom = 12.dp))
            }
            GobangBoard(
                game = game,
                onPositionClick = onPositionClick,
                isInteractive = !isReviewing && !isAiThinking &&
                    (gameMode == GameMode.LOCAL || game.currentPlayer == Stone.BLACK),
            )
            if (isReviewing) {
                ReviewActions(
                    reviewMoveCount = reviewMoveCount,
                    totalMoveCount = totalMoveCount,
                    onPreviousMove = onPreviousMove,
                    onNextMove = onNextMove,
                    onExitReview = onExitReview,
                    modifier = Modifier.padding(top = 20.dp),
                )
            } else {
                GameActions(
                    game = game,
                    onUndo = onUndo,
                    onRestart = onRestart,
                    onStartReview = onStartReview,
                    onOpenLearning = onOpenLearning,
                    enabled = !isAiThinking,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun ExpandedGobangLayout(
    game: GobangGame,
    gameMode: GameMode,
    aiDifficulty: AiDifficulty,
    isAiThinking: Boolean,
    totalMoveCount: Int,
    isReviewing: Boolean,
    reviewMoveCount: Int,
    onPositionClick: (Position) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLearning: () -> Unit,
    onGameModeChange: (GameMode) -> Unit,
    onDifficultyChange: (AiDifficulty) -> Unit,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onExitReview: () -> Unit,
) {
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
                isInteractive = !isReviewing && !isAiThinking &&
                    (gameMode == GameMode.LOCAL || game.currentPlayer == Stone.BLACK),
                modifier = Modifier.size(minOf(maxWidth, maxHeight)),
            )
        }
        Column(
            modifier = Modifier
                .width(300.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameHeader(gameMode)
            GameSettings(
                gameMode = gameMode,
                aiDifficulty = aiDifficulty,
                enabled = !isAiThinking && !isReviewing,
                onGameModeChange = onGameModeChange,
                onDifficultyChange = onDifficultyChange,
                modifier = Modifier.padding(top = 16.dp),
            )
            PlayerCards(Modifier.padding(top = 16.dp), game, gameMode)
            GameStatus(game, gameMode, isAiThinking, Modifier.padding(vertical = 16.dp))
            if (isReviewing) {
                ReviewStatus(game, reviewMoveCount, totalMoveCount, Modifier.padding(bottom = 16.dp))
                ReviewActions(
                    reviewMoveCount = reviewMoveCount,
                    totalMoveCount = totalMoveCount,
                    onPreviousMove = onPreviousMove,
                    onNextMove = onNextMove,
                    onExitReview = onExitReview,
                )
            } else {
                GameActions(
                    game = game,
                    onUndo = onUndo,
                    onRestart = onRestart,
                    onStartReview = onStartReview,
                    onOpenLearning = onOpenLearning,
                    enabled = !isAiThinking,
                )
            }
            MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun GameHeader(gameMode: GameMode, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.game_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(
                if (gameMode == GameMode.LOCAL) R.string.local_two_players else R.string.local_vs_ai,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GameSettings(
    gameMode: GameMode,
    aiDifficulty: AiDifficulty,
    enabled: Boolean,
    onGameModeChange: (GameMode) -> Unit,
    onDifficultyChange: (AiDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = gameMode == GameMode.LOCAL,
                onClick = { onGameModeChange(GameMode.LOCAL) },
                label = { Text(stringResource(R.string.local_mode)) },
                enabled = enabled,
                modifier = Modifier.testTag("local_mode"),
            )
            FilterChip(
                selected = gameMode == GameMode.VS_AI,
                onClick = { onGameModeChange(GameMode.VS_AI) },
                label = { Text(stringResource(R.string.ai_mode)) },
                enabled = enabled,
                modifier = Modifier.testTag("ai_mode"),
            )
        }
        if (gameMode == GameMode.VS_AI) {
            Text(
                text = stringResource(R.string.difficulty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiDifficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = aiDifficulty == difficulty,
                        onClick = { onDifficultyChange(difficulty) },
                        label = { Text(stringResource(difficulty.labelResource)) },
                        enabled = enabled,
                        modifier = Modifier.testTag(difficulty.testTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCards(modifier: Modifier = Modifier, game: GobangGame, gameMode: GameMode) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerCard(
            stone = Stone.BLACK,
            labelResource = if (gameMode == GameMode.LOCAL) R.string.black else R.string.player_black,
            currentPlayer = game.currentPlayer,
            gameInProgress = !game.isOver,
            modifier = Modifier.weight(1f),
        )
        PlayerCard(
            stone = Stone.WHITE,
            labelResource = if (gameMode == GameMode.LOCAL) R.string.white else R.string.computer_white,
            currentPlayer = game.currentPlayer,
            gameInProgress = !game.isOver,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GameStatus(
    game: GobangGame,
    gameMode: GameMode,
    isAiThinking: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = game.statusText(gameMode, isAiThinking),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.testTag("game_status"),
    )
}

@Composable
internal fun ReviewStatus(
    game: GobangGame,
    reviewMoveCount: Int,
    totalMoveCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.review_progress, reviewMoveCount, totalMoveCount),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag("review_progress"),
        )
        val lastMove = game.moves.lastOrNull()
        Text(
            text = if (lastMove == null) {
                stringResource(R.string.review_initial_position)
            } else {
                stringResource(
                    R.string.review_move_description,
                    reviewMoveCount,
                    stringResource(if (lastMove.stone == Stone.BLACK) R.string.black else R.string.white),
                    lastMove.position.row + 1,
                    lastMove.position.column + 1,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).testTag("review_move_description"),
        )
    }
}

@Composable
private fun GameActions(
    game: GobangGame,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLearning: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onUndo,
                enabled = enabled && game.moves.isNotEmpty(),
                modifier = Modifier.testTag("undo_button"),
            ) {
                Text(stringResource(R.string.undo))
            }
            Button(
                onClick = onRestart,
                enabled = enabled,
                modifier = Modifier.testTag("restart_button"),
            ) {
                Text(stringResource(R.string.restart))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            OutlinedButton(
                onClick = onStartReview,
                enabled = enabled && game.moves.isNotEmpty(),
                modifier = Modifier.testTag("review_button"),
            ) {
                Text(stringResource(R.string.review))
            }
            OutlinedButton(
                onClick = onOpenLearning,
                enabled = enabled,
                modifier = Modifier.testTag("learning_home_button"),
            ) {
                Text(stringResource(R.string.learning_home))
            }
        }
    }
}

@Composable
internal fun ReviewActions(
    reviewMoveCount: Int,
    totalMoveCount: Int,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onExitReview: () -> Unit,
    exitLabelResource: Int = R.string.exit_review,
    exitTestTag: String = "exit_review_button",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPreviousMove,
                enabled = reviewMoveCount > 0,
                modifier = Modifier.testTag("previous_move_button"),
            ) {
                Text(stringResource(R.string.previous_move))
            }
            Button(
                onClick = onNextMove,
                enabled = reviewMoveCount < totalMoveCount,
                modifier = Modifier.testTag("next_move_button"),
            ) {
                Text(stringResource(R.string.next_move))
            }
        }
        TextButton(
            onClick = onExitReview,
            modifier = Modifier.padding(top = 4.dp).testTag(exitTestTag),
        ) {
            Text(stringResource(exitLabelResource))
        }
    }
}

@Composable
internal fun MoveCount(moveCount: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.move_count, moveCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun PlayerCard(
    stone: Stone,
    labelResource: Int,
    currentPlayer: Stone,
    gameInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val isCurrent = gameInProgress && stone == currentPlayer
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (stone == Stone.BLACK) Color.Black else Color.White),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(labelResource),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
internal fun GobangBoard(
    game: GobangGame,
    onPositionClick: (Position) -> Unit,
    isInteractive: Boolean = true,
    showLastMove: Boolean = true,
    highlightedPositions: Set<Position> = emptySet(),
    highlightColor: Color = LastMoveColor,
    boardDescriptionResource: Int = if (isInteractive) {
        R.string.board_description
    } else {
        R.string.review_board_description
    },
    modifier: Modifier = Modifier,
) {
    val boardDescription = stringResource(boardDescriptionResource)
    val lastMove = if (showLastMove) game.moves.lastOrNull() else null
    val moveHighlightProgress = remember { Animatable(1f) }
    LaunchedEffect(lastMove) {
        if (lastMove == null) {
            moveHighlightProgress.snapTo(1f)
        } else {
            moveHighlightProgress.snapTo(0f)
            moveHighlightProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            )
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(BoardColor)
            .semantics { contentDescription = boardDescription }
            .testTag("gobang_board")
            .pointerInput(game, isInteractive) {
                if (isInteractive) {
                    detectTapGestures { offset ->
                        val step = size.width / (BOARD_SIZE + 1f)
                        val row = ((offset.y - step) / step).roundToInt()
                        val column = ((offset.x - step) / step).roundToInt()
                        val position = Position(row, column)
                        if (position.isValid) onPositionClick(position)
                    }
                }
            },
    ) {
        val step = size.width / (BOARD_SIZE + 1f)
        val gridStart = step
        val gridEnd = step * BOARD_SIZE
        val lineWidth = 1.dp.toPx()

        for (index in 0 until BOARD_SIZE) {
            val coordinate = step * (index + 1)
            drawLine(GridColor, Offset(gridStart, coordinate), Offset(gridEnd, coordinate), lineWidth)
            drawLine(GridColor, Offset(coordinate, gridStart), Offset(coordinate, gridEnd), lineWidth)
        }

        listOf(3, 7, 11).forEach { row ->
            listOf(3, 7, 11).forEach { column ->
                drawCircle(
                    color = GridColor,
                    radius = step * 0.09f,
                    center = Offset(step * (column + 1), step * (row + 1)),
                )
            }
        }

        if (game.winningLine.isNotEmpty()) {
            val first = game.winningLine.first()
            val last = game.winningLine.last()
            drawLine(
                color = WinningLineColor,
                start = Offset(step * (first.column + 1), step * (first.row + 1)),
                end = Offset(step * (last.column + 1), step * (last.row + 1)),
                strokeWidth = step * 0.18f,
            )
        }

        game.moves.forEach { move ->
            val center = Offset(
                step * (move.position.column + 1),
                step * (move.position.row + 1),
            )
            drawCircle(
                color = if (move.stone == Stone.BLACK) Color(0xFF171717) else Color(0xFFF8F8F8),
                radius = step * 0.43f,
                center = center,
            )
            drawCircle(
                color = if (move.stone == Stone.BLACK) Color.Black else Color(0xFF9E9E9E),
                radius = step * 0.43f,
                center = center,
                style = Stroke(width = lineWidth),
            )
        }

        highlightedPositions.forEach { position ->
            val center = Offset(
                step * (position.column + 1),
                step * (position.row + 1),
            )
            drawCircle(
                color = highlightColor,
                radius = step * 0.49f,
                center = center,
                style = Stroke(width = step * 0.05f),
            )
            if (game.stoneAt(position) == null) {
                drawCircle(
                    color = highlightColor,
                    radius = step * 0.08f,
                    center = center,
                )
            }
        }

        lastMove?.let { move ->
            val center = Offset(
                step * (move.position.column + 1),
                step * (move.position.row + 1),
            )
            drawCircle(
                color = LastMoveColor,
                radius = step * 0.49f,
                center = center,
                style = Stroke(width = step * 0.055f),
            )
            drawCircle(
                color = LastMoveColor,
                radius = step * 0.10f,
                center = center,
            )

            val progress = moveHighlightProgress.value
            if (progress < 1f) {
                drawCircle(
                    color = LastMoveColor.copy(alpha = 0.85f * (1f - progress)),
                    radius = step * (0.50f + 0.40f * progress),
                    center = center,
                    style = Stroke(width = step * 0.07f * (1f - progress)),
                )
            }
        }
    }
}

@Composable
private fun GobangGame.statusText(gameMode: GameMode, isAiThinking: Boolean): String = when {
    winner == Stone.BLACK && gameMode == GameMode.VS_AI -> stringResource(R.string.player_wins)
    winner == Stone.WHITE && gameMode == GameMode.VS_AI -> stringResource(R.string.computer_wins)
    winner == Stone.BLACK -> stringResource(R.string.black_wins)
    winner == Stone.WHITE -> stringResource(R.string.white_wins)
    isDraw -> stringResource(R.string.draw)
    isAiThinking -> stringResource(R.string.computer_thinking)
    gameMode == GameMode.VS_AI && currentPlayer == Stone.BLACK -> stringResource(R.string.player_turn)
    gameMode == GameMode.VS_AI -> stringResource(R.string.computer_turn)
    currentPlayer == Stone.BLACK -> stringResource(R.string.black_turn)
    else -> stringResource(R.string.white_turn)
}

private val AiDifficulty.labelResource: Int
    get() = when (this) {
        AiDifficulty.EASY -> R.string.difficulty_easy
        AiDifficulty.MEDIUM -> R.string.difficulty_medium
        AiDifficulty.HARD -> R.string.difficulty_hard
    }

private val AiDifficulty.testTag: String
    get() = when (this) {
        AiDifficulty.EASY -> "difficulty_easy"
        AiDifficulty.MEDIUM -> "difficulty_medium"
        AiDifficulty.HARD -> "difficulty_hard"
    }

@Preview(name = "Phone", showBackground = true)
@Composable
private fun GobangScreenPreview() {
    GobangptTheme {
        GobangScreen()
    }
}

@Preview(name = "Tablet portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun GobangScreenTabletPortraitPreview() {
    GobangptTheme {
        GobangScreen()
    }
}

@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun GobangScreenTabletLandscapePreview() {
    GobangptTheme {
        GobangScreen()
    }
}
