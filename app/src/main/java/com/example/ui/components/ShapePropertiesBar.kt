package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.LaserShape
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ShapePropertiesBar(
  shape: LaserShape,
  layers: List<LaserLayer>,
  onUpdateShape: (LaserShape) -> Unit,
  onAssignLayer: (String) -> Unit,
  onCenter: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
  onDeselect: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = SurfaceElevatedDark,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("shape_properties_bar")
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Top Row: Title, Layer Chips & Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = shape.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          // Layer Assignment Selector
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            layers.forEach { layer ->
              val isAssigned = layer.id == shape.layerId
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .background(layer.color, RoundedCornerShape(4.dp))
                  .clickable { onAssignLayer(layer.id) }
                  .then(
                    if (isAssigned) Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp))
                    else Modifier
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = layer.id.drop(1),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Black
                )
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onCenter, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center", tint = TextPrimary, modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextPrimary, modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LaserRed, modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDeselect, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Coordinates & Dimensions row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        CoordField(label = "X", value = String.format(Locale.US, "%.1f", shape.x))
        CoordField(label = "Y", value = String.format(Locale.US, "%.1f", shape.y))
        CoordField(label = "W", value = String.format(Locale.US, "%.1f", shape.width))
        CoordField(label = "H", value = String.format(Locale.US, "%.1f", shape.height))

        // Quick resize buttons (-10% / +10%)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilledTonalButton(
            onClick = {
              val nw = (shape.width * 0.9f).coerceAtLeast(5f)
              val nh = (shape.height * 0.9f).coerceAtLeast(5f)
              onUpdateShape(shape.copy(width = nw, height = nh))
            },
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Text("-10%", fontSize = 10.sp)
          }

          FilledTonalButton(
            onClick = {
              val nw = (shape.width * 1.1f).coerceAtMost(380f)
              val nh = (shape.height * 1.1f).coerceAtMost(380f)
              onUpdateShape(shape.copy(width = nw, height = nh))
            },
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Text("+10%", fontSize = 10.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun CoordField(label: String, value: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = "$label: ",
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary
    )
    Text(
      text = "${value}mm",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = TextPrimary
    )
  }
}
