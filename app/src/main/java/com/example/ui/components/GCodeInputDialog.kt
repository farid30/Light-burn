package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class GCodePreset(val title: String, val fileName: String, val gcode: String)

val sampleGCodePresets = listOf(
  GCodePreset(
    title = "50mm Box Cut",
    fileName = "box_cut_50mm.gcode",
    gcode = """; 50mm Square Cut Program
G21 ; mm units
G90 ; absolute
M05 ; laser off
G0 X30.000 Y30.000 ; rapid to start
M03 S900 ; laser on 90%
G1 X80.000 Y30.000 F800 ; cut bottom
G1 X80.000 Y80.000 ; cut right
G1 X30.000 Y80.000 ; cut top
G1 X30.000 Y30.000 ; cut left
M05 ; laser off
G0 X0 Y0 ; return home"""
  ),
  GCodePreset(
    title = "Circular Coaster",
    fileName = "circle_coaster_60mm.nc",
    gcode = """; 60mm Circular Contour
G21
G90
M05
G0 X60.000 Y30.000
M03 S850
G1 X70.607 Y34.393 F1200
G1 X78.284 Y41.716
G1 X81.213 Y50.000
G1 X80.000 Y60.000
G1 X74.142 Y68.284
G1 X65.858 Y74.142
G1 X50.000 Y78.284
G1 X41.716 Y74.142
G1 X34.393 Y65.858
G1 X30.000 Y50.000
G1 X34.393 Y34.393
G1 X41.716 Y25.858
G1 X60.000 Y30.000
M05
G0 X0 Y0"""
  ),
  GCodePreset(
    title = "Laser Power Ramp",
    fileName = "power_ramp_test.gcode",
    gcode = """; 4-Step Laser Power Ramp Test
G21
G90
M05
; Step 1: 25% Power
G0 X20.000 Y20.000
M03 S250
G1 X70.000 Y20.000 F1500
M05
; Step 2: 50% Power
G0 X20.000 Y35.000
M03 S500
G1 X70.000 Y35.000 F1500
M05
; Step 3: 75% Power
G0 X20.000 Y50.000
M03 S750
G1 X70.000 Y50.000 F1500
M05
; Step 4: 100% Power
G0 X20.000 Y65.000
M03 S1000
G1 X70.000 Y65.000 F1500
M05
G0 X0 Y0"""
  )
)

@Composable
fun GCodeInputDialog(
  initialGCode: String,
  onImportGCode: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var gcodeText by remember { mutableStateOf(initialGCode.ifEmpty { sampleGCodePresets.first().gcode }) }
  var selectedFileName by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  // System Document File Picker for .gcode, .nc, .txt, .tap files
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { fileUri ->
      try {
        context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { reader ->
          val content = reader.readText()
          if (content.isNotBlank()) {
            gcodeText = content
            var name = "selected_file.gcode"
            context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
              val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
              }
            }
            selectedFileName = name
          }
        }
      } catch (e: Exception) {
        // Fallback gracefully
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CarbonDark,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .background(LaserAmber, RoundedCornerShape(4.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Terminal, contentDescription = "GCode", tint = Color.Black, modifier = Modifier.size(16.dp))
        }
        Text(
          text = "G-Code Input & File Importer",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Load an external G-Code file (.gcode, .nc, .txt) or paste code directly:",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // PRIMARY BUTTON: Input G-Code File from Device
        Button(
          onClick = {
            filePickerLauncher.launch(arrayOf("*/*"))
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevatedDark,
            contentColor = LaserAmber
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, LaserAmber),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("choose_gcode_file_button")
        ) {
          Icon(Icons.Default.FileOpen, contentDescription = "Choose File", modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (selectedFileName != null) "File: $selectedFileName" else "Select G-Code File (.gcode / .nc)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }

        if (selectedFileName != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "✓ Loaded: $selectedFileName (${gcodeText.lines().size} lines)",
            style = MaterialTheme.typography.labelSmall,
            color = LayerC00Cut,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips
        Text(
          text = "OR CHOOSE SAMPLE G-CODE FILE:",
          style = MaterialTheme.typography.labelSmall,
          color = TextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          sampleGCodePresets.forEach { preset ->
            FilterChip(
              selected = selectedFileName == preset.fileName,
              onClick = {
                gcodeText = preset.gcode
                selectedFileName = preset.fileName
              },
              label = { Text(preset.title, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = SurfaceElevatedDark,
                labelColor = TextPrimary,
                selectedContainerColor = LaserAmber.copy(alpha = 0.2f),
                selectedLabelColor = LaserAmber
              ),
              modifier = Modifier.height(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // G-Code Input Text Area
        OutlinedTextField(
          value = gcodeText,
          onValueChange = {
            gcodeText = it
            selectedFileName = null
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("gcode_input_textarea"),
          placeholder = { Text("Paste G0, G1, M03, M05 commands here...", fontFamily = FontFamily.Monospace) },
          textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            color = LayerC00Cut
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LaserAmber,
            unfocusedBorderColor = BorderDark,
            focusedContainerColor = BedBackground,
            unfocusedContainerColor = BedBackground
          )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Lines: ${gcodeText.lines().size} | Bytes: ${gcodeText.length} B",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          color = TextSecondary
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (gcodeText.isNotBlank()) {
            onImportGCode(gcodeText)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = LaserAmber, contentColor = Color.Black),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("submit_gcode_input_button")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Load & Preview Toolpath", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
