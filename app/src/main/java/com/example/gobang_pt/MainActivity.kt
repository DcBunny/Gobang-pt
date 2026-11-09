package com.example.gobang_pt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlin.math.roundToInt

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
    var showRestartConfirmation by remember { mutableStateOf(false) }
    var isReviewing by rememberSaveable { mutableStateOf(false) }
    var reviewMoveCount by rememberSaveable { mutableStateOf(0) }
    val currentReviewMoveCount = reviewMoveCount.coerceIn(0, game.moves.size)
    val displayedGame = if (isReviewing) game.replayTo(currentReviewMoveCount) else game

    val onRestart = {
        if (game.moves.isEmpty()) game = GobangGame() else showRestartConfirmation = true
    }
    val onStartReview = {
        reviewMoveCount = 0
        isReviewing = true
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (maxWidth >= 700.dp && maxWidth > maxHeight) {
                ExpandedGobangLayout(
                    game = displayedGame,
                    totalMoveCount = game.moves.size,
                    isReviewing = isReviewing,
                    reviewMoveCount = currentReviewMoveCount,
                    onPositionClick = { position -> game = game.play(position) },
                    onUndo = { game = game.undo() },
                    onRestart = onRestart,
                    onStartReview = onStartReview,
                    onPreviousMove = { reviewMoveCount = currentReviewMoveCount - 1 },
                    onNextMove = { reviewMoveCount = currentReviewMoveCount + 1 },
                    onExitReview = { isReviewing = false },
                )
            } else {
                CompactGobangLayout(
                    game = displayedGame,
                    totalMoveCount = game.moves.size,
                    isReviewing = isReviewing,
                    reviewMoveCount = currentReviewMoveCount,
                    onPositionClick = { position -> game = game.play(position) },
                    onUndo = { game = game.undo() },
                    onRestart = onRestart,
                    onStartReview = onStartReview,
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
}

@Composable
private fun CompactGobangLayout(
    game: GobangGame,
    totalMoveCount: Int,
    isReviewing: Boolean,
    reviewMoveCount: Int,
    onPositionClick: (Position) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onStartReview: () -> Unit,
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
            GameHeader()
            PlayerCards(Modifier.padding(top = 16.dp), game)
            GameStatus(game, Modifier.padding(vertical = 16.dp))
            if (isReviewing) {
                ReviewStatus(game, reviewMoveCount, totalMoveCount, Modifier.padding(bottom = 12.dp))
            }
            GobangBoard(game, onPositionClick, isInteractive = !isReviewing)
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
                GameActions(game, onUndo, onRestart, onStartReview, Modifier.padding(top = 20.dp))
            }
            MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun ExpandedGobangLayout(
    game: GobangGame,
    totalMoveCount: Int,
    isReviewing: Boolean,
    reviewMoveCount: Int,
    onPositionClick: (Position) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onStartReview: () -> Unit,
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
                isInteractive = !isReviewing,
                modifier = Modifier.size(minOf(maxWidth, maxHeight)),
            )
        }
        Column(
            modifier = Modifier
                .width(300.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameHeader()
            PlayerCards(Modifier.padding(top = 24.dp), game)
            GameStatus(game, Modifier.padding(vertical = 20.dp))
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
                GameActions(game, onUndo, onRestart, onStartReview)
            }
            MoveCount(game.moves.size, Modifier.padding(top = 12.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun GameHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.game_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.local_two_players),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PlayerCards(modifier: Modifier = Modifier, game: GobangGame) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerCard(Stone.BLACK, game.currentPlayer, !game.isOver, Modifier.weight(1f))
        PlayerCard(Stone.WHITE, game.currentPlayer, !game.isOver, Modifier.weight(1f))
    }
}

@Composable
private fun GameStatus(game: GobangGame, modifier: Modifier = Modifier) {
    Text(
        text = game.statusText(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.testTag("game_status"),
    )
}

@Composable
private fun ReviewStatus(
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onUndo,
                enabled = game.moves.isNotEmpty(),
                modifier = Modifier.testTag("undo_button"),
            ) {
                Text(stringResource(R.string.undo))
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.testTag("restart_button"),
            ) {
                Text(stringResource(R.string.restart))
            }
        }
        OutlinedButton(
            onClick = onStartReview,
            enabled = game.moves.isNotEmpty(),
            modifier = Modifier.padding(top = 8.dp).testTag("review_button"),
        ) {
            Text(stringResource(R.string.review))
        }
    }
}

@Composable
private fun ReviewActions(
    reviewMoveCount: Int,
    totalMoveCount: Int,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onExitReview: () -> Unit,
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
            modifier = Modifier.padding(top = 4.dp).testTag("exit_review_button"),
        ) {
            Text(stringResource(R.string.exit_review))
        }
    }
}

@Composable
private fun MoveCount(moveCount: Int, modifier: Modifier = Modifier) {
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
                text = stringResource(if (stone == Stone.BLACK) R.string.black else R.string.white),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun GobangBoard(
    game: GobangGame,
    onPositionClick: (Position) -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val boardDescription = stringResource(
        if (isInteractive) R.string.board_description else R.string.review_board_description,
    )
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

        game.moves.lastOrNull()?.let { lastMove ->
            drawCircle(
                color = LastMoveColor,
                radius = step * 0.10f,
                center = Offset(
                    step * (lastMove.position.column + 1),
                    step * (lastMove.position.row + 1),
                ),
            )
        }
    }
}

@Composable
private fun GobangGame.statusText(): String = when {
    winner == Stone.BLACK -> stringResource(R.string.black_wins)
    winner == Stone.WHITE -> stringResource(R.string.white_wins)
    isDraw -> stringResource(R.string.draw)
    currentPlayer == Stone.BLACK -> stringResource(R.string.black_turn)
    else -> stringResource(R.string.white_turn)
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
