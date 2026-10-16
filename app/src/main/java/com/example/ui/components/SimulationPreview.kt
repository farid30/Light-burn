package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimulationResult
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun SimulationPreview(
  bedWidthMm: Float,
  bedHeightMm: Float,
  simulation: SimulationResult,
  isSimulating: Boolean,
  simProgress: Float,
  simPlaybackSpeed: Float,
  laserX: Float,
  laserY: Float,
  isFiring: Boolean,
  currentPower: Float,
  currentSpeed: Double,
  currentLayerColor: Color,
  elapsedSeconds: Double,
  onPlayPause: () -> Unit,
  onReset: () -> Unit,
  onSeek: (Float) -> Unit,
  onSetSpeed: (Float) -> Unit,
  onStartPhysicalJob: () -> Unit,
  modifier: Modifier = Modifier
) {
  val totalSec = simulation.totalDurationSeconds
  val remainingSec = (totalSec - elapsedSeconds).coerceAtLeast(0.0)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonDark)
  ) {
    // Top Telemetry Header
    Surface(
      color = SurfaceDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Time & Progress
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(
                  color = if (isSimulating) LaserAmber else TextTertiary,
                  shape = CircleShape
                )
            )
            Text(
              text = if (isSimulating) "SIMULATING TOOLPATH" else "PREVIEW PAUSED",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isSimulating) LaserAmber else TextSecondary
            )
          }
          Text(
            text = "${formatDuration(elapsedSeconds)} / ${formatDuration(totalSec)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
          )
        }

        // Live Feed & Power
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "SPEED",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
            Text(
              text = "${currentSpeed.toInt()} mm/min",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              color = LaserAmber
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "POWER",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
            Text(
              text = "${currentPower.toInt()}%",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              color = if (isFiring) LaserRed else TextSecondary
            )
          }
        }
      }
    }

    // Main Simulation Canvas
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(BedBackground)
        .testTag("simulation_preview_canvas")
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height

        // Fit bed inside view with margins
        val margin = 24.dp.toPx()
        val availW = cw - margin * 2
        val availH = ch - margin * 2

        val scale = min(availW / bedWidthMm, availH / bedHeightMm)
        val bedPixelW = bedWidthMm * scale
        val bedPixelH = bedHeightMm * scale
        val offsetX = (cw - bedPixelW) / 2f
        val offsetY = (ch - bedPixelH) / 2f

        // Draw bed bounds
        drawRect(
          color = SurfaceDark,
          topLeft = Offset(offsetX, offsetY),
          size = Size(bedPixelW, bedPixelH)
        )

        // Draw bed grid lines
        val step50 = 50f * scale
        val cols = (bedWidthMm / 50f).toInt()
        val rows = (bedHeightMm / 50f).toInt()
        for (i in 0..cols) {
          val x = offsetX + i * step50
          drawLine(BedGridMinor, Offset(x, offsetY), Offset(x, offsetY + bedPixelH), 1f)
        }
        for (j in 0..rows) {
          val y = offsetY + j * step50
          drawLine(BedGridMinor, Offset(offsetX, y), Offset(offsetX + bedPixelW, y), 1f)
        }

        // Draw origin indicator
        drawLine(BedOriginAxisX, Offset(offsetX, offsetY), Offset(offsetX + 30f * scale, offsetY), 2.5f)
        drawLine(BedOriginAxisY, Offset(offsetX, offsetY), Offset(offsetX, offsetY + 30f * scale), 2.5f)

        // Calculate progress in seconds
        val targetSeconds = simProgress * totalSec
        var accumulatedSec = 0.0

        val segments = simulation.segments

        // 1. Draw future paths in ghosted/dimmed colors
        for (seg in segments) {
          val p1 = Offset(offsetX + seg.startX * scale, offsetY + seg.startY * scale)
          val p2 = Offset(offsetX + seg.endX * scale, offsetY + seg.endY * scale)

          if (seg.isRapid) {
            // Rapid moves dashed
            drawLine(
              color = RapidTravelColor.copy(alpha = 0.2f),
              start = p1,
              end = p2,
              strokeWidth = 1f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
          } else {
            // Unburned paths dim
            drawLine(
              color = seg.layerColor.copy(alpha = 0.25f),
              start = p1,
              end = p2,
              strokeWidth = 1.2f
            )
          }
        }

        // 2. Draw completed (burned) paths with glowing high contrast
        for (seg in segments) {
          val segDur = seg.durationSeconds
          val p1 = Offset(offsetX + seg.startX * scale, offsetY + seg.startY * scale)
          val p2 = Offset(offsetX + seg.endX * scale, offsetY + seg.endY * scale)

          if (accumulatedSec + segDur <= targetSeconds) {
            // Fully completed segment
            if (seg.isRapid) {
              drawLine(
                color = RapidTravelColor.copy(alpha = 0.6f),
                start = p1,
                end = p2,
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
              )
            } else {
              // Burned laser mark
              drawLine(
                color = seg.layerColor,
                start = p1,
                end = p2,
                strokeWidth = 2.5f
              )
            }
            accumulatedSec += segDur
          } else if (accumulatedSec < targetSeconds) {
            // Partially completed active segment
            val fraction = ((targetSeconds - accumulatedSec) / segDur).toFloat().coerceIn(0f, 1f)
            val partialEnd = Offset(p1.x + (p2.x - p1.x) * fraction, p1.y + (p2.y - p1.y) * fraction)

            if (seg.isRapid) {
              drawLine(
                color = RapidTravelColor,
                start = p1,
                end = partialEnd,
                strokeWidth = 1.5f
              )
            } else {
              drawLine(
                color = seg.layerColor,
                start = p1,
                end = partialEnd,
                strokeWidth = 2.8f
              )
            }
            accumulatedSec += segDur
          } else {
            // Future segment
            break
          }
        }

        // 3. Draw Animated Laser Head with Spark / Plasma Glow
        val headPx = offsetX + laserX * scale
        val headPy = offsetY + laserY * scale

        if (isFiring) {
          // Intense outer burning flare
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(LaserSparkColor, LaserAmber, Color.Transparent),
              center = Offset(headPx, headPy),
              radius = 26f
            ),
            radius = 26f,
            center = Offset(headPx, headPy)
          )
          // Core laser spot
          drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(headPx, headPy)
          )
        } else {
          // Non-firing transit dot
          drawCircle(
            color = RapidTravelColor,
            radius = 3.5f,
            center = Offset(headPx, headPy)
          )
        }

        // Crosshairs on head
        drawLine(LaserAmber, Offset(headPx - 14f, headPy), Offset(headPx + 14f, headPy), 1.5f)
        drawLine(LaserAmber, Offset(headPx, headPy - 14f), Offset(headPx, headPy + 14f), 1.5f)
      }

      // Floating Live Position HUD in Canvas
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevatedDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text(
            text = "X: ${String.format(Locale.US, "%.2f", laserX)} mm",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
          )
          Text(
            text = "Y: ${String.format(Locale.US, "%.2f", laserY)} mm",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
          )
          Text(
            text = if (isFiring) "● LASER EMITTING" else "○ RAPID TRAVERSE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isFiring) LaserRed else RapidTravelColor
          )
        }
      }
    }

    // Bottom Transport & Simulation Controls
    Surface(
      color = SurfaceDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        // Scrubber Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "${(simProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = LaserAmber,
            modifier = Modifier.width(42.dp)
          )
          Slider(
            value = simProgress,
            onValueChange = onSeek,
            modifier = Modifier
              .weight(1f)
              .testTag("simulation_scrubber_slider"),
            colors = SliderDefaults.colors(
              thumbColor = LaserAmber,
              activeTrackColor = LaserAmber,
              inactiveTrackColor = SurfaceVariantDark
            )
          )
        }

        // Playback Buttons & Speed Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Play / Pause / Reset
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilledIconButton(
              onClick = onPlayPause,
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = LaserAmber,
                contentColor = Color.Black
              ),
              modifier = Modifier
                .size(44.dp)
                .testTag("sim_play_pause_button")
            ) {
              Icon(
                if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isSimulating) "Pause" else "Play"
              )
            }

            IconButton(
              onClick = onReset,
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Default.Replay, contentDescription = "Reset", tint = TextPrimary)
            }

            // Speed selector pills
            listOf(1f, 2f, 5f, 10f).forEach { spd ->
              FilterChip(
                selected = simPlaybackSpeed == spd,
                onClick = { onSetSpeed(spd) },
                label = { Text("${spd.toInt()}x", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LaserAmber.copy(alpha = 0.2f),
                  selectedLabelColor = LaserAmber
                ),
                modifier = Modifier.height(30.dp)
              )
            }
          }

          // Statistics Info Button
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "CUT: ${String.format(Locale.US, "%.1f", simulation.totalCutDistanceMm / 1000f)}m",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = LayerC00Cut
            )
            Text(
              text = "RAPID: ${String.format(Locale.US, "%.1f", simulation.totalRapidDistanceMm / 1000f)}m",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = RapidTravelColor
            )
          }
        }
      }
    }
  }
}

private fun formatDuration(seconds: Double): String {
  val total = seconds.roundToInt().coerceAtLeast(0)
  val m = total / 60
  val s = total % 60
  return String.format(Locale.US, "%02d:%02d", m, s)
}
