package com.senku.headshotai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senku.headshotai.domain.model.TrainingMode
import com.senku.headshotai.domain.model.TrainingSessionSummary
import com.senku.headshotai.domain.physics.HeadshotPhysicsEngine
import com.senku.headshotai.domain.physics.ShotOutcome
import com.senku.headshotai.ui.components.MetricPill
import com.senku.headshotai.ui.components.SectionHeader
import com.senku.headshotai.ui.components.SenkuCharcoalCard
import com.senku.headshotai.ui.theme.SenkuCardBorder
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuElectricBlue
import com.senku.headshotai.ui.theme.SenkuEmeraldSuccess
import com.senku.headshotai.ui.theme.SenkuHeadshotRed
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuUiState
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun TrainingScreen(
    state: SenkuUiState,
    onRecordSession: (TrainingSessionSummary) -> Unit,
    onClearHistory: () -> Unit
) {
    val density = LocalDensity.current.density
    var selectedMode by remember { mutableStateOf(TrainingMode.DRAG_HEADSHOT) }
    var isRunning by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(selectedMode.durationSeconds) }

    // Target coordinates normalized (0.15..0.85)
    var targetNormX by remember { mutableFloatStateOf(0.5f) }
    var targetNormY by remember { mutableFloatStateOf(0.36f) }
    var targetVelX by remember { mutableFloatStateOf(0.0065f) }
    var targetSpawnTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Drag gesture tracking
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragCurrentOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartTimeMs by remember { mutableLongStateOf(0L) }
    var lastImpactOffset by remember { mutableStateOf<Offset?>(null) }
    var lastOutcome by remember { mutableStateOf<ShotOutcome?>(null) }
    var liveFeedback by remember {
        mutableStateOf("Swipe UP from the red Fire Button toward the enemy head hitbox!")
    }

    // Session metrics
    var score by remember { mutableIntStateOf(0) }
    var totalShots by remember { mutableIntStateOf(0) }
    var headshots by remember { mutableIntStateOf(0) }
    var bodyShots by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    val reactionTimes = remember { mutableStateListOf<Long>() }
    val dragVelocities = remember { mutableStateListOf<Float>() }
    val dragAngles = remember { mutableStateListOf<Float>() }

    fun resetSessionStats(mode: TrainingMode) {
        remainingSeconds = mode.durationSeconds
        score = 0
        totalShots = 0
        headshots = 0
        bodyShots = 0
        misses = 0
        reactionTimes.clear()
        dragVelocities.clear()
        dragAngles.clear()
        lastImpactOffset = null
        lastOutcome = null
        targetNormX = 0.5f
        targetNormY = 0.35f
        targetSpawnTimestamp = System.currentTimeMillis()
    }

    // 60FPS target movement & countdown loop
    LaunchedEffect(isRunning, selectedMode, state.targetSpeedMultiplier) {
        if (!isRunning) return@LaunchedEffect
        val startMs = System.currentTimeMillis()
        var lastTickMs = startMs
        while (isRunning) {
            val now = System.currentTimeMillis()
            val dtSec = ((now - lastTickMs).coerceAtLeast(1L)) / 1000f
            lastTickMs = now

            if (selectedMode != TrainingMode.REACTION_BLITZ) {
                val speedFactor = if (selectedMode == TrainingMode.STRAFE_TRACKING) 1.55f else 1.0f
                targetNormX += targetVelX * speedFactor * state.targetSpeedMultiplier * (dtSec * 60f)
                if (targetNormX > 0.82f) {
                    targetNormX = 0.82f
                    targetVelX = -kotlin.math.abs(targetVelX)
                } else if (targetNormX < 0.18f) {
                    targetNormX = 0.18f
                    targetVelX = kotlin.math.abs(targetVelX)
                }
            }

            val elapsedSec = ((now - startMs) / 1000L).toInt()
            val left = (selectedMode.durationSeconds - elapsedSec).coerceAtLeast(0)
            remainingSeconds = left

            if (left <= 0) {
                isRunning = false
                if (totalShots > 0) {
                    val hsRate = (headshots.toFloat() / totalShots.toFloat()) * 100f
                    val avgReact = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 320L
                    val bestReact = reactionTimes.minOrNull() ?: avgReact
                    val avgVel = if (dragVelocities.isNotEmpty()) dragVelocities.average().toFloat() else 1350f
                    val avgAng = if (dragAngles.isNotEmpty()) dragAngles.average().toFloat() else 85f
                    onRecordSession(
                        TrainingSessionSummary(
                            mode = selectedMode,
                            score = score,
                            totalShots = totalShots,
                            headshots = headshots,
                            bodyShots = bodyShots,
                            misses = misses,
                            headshotRatePercent = hsRate,
                            avgReactionTimeMs = avgReact,
                            bestReactionTimeMs = bestReact,
                            avgDragVelocityDpPerSec = avgVel,
                            avgDragAngleDeg = avgAng
                        )
                    )
                }
            }
            delay(16L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Standalone Headshot Training Arena",
                    subtitle = selectedMode.subtitle,
                    badgeText = if (isRunning) "${remainingSeconds}s LEFT" else "READY",
                    badgeColor = if (isRunning) SenkuHeadshotRed else SenkuCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrainingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                if (!isRunning) {
                                    selectedMode = mode
                                    resetSessionStats(mode)
                                }
                            },
                            label = { Text(mode.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SenkuHeadshotRed.copy(alpha = 0.22f),
                                selectedLabelColor = SenkuHeadshotRed
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("Score", "$score", SenkuCyan, Modifier.weight(1f))
                    val hsPct = if (totalShots > 0) ((headshots * 100f) / totalShots).roundToInt() else 0
                    MetricPill("Headshots", "$headshots/$totalShots ($hsPct%)", SenkuHeadshotRed, Modifier.weight(1f))
                    val bestMs = reactionTimes.minOrNull() ?: 0L
                    MetricPill("Best React", if (bestMs > 0) "${bestMs}ms" else "--", SenkuEmeraldSuccess, Modifier.weight(1f))
                }
            }
        }

        // Interactive Combat Range Canvas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF090E17))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedMode, isRunning, state.activeSensitivity) {
                            if (selectedMode == TrainingMode.REACTION_BLITZ) {
                                detectTapGestures { tapOffset ->
                                    if (!isRunning) return@detectTapGestures
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val tx = targetNormX * w
                                    val ty = targetNormY * h
                                    val headRadius = 26f * density
                                    val dist = hypot(tapOffset.x - tx, tapOffset.y - ty)
                                    totalShots++
                                    val reactMs = (System.currentTimeMillis() - targetSpawnTimestamp).coerceIn(90L, 2500L)
                                    if (dist <= headRadius * 1.35f) {
                                        headshots++
                                        reactionTimes.add(reactMs)
                                        score += (120 - (reactMs / 10).toInt()).coerceAtLeast(40)
                                        lastOutcome = ShotOutcome.HEADSHOT
                                        lastImpactOffset = tapOffset
                                        liveFeedback = "INSTANT HEAD LOCK! Reaction: ${reactMs}ms"
                                        targetNormX = Random.nextFloat() * 0.64f + 0.18f
                                        targetNormY = Random.nextFloat() * 0.42f + 0.18f
                                        targetSpawnTimestamp = System.currentTimeMillis()
                                    } else {
                                        misses++
                                        lastOutcome = ShotOutcome.MISS
                                        lastImpactOffset = tapOffset
                                        liveFeedback = "Missed Reflex Target (${reactMs}ms)"
                                    }
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        if (!isRunning) return@detectDragGestures
                                        isDragging = true
                                        dragStartOffset = startOffset
                                        dragCurrentOffset = startOffset
                                        dragStartTimeMs = System.currentTimeMillis()
                                    },
                                    onDrag = { change, _ ->
                                        if (!isRunning) return@detectDragGestures
                                        change.consume()
                                        dragCurrentOffset = change.position
                                    },
                                    onDragEnd = {
                                        if (!isRunning || !isDragging) return@detectDragGestures
                                        isDragging = false
                                        val duration = (System.currentTimeMillis() - dragStartTimeMs).coerceAtLeast(16L)
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val tx = targetNormX * w
                                        val bodyY = targetNormY * h
                                        val headRadius = 17f * density
                                        val bodyRadius = 34f * density
                                        val headY = bodyY - bodyRadius - headRadius * 0.7f
                                        // Crosshair starts near upper-chest height aligned horizontally with player's initial aim
                                        val aimOriginX = tx + (dragStartOffset.x - (w * 0.5f)) * 0.35f
                                        val aimOriginY = bodyY + (12f * density)

                                        val eval = HeadshotPhysicsEngine.evaluateDragShot(
                                            aimOriginX = aimOriginX,
                                            aimOriginY = aimOriginY,
                                            dragStartX = dragStartOffset.x,
                                            dragStartY = dragStartOffset.y,
                                            dragEndX = dragCurrentOffset.x,
                                            dragEndY = dragCurrentOffset.y,
                                            dragDurationMs = duration,
                                            density = density,
                                            targetCenterX = tx,
                                            targetBodyCenterY = bodyY,
                                            targetHeadCenterY = headY,
                                            headRadiusPx = headRadius,
                                            bodyRadiusPx = bodyRadius,
                                            sensitivityConfig = state.activeSensitivity
                                        )

                                        totalShots++
                                        val reactMs = (System.currentTimeMillis() - targetSpawnTimestamp).coerceIn(110L, 2500L)
                                        reactionTimes.add(reactMs)
                                        dragVelocities.add(eval.dragVelocityDpPerSec)
                                        dragAngles.add(eval.dragAngleDeg)
                                        score += eval.pointsAwarded
                                        lastImpactOffset = Offset(eval.impactX, eval.impactY)
                                        lastOutcome = eval.outcome
                                        liveFeedback = eval.feedbackText

                                        when (eval.outcome) {
                                            ShotOutcome.HEADSHOT -> {
                                                headshots++
                                                targetNormX = Random.nextFloat() * 0.62f + 0.19f
                                                targetSpawnTimestamp = System.currentTimeMillis()
                                            }
                                            ShotOutcome.BODYSHOT -> bodyShots++
                                            ShotOutcome.MISS -> misses++
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Tactical grid lines
                    val gridStep = 40f * density
                    var gx = gridStep
                    while (gx < w) {
                        drawLine(
                            color = SenkuCardBorder.copy(alpha = 0.35f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        gx += gridStep
                    }
                    var gy = gridStep
                    while (gy < h) {
                        drawLine(
                            color = SenkuCardBorder.copy(alpha = 0.35f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                        gy += gridStep
                    }

                    val tx = targetNormX * w
                    val bodyY = targetNormY * h
                    val headRadius = 17f * density
                    val bodyRadius = 34f * density
                    val headY = bodyY - bodyRadius - headRadius * 0.7f

                    if (selectedMode == TrainingMode.REACTION_BLITZ) {
                        // Reflex target rings
                        drawCircle(
                            color = SenkuHeadshotRed.copy(alpha = 0.28f),
                            radius = headRadius * 1.45f,
                            center = Offset(tx, bodyY)
                        )
                        drawCircle(
                            color = SenkuHeadshotRed,
                            radius = headRadius * 0.85f,
                            center = Offset(tx, bodyY)
                        )
                    } else {
                        // Enemy Torso Silhouette
                        drawRoundRect(
                            color = SenkuElectricBlue.copy(alpha = 0.42f),
                            topLeft = Offset(tx - bodyRadius * 0.75f, bodyY - bodyRadius),
                            size = Size(bodyRadius * 1.5f, bodyRadius * 2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                        // Red Headshot Hitbox Indicator
                        drawCircle(
                            color = SenkuHeadshotRed,
                            radius = headRadius,
                            center = Offset(tx, headY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = headRadius * 0.35f,
                            center = Offset(tx, headY)
                        )

                        // Fire Button Zone at bottom center-right
                        val fireBtnRadius = (state.activeSensitivity.recommendedFireButtonSizePercent * 0.55f) * density
                        val fireCenter = Offset(w * 0.5f, h * 0.80f)
                        drawCircle(
                            color = SenkuHeadshotRed.copy(alpha = 0.25f),
                            radius = fireBtnRadius * 1.25f,
                            center = fireCenter
                        )
                        drawCircle(
                            color = SenkuHeadshotRed,
                            radius = fireBtnRadius,
                            center = fireCenter,
                            style = Stroke(width = 3f * density)
                        )

                        // Active Drag Vector Trail
                        if (isDragging) {
                            drawLine(
                                color = SenkuCyan,
                                start = dragStartOffset,
                                end = dragCurrentOffset,
                                strokeWidth = 4f * density,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                            )
                            drawCircle(
                                color = SenkuCyan,
                                radius = 8f * density,
                                center = dragCurrentOffset
                            )
                        }
                    }

                    // Last Shot Impact Marker
                    lastImpactOffset?.let { impact ->
                        val markerColor = when (lastOutcome) {
                            ShotOutcome.HEADSHOT -> SenkuHeadshotRed
                            ShotOutcome.BODYSHOT -> Color(0xFFFFB300)
                            else -> Color.Gray
                        }
                        drawCircle(
                            color = markerColor,
                            radius = 7f * density,
                            center = impact
                        )
                    }
                }

                // Live Feedback HUD Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC070A0F))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = liveFeedback,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (lastOutcome == ShotOutcome.HEADSHOT) SenkuHeadshotRed else SenkuCyan
                    )
                }
            }
        }

        // Start / Stop Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!isRunning) {
                            resetSessionStats(selectedMode)
                            isRunning = true
                        } else {
                            isRunning = false
                            if (totalShots > 0) {
                                val hsRate = (headshots.toFloat() / totalShots.toFloat()) * 100f
                                val avgReact = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 320L
                                val bestReact = reactionTimes.minOrNull() ?: avgReact
                                val avgVel = if (dragVelocities.isNotEmpty()) dragVelocities.average().toFloat() else 1350f
                                val avgAng = if (dragAngles.isNotEmpty()) dragAngles.average().toFloat() else 85f
                                onRecordSession(
                                    TrainingSessionSummary(
                                        mode = selectedMode,
                                        score = score,
                                        totalShots = totalShots,
                                        headshots = headshots,
                                        bodyShots = bodyShots,
                                        misses = misses,
                                        headshotRatePercent = hsRate,
                                        avgReactionTimeMs = avgReact,
                                        bestReactionTimeMs = bestReact,
                                        avgDragVelocityDpPerSec = avgVel,
                                        avgDragAngleDeg = avgAng
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) SenkuHeadshotRed else SenkuEmeraldSuccess
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Finish & Save Session" else "Start Training Drill",
                        fontWeight = FontWeight.Black
                    )
                }
                if (state.trainingHistory.isNotEmpty()) {
                    OutlinedButton(onClick = onClearHistory) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = SenkuTextSecondary)
                    }
                }
            }
        }

        // Training Performance History
        item {
            SectionHeader(
                title = "Training Performance History",
                subtitle = "Stored locally via Room Database • Feeds SENKU AI Assistant",
                badgeText = "${state.trainingHistory.size} SESSIONS"
            )
        }

        items(state.trainingHistory, key = { it.id }) { session ->
            SenkuCharcoalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${session.mode.title} • Score ${session.score}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SenkuCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Headshots: ${session.headshots}/${session.totalShots} (${session.headshotRatePercent.roundToInt()}%) • Best React: ${session.bestReactionTimeMs}ms • Drag: ${session.avgDragVelocityDpPerSec.roundToInt()} dp/s @ ${session.avgDragAngleDeg.roundToInt()}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SenkuTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
