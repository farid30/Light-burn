package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun LayersPanel(
  layers: List<LaserLayer>,
  selectedLayerId: String,
  onSelectLayer: (String) -> Unit,
  onToggleOutput: (String) -> Unit,
  onToggleAirAssist: (String) -> Unit,
  onOpenLayerSettings: (String) -> Unit,
  onOpenMaterialsLibrary: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonDark)
      .padding(16.dp)
  ) {
    // Header & Quick Action
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Cut / Layers",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Configure speed, power, mode and air assist per layer",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }

      FilledTonalButton(
        onClick = onOpenMaterialsLibrary,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = SurfaceElevatedDark,
          contentColor = LaserAmber
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("open_materials_button")
      ) {
        Icon(Icons.Default.MenuBook, contentDescription = "Library", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Materials")
      }
    }

    // Table Header
    Surface(
      shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
      color = SurfaceElevatedDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("LAYER", modifier = Modifier.width(85.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("MODE", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("SPD (mm/m)", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("PWR", modifier = Modifier.width(55.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("PASS", modifier = Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("OUT", modifier = Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      }
    }

    // Layer Rows
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(SurfaceDark)
        .border(1.dp, BorderDark, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
    ) {
      items(layers) { layer ->
        val isSelected = layer.id == selectedLayerId

        Surface(
          color = if (isSelected) SurfaceVariantDark else Color.Transparent,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectLayer(layer.id) }
            .testTag("layer_row_${layer.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Layer color swatch + ID
            Row(
              modifier = Modifier.width(85.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(layer.color, RoundedCornerShape(3.dp))
              )
              Text(
                text = layer.id,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }

            // Mode Label & Icon
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = when (layer.mode) {
                  LaserMode.LINE -> Icons.Default.Timeline
                  LaserMode.FILL -> Icons.Default.Texture
                  LaserMode.FILL_AND_LINE -> Icons.Default.Layers
                  LaserMode.OFFSET_FILL -> Icons.Default.CropFree
                },
                contentDescription = layer.mode.label,
                tint = layer.color,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = layer.mode.label.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
              )
            }

            // Speed
            Text(
              text = "${layer.speedMmMin.toInt()}",
              modifier = Modifier.width(70.dp),
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = TextPrimary
            )

            // Power
            Text(
              text = "${layer.powerMaxPercent.toInt()}%",
              modifier = Modifier.width(55.dp),
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = LaserAmber
            )

            // Passes
            Text(
              text = "${layer.passes}x",
              modifier = Modifier.width(45.dp),
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = TextSecondary
            )

            // Output Switch / Checkbox
            Checkbox(
              checked = layer.outputEnabled,
              onCheckedChange = { onToggleOutput(layer.id) },
              modifier = Modifier
                .size(36.dp)
                .testTag("layer_output_toggle_${layer.id}"),
              colors = CheckboxDefaults.colors(
                checkedColor = LaserAmber,
                uncheckedColor = BorderDark
              )
            )

            // Edit gear icon
            IconButton(
              onClick = { onOpenLayerSettings(layer.id) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Tune, contentDescription = "Edit Layer", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
          }
        }
        HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
      }
    }

    // Selected Layer Detailed Quick Stats Card
    val activeLayer = layers.find { it.id == selectedLayerId } ?: layers.firstOrNull()
    activeLayer?.let { layer ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(modifier = Modifier.size(12.dp).background(layer.color, CircleShape))
              Text(
                text = "${layer.name} (${layer.mode.label})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }

            OutlinedButton(
              onClick = { onOpenLayerSettings(layer.id) },
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Text("Fine Tune", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Interval: ${layer.lineIntervalMm} mm", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text("Kerf Offset: ${layer.kerfOffsetMm} mm", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Column {
              Text("Air Assist: ${if (layer.airAssist) "ON (M08)" else "OFF"}", style = MaterialTheme.typography.bodySmall, color = if (layer.airAssist) LayerC01Score else TextTertiary)
              Text("Z-Offset: ${layer.zOffsetMm} mm", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
          }
        }
      }
    }
  }
}
