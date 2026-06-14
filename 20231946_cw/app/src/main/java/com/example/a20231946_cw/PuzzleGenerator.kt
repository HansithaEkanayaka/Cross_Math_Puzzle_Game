package com.example.a20231946_cw

import kotlin.random.Random

//PuzzleGenerator handles the creation of CrossMath puzzles.
class PuzzleGenerator(private val random: Random = Random.Default) {

    //Generate a standard puzzle with exactly one blank per equation.
    fun generateStandard(targetEquationCount: Int = -1): GameState {
        val rows = random.nextInt(11, 21)// Randomly selects the number of rows between 11 and 20.
        val cols = random.nextInt(11, 21)// Randomly selects the number of columns between 11 and 20.
        return buildPuzzle(rows, cols, targetEquationCount, advancedMode = false)
    }

    //Generate an advanced puzzle - maximizes blank cells using constraint propagation.
    fun generateAdvanced(targetEquationCount: Int = -1): GameState {
        val rows = random.nextInt(11, 21)
        val cols = random.nextInt(11, 21)
        return buildPuzzle(rows, cols, targetEquationCount, advancedMode = true)
    }

    private fun buildPuzzle(
        rows: Int,
        cols: Int,
        targetEquationCount: Int,
        advancedMode: Boolean
    ): GameState {
        // create an empty Grid.
        val grid = Array(rows) { Array(cols) { GridCell(CellContent.Empty) } }
        val placedEquations = mutableListOf<Equation>()
        val maxAttempts = 400
        var placedCount = 0

        // Determine the number of equations to create (default is 5).
        val targetCount = if (targetEquationCount >0) targetEquationCount else 5
        var attempts = 0

        while (placedCount < targetCount && attempts < maxAttempts) {
            attempts++
            // Choose whether the equation is horizontal or vertical.
            val direction = if (random.nextBoolean()) Direction.HORIZONTAL else Direction.VERTICAL
            // Constrain the starting row and column so that the equation does not go outside the grid.
            val maxR = if (direction == Direction.HORIZONTAL) rows - 1 else rows - 5
            val maxC = if (direction == Direction.HORIZONTAL) cols - 5 else cols - 1

            if (maxR < 0 || maxC < 0) continue

            val startRow = random.nextInt(0, maxR + 1)
            val startCol = random.nextInt(0, maxC + 1)
            val cells = getCells(startRow, startCol, direction) // Getting the 5 boxes required for the equation.
            // Checks if those boxes are already used.
            if (!areCellsAvailable(grid, cells)) continue
            //Generates an equation
            val eq = generateEquation(startRow, startCol, direction) ?: continue
            // Inserts the equation into the Grid.
            writeEquationToGrid(grid, eq, cells)
            placedEquations.add(eq)
            placedCount++
        }
        // Advanced Mode creates more spaces, or randomly leaves one space each.
        val finalEquations = if (advancedMode) {
            maximiseBlanks(placedEquations)
        } else {
            placedEquations.onEach { it.blankIndices.add(random.nextInt(0, 3)) }
        }

        return GameState(
            grid = buildFinalImmutableGrid(rows, cols, finalEquations),
            equations = finalEquations,
            rows = rows,
            cols = cols
        )
    }
    // Returns the coordinates of 5 cells in an equation.
    private fun getCells(row: Int, col: Int, dir: Direction): List<Pair<Int, Int>> {
        return (0 until 5).map { i ->
            if (dir == Direction.HORIZONTAL) row to (col + i) else (row + i) to col
        }
    }
    // Checks if the selected cells already contain anything.
    private fun areCellsAvailable(grid: Array<Array<GridCell>>, cells: List<Pair<Int, Int>>): Boolean {
        return cells.all { (r, c) -> grid[r][c].content is CellContent.Empty }
    }
    // Creates a random mathematical equation.
    private fun generateEquation(row: Int, col: Int, dir: Direction): Equation? {
        val op = Operator.entries.random(random)// Chooses one of +, -, *, /.
        var a: Int; var b: Int; var res: Int

        when (op) {
            Operator.PLUS -> { a = random.nextInt(1, 50); b = random.nextInt(1, 50) ; res = a + b }
            Operator.MINUS -> { a = random.nextInt(10, 99); b = random.nextInt(1, a); res = a - b }
            Operator.TIMES -> { a = random.nextInt(2, 13); b = random.nextInt(2, 13); res = a * b }
            Operator.DIVIDE -> { b = random.nextInt(2, 12); res = random.nextInt(2, 13); a = b * res }
        }
        return Equation(row, col, dir, a, op, b, res, mutableListOf())
    }
    // The values of the created equation are written to the cells of the Grid.
    private fun writeEquationToGrid(grid: Array<Array<GridCell>>, eq: Equation, cells: List<Pair<Int, Int>>) {
        val contents = listOf(
            CellContent.Number(eq.a),
            CellContent.Op(eq.op),
            CellContent.Number(eq.b),
            CellContent.Equals,
            CellContent.Number(eq.result)
        )
        cells.forEachIndexed { i, (r, c) -> grid[r][c] = GridCell(contents[i]) }
    }
    // Attempts to increase the amount of blanks in an equation
    private fun maximiseBlanks(equations: List<Equation>): List<Equation> {
        val resultList = equations.map { it.copy(blankIndices = it.blankIndices.toMutableList()) }
        for (eq in resultList) {
            val possibleIndices = (0..2).shuffled(random)
            for (idx in possibleIndices) {
                eq.blankIndices.add(idx)
                // Checks if a blank is still solvable after adding it.
                if (!isSolvable(resultList)) {
                    eq.blankIndices.remove(idx)//Removes the blank if it is not solvable.
                }
            }
        }
        return resultList
    }
    // Checks if the puzzle can be solved using logic.
    private fun isSolvable(equations: List<Equation>): Boolean {
        val solvedCoords = mutableMapOf<Pair<Int, Int>, Int>()
        equations.forEach { eq ->
            listOf(eq.a, eq.b, eq.result).forEachIndexed { i, value ->
                if (i !in eq.blankIndices) {
                    solvedCoords[getTermCoord(eq, i)] = value
                }
            }
        }

        var changed = true
        while (changed) {
            changed = false
            for (eq in equations) {
                // An equation can only be solved if the number of unknown values is 1.
                val unknowns = (0..2).filter { i -> solvedCoords[getTermCoord(eq, i)] == null }
                if (unknowns.size == 1) {
                    val targetIdx = unknowns[0]
                    val a = solvedCoords[getTermCoord(eq, 0)]
                    val b = solvedCoords[getTermCoord(eq, 1)]
                    val r = solvedCoords[getTermCoord(eq, 2)]

                    val derived = when (targetIdx) {
                        0 -> if (b != null && r != null) solveForA(eq.op, b, r) else null
                        1 -> if (a != null && r != null) solveForB(eq.op, a, r) else null
                        2 -> if (a != null && b != null) solveForR(a, eq.op, b) else null
                        else -> null
                    }

                    if (derived != null) {
                        solvedCoords[getTermCoord(eq, targetIdx)] = derived
                        changed = true
                    }
                }
            }
        }
        return equations.all { eq ->
            eq.blankIndices.all { i -> solvedCoords[getTermCoord(eq, i)] != null }
        }
    }

    private fun getTermCoord(eq: Equation, termIdx: Int): Pair<Int, Int> {
        val step = when(termIdx) { 0 -> 0; 1 -> 2; else -> 4 }
        return if (eq.direction == Direction.HORIZONTAL) {
            eq.row to (eq.col + step)
        } else {
            (eq.row + step) to eq.col
        }
    }

    private fun solveForA(op: Operator, b: Int, r: Int) = when(op) {
        Operator.PLUS -> r - b; Operator.MINUS -> r + b
        Operator.TIMES -> if (r % b == 0) r / b else null
        Operator.DIVIDE -> r * b
    }

    private fun solveForB(op: Operator, a: Int, r: Int) = when(op) {
        Operator.PLUS -> r - a; Operator.MINUS -> a - r
        Operator.TIMES -> if (r % a == 0) r / a else null
        Operator.DIVIDE -> if (r != 0) a / r else null
    }

    private fun solveForR(a: Int, op: Operator, b: Int) = when(op) {
        Operator.PLUS -> a + b; Operator.MINUS -> a - b
        Operator.TIMES -> a * b; Operator.DIVIDE -> if (b != 0) a / b else null
    }

    private fun buildFinalImmutableGrid(r: Int, c: Int, equations: List<Equation>): List<List<GridCell>> {
        val workingGrid = Array(r) { Array(c) { GridCell(CellContent.Empty) } }
        equations.forEach { eq ->
            val cells = getCells(eq.row, eq.col, eq.direction)
            val contents = listOf(
                CellContent.Number(eq.a), CellContent.Op(eq.op),
                CellContent.Number(eq.b), CellContent.Equals, CellContent.Number(eq.result)
            )
            cells.forEachIndexed { i, (row, col) -> workingGrid[row][col] = GridCell(contents[i]) }
        }
        // Sets fields named empty to "user-fillable".
        equations.forEach { eq ->
            eq.blankIndices.forEach { bIdx ->
                val (row, col) = getTermCoord(eq, bIdx)
                workingGrid[row][col] = GridCell(CellContent.Blank, isUserFillable = true)
            }
        }
        return workingGrid.map { it.toList() }.toList()
    }
}
