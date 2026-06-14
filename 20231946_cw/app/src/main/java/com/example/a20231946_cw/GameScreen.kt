package com.example.a20231946_cw

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.example.a20231946_cw.viewmodel.*

// Represents each operator used in equations
enum class Operator(val symbol: String) {
    PLUS("+"), MINUS("-"), TIMES("x"), DIVIDE("/") }

// Types of content a grid cell can hold
sealed class CellContent {
    data class Number(val value: Int) : CellContent()
    data class Op(val operator: Operator) : CellContent()
    object Equals : CellContent()
    object Empty : CellContent() // Black/unused cell
    object Blank : CellContent() // User-fillable cell
}

// Equation direction
enum class Direction { HORIZONTAL, VERTICAL }

// One equation in the puzzle: a op b = result
data class Equation(
    val row: Int,
    val col: Int,
    val direction: Direction,
    val a: Int,
    val op: Operator,
    val b: Int,
    val result: Int,
    // Which term is blank: 0 = a, 1 = b, 2 = result (basic), or -1 = none
    val blankIndices: MutableList<Int> = mutableListOf()
) {
    fun compute(aVal: Int, bVal: Int): Int = when (op) {
        Operator.PLUS -> aVal + bVal
        Operator.MINUS -> aVal - bVal
        Operator.TIMES -> aVal * bVal
        Operator.DIVIDE -> if (bVal != 0) aVal / bVal else Int.MIN_VALUE
    }
    fun isCorrect(aVal: Int, bVal: Int, resVal: Int): Boolean {
        return compute(aVal, bVal) == resVal
    }
}

// Represents a single cell in the grid
data class GridCell(
    val content: CellContent,
    val isUserFillable: Boolean = false,
    val userValue: Int? = null,         // value entered by user
    val highlightState: HighlightState = HighlightState.NONE
)

enum class HighlightState { NONE, GREEN, RED }

// Full game state
data class GameState(
    val grid: List<List<GridCell>>,
    val equations: List<Equation>,
    val score: Int = 0,
    val isComplete: Boolean = false,
    val rows: Int = grid.size,
    val cols: Int = if (grid.isNotEmpty()) grid[0].size else 0
)

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseTimer()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumeTimer()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // Handle Android back button
    BackHandler { onBack() }

    val state = viewModel.gameState
    var selectedCell by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var showInputDialog by rememberSaveable { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(verticalScrollState)
        ) {
            // Top bar: timer, title, score
            TopBar(
                timerEnabled = viewModel.timerEnabled,
                timerValue = viewModel.timerValue,
                score = viewModel.score,
                isGameOver = viewModel.isGameOver,
                onTimerToggle = { viewModel.toggleTimer(it) }
            )

            // Game over overlay
            if (viewModel.isGameOver) {
                GameOverBanner()
            }

            // Puzzle complete overlay
            if (viewModel.isPuzzleComplete) {
                PuzzleCompleteBanner(score = viewModel.score)
            }

            // Grid with scroll support for large grids
            if (state != null) {
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

               // Center the grid container in the remaining screen space
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // This is the "Box" representing the grid area
                    Surface(
                        modifier = Modifier.wrapContentSize().border(2.dp, Color.Gray, RoundedCornerShape(8.dp)), // Border for the box
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp) // Space between border and grid
                                .verticalScroll(verticalScrollState)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            PuzzleGrid(
                                gameState = state,
                                isInteractable = !viewModel.isGameOver && !viewModel.isPuzzleComplete,
                                onCellClick = { row, col ->
                                    val cell = state.grid[row][col]
                                    if (cell.isUserFillable) {
                                        selectedCell = row to col
                                        inputText = cell.userValue?.toString() ?: ""
                                        showInputDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Back to Home Button
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                    modifier = Modifier.height(50.dp).fillMaxWidth(0.6f)
                ) { Text(text = "Back to Home", color = Color.White, fontSize = 16.sp)
                }
            }

            // Number input dialog
            if (showInputDialog && selectedCell != null) {
                NumberInputDialog(
                    currentValue = inputText,
                    onDismiss = {
                        showInputDialog = false
                        selectedCell = null
                        inputText = ""
                    },
                    onConfirm = { number ->

                        selectedCell?.let { (row, col) ->
                            viewModel.enterNumber(row, col, number)
                        }
                        val targetCell = selectedCell
                        showInputDialog = false
                        selectedCell = null
                        inputText = ""
                        targetCell?.let { (row, col) ->
                            viewModel.enterNumber(row, col, number)
                        }
                    }
                )
            }

        }
    }
}
@Composable
fun TopBar(
    timerEnabled: Boolean,
    timerValue: Int,
    score: Int,
    isGameOver: Boolean,
    onTimerToggle: (Boolean) -> Unit
) {
    Surface(
        color = Color.Blue,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Timer
                Text(
                    text = if (timerEnabled) "⏱ $timerValue" else "",
                    color = if (timerValue <= 10) Color(0xFFFF4444) else Color(0xFF00D4FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.width(80.dp)
                )

                Text(
                    text = "Cross Math",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                text = "Score: $score",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }

    // Timer switch row
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (timerEnabled) "$timerValue-second timer" else "60-second timer",
            color = if (timerValue <= 10) Color.Red else Color.Black,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = timerEnabled,
            onCheckedChange = { onTimerToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Black,
                uncheckedThumbColor = Color.Black,
                uncheckedTrackColor = Color.White
            )
        )
    }
}

@Composable
fun GameOverBanner() {
    Box(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFCC0000)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "GAME OVER!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
    }
}
@Composable
fun PuzzleCompleteBanner(score: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF006600)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🎉 Puzzle Complete! Score: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PuzzleGrid(
    gameState: GameState,
    isInteractable: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    val cellSize = 42.dp // Slightly larger for better touch targets

    Column(
        modifier = Modifier.background(Color.White), // Grid line color
        verticalArrangement = Arrangement.spacedBy(1.dp) // Thickness of horizontal lines
    ) {
        gameState.grid.forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { // Thickness of vertical lines
                row.forEachIndexed { colIdx, cell ->
                    GridCellView(
                        cell = cell,
                        size = cellSize,
                        onClick = {
                            if (isInteractable && cell.isUserFillable) {
                                onCellClick(rowIdx, colIdx)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridCellView(
    cell: GridCell,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val bgColor = when {
        // Black cells (spacers)
        cell.content == CellContent.Empty -> Color.Black
        // Correct equations turn Green (as per your image description)
        cell.highlightState == HighlightState.GREEN -> Color(0xFF4CAF50)
        // Incorrect highlights
        cell.highlightState == HighlightState.RED -> Color(0xFFF44336)
        // All other gameplay cells are White
        else -> Color.White
    }

    val textColor = Color.Black

    val displayText = when {
        cell.content == CellContent.Empty -> ""
        cell.isUserFillable -> cell.userValue?.toString() ?: ""
        cell.content is CellContent.Number -> cell.content.value.toString()
        cell.content is CellContent.Op -> cell.content.operator.symbol
        cell.content == CellContent.Equals -> "="
        else -> ""
    }

    val modifier = Modifier.size(size).background(bgColor)
        // Add a thin black border to every cell to create the grid lines
        .border(0.5.dp, Color.Black)
        .then(
            if (cell.isUserFillable) Modifier.clickable { onClick() }
            else Modifier
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center
    ) {
        if (cell.content != CellContent.Empty) {
            Text(text = displayText, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun NumberInputDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(currentValue) }
    var errorMsg by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),

            ) {
                Text(text = "Enter a Number", fontSize = 18.sp,fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorMsg = ""
                    },
                    placeholder = {Text(text = "Enter a Number",color=Color(0xFF666666))},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    isError = errorMsg.isNotEmpty(),
                    supportingText = {
                        if (errorMsg.isNotEmpty()) Text(errorMsg, color = Color(0xFFFF6B6B))
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    // OK Button
                    Button(
                        onClick = {
                            val number = text.toIntOrNull()
                            if (number == null) {
                                errorMsg = "Please enter a valid number"
                            } else if (number <= 0) {
                                errorMsg = "Number must be positive"
                            } else {
                                onConfirm(number)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}