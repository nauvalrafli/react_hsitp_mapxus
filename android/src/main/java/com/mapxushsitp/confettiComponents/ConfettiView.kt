package com.mapxushsitp.confettiComponents

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class ConfettiSource {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

data class Confetti(var x: Float, var y: Float, val color: Color, var velocityY: Float = 0f, var velocityX: Float = 0f)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ConfettiView(source: ConfettiSource = ConfettiSource.CENTER, quantity: Int = 50) {
    var screenWidthPx by remember { mutableStateOf(0f) }
    var screenHeightPx by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val size = it.size
                screenWidthPx = size.width.toFloat()
                screenHeightPx = size.height.toFloat()
            }
    ) {
        if (screenWidthPx == 0f || screenHeightPx == 0f) return@BoxWithConstraints

        var confettiList by remember {
            mutableStateOf(List(quantity) {
                createConfetti(source, screenWidthPx, screenHeightPx)
            })
        }
        var isRunning by remember { mutableStateOf(true) }

        LaunchedEffect(isRunning) {
            while (isRunning) {
                confettiList = confettiList.map { confetti ->
                    val updatedY = confetti.y + confetti.velocityY
                    val updatedX = confetti.x + confetti.velocityX
                    val updatedVelocityY = confetti.velocityY + 0.5f
                    if (updatedY > screenHeightPx * 1.2f || updatedX < -100f || updatedX > screenWidthPx + 100f) {
                        createConfetti(source, screenWidthPx, screenHeightPx)
                    } else {
                        confetti.copy(x = updatedX, y = updatedY, velocityY = updatedVelocityY)
                    }
                }
                delay(16)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            confettiList.forEach { confetti ->
                drawCircle(
                    color = confetti.color,
                    radius = 8f,
                    center = Offset(confetti.x, confetti.y),
                    style = Fill
                )
            }
        }
    }
}


fun createConfetti(source: ConfettiSource, screenWidth: Float, screenHeight: Float): Confetti {
    val x = when (source) {
        ConfettiSource.LEFT -> -50f
        ConfettiSource.RIGHT -> screenWidth + 50f
        ConfettiSource.CENTER -> Random.nextFloat() * screenWidth
        else -> Random.nextFloat() * screenWidth// Top or Bottom defaults to random X
    }
    val y = when (source) {
        ConfettiSource.TOP -> -50f
        ConfettiSource.BOTTOM -> screenHeight + 50f
        ConfettiSource.CENTER -> Random.nextFloat() * screenHeight
        else -> if (source == ConfettiSource.LEFT || source == ConfettiSource.RIGHT) Random.nextFloat() * screenHeight else -50f //Left or Right defaults to random Y
    }
    val velocityX = when (source) {
        ConfettiSource.LEFT -> Random.nextFloat() * 10 + 5
        ConfettiSource.RIGHT -> Random.nextFloat() * -10 - 5
        else -> Random.nextFloat() * 20 - 10
    }
    val velocityY = when (source) {
        ConfettiSource.BOTTOM -> Random.nextFloat() * -10 - 5
        else -> Random.nextFloat() * 10 - 5
    }
    return Confetti(
        x = x,
        y = y,
        color = Color(Random.nextInt()),
        velocityY = velocityY,
        velocityX = velocityX
    )
}
