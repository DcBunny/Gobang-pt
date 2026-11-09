package com.example.gobang_pt

import kotlin.math.abs

enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD,
}

object GobangAi {
    private const val WIN_SCORE = 100_000_000
    private val directions = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    fun chooseMove(
        game: GobangGame,
        difficulty: AiDifficulty,
    ): Position? {
        if (game.isOver) return null
        val candidates = candidatePositions(game.board)
        if (candidates.isEmpty()) return null

        val winningMove = candidates.firstOrNull {
            isWinningMove(game.board, it, game.currentPlayer)
        }
        if (winningMove != null) return winningMove

        return when (difficulty) {
            AiDifficulty.EASY -> chooseHeuristicMove(game.board, candidates, game.currentPlayer)
            AiDifficulty.MEDIUM -> chooseSearchMove(
                board = game.board,
                candidates = candidates,
                stone = game.currentPlayer,
                rootCandidateLimit = 10,
                searchDepth = 2,
                branchLimit = 8,
            )
            AiDifficulty.HARD -> chooseSearchMove(
                board = game.board,
                candidates = candidates,
                stone = game.currentPlayer,
                rootCandidateLimit = 12,
                searchDepth = 3,
                branchLimit = 10,
            )
        }
    }

    private fun chooseHeuristicMove(
        board: List<Stone?>,
        candidates: List<Position>,
        stone: Stone,
    ): Position {
        blockingMove(board, candidates, stone)?.let { return it }
        return candidates.maxBy { position ->
            moveScore(board, position, stone) * 10 +
                moveScore(board, position, stone.opponent) * 9
        }
    }

    private fun chooseSearchMove(
        board: List<Stone?>,
        candidates: List<Position>,
        stone: Stone,
        rootCandidateLimit: Int,
        searchDepth: Int,
        branchLimit: Int,
    ): Position {
        blockingMove(board, candidates, stone)?.let { return it }

        val rankedCandidates = rankCandidates(board, candidates, stone).take(rootCandidateLimit)
        val mutableBoard = board.toMutableList()
        var bestPosition = rankedCandidates.first()
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE

        rankedCandidates.forEach { position ->
            mutableBoard[position.index] = stone
            val score = minimax(
                board = mutableBoard,
                currentStone = stone.opponent,
                aiStone = stone,
                depth = searchDepth,
                branchLimit = branchLimit,
                alpha = alpha,
                beta = Int.MAX_VALUE,
                lastMove = position,
                lastStone = stone,
            )
            mutableBoard[position.index] = null
            if (score > bestScore) {
                bestScore = score
                bestPosition = position
            }
            alpha = maxOf(alpha, bestScore)
        }
        return bestPosition
    }

    private fun minimax(
        board: MutableList<Stone?>,
        currentStone: Stone,
        aiStone: Stone,
        depth: Int,
        branchLimit: Int,
        alpha: Int,
        beta: Int,
        lastMove: Position,
        lastStone: Stone,
    ): Int {
        if (isWinningMove(board, lastMove, lastStone)) {
            return if (lastStone == aiStone) WIN_SCORE + depth else -WIN_SCORE - depth
        }
        if (depth == 0) return evaluateBoard(board, aiStone)

        val candidates = candidatePositions(board)
        if (candidates.isEmpty()) return 0
        val rankedCandidates = rankCandidates(board, candidates, currentStone).take(branchLimit)
        val maximizing = currentStone == aiStone
        var bestScore = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var currentAlpha = alpha
        var currentBeta = beta

        rankedCandidates.forEach { position ->
            board[position.index] = currentStone
            val score = minimax(
                board = board,
                currentStone = currentStone.opponent,
                aiStone = aiStone,
                depth = depth - 1,
                branchLimit = branchLimit,
                alpha = currentAlpha,
                beta = currentBeta,
                lastMove = position,
                lastStone = currentStone,
            )
            board[position.index] = null

            if (maximizing) {
                bestScore = maxOf(bestScore, score)
                currentAlpha = maxOf(currentAlpha, bestScore)
            } else {
                bestScore = minOf(bestScore, score)
                currentBeta = minOf(currentBeta, bestScore)
            }
            if (currentBeta <= currentAlpha) return bestScore
        }
        return bestScore
    }

    private fun blockingMove(
        board: List<Stone?>,
        candidates: List<Position>,
        stone: Stone,
    ): Position? = candidates.firstOrNull { isWinningMove(board, it, stone.opponent) }

    private fun rankCandidates(
        board: List<Stone?>,
        candidates: List<Position>,
        stone: Stone,
    ): List<Position> = candidates.sortedByDescending { position ->
        moveScore(board, position, stone) * 10 +
            moveScore(board, position, stone.opponent) * 9 +
            centerBonus(position)
    }

    private fun candidatePositions(board: List<Stone?>): List<Position> {
        if (board.all { it == null }) return listOf(Position(BOARD_SIZE / 2, BOARD_SIZE / 2))

        val candidates = linkedSetOf<Position>()
        board.forEachIndexed { index, stone ->
            if (stone == null) return@forEachIndexed
            val row = index / BOARD_SIZE
            val column = index % BOARD_SIZE
            for (rowOffset in -2..2) {
                for (columnOffset in -2..2) {
                    val position = Position(row + rowOffset, column + columnOffset)
                    if (position.isValid && board[position.index] == null) candidates.add(position)
                }
            }
        }
        if (candidates.isEmpty()) {
            board.forEachIndexed { index, stone ->
                if (stone == null) candidates.add(Position(index / BOARD_SIZE, index % BOARD_SIZE))
            }
        }
        return candidates.toList()
    }

    private fun moveScore(board: List<Stone?>, position: Position, stone: Stone): Int {
        if (board[position.index] != null) return Int.MIN_VALUE
        return directions.sumOf { (rowDelta, columnDelta) ->
            val forward = connectedCount(board, position, stone, rowDelta, columnDelta)
            val backward = connectedCount(board, position, stone, -rowDelta, -columnDelta)
            val count = 1 + forward.count + backward.count
            val openEnds = listOf(forward.end, backward.end).count { end ->
                end.isValid && board[end.index] == null
            }
            patternScore(count, openEnds)
        } + centerBonus(position)
    }

    private fun evaluateBoard(board: List<Stone?>, aiStone: Stone): Int {
        var score = 0
        board.forEachIndexed { index, stone ->
            if (stone == null) return@forEachIndexed
            val position = Position(index / BOARD_SIZE, index % BOARD_SIZE)
            val stoneScore = directions.sumOf { (rowDelta, columnDelta) ->
                val previous = Position(position.row - rowDelta, position.column - columnDelta)
                if (previous.isValid && board[previous.index] == stone) {
                    0
                } else {
                    val run = connectedCount(board, position, stone, rowDelta, columnDelta, includeOrigin = true)
                    val beforeOpen = previous.isValid && board[previous.index] == null
                    val afterOpen = run.end.isValid && board[run.end.index] == null
                    patternScore(run.count, listOf(beforeOpen, afterOpen).count { it })
                }
            }
            score += if (stone == aiStone) stoneScore else -(stoneScore * 11 / 10)
        }
        return score
    }

    private data class Run(val count: Int, val end: Position)

    private fun connectedCount(
        board: List<Stone?>,
        origin: Position,
        stone: Stone,
        rowDelta: Int,
        columnDelta: Int,
        includeOrigin: Boolean = false,
    ): Run {
        var count = if (includeOrigin) 1 else 0
        val start = if (includeOrigin) 1 else 1
        var step = start
        var position = Position(origin.row + rowDelta * step, origin.column + columnDelta * step)
        while (position.isValid && board[position.index] == stone) {
            count++
            step++
            position = Position(origin.row + rowDelta * step, origin.column + columnDelta * step)
        }
        return Run(count, position)
    }

    private fun patternScore(count: Int, openEnds: Int): Int = when {
        count >= 5 -> 10_000_000
        count == 4 && openEnds == 2 -> 1_000_000
        count == 4 && openEnds == 1 -> 100_000
        count == 3 && openEnds == 2 -> 50_000
        count == 3 && openEnds == 1 -> 5_000
        count == 2 && openEnds == 2 -> 1_000
        count == 2 && openEnds == 1 -> 100
        count == 1 && openEnds == 2 -> 10
        else -> 0
    }

    private fun centerBonus(position: Position): Int {
        val center = BOARD_SIZE / 2
        return BOARD_SIZE - abs(position.row - center) - abs(position.column - center)
    }

    private fun isWinningMove(board: List<Stone?>, position: Position, stone: Stone): Boolean {
        if (position.isValid.not() || board[position.index] != stone && board[position.index] != null) return false
        return directions.any { (rowDelta, columnDelta) ->
            val count = 1 +
                connectedCount(board, position, stone, rowDelta, columnDelta).count +
                connectedCount(board, position, stone, -rowDelta, -columnDelta).count
            count >= 5
        }
    }

    private val Position.index: Int
        get() = row * BOARD_SIZE + column
}
