package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConsoleMessage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GCodeConsolePanel(
  gcodeText: String,
  consoleHistory: List<ConsoleMessage>,
  onSendCommand: (String) -> Unit,
  onOpenGCodeInput: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableIntStateOf(0) } // 0: G-Code, 1: Serial Terminal
  var inputCmd by remember { mutableStateOf("") }
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  val consoleListState = rememberLazyListState()

  // Auto scroll console to bottom on new messages
  LaunchedEffect(consoleHistory.size) {
    if (consoleHistory.isNotEmpty()) {
      consoleListState.animateScrollToItem(consoleHistory.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonDark)
      .padding(16.dp)
  ) {
    // Top Tab Selector & Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TabRow(
        selectedTabIndex = selectedSubTab,
        containerColor = SurfaceDark,
        contentColor = LaserAmber,
        modifier = Modifier.width(260.dp)
      ) {
        Tab(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          text = { Text("G-Code View", fontSize = 12.sp) }
        )
        Tab(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          text = { Text("GRBL Terminal", fontSize = 12.sp) }
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // G-Code Input File Button
        Button(
          onClick = onOpenGCodeInput,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = LaserAmber,
            contentColor = Color.Black
          ),
          modifier = Modifier.testTag("panel_input_gcode_file_button")
        ) {
          Icon(Icons.Default.FileOpen, contentDescription = "Input G-Code File", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Input File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (selectedSubTab == 0) {
          FilledTonalButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(gcodeText))
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = SurfaceElevatedDark,
              contentColor = TextPrimary
            ),
            modifier = Modifier.testTag("copy_gcode_button")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy", fontSize = 12.sp)
          }
        }
      }
    }

    if (selectedSubTab == 0) {
      // G-Code Program Viewer
      val lines = remember(gcodeText) { gcodeText.split("\n") }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.weight(1f)
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("gcode_program_list")
        ) {
          items(lines.size) { index ->
            val line = lines[index]
            val isComment = line.trimStart().startsWith(";")
            val isCommand = line.startsWith("G0") || line.startsWith("G1")
            val isLaser = line.startsWith("M03") || line.startsWith("M05") || line.startsWith("M08") || line.startsWith("M09")

            Row(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = String.format("%4d ", index + 1),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary,
                modifier = Modifier.width(42.dp)
              )
              Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = when {
                  isComment -> TextSecondary
                  isLaser -> LaserAmber
                  isCommand -> LayerC01Score
                  else -> TextPrimary
                }
              )
            }
          }
        }
      }
    } else {
      // Interactive GRBL Terminal
      Column(modifier = Modifier.weight(1f)) {
        // Console Output Log
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = BedBackground,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          LazyColumn(
            state = consoleListState,
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp)
              .testTag("terminal_console_list")
          ) {
            items(consoleHistory) { msg ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp)
              ) {
                Text(
                  text = "[${msg.timestamp}] ",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = TextTertiary
                )
                Text(
                  text = if (msg.isOutgoing) "> ${msg.text}" else "< ${msg.text}",
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = if (msg.isOutgoing) FontWeight.Bold else FontWeight.Normal,
                  color = if (msg.isOutgoing) LaserAmber else LayerC00Cut
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Command Pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("?", "\$H", "\$\$", "\$X", "G92 X0 Y0").forEach { cmd ->
            FilterChip(
              selected = false,
              onClick = { onSendCommand(cmd) },
              label = { Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = SurfaceElevatedDark,
                labelColor = TextPrimary
              ),
              modifier = Modifier.height(30.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Command Input Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = inputCmd,
            onValueChange = { inputCmd = it },
            placeholder = { Text("Send G-Code command (e.g. G0 X50 Y50)", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("terminal_input_field"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = LaserAmber,
              unfocusedBorderColor = BorderDark,
              focusedContainerColor = SurfaceDark,
              unfocusedContainerColor = SurfaceDark,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          IconButton(
            onClick = {
              if (inputCmd.isNotBlank()) {
                onSendCommand(inputCmd)
                inputCmd = ""
              }
            },
            colors = IconButtonDefaults.iconButtonColors(containerColor = LaserAmber, contentColor = Color.Black),
            modifier = Modifier
              .size(48.dp)
              .testTag("send_command_button")
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
          }
        }
      }
    }
  }
}
