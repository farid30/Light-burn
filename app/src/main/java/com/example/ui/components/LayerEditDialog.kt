package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LaserLayer
import com.example.model.LaserMode
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LayerEditDialog(
  layer: LaserLayer,
  onSave: (LaserLayer) -> Unit,
  onDismiss: () -> Unit
) {
  var mode by remember { mutableStateOf(layer.mode) }
  var speedMmMin by remember { mutableFloatStateOf(layer.speedMmMin.toFloat()) }
  var powerMax by remember { mutableFloatStateOf(layer.powerMaxPercent) }
  var powerMin by remember { mutableFloatStateOf(layer.powerMinPercent) }
  var passes by remember { mutableIntStateOf(layer.passes) }
  var airAssist by remember { mutableStateOf(layer.airAssist) }
  var lineInterval by remember { mutableFloatStateOf(layer.lineIntervalMm.toFloat()) }
  var kerfOffset by remember { mutableFloatStateOf(layer.kerfOffsetMm.toFloat()) }
  var zOffset by remember { mutableFloatStateOf(layer.zOffsetMm.toFloat()) }

  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CarbonDark,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .background(layer.color, CircleShape)
        )
        Text(
          text = "Cut Settings - ${layer.name}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState)
      ) {
        // Mode Selector Tabs
        Text("CUT / ENGRAVE MODE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LaserMode.values().forEach { m ->
            FilterChip(
              selected = mode == m,
              onClick = { mode = m },
              label = { Text(m.label.split(" ").first(), fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = layer.color,
                selectedLabelColor = Color.Black
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("SPEED", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Text(
            text = "${speedMmMin.toInt()} mm/min",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LaserAmber
          )
        }
        Slider(
          value = speedMmMin,
          onValueChange = { speedMmMin = it },
          valueRange = 100f..10000f,
          colors = SliderDefaults.colors(thumbColor = LaserAmber, activeTrackColor = LaserAmber),
          modifier = Modifier.testTag("layer_speed_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Max Power Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("MAX POWER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Text(
            text = "${powerMax.toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LaserRed
          )
        }
        Slider(
          value = powerMax,
          onValueChange = { powerMax = it },
          valueRange = 5f..100f,
          colors = SliderDefaults.colors(thumbColor = LaserRed, activeTrackColor = LaserRed),
          modifier = Modifier.testTag("layer_power_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Min Power Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("MIN POWER (Corner Throttle)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Text(
            text = "${powerMin.toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
          )
        }
        Slider(
          value = powerMin,
          onValueChange = { powerMin = it.coerceAtMost(powerMax) },
          valueRange = 0f..100f,
          colors = SliderDefaults.colors(thumbColor = TextSecondary, activeTrackColor = BorderDark)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Passes & Air Assist
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Passes counter
          Column {
            Text("NUMBER OF PASSES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              IconButton(
                onClick = { passes = (passes - 1).coerceAtLeast(1) },
                modifier = Modifier.size(32.dp)
              ) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              }
              Text(
                text = "$passes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
              )
              IconButton(
                onClick = { passes = (passes + 1).coerceAtMost(10) },
                modifier = Modifier.size(32.dp)
              ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              }
            }
          }

          // Air Assist switch
          Column(horizontalAlignment = Alignment.End) {
            Text("AIR ASSIST (M08)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Switch(
              checked = airAssist,
              onCheckedChange = { airAssist = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = LaserAmber,
                checkedTrackColor = SurfaceElevatedDark
              )
            )
          }
        }

        // Fill Mode Line Interval
        if (mode == LaserMode.FILL || mode == LaserMode.FILL_AND_LINE) {
          Spacer(modifier = Modifier.height(12.dp))
          val dpi = (25.4f / lineInterval).roundToInt()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("LINE INTERVAL (Density)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = "${String.format(Locale.US, "%.3f", lineInterval)}mm ($dpi DPI)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = LayerC01Score
            )
          }
          Slider(
            value = lineInterval,
            onValueChange = { lineInterval = it },
            valueRange = 0.04f..0.30f,
            colors = SliderDefaults.colors(thumbColor = LayerC01Score, activeTrackColor = LayerC01Score)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            layer.copy(
              mode = mode,
              speedMmMin = speedMmMin.toDouble(),
              powerMaxPercent = powerMax,
              powerMinPercent = powerMin,
              passes = passes,
              airAssist = airAssist,
              lineIntervalMm = lineInterval.toDouble(),
              kerfOffsetMm = kerfOffset.toDouble(),
              zOffsetMm = zOffset.toDouble()
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = LaserAmber, contentColor = Color.Black),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("save_layer_settings_button")
      ) {
        Text("Save Parameters", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
