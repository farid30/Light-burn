package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.MaterialCategory
import com.example.model.MaterialPreset
import com.example.model.ShapeType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsLibrarySheet(
  materials: List<MaterialPreset>,
  targetLayerId: String,
  onApplyPreset: (MaterialPreset, String) -> Unit,
  onGenerateTestMatrix: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf<MaterialCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val filtered = remember(materials, selectedCategory, searchQuery) {
    materials.filter { item ->
      val catMatch = selectedCategory == null || item.category == selectedCategory
      val textMatch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) || item.tips.contains(searchQuery, ignoreCase = true)
      catMatch && textMatch
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CarbonDark,
    dragHandle = { BottomSheetDefaults.DragHandle(color = BorderDark) }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Materials Library",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Apply tested laser parameters to Layer $targetLayerId",
            style = MaterialTheme.typography.bodySmall,
            color = LaserAmber
          )
        }

        // Test Matrix Generator Button
        FilledTonalButton(
          onClick = {
            onGenerateTestMatrix()
            onDismiss()
          },
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = SurfaceElevatedDark,
            contentColor = LaserAmber
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("generate_test_matrix_button")
        ) {
          Icon(Icons.Default.GridOn, contentDescription = "Matrix", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Test Grid", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search materials (e.g. acrylic, birch, leather...)", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = LaserAmber,
          unfocusedBorderColor = BorderDark,
          focusedContainerColor = SurfaceDark,
          unfocusedContainerColor = SurfaceDark,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Category Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = selectedCategory == null,
          onClick = { selectedCategory = null },
          label = { Text("All", fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LaserAmber,
            selectedLabelColor = Color.Black
          )
        )
        MaterialCategory.values().take(4).forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat.label.split(" ").first(), fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = LaserAmber,
              selectedLabelColor = Color.Black
            )
          )
        }
      }

      // Materials List
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filtered) { item ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = "${item.category.label} • ${item.thicknessMm}mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                  )
                }

                Button(
                  onClick = { onApplyPreset(item, targetLayerId) },
                  colors = ButtonDefaults.buttonColors(containerColor = LaserAmber, contentColor = Color.Black),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier
                    .height(36.dp)
                    .testTag("apply_preset_${item.id}")
                ) {
                  Text("Apply to $targetLayerId", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Settings summary chips
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                ParamBadge(label = "MODE", value = item.recommendedMode.label.take(8))
                ParamBadge(label = "SPEED", value = "${item.speedMmMin.toInt()} mm/m")
                ParamBadge(label = "POWER", value = "${item.powerPercent.toInt()}%")
                ParamBadge(label = "PASSES", value = "${item.passes}x")
                ParamBadge(label = "AIR", value = if (item.airAssist) "ON" else "OFF")
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Tip: ${item.tips}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ParamBadge(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
  }
}
