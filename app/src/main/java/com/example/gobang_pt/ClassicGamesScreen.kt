package com.example.gobang_pt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gobang_pt.ui.theme.GobangptTheme

internal enum class LearningCategory {
    CLASSIC,
    PATTERNS,
    LAYOUTS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LearningHomeScreen(
    selectedCategory: LearningCategory,
    onCategoryChange: (LearningCategory) -> Unit,
    onSelectGame: (ClassicGame) -> Unit,
    onSelectLesson: (PatternLesson) -> Unit,
    onSelectLayoutLesson: (LayoutLesson) -> Unit,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = onBackToGame,
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("learning_back_button"),
            ) {
                Text(stringResource(R.string.back_to_game))
            }
            Text(
                text = stringResource(R.string.learning_home),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("learning_home_title"),
            )
            Text(
                text = stringResource(R.string.learning_home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == LearningCategory.CLASSIC,
                    onClick = { onCategoryChange(LearningCategory.CLASSIC) },
                    label = { Text(stringResource(R.string.classic_games_category)) },
                    modifier = Modifier.testTag("learning_classic_category"),
                )
                FilterChip(
                    selected = selectedCategory == LearningCategory.PATTERNS,
                    onClick = { onCategoryChange(LearningCategory.PATTERNS) },
                    label = { Text(stringResource(R.string.pattern_lessons)) },
                    modifier = Modifier.testTag("learning_patterns_category"),
                )
                FilterChip(
                    selected = selectedCategory == LearningCategory.LAYOUTS,
                    onClick = { onCategoryChange(LearningCategory.LAYOUTS) },
                    label = { Text(stringResource(R.string.layout_lessons)) },
                    modifier = Modifier.testTag("learning_layouts_category"),
                )
            }
            when (selectedCategory) {
                LearningCategory.CLASSIC -> classicGames.forEach { game ->
                    ClassicGameCard(
                        game = game,
                        onClick = { onSelectGame(game) },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                LearningCategory.PATTERNS -> PatternLessonList(onSelectLesson = onSelectLesson)
                LearningCategory.LAYOUTS -> LayoutLessonList(onSelectLesson = onSelectLayoutLesson)
            }
        }
    }
}

@Composable
private fun ClassicGameCard(
    game: ClassicGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("classic_game_${game.id}"),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = game.event,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.classic_game_players, game.blackPlayer, game.whitePlayer),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(game.result.labelResource),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.classic_game_length, game.moves.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ClassicGameReviewScreen(
    classicGame: ClassicGame,
    moveCount: Int,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onBackToList: () -> Unit,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullGame = remember(classicGame) { classicGame.toGobangGame() }
    val currentMoveCount = moveCount.coerceIn(0, fullGame.moves.size)
    val displayedGame = remember(fullGame, currentMoveCount) {
        fullGame.replayTo(currentMoveCount)
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
                        game = displayedGame,
                        onPositionClick = {},
                        isInteractive = false,
                        modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ClassicGameReviewHeader(classicGame, onBackToGame)
                    ReviewStatus(
                        game = displayedGame,
                        reviewMoveCount = currentMoveCount,
                        totalMoveCount = fullGame.moves.size,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    )
                    ClassicReviewActions(
                        reviewMoveCount = currentMoveCount,
                        totalMoveCount = fullGame.moves.size,
                        onPreviousMove = onPreviousMove,
                        onNextMove = onNextMove,
                        onBackToList = onBackToList,
                    )
                    ClassicGameSource(classicGame)
                    MoveCount(
                        moveCount = displayedGame.moves.size,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
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
                    ClassicGameReviewHeader(classicGame, onBackToGame)
                    ReviewStatus(
                        game = displayedGame,
                        reviewMoveCount = currentMoveCount,
                        totalMoveCount = fullGame.moves.size,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    )
                    GobangBoard(
                        game = displayedGame,
                        onPositionClick = {},
                        isInteractive = false,
                    )
                    ClassicReviewActions(
                        reviewMoveCount = currentMoveCount,
                        totalMoveCount = fullGame.moves.size,
                        onPreviousMove = onPreviousMove,
                        onNextMove = onNextMove,
                        onBackToList = onBackToList,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    ClassicGameSource(classicGame)
                    MoveCount(
                        moveCount = displayedGame.moves.size,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicGameReviewHeader(
    game: ClassicGame,
    onBackToGame: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            TextButton(
                onClick = onBackToGame,
                modifier = Modifier.testTag("classic_review_back_to_game_button"),
            ) {
                Text(stringResource(R.string.back_to_game))
            }
        }
        Text(
            text = game.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("classic_review_title"),
        )
        Text(
            text = game.event,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.classic_game_players, game.blackPlayer, game.whitePlayer),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = game.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.classic_game_result, stringResource(game.result.labelResource)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp).testTag("classic_game_result"),
        )
    }
}

@Composable
private fun ClassicReviewActions(
    reviewMoveCount: Int,
    totalMoveCount: Int,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReviewActions(
        reviewMoveCount = reviewMoveCount,
        totalMoveCount = totalMoveCount,
        onPreviousMove = onPreviousMove,
        onNextMove = onNextMove,
        onExitReview = onBackToList,
        exitLabelResource = R.string.back_to_classic_games,
        exitTestTag = "back_to_classic_games_button",
        modifier = modifier,
    )
}

@Composable
private fun ClassicGameSource(game: ClassicGame) {
    Text(
        text = stringResource(R.string.classic_game_source, game.source),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

private val ClassicGameResult.labelResource: Int
    get() = when (this) {
        ClassicGameResult.BLACK_WIN -> R.string.classic_result_black_win
        ClassicGameResult.WHITE_WIN -> R.string.classic_result_white_win
        ClassicGameResult.DRAW -> R.string.classic_result_draw
    }

@Preview(name = "Learning home", showBackground = true)
@Composable
private fun LearningHomePreview() {
    GobangptTheme {
        LearningHomeScreen(
            selectedCategory = LearningCategory.CLASSIC,
            onCategoryChange = {},
            onSelectGame = {},
            onSelectLesson = {},
            onSelectLayoutLesson = {},
            onBackToGame = {},
        )
    }
}

@Preview(name = "Classic game review", showBackground = true)
@Composable
private fun ClassicGameReviewPreview() {
    GobangptTheme {
        ClassicGameReviewScreen(
            classicGame = classicGames.first(),
            moveCount = 20,
            onPreviousMove = {},
            onNextMove = {},
            onBackToList = {},
            onBackToGame = {},
        )
    }
}
