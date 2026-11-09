package com.example.gobang_pt

const val BOARD_SIZE = 15

enum class Stone {
    BLACK,
    WHITE;

    val opponent: Stone
        get() = if (this == BLACK) WHITE else BLACK
}

data class Position(val row: Int, val column: Int) {
    val isValid: Boolean
        get() = row in 0 until BOARD_SIZE && column in 0 until BOARD_SIZE
}

data class Move(val position: Position, val stone: Stone)

data class GobangGame(
    val board: List<Stone?> = List(BOARD_SIZE * BOARD_SIZE) { null },
    val moves: List<Move> = emptyList(),
    val currentPlayer: Stone = Stone.BLACK,
    val winner: Stone? = null,
    val winningLine: List<Position> = emptyList(),
    val isDraw: Boolean = false,
) {
    val isOver: Boolean
        get() = winner != null || isDraw

    fun stoneAt(position: Position): Stone? {
        require(position.isValid) { "Position must be on the board" }
        return board[position.row * BOARD_SIZE + position.column]
    }

    fun play(position: Position): GobangGame {
        if (!position.isValid || isOver || stoneAt(position) != null) return this

        val updatedBoard = board.toMutableList().apply {
            this[position.row * BOARD_SIZE + position.column] = currentPlayer
        }
        val line = winningLine(updatedBoard, position, currentPlayer)
        val updatedMoves = moves + Move(position, currentPlayer)
        val hasWinner = line.size >= 5

        return copy(
            board = updatedBoard,
            moves = updatedMoves,
            currentPlayer = if (hasWinner) currentPlayer else currentPlayer.opponent,
            winner = if (hasWinner) currentPlayer else null,
            winningLine = if (hasWinner) line else emptyList(),
            isDraw = !hasWinner && updatedMoves.size == BOARD_SIZE * BOARD_SIZE,
        )
    }

    fun replayTo(moveCount: Int): GobangGame {
        require(moveCount in 0..moves.size) { "Move count must be within the game history" }
        return moves.take(moveCount).fold(GobangGame()) { game, move -> game.play(move.position) }
    }

    fun undo(): GobangGame {
        if (moves.isEmpty()) return this
        return replayTo(moves.lastIndex)
    }

    private fun winningLine(
        board: List<Stone?>,
        lastMove: Position,
        stone: Stone,
    ): List<Position> {
        val directions = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)
        return directions.map { (rowDelta, columnDelta) ->
            val before = connectedPositions(board, lastMove, stone, -rowDelta, -columnDelta).reversed()
            val after = connectedPositions(board, lastMove, stone, rowDelta, columnDelta)
            before + lastMove + after
        }.maxBy { it.size }
    }

    private fun connectedPositions(
        board: List<Stone?>,
        origin: Position,
        stone: Stone,
        rowDelta: Int,
        columnDelta: Int,
    ): List<Position> = buildList {
        var position = Position(origin.row + rowDelta, origin.column + columnDelta)
        while (position.isValid && board[position.row * BOARD_SIZE + position.column] == stone) {
            add(position)
            position = Position(position.row + rowDelta, position.column + columnDelta)
        }
    }
}
