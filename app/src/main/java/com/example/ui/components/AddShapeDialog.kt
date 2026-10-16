package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShapeType
import com.example.ui.theme.*

data class ShapeOption(
  val type: ShapeType,
  val title: String,
  val description: String,
  val icon: ImageVector
)

val availableShapes = listOf(
  ShapeOption(ShapeType.RECTANGLE, "Rectangle", "Standard box or rounded border", Icons.Default.CropSquare),
  ShapeOption(ShapeType.CIRCLE, "Circle / Oval", "Smooth ellipse or coin coaster", Icons.Default.RadioButtonUnchecked),
  ShapeOption(ShapeType.STAR, "Star / Polygon", "Multi-point star or badge", Icons.Default.StarOutline),
  ShapeOption(ShapeType.TEXT, "Vector Text", "Laser engraved typography", Icons.Default.TextFields),
  ShapeOption(ShapeType.LIVING_HINGE, "Living Hinge", "Plywood flexible bending slots", Icons.Default.ViewWeek),
  ShapeOption(ShapeType.CALIBRATION_GRID, "Test Matrix", "Speed vs Power calibration", Icons.Default.Grid4x4),
  ShapeOption(ShapeType.FRAME, "Border Frame", "Double contour perimeter", Icons.Default.CropFree)
)

@Composable
fun AddShapeDialog(
  onAddShape: (ShapeType, String) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedType by remember { mutableStateOf<ShapeType?>(null) }
  var textInput by remember { mutableStateOf("LIGHTBURN") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CarbonDark,
    title = {
      Text(
        text = "Add Laser Shape",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Select vector element to place on workspace bed:",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
        ) {
          items(availableShapes) { item ->
            val isSelected = selectedType == item.type
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) SurfaceVariantDark else SurfaceDark,
              border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LaserAmber else BorderDark
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedType = item.type }
                .testTag("shape_option_${item.type.name}")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = if (isSelected) LaserAmber else TextPrimary,
                  modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = TextSecondary,
                  maxLines = 2
                )
              }
            }
          }
        }

        if (selectedType == ShapeType.TEXT) {
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Text to Engrave") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("shape_text_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = LaserAmber,
              unfocusedBorderColor = BorderDark,
              focusedContainerColor = SurfaceDark,
              unfocusedContainerColor = SurfaceDark,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          selectedType?.let { type ->
            onAddShape(type, textInput)
          }
        },
        enabled = selectedType != null,
        colors = ButtonDefaults.buttonColors(containerColor = LaserAmber, contentColor = androidx.compose.ui.graphics.Color.Black),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("confirm_add_shape_button")
      ) {
        Text("Add to Bed", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
