package com.example.a20231946_cw.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a20231946_cw.CellContent
import com.example.a20231946_cw.Direction
import com.example.a20231946_cw.Equation
import com.example.a20231946_cw.GameState
import com.example.a20231946_cw.GridCell
import com.example.a20231946_cw.HighlightState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val puzzleGenerator = com.example.a20231946_cw.PuzzleGenerator()

    var gameState by mutableStateOf<GameState?>(null)
        private set

    var score by mutableStateOf(0)
        private set

    var isGameOver by mutableStateOf(false)
        private set

    var isPuzzleComplete by mutableStateOf(false)
        private set

    // Timer state
    var timerEnabled by mutableStateOf(false)
        private set

    var timerValue by mutableStateOf(60)
        private set

    var timerRunning by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    // Current mutable grid (copied from gameState for mutation)
    private var mutableGrid: MutableList<MutableList<GridCell>> = mutableListOf()

    fun startNewGame(targetEquationCount: Int = 5, advancedMode: Boolean = false) {
        resetTimer()
        isGameOver = false
        isPuzzleComplete = false
        score = 0

        // Use the instance 'puzzleGenerator' instead of the Class name
        val state = if (advancedMode)
            puzzleGenerator.generateAdvanced(targetEquationCount)
        else
            puzzleGenerator.generateStandard(targetEquationCount)

        mutableGrid = state.grid.map { row -> row.toMutableList() }.toMutableList()
        gameState = state
    }

    fun enterNumber(row: Int, col: Int, value: Int) {
        val currentState = gameState ?: return
        if (row !in currentState.grid.indices || col !in currentState.grid[0].indices) return
        if (isGameOver || isPuzzleComplete) return
        val state = gameState ?: return
        val cell = mutableGrid[row][col]
        if (!cell.isUserFillable) return

        // Update the cell with user value
        mutableGrid[row][col] = cell.copy(userValue = value)

        val updatedGrid = currentState.grid.mapIndexed { rIdx, gridRow ->
            gridRow.mapIndexed { cIdx, cell ->
                if (rIdx == row && cIdx == col) {
                    // Use .copy() to create a new cell instance
                    cell.copy(userValue = value)
                } else {
                    cell
                }
            }
        }
        gameState = currentState.copy(grid = updatedGrid)
        // Re-evaluate all equations and score
        recalculate(state)
    }

    private fun recalculate(state: GameState) {
        // Reset highlights...

        var newScore = 0
        state.equations.forEach { eq ->
            val cells = getEquationCells(eq)
            val aVal = getCellValue(cells[0].first, cells[0].second)
            val bVal = getCellValue(cells[2].first, cells[2].second)
            val rVal = getCellValue(cells[4].first, cells[4].second)

            if (aVal != null && bVal != null && rVal != null) {
                val isCorrect = eq.isCorrect(aVal, bVal, rVal)
                if (isCorrect) newScore++

                val highlight = if (isCorrect) HighlightState.GREEN else HighlightState.RED
                cells.forEach { (r, c) ->
                    if (mutableGrid[r][c].content != CellContent.Empty) {
                        mutableGrid[r][c] = mutableGrid[r][c].copy(highlightState = highlight)
                    }
                }
            }
        }

        state.equations.forEach { eq ->
            val termCellIndices = listOf(0, 2, 4)
            val cells = getEquationCells(eq)

            // Get current values for a, b, result
            val aCell = cells[0]
            val bCell = cells[2]
            val rCell = cells[4]

            val aVal = getCellValue(aCell.first, aCell.second)
            val bVal = getCellValue(bCell.first, bCell.second)
            val rVal = getCellValue(rCell.first, rCell.second)

            // Check if all terms are filled
            if (aVal == null || bVal == null || rVal == null) return@forEach

            // Evaluate
            val correct = eq.isCorrect(aVal, bVal, rVal)
            val highlight = if (correct) HighlightState.GREEN else HighlightState.RED

            if (correct) newScore++

            // Apply highlight to all 5 cells of the equation
            cells.forEach { (r, c) ->
                val cell = mutableGrid[r][c]
                // Only update highlight if this cell is not empty background
                if (cell.content != CellContent.Empty) {
                    mutableGrid[r][c] = cell.copy(highlightState = highlight)
                }
            }
        }

        score = newScore

        // Update the game state with new grid snapshot
        gameState = state.copy(
            grid = mutableGrid.map { row -> row.toList() },
            score = newScore
        )

        // Check puzzle completion
        val allCorrect = state.equations.all { eq ->
            val cells = getEquationCells(eq)
            val aVal = getCellValue(cells[0].first, cells[0].second)
            val bVal = getCellValue(cells[2].first, cells[2].second)
            val rVal = getCellValue(cells[4].first, cells[4].second)
            aVal != null && bVal != null && rVal != null && eq.isCorrect(aVal, bVal, rVal)
        }
        isPuzzleComplete = allCorrect
        //Stop timer if won
        if (allCorrect) {
            stopTimer()
        }
    }

    private fun getCellValue(row: Int, col: Int): Int? {
        val cell = mutableGrid[row][col]
        return when {
            cell.isUserFillable -> cell.userValue
            cell.content is CellContent.Number -> cell.content.value
            else -> null
        }
    }

    private fun getEquationCells(eq: Equation): List<Pair<Int, Int>> {
        return (0 until 5).map{i ->
        if (eq.direction == Direction.HORIZONTAL) {
            eq.row to (eq.col+i)
        } else {
            (eq.row + i ) to eq.col
        }
        }
    }
    // Task 10: Timer
    fun toggleTimer(enabled: Boolean) {
        timerEnabled = enabled
        if (enabled) {
            startTimer()
        } else {
            stopTimer()
            timerValue = 60
        }
    }

    private fun startTimer() {
        if (timerRunning) return
        timerRunning = true
        timerValue = 60
        timerJob = viewModelScope.launch {
            while (timerValue > 0 && timerEnabled) {
                delay(1000L)
                timerValue--
            }
            if (timerValue <= 0) {
                isGameOver = true
                timerRunning = false
            }
        }
    }
    fun pauseTimer() {
        timerJob?.cancel()
        timerRunning = false
    }

    fun resumeTimer() {
        if (timerEnabled && !isGameOver && !isPuzzleComplete && !timerRunning) {
            // Start from current timerValue instead of resetting to 60
            timerRunning = true
            timerJob = viewModelScope.launch {
                while (timerValue > 0 && timerEnabled) {
                    delay(1000L)
                    timerValue--
                }
                if (timerValue <= 0) {
                    isGameOver = true
                    timerRunning = false
                }
            }
        }
    }
    private fun stopTimer() {
        timerJob?.cancel()
        timerRunning = false
    }

    fun resetTimer() {
        stopTimer()
        timerEnabled = false
        timerValue = 60
        timerRunning = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}