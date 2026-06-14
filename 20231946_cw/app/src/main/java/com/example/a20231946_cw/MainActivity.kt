package com.example.a20231946_cw

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.listSaver
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import com.example.a20231946_cw.viewmodel.GameViewModel


// Video link: [https://drive.google.com/file/d/1OxoIH-6-OAnjByEBl4ot_PwMb-7JdS2w/view?usp=drive_link]
// Student: E.A.R.Hansitha Ekanayaka
// Student ID: 20231946
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}
// Define this at the top level of your package
sealed class Screen {
    object Home : Screen()
    data class Game(val advancedMode: Boolean) : Screen()
}
// Saver used to save a screen (so that the screen does not change even if the phone is rotated)
val ScreenSaver = listSaver<Screen, Any>(
    save = { screen ->
        when (screen) {
            is Screen.Home -> listOf("Home")
            is Screen.Game -> listOf("Game", screen.advancedMode)
        }
    },
    restore = { list ->
        val type = list[0] as String
        if (type == "Home") Screen.Home
        else Screen.Game(advancedMode = list[1] as Boolean)
    }
)
@Composable
fun AppNavigation() {
    // Remembering the current screen
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Home)
    }
    val viewModel: GameViewModel = viewModel()


    when (val screen = currentScreen) {
        is Screen.Home -> HomeScreen(
            onNewGame = { count ->
                viewModel.startNewGame(count, false)
                currentScreen = Screen.Game(advancedMode = false)
            },
            onAdvancedLevel = { count ->
                viewModel.startNewGame(count, true)
                currentScreen = Screen.Game(advancedMode = true)
            }
        )

        is Screen.Game -> GameScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.resetTimer()
                currentScreen = Screen.Home
            }
        )
    }
}

@Composable
fun HomeScreen(
    onNewGame: (Int) -> Unit,
    onAdvancedLevel: (Int) -> Unit
) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var equationCountText by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Title
            Text(text = "Cross Math \n Puzzle", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Equation count input
            Text(text = "Number of Equations", color = Color.Black, fontSize = 14.sp)

            OutlinedTextField(
                value = equationCountText,
                onValueChange = { equationCountText = it },
                placeholder = { Text("set the number of equations", color = Color(0xFF666666)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            // New Game button
            Button(
                onClick = {
                    val count = equationCountText.toIntOrNull() ?: 5
                    onNewGame(count)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "New Game", fontSize = 18.sp, color = Color(0xFF1A1A2E))
            }

            // Advanced Level button
            Button(
                onClick = {
                    val count = equationCountText.toIntOrNull() ?: -1
                    onAdvancedLevel(count)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Advanced Level", fontSize = 18.sp, color = Color.Black)
            }

            // About button
            OutlinedButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "About", fontSize = 18.sp, color = Color.Black)
            }
            // Dialog check
            if (showAboutDialog) { AboutPopup(onDismiss = { showAboutDialog = false })
            }
        }
    }
}
//A popup dialog displaying student information and the academic integrity statement.
@Composable
fun AboutPopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "About", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Text(
                text = "Student Name : E.A.R.Hansitha Ekanayaka\n" +
                        "Student ID: 20231946\n\n" +
                        "I confirm that I understand what plagiarism is and have read and " +
                        "understood the section on Assessment Offences in the Essential " +
                        "Information for Students. \n " +
                        "The work that I have submitted is entirely my own. Any work from " +
                        "other authors is duly referenced and acknowledged.",
                textAlign = TextAlign.Justify,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        },
        // Close button
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ok", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}