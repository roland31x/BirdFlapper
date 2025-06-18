import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.useResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.WindowState

import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GameState { START, PLAYING, GAME_OVER }

@Composable
@Preview
fun App() {
    val screenWidth = 800f
    val screenHeight = 800f

    var birdY by remember { mutableStateOf(300f) }
    var velocity by remember { mutableStateOf(0f) }
    val gravity = 0.25f
    val jumpForce = -8f

    var score by remember { mutableStateOf(0) }
    val pipeX = remember { mutableStateListOf(screenWidth) }
    val pipeGap = 200f
    val pipeWidth = 80f
    val pipeSpeed = 5f
    val pipeOffset = remember { mutableStateListOf(Random.nextInt(100, 600)) }
    val focusRequester = remember { FocusRequester() }
    val pipeCount = 3
    val pipeSpacing = 300f // distance between pipes
    val pipeStartX = 800f  // off-screen start

    var gameState by remember { mutableStateOf(GameState.START) }

    val birdImage = useResource("bird.png") {
        ImageIO.read(it).toComposeImageBitmap()
    }

    LaunchedEffect(gameState) {
        focusRequester.requestFocus()
        if (gameState == GameState.PLAYING) {
            birdY = 300f
            velocity = 0f
            score = 0
            for (i in 0 until pipeCount) {
                pipeX.add(pipeStartX + i * pipeSpacing)
                pipeOffset.add(Random.nextInt(100, 600))
            }

            while (gameState == GameState.PLAYING) {
                velocity += gravity
                birdY += velocity

                // Out of bounds
                if (birdY < 0f || birdY > screenHeight) {
                    gameState = GameState.GAME_OVER
                }

                for (i in pipeX.indices) {
                    pipeX[i] -= pipeSpeed

                    val pipeLeft = pipeX[i]
                    val pipeRight = pipeX[i] + pipeWidth
                    val birdLeft = 100f - 20f
                    val birdRight = 100f + 20f
                    val gapY = pipeOffset[i]

                    if (birdRight >= pipeLeft && birdLeft <= pipeRight) {
                        if (birdY < gapY || birdY > gapY + pipeGap) {
                            gameState = GameState.GAME_OVER
                        }
                    }

                    // When a pipe goes off screen, recycle it
                    if (pipeX[i] < -pipeWidth) {
                        pipeX[i] = pipeX.max() + pipeSpacing
                        pipeOffset[i] = Random.nextInt(100, 600)
                        score++
                    }
                }

                delay(16L)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Cyan)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                println("Key pressed: ${it.key}, type: ${it.type}") // add this for live output
                if (gameState == GameState.START &&
                    it.type == KeyEventType.KeyDown &&
                    it.key == Key.Enter
                ) {
                    gameState = GameState.PLAYING
                    true
                } else if (gameState == GameState.PLAYING &&
                    it.type == KeyEventType.KeyDown &&
                    it.key == Key.Spacebar
                ) {
                    velocity = jumpForce
                    true
                } else false
            }
    ) {
        // 🎨 Game graphics
        Canvas(Modifier.fillMaxSize()) {
            if (gameState == GameState.PLAYING) {
                with(drawContext.canvas.nativeCanvas) {
                    val saveCount = save()
                    translate(100f, birdY)
                    rotate(velocity.coerceIn(-15f, 15f) * 10f)
                    drawImage(
                        birdImage,
                        dstSize = IntSize(40, 40),
                        dstOffset = IntOffset(-20, -20)
                    )
                    restoreToCount(saveCount)
                }

                for (i in pipeX.indices) {
                    val pipeLeft = pipeX[i]
                    val gapY = pipeOffset[i].toFloat()
                    val pipeHeightTop = gapY
                    val pipeHeightBottom = 800 - (gapY + pipeGap)

                    // Create a vertical gradient from light to dark green
                    val greenGradient = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF4CAF50),
                            0.2f to Color(0xFFE8F5E9),
                            0.6f to Color(0xFF4CAF50),
                            1.0f to Color(0xFF087F23),
                        ),
                        startX = pipeLeft,
                        endX = pipeLeft + pipeWidth
                    )

                    // Draw top pipe (rectangle with gradient fill)
                    drawRect(
                        brush = greenGradient,
                        topLeft = Offset(pipeX[i], 0f),
                        size = androidx.compose.ui.geometry.Size(pipeWidth, pipeHeightTop)
                    )
                    // Draw black border for top pipe
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(pipeX[i], 0f),
                        size = androidx.compose.ui.geometry.Size(pipeWidth, pipeHeightTop),
                        style = Stroke(width = 4f)
                    )

                    // Draw bottom pipe
                    drawRect(
                        brush = greenGradient,
                        topLeft = Offset(pipeX[i], gapY + pipeGap),
                        size = androidx.compose.ui.geometry.Size(pipeWidth, pipeHeightBottom)
                    )
                    // Draw black border for bottom pipe
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(pipeX[i], gapY + pipeGap),
                        size = androidx.compose.ui.geometry.Size(pipeWidth, pipeHeightBottom),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        // 🧩 UI overlay (on top of Canvas)
        when (gameState) {
            GameState.START -> {
                Text(
                    "Press ENTER to Start",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            GameState.GAME_OVER -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.White)
                        .border(2.dp, Color.Transparent, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Game Over", fontSize = 32.sp, color = Color.Red)
                    Spacer(Modifier.height(8.dp))
                    Text("Score: $score", fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        gameState = GameState.START
                    }) {
                        Text("Restart")
                    }
                }
            }

            GameState.PLAYING -> {
                Text(
                    "Score: $score",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication,
        title = "Flappy Bird in Kotlin",
        icon = painterResource("bird.png"),
        resizable = false,
        state = WindowState(width = 800.dp, height = 800.dp)
    ) {
        App()
    }
}