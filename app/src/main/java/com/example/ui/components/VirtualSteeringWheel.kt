package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlType
import com.example.ui.theme.*
import kotlin.math.atan2

@Composable
fun VirtualSteeringControl(
    controlType: ControlType,
    steerValue: Float,
    onSteerChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    when (controlType) {
        ControlType.VIRTUAL_WHEEL -> {
            var currentAngle by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(Color(0x3310141E))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                val touch = change.position
                                val angle = atan2(touch.x - center.x, center.y - touch.y)
                                val clampedSteer = (angle / 1.1f).coerceIn(-1f, 1f)
                                currentAngle = clampedSteer * 75f
                                onSteerChange(clampedSteer)
                            },
                            onDragEnd = {
                                currentAngle = 0f
                                onSteerChange(0f)
                            },
                            onDragCancel = {
                                currentAngle = 0f
                                onSteerChange(0f)
                            }
                        )
                    }
                    .testTag("virtual_steering_wheel"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.44f
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)

                    // Outer carbon rim
                    drawCircle(
                        color = Color(0xFF222838),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 16.dp.toPx())
                    )

                    // Top center red marker (racing stripe)
                    drawArc(
                        color = ApexRed,
                        startAngle = 260f + currentAngle,
                        sweepAngle = 20f,
                        useCenter = false,
                        style = Stroke(width = 18.dp.toPx())
                    )

                    // Central spoke hub
                    drawCircle(
                        color = Color(0xFF151922),
                        radius = radius * 0.45f,
                        center = center
                    )
                }

                Text(
                    text = "APEX",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        ControlType.LEFT_RIGHT_BUTTONS, ControlType.TILT_SENSOR -> {
            Row(
                modifier = modifier.width(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steer Left Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (steerValue < -0.2f) ApexCyan.copy(alpha = 0.4f) else Color(0x66181D2A))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onSteerChange(-0.85f)
                                    tryAwaitRelease()
                                    onSteerChange(0f)
                                }
                            )
                        }
                        .testTag("steer_left_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◀", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }

                // Steer Right Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (steerValue > 0.2f) ApexCyan.copy(alpha = 0.4f) else Color(0x66181D2A))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onSteerChange(0.85f)
                                    tryAwaitRelease()
                                    onSteerChange(0f)
                                }
                            )
                        }
                        .testTag("steer_right_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PedalControls(
    throttle: Float,
    brake: Float,
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Progressive Brake Pedal
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (brake > 0.1f) ApexRed else Color(0xFF2C1618),
                            Color(0xFF180A0B)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val ratio = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            onBrakeChange(ratio.coerceAtLeast(0.5f))
                        },
                        onDrag = { change, _ ->
                            val ratio = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            onBrakeChange(ratio)
                        },
                        onDragEnd = { onBrakeChange(0f) },
                        onDragCancel = { onBrakeChange(0f) }
                    )
                }
                .testTag("brake_pedal"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BRAKE",
                    color = if (brake > 0.1f) Color.White else ApexRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(brake * 100).toInt()}%",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Progressive Throttle Pedal
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (throttle > 0.1f) ApexGreen else Color(0xFF10281E),
                            Color(0xFF081810)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val ratio = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            onThrottleChange(ratio.coerceAtLeast(0.6f))
                        },
                        onDrag = { change, _ ->
                            val ratio = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            onThrottleChange(ratio)
                        },
                        onDragEnd = { onThrottleChange(0f) },
                        onDragCancel = { onThrottleChange(0f) }
                    )
                }
                .testTag("throttle_pedal"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "THROTTLE",
                    color = if (throttle > 0.1f) Color.White else ApexGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(throttle * 100).toInt()}%",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
