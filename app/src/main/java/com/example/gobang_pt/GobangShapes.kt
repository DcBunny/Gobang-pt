package com.example.gobang_pt

internal data class DirectionPattern(
    val count: Int,
    val openEnds: Int,
    val winningPoints: Int,
    val hasOpenThree: Boolean,
    val hasThree: Boolean,
)

internal object GobangShapes {
    private const val LINE_RADIUS = 5
    private val openThreeMasks = intArrayOf(0b001110, 0b010110, 0b011010)
    val directions = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    fun analyzeMove(
        board: List<Stone?>,
        position: Position,
        stone: Stone,
    ): List<DirectionPattern> = directions.map { (rowDelta, columnDelta) ->
        analyze(board, position, stone, rowDelta, columnDelta)
    }

    fun analyze(
        board: List<Stone?>,
        position: Position,
        stone: Stone,
        rowDelta: Int,
        columnDelta: Int,
    ): DirectionPattern {
        val forward = connectedCount(board, position, stone, rowDelta, columnDelta)
        val backward = connectedCount(board, position, stone, -rowDelta, -columnDelta)
        val count = 1 + forward.count + backward.count
        val openEnds = listOf(forward.end, backward.end).count { end ->
            end.isValid && board[end.index] == null
        }
        var ownBits = 0
        var emptyBits = 0
        var validBits = 0

        for (lineIndex in 0..LINE_RADIUS * 2) {
            val offset = lineIndex - LINE_RADIUS
            val current = Position(
                position.row + rowDelta * offset,
                position.column + columnDelta * offset,
            )
            if (!current.isValid) continue
            val bit = 1 shl lineIndex
            validBits = validBits or bit
            val cell = if (offset == 0) stone else board[current.index]
            if (cell == stone) ownBits = ownBits or bit
            if (cell == null) emptyBits = emptyBits or bit
        }

        var winningPointBits = 0
        var hasThree = false
        for (start in 1..LINE_RADIUS) {
            val windowMask = 0b11111 shl start
            if (validBits and windowMask != windowMask) continue
            val ownCount = (ownBits and windowMask).countOneBits()
            val emptyCount = (emptyBits and windowMask).countOneBits()
            if (ownCount == 4 && emptyCount == 1) {
                winningPointBits = winningPointBits or (emptyBits and windowMask)
            }
            if (ownCount == 3 && emptyCount == 2) hasThree = true
        }

        val hasOpenThree = winningPointBits == 0 && (0..LINE_RADIUS).any { start ->
            val windowMask = 0b111111 shl start
            if (validBits and windowMask != windowMask) return@any false
            val occupiedMask = (ownBits or emptyBits) and windowMask
            occupiedMask == windowMask && openThreeMasks.any { mask ->
                ownBits and windowMask == mask shl start
            }
        }

        return DirectionPattern(
            count = count,
            openEnds = openEnds,
            winningPoints = winningPointBits.countOneBits(),
            hasOpenThree = hasOpenThree,
            hasThree = hasThree,
        )
    }

    fun immediateWinningMoves(board: List<Stone?>, stone: Stone): List<Position> =
        List(BOARD_SIZE * BOARD_SIZE) { index ->
            Position(index / BOARD_SIZE, index % BOARD_SIZE)
        }.filter { position ->
            board[position.row * BOARD_SIZE + position.column] == null &&
                analyzeMove(board, position, stone).any { it.count >= 5 }
        }

    private data class Run(val count: Int, val end: Position)

    private fun connectedCount(
        board: List<Stone?>,
        origin: Position,
        stone: Stone,
        rowDelta: Int,
        columnDelta: Int,
    ): Run {
        var count = 0
        var step = 1
        var position = Position(origin.row + rowDelta * step, origin.column + columnDelta * step)
        while (position.isValid && board[position.index] == stone) {
            count++
            step++
            position = Position(origin.row + rowDelta * step, origin.column + columnDelta * step)
        }
        return Run(count, position)
    }

    private val Position.index: Int
        get() = row * BOARD_SIZE + column
}
