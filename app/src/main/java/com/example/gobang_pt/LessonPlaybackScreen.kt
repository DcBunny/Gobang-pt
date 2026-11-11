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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gobang_pt.ui.theme.GobangptTheme

@Composable
internal fun LessonPlaybackScreen(
    playback: LessonPlayback,
    frameIndex: Int,
    onPreviousFrame: () -> Unit,
    onNextFrame: () -> Unit,
    onBackToList: () -> Unit,
    onBackToGame: () -> Unit,
    onStartPractice: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentFrameIndex = frameIndex.coerceIn(0, playback.frames.lastIndex)
    val frame = playback.frames[currentFrameIndex]

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
                    LessonPlaybackBoard(
                        frame = frame,
                        modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LessonPlaybackHeader(playback, onBackToGame)
                    LessonPlaybackGuidance(playback, frame, currentFrameIndex)
                    LessonPlaybackActions(
                        playback = playback,
                        frameIndex = currentFrameIndex,
                        onPreviousFrame = onPreviousFrame,
                        onNextFrame = onNextFrame,
                        onBackToList = onBackToList,
                        onStartPractice = onStartPractice,
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
                    LessonPlaybackHeader(playback, onBackToGame)
                    LessonPlaybackGuidance(playback, frame, currentFrameIndex)
                    LessonPlaybackBoard(frame, Modifier.padding(top = 16.dp))
                    LessonPlaybackActions(
                        playback = playback,
                        frameIndex = currentFrameIndex,
                        onPreviousFrame = onPreviousFrame,
                        onNextFrame = onNextFrame,
                        onBackToList = onBackToList,
                        onStartPractice = onStartPractice,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonPlaybackBoard(
    frame: LessonPlaybackFrame,
    modifier: Modifier = Modifier,
) {
    GobangBoard(
        game = frame.game,
        onPositionClick = {},
        isInteractive = false,
        showLastMove = false,
        highlightedPositions = frame.highlights,
        highlightColor = MaterialTheme.colorScheme.primary,
        boardDescriptionResource = R.string.lesson_playback_board_description,
        modifier = modifier,
    )
}

@Composable
private fun LessonPlaybackHeader(
    playback: LessonPlayback,
    onBackToGame: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            TextButton(
                onClick = onBackToGame,
                modifier = Modifier.testTag("lesson_back_to_game_button"),
            ) {
                Text(stringResource(R.string.back_to_game))
            }
        }
        Text(
            text = playback.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("lesson_playback_title"),
        )
        Text(
            text = playback.subtitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = playback.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LessonPlaybackGuidance(
    playback: LessonPlayback,
    frame: LessonPlaybackFrame,
    frameIndex: Int,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = stringResource(
                R.string.lesson_playback_progress,
                frameIndex + 1,
                playback.frames.size,
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("lesson_playback_progress"),
        )
        Text(
            text = frame.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp).testTag("lesson_frame_title"),
        )
        Text(
            text = frame.explanation,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp).testTag("lesson_frame_explanation"),
        )
    }
}

@Composable
private fun LessonPlaybackActions(
    playback: LessonPlayback,
    frameIndex: Int,
    onPreviousFrame: () -> Unit,
    onNextFrame: () -> Unit,
    onBackToList: () -> Unit,
    onStartPractice: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ReviewActions(
            reviewMoveCount = frameIndex,
            totalMoveCount = playback.frames.lastIndex,
            onPreviousMove = onPreviousFrame,
            onNextMove = onNextFrame,
            onExitReview = onBackToList,
            exitLabelResource = playback.backLabelResource,
            exitTestTag = "lesson_back_to_list_button",
        )
        if (frameIndex == playback.frames.lastIndex && onStartPractice != null) {
            Button(
                onClick = onStartPractice,
                modifier = Modifier.padding(top = 8.dp).testTag("lesson_start_practice_button"),
            ) {
                Text(stringResource(R.string.practice_mode))
            }
        }
    }
}

@Preview(name = "Pattern playback", showBackground = true)
@Composable
private fun PatternPlaybackPreview() {
    GobangptTheme {
        LessonPlaybackScreen(
            playback = patternLessons.first().toPlayback(),
            frameIndex = 0,
            onPreviousFrame = {},
            onNextFrame = {},
            onBackToList = {},
            onBackToGame = {},
        )
    }
}

@Preview(name = "Layout playback", showBackground = true)
@Composable
private fun LayoutPlaybackPreview() {
    GobangptTheme {
        LessonPlaybackScreen(
            playback = layoutLessons.first().toPlayback(),
            frameIndex = 2,
            onPreviousFrame = {},
            onNextFrame = {},
            onBackToList = {},
            onBackToGame = {},
            onStartPractice = {},
        )
    }
}
