package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LaserMode
import com.example.model.ShapeType
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaserStudioScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showProjectMenu by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = CarbonDark,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(LaserAmber, RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Laser",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "LIGHTBURN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
              )
              // Project template picker trigger
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showProjectMenu = true }
              ) {
                Text(
                  text = state.activeProjectName,
                  style = MaterialTheme.typography.labelSmall,
                  color = LaserAmber
                )
                Icon(
                  Icons.Default.ArrowDropDown,
                  contentDescription = "Projects",
                  tint = LaserAmber,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        },
        actions = {
          // Machine Status Badge
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceVariantDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier
              .padding(end = 8.dp)
              .clickable { viewModel.setTab(StudioTab.CONTROL) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(if (state.machineState.connectionStatus.isReady) LayerC00Cut else LaserRed, CircleShape)
              )
              Text(
                text = if (state.machineState.connectionStatus.isReady) "GRBL ONLINE" else "ALARM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 10.sp
              )
            }
          }

          // Add Shape Button (when in workspace)
          if (state.currentTab == StudioTab.WORKSPACE) {
            IconButton(
              onClick = { viewModel.setAddShapeDialogVisible(true) },
              modifier = Modifier.testTag("topbar_add_shape_button")
            ) {
              Icon(Icons.Default.AddBox, contentDescription = "Add Shape", tint = LaserAmber)
            }
          }

          // G-Code Input Action Button
          IconButton(
            onClick = { viewModel.setGCodeInputDialogVisible(true) },
            modifier = Modifier.testTag("topbar_gcode_input_button")
          ) {
            Icon(Icons.Default.Terminal, contentDescription = "G-Code Input", tint = LaserAmber)
          }

          // G-Code Input File Action Button
          IconButton(
            onClick = { viewModel.setGCodeInputDialogVisible(true) },
            modifier = Modifier.testTag("topbar_gcode_file_button")
          ) {
            Icon(Icons.Default.FileOpen, contentDescription = "Input G-Code File", tint = LayerC01Score)
          }

          // Materials Library Shortcut
          IconButton(
            onClick = { viewModel.setMaterialsSheetVisible(true) },
            modifier = Modifier.testTag("topbar_materials_button")
          ) {
            Icon(Icons.Default.MenuBook, contentDescription = "Materials", tint = TextPrimary)
          }

          // Project Template Dropdown Menu
          DropdownMenu(
            expanded = showProjectMenu,
            onDismissRequest = { showProjectMenu = false },
            modifier = Modifier.background(SurfaceDark)
          ) {
            DropdownMenuItem(
              text = { Text("Mandala Coaster (Cut + Score + Engrave)") },
              onClick = {
                viewModel.loadProjectTemplate("Mandala Coaster")
                showProjectMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Living Hinge Box (Flexible Wood Kerf)") },
              onClick = {
                viewModel.loadProjectTemplate("Living Hinge Box")
                showProjectMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Laser Calibration Grid (Speed vs Power)") },
              onClick = {
                viewModel.loadProjectTemplate("Laser Calibration Grid")
                showProjectMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Leather Keyring Tag (Rounded + Name)") },
              onClick = {
                viewModel.loadProjectTemplate("Leather Keyring Tag")
                showProjectMenu = false
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SurfaceDark,
          titleContentColor = TextPrimary
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = state.currentTab == StudioTab.WORKSPACE,
          onClick = { viewModel.setTab(StudioTab.WORKSPACE) },
          icon = { Icon(Icons.Default.DesignServices, contentDescription = "Workspace") },
          label = { Text("Bed Canvas", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = LaserAmber,
            indicatorColor = LaserAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_workspace")
        )

        NavigationBarItem(
          selected = state.currentTab == StudioTab.PREVIEW,
          onClick = { viewModel.setTab(StudioTab.PREVIEW) },
          icon = {
            BadgedBox(badge = {
              if (state.isSimulating) {
                Badge(containerColor = LaserAmber) { Text("LIVE", fontSize = 8.sp) }
              }
            }) {
              Icon(Icons.Default.PlayCircle, contentDescription = "Preview")
            }
          },
          label = { Text("Preview", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = LaserAmber,
            indicatorColor = LaserAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_preview")
        )

        NavigationBarItem(
          selected = state.currentTab == StudioTab.CONTROL,
          onClick = { viewModel.setTab(StudioTab.CONTROL) },
          icon = { Icon(Icons.Default.Gamepad, contentDescription = "Jog") },
          label = { Text("Jog & Move", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = LaserAmber,
            indicatorColor = LaserAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_control")
        )

        NavigationBarItem(
          selected = state.currentTab == StudioTab.LAYERS,
          onClick = { viewModel.setTab(StudioTab.LAYERS) },
          icon = { Icon(Icons.Default.Layers, contentDescription = "Layers") },
          label = { Text("Layers", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = LaserAmber,
            indicatorColor = LaserAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_layers")
        )

        NavigationBarItem(
          selected = state.currentTab == StudioTab.GCODE,
          onClick = { viewModel.setTab(StudioTab.GCODE) },
          icon = { Icon(Icons.Default.Code, contentDescription = "GCode") },
          label = { Text("G-Code", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = LaserAmber,
            indicatorColor = LaserAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_gcode")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (state.currentTab) {
        StudioTab.WORKSPACE -> {
          Box(modifier = Modifier.fillMaxSize()) {
            LaserCanvas(
              bedWidthMm = state.bedWidthMm,
              bedHeightMm = state.bedHeightMm,
              shapes = state.shapes,
              layers = state.layers,
              selectedShapeId = state.selectedShapeId,
              laserHeadX = if (state.isSimulating) state.simLaserX else state.machineState.posX,
              laserHeadY = if (state.isSimulating) state.simLaserY else state.machineState.posY,
              isLaserFiring = if (state.isSimulating) state.simIsFiring else state.machineState.isLaserFiring,
              isGridSnapEnabled = state.isGridSnapEnabled,
              gridSnapMm = state.gridSnapMm,
              onSelectShape = { viewModel.selectShape(it) },
              onMoveShape = { id, dx, dy -> viewModel.moveShape(id, dx, dy) },
              onResetView = { viewModel.selectShape(null) }
            )

            // Shape Quick Properties Bar (Floats at bottom if shape is selected)
            val selectedShape = state.shapes.find { it.id == state.selectedShapeId }
            if (selectedShape != null) {
              ShapePropertiesBar(
                shape = selectedShape,
                layers = state.layers,
                onUpdateShape = { viewModel.updateShape(it) },
                onAssignLayer = { viewModel.assignLayerToSelectedShape(it) },
                onCenter = { viewModel.centerSelectedShape() },
                onDuplicate = { viewModel.duplicateSelectedShape() },
                onDelete = { viewModel.deleteSelectedShape() },
                onDeselect = { viewModel.selectShape(null) },
                modifier = Modifier.align(Alignment.BottomCenter)
              )
            } else {
              // Quick Add Shape Floating Action Button
              FloatingActionButton(
                onClick = { viewModel.setAddShapeDialogVisible(true) },
                containerColor = LaserAmber,
                contentColor = Color.Black,
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(16.dp)
                  .testTag("fab_add_shape")
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shape")
              }
            }
          }
        }

        StudioTab.PREVIEW -> {
          SimulationPreview(
            bedWidthMm = state.bedWidthMm,
            bedHeightMm = state.bedHeightMm,
            simulation = state.simulation,
            isSimulating = state.isSimulating,
            simProgress = state.simProgress,
            simPlaybackSpeed = state.simPlaybackSpeed,
            laserX = state.simLaserX,
            laserY = state.simLaserY,
            isFiring = state.simIsFiring,
            currentPower = state.simCurrentPower,
            currentSpeed = state.simCurrentSpeed,
            currentLayerColor = state.simCurrentLayerColor,
            elapsedSeconds = state.simElapsedSeconds,
            onPlayPause = {
              if (state.isSimulating) viewModel.pauseSimulation() else viewModel.startSimulation()
            },
            onReset = { viewModel.resetSimulation() },
            onSeek = { viewModel.seekSimulation(it) },
            onSetSpeed = { viewModel.setPlaybackSpeed(it) },
            onStartPhysicalJob = {
              viewModel.setTab(StudioTab.CONTROL)
              viewModel.frameWorkpiece()
            }
          )
        }

        StudioTab.CONTROL -> {
          MachineJogPanel(
            machineState = state.machineState,
            onJog = { dx, dy, dz -> viewModel.jog(dx, dy, dz) },
            onSetJogStep = { viewModel.setJogStep(it) },
            onHome = { viewModel.homeMachine() },
            onSetOrigin = { viewModel.setOrigin() },
            onGoToOrigin = { viewModel.goToOrigin() },
            onFrame = { viewModel.frameWorkpiece() },
            onTestFire = { viewModel.testFireLaser() },
            onEmergencyStop = { viewModel.emergencyStop() },
            onClearAlarm = { viewModel.clearAlarm() }
          )
        }

        StudioTab.LAYERS -> {
          LayersPanel(
            layers = state.layers,
            selectedLayerId = state.selectedLayerId,
            onSelectLayer = { viewModel.selectLayer(it) },
            onToggleOutput = { viewModel.toggleLayerOutput(it) },
            onToggleAirAssist = { viewModel.toggleLayerAirAssist(it) },
            onOpenLayerSettings = { viewModel.setLayerSettingsDialog(it, true) },
            onOpenMaterialsLibrary = { viewModel.setMaterialsSheetVisible(true) }
          )
        }

        StudioTab.GCODE -> {
          GCodeConsolePanel(
            gcodeText = state.gcodeText,
            consoleHistory = state.machineState.consoleHistory,
            onSendCommand = { viewModel.sendConsoleCommand(it) },
            onOpenGCodeInput = { viewModel.setGCodeInputDialogVisible(true) }
          )
        }
      }

      // Dialogs
      if (state.showAddShapeDialog) {
        AddShapeDialog(
          onAddShape = { type, text ->
            viewModel.addShape(type, text)
          },
          onDismiss = { viewModel.setAddShapeDialogVisible(false) }
        )
      }

      if (state.showGCodeInputDialog) {
        GCodeInputDialog(
          initialGCode = state.gcodeText,
          onImportGCode = { gcode ->
            viewModel.importGCode(gcode)
          },
          onDismiss = { viewModel.setGCodeInputDialogVisible(false) }
        )
      }

      if (state.showMaterialsSheet) {
        MaterialsLibrarySheet(
          materials = state.materials,
          targetLayerId = state.selectedLayerId,
          onApplyPreset = { preset, layerId ->
            viewModel.applyMaterialPreset(preset, layerId)
          },
          onGenerateTestMatrix = {
            viewModel.addShape(ShapeType.CALIBRATION_GRID)
          },
          onDismiss = { viewModel.setMaterialsSheetVisible(false) }
        )
      }

      if (state.showLayerSettingsDialog && state.editingLayerId != null) {
        val layerToEdit = state.layers.find { it.id == state.editingLayerId }
        if (layerToEdit != null) {
          LayerEditDialog(
            layer = layerToEdit,
            onSave = { updated ->
              viewModel.updateLayer(updated)
              viewModel.setLayerSettingsDialog(null, false)
            },
            onDismiss = { viewModel.setLayerSettingsDialog(null, false) }
          )
        }
      }
    }
  }
}
