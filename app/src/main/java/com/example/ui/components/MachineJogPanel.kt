package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.model.ConnectionStatus
import com.example.model.MachineState
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun MachineJogPanel(
  machineState: MachineState,
  onJog: (Float, Float, Float) -> Unit,
  onSetJogStep: (Float) -> Unit,
  onHome: () -> Unit,
  onSetOrigin: () -> Unit,
  onGoToOrigin: () -> Unit,
  onFrame: () -> Unit,
  onTestFire: () -> Unit,
  onEmergencyStop: () -> Unit,
  onClearAlarm: () -> Unit,
  modifier: Modifier = Modifier
) {
  val step = machineState.jogStepMm
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonDark)
      .verticalScroll(scrollState)
      .padding(16.dp)
  ) {
    // Top Status Header
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = SurfaceDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(
                  if (machineState.connectionStatus.isReady) LayerC00Cut else LaserRed,
                  CircleShape
                )
            )
            Text(
              text = machineState.connectionStatus.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
          Text(
            text = "Buffer: 100% | Mode: \$32 Laser",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
          )
        }

        if (machineState.connectionStatus == ConnectionStatus.ALARM) {
          Button(
            onClick = onClearAlarm,
            colors = ButtonDefaults.buttonColors(containerColor = LaserAmber),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Unlock (\$X)", color = Color.Black)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Position Readouts Card (DRO)
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "DIGITAL READOUT (DRO)",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          AxisDisplay(axis = "X", value = machineState.posX, color = BedOriginAxisX)
          AxisDisplay(axis = "Y", value = machineState.posY, color = BedOriginAxisY)
          AxisDisplay(axis = "Z", value = machineState.posZ, color = LayerC01Score)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Work Origin: (${String.format(Locale.US, "%.1f", machineState.originX)}, ${String.format(Locale.US, "%.1f", machineState.originY)})",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
          )
          Text(
            text = "Feed: ${machineState.jogSpeedMmMin.toInt()} mm/min",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Step Distance Selector
    Text(
      text = "JOG STEP DISTANCE",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = TextSecondary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(0.1f, 1f, 10f, 50f, 100f).forEach { stepMm ->
        FilterChip(
          selected = machineState.jogStepMm == stepMm,
          onClick = { onSetJogStep(stepMm) },
          label = {
            Text(
              if (stepMm < 1f) "${stepMm}mm" else "${stepMm.toInt()}mm",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LaserAmber,
            selectedLabelColor = Color.Black
          ),
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .testTag("jog_step_${stepMm}")
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Tactile D-PAD Jog Controller
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "MANUAL GANTRY JOG",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // D-Pad Layout
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // X/Y D-pad (Left)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Y+ (Forward)
            JogButton(
              label = "Y+",
              icon = Icons.Default.KeyboardArrowUp,
              onClick = { onJog(0f, step, 0f) },
              modifier = Modifier.testTag("jog_y_plus")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // X- (Left)
              JogButton(
                label = "X-",
                icon = Icons.Default.KeyboardArrowLeft,
                onClick = { onJog(-step, 0f, 0f) },
                modifier = Modifier.testTag("jog_x_minus")
              )

              // Center Home Origin Quick Button
              FilledTonalButton(
                onClick = onGoToOrigin,
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = SurfaceElevatedDark,
                  contentColor = LaserAmber
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                  .size(54.dp)
                  .testTag("jog_center_origin")
              ) {
                Text("0,0", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              // X+ (Right)
              JogButton(
                label = "X+",
                icon = Icons.Default.KeyboardArrowRight,
                onClick = { onJog(step, 0f, 0f) },
                modifier = Modifier.testTag("jog_x_plus")
              )
            }

            // Y- (Backward)
            JogButton(
              label = "Y-",
              icon = Icons.Default.KeyboardArrowDown,
              onClick = { onJog(0f, -step, 0f) },
              modifier = Modifier.testTag("jog_y_minus")
            )
          }

          Spacer(modifier = Modifier.width(28.dp))

          // Z-Axis Vertical Column
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text("Z (Focus)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            JogButton(
              label = "Z+",
              icon = Icons.Default.ArrowUpward,
              onClick = { onJog(0f, 0f, (step / 5f).coerceAtLeast(0.1f)) },
              modifier = Modifier.size(46.dp)
            )
            JogButton(
              label = "Z-",
              icon = Icons.Default.ArrowDownward,
              onClick = { onJog(0f, 0f, -(step / 5f).coerceAtLeast(0.1f)) },
              modifier = Modifier.size(46.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Operational Actions Grid (Home, Set Origin, Frame, Test Fire)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = onHome,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("machine_home_button")
      ) {
        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Home (\$H)")
      }

      OutlinedButton(
        onClick = onSetOrigin,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("set_origin_button")
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = "Set Origin", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Set 0,0")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onFrame,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (machineState.isFraming) LayerC01Score else SurfaceElevatedDark,
          contentColor = if (machineState.isFraming) Color.Black else TextPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("frame_project_button")
      ) {
        Icon(Icons.Default.CropFree, contentDescription = "Frame", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (machineState.isFraming) "Framing..." else "Frame Job")
      }

      Button(
        onClick = onTestFire,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (machineState.isLaserFiring) LaserRed else SurfaceElevatedDark,
          contentColor = if (machineState.isLaserFiring) Color.White else TextPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("test_fire_button")
      ) {
        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Fire", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (machineState.isLaserFiring) "FIRING!" else "Test Fire (1%)")
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // EMERGENCY STOP BUTTON
    Button(
      onClick = onEmergencyStop,
      shape = RoundedCornerShape(10.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = LaserRed,
        contentColor = Color.White
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("emergency_stop_button")
    ) {
      Icon(Icons.Default.StopCircle, contentDescription = "E-Stop", modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("EMERGENCY STOP (!)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
  }
}

@Composable
private fun AxisDisplay(axis: String, value: Float, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(axis, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    Text(
      text = String.format(Locale.US, "%06.2f", value),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = TextPrimary
    )
    Text("mm", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
  }
}

@Composable
private fun JogButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  FilledTonalButton(
    onClick = onClick,
    shape = CircleShape,
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = SurfaceElevatedDark,
      contentColor = TextPrimary
    ),
    contentPadding = PaddingValues(0.dp),
    modifier = modifier.size(54.dp)
  ) {
    Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
  }
}
