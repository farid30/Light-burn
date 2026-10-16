package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.LaserGCodeGenerator
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class StudioTab(val label: String) {
  WORKSPACE("Workspace"),
  PREVIEW("Laser Preview"),
  CONTROL("Jog & Control"),
  LAYERS("Layers & Cut"),
  GCODE("G-Code & Console")
}

data class StudioUiState(
  val currentTab: StudioTab = StudioTab.WORKSPACE,
  val bedWidthMm: Float = 400f,
  val bedHeightMm: Float = 400f,
  val gridSnapMm: Float = 5f,
  val isGridSnapEnabled: Boolean = true,
  val shapes: List<LaserShape> = emptyList(),
  val selectedShapeId: String? = null,
  val layers: List<LaserLayer> = LaserLayer.DefaultLayers,
  val selectedLayerId: String = "C00",
  // Toolpath & GCode
  val gcodeText: String = "",
  val simulation: SimulationResult = SimulationResult(),
  // Real-Time Simulation State
  val isSimulating: Boolean = false,
  val simProgress: Float = 0f,
  val simPlaybackSpeed: Float = 3f, // 1x, 2x, 3x, 5x, 10x
  val simLaserX: Float = 0f,
  val simLaserY: Float = 0f,
  val simIsFiring: Boolean = false,
  val simCurrentPower: Float = 0f,
  val simCurrentSpeed: Double = 0.0,
  val simCurrentLayerColor: Color = Color.Transparent,
  val simElapsedSeconds: Double = 0.0,
  // Machine Jog & Control
  val machineState: MachineState = MachineState(),
  // Materials Library
  val materials: List<MaterialPreset> = MaterialPreset.Presets,
  val activeProjectName: String = "Mandala Coaster",
  val showAddShapeDialog: Boolean = false,
  val showMaterialsSheet: Boolean = false,
  val showLayerSettingsDialog: Boolean = false,
  val editingLayerId: String? = null,
  val showGCodeInputDialog: Boolean = false
)

class MainViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(StudioUiState())
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  private var simulationJob: Job? = null
  private var framingJob: Job? = null

  init {
    loadProjectTemplate("Mandala Coaster")
    appendConsoleOutput("LightBurn Laser Studio initialized.")
    appendConsoleOutput("Connected to GRBL 1.1h on /dev/ttyUSB0 (115200 baud).")
    appendConsoleOutput("Laser mode active: \$32=1, Homing enabled: \$22=1.")
  }

  fun setTab(tab: StudioTab) {
    _uiState.update { it.copy(currentTab = tab) }
    if (tab == StudioTab.PREVIEW && !_uiState.value.isSimulating && _uiState.value.simProgress == 0f) {
      // Auto-refresh toolpath on preview
      regenerateToolpath()
    }
  }

  fun selectShape(id: String?) {
    _uiState.update { it.copy(selectedShapeId = id) }
  }

  fun selectLayer(layerId: String) {
    _uiState.update { it.copy(selectedLayerId = layerId) }
  }

  fun toggleGridSnap() {
    _uiState.update { it.copy(isGridSnapEnabled = !it.isGridSnapEnabled) }
  }

  fun setGridSnapSize(stepMm: Float) {
    _uiState.update { it.copy(gridSnapMm = stepMm) }
  }

  fun updateShape(updated: LaserShape) {
    _uiState.update { state ->
      val newShapes = state.shapes.map { if (it.id == updated.id) updated else it }
      state.copy(shapes = newShapes)
    }
    regenerateToolpath()
  }

  fun moveShape(shapeId: String, deltaX: Float, deltaY: Float) {
    _uiState.update { state ->
      val snap = if (state.isGridSnapEnabled) state.gridSnapMm else 1f
      val newShapes = state.shapes.map { shape ->
        if (shape.id == shapeId) {
          val rawX = (shape.x + deltaX).coerceIn(0f, state.bedWidthMm - shape.width)
          val rawY = (shape.y + deltaY).coerceIn(0f, state.bedHeightMm - shape.height)
          val nx = (rawX / snap).roundToInt() * snap
          val ny = (rawY / snap).roundToInt() * snap
          shape.copy(x = nx, y = ny)
        } else shape
      }
      state.copy(shapes = newShapes)
    }
    regenerateToolpath()
  }

  fun addShape(type: ShapeType, textContent: String = "") {
    val state = _uiState.value
    val targetLayer = state.selectedLayerId
    val shape = when (type) {
      ShapeType.RECTANGLE -> LaserShape(
        name = "Rectangle",
        type = ShapeType.RECTANGLE,
        layerId = targetLayer,
        x = 50f,
        y = 50f,
        width = 80f,
        height = 60f,
        cornerRadius = 4f
      )
      ShapeType.CIRCLE -> LaserShape(
        name = "Circle",
        type = ShapeType.CIRCLE,
        layerId = targetLayer,
        x = 60f,
        y = 60f,
        width = 70f,
        height = 70f
      )
      ShapeType.STAR -> LaserShape(
        name = "Star Emblem",
        type = ShapeType.STAR,
        layerId = targetLayer,
        x = 50f,
        y = 50f,
        width = 65f,
        height = 65f,
        polygonPoints = 5
      )
      ShapeType.TEXT -> LaserShape(
        name = "Text Label",
        type = ShapeType.TEXT,
        layerId = targetLayer,
        x = 40f,
        y = 80f,
        width = 100f,
        height = 20f,
        text = textContent.ifEmpty { "LASERCRAFT" }
      )
      ShapeType.FRAME -> LaserShape(
        name = "Border Frame",
        type = ShapeType.FRAME,
        layerId = targetLayer,
        x = 30f,
        y = 30f,
        width = 120f,
        height = 90f
      )
      ShapeType.LIVING_HINGE -> LaserShape(
        name = "Living Hinge Flex",
        type = ShapeType.LIVING_HINGE,
        layerId = targetLayer,
        x = 40f,
        y = 40f,
        width = 100f,
        height = 60f
      )
      ShapeType.CALIBRATION_GRID -> LaserShape(
        name = "Test Matrix",
        type = ShapeType.CALIBRATION_GRID,
        layerId = targetLayer,
        x = 30f,
        y = 30f,
        width = 90f,
        height = 90f
      )
    }

    _uiState.update { it.copy(shapes = it.shapes + shape, selectedShapeId = shape.id, showAddShapeDialog = false) }
    regenerateToolpath()
  }

  fun deleteSelectedShape() {
    val selId = _uiState.value.selectedShapeId ?: return
    _uiState.update { state ->
      state.copy(
        shapes = state.shapes.filterNot { it.id == selId },
        selectedShapeId = null
      )
    }
    regenerateToolpath()
  }

  fun duplicateSelectedShape() {
    val sel = _uiState.value.shapes.find { it.id == _uiState.value.selectedShapeId } ?: return
    val duplicate = sel.copy(
      id = UUID.randomUUID().toString(),
      name = "${sel.name} Copy",
      x = (sel.x + 10f).coerceAtMost(_uiState.value.bedWidthMm - sel.width),
      y = (sel.y + 10f).coerceAtMost(_uiState.value.bedHeightMm - sel.height)
    )
    _uiState.update { it.copy(shapes = it.shapes + duplicate, selectedShapeId = duplicate.id) }
    regenerateToolpath()
  }

  fun centerSelectedShape() {
    val sel = _uiState.value.shapes.find { it.id == _uiState.value.selectedShapeId } ?: return
    val state = _uiState.value
    val newX = (state.bedWidthMm - sel.width) / 2f
    val newY = (state.bedHeightMm - sel.height) / 2f
    updateShape(sel.copy(x = newX, y = newY))
  }

  fun assignLayerToSelectedShape(layerId: String) {
    val sel = _uiState.value.shapes.find { it.id == _uiState.value.selectedShapeId } ?: return
    updateShape(sel.copy(layerId = layerId))
  }

  // --- Layer Management ---
  fun updateLayer(updated: LaserLayer) {
    _uiState.update { state ->
      val newLayers = state.layers.map { if (it.id == updated.id) updated else it }
      state.copy(layers = newLayers)
    }
    regenerateToolpath()
  }

  fun toggleLayerOutput(layerId: String) {
    _uiState.update { state ->
      val newLayers = state.layers.map {
        if (it.id == layerId) it.copy(outputEnabled = !it.outputEnabled) else it
      }
      state.copy(layers = newLayers)
    }
    regenerateToolpath()
  }

  fun toggleLayerAirAssist(layerId: String) {
    _uiState.update { state ->
      val newLayers = state.layers.map {
        if (it.id == layerId) it.copy(airAssist = !it.airAssist) else it
      }
      state.copy(layers = newLayers)
    }
    regenerateToolpath()
  }

  fun applyMaterialPreset(preset: MaterialPreset, targetLayerId: String) {
    _uiState.update { state ->
      val newLayers = state.layers.map { layer ->
        if (layer.id == targetLayerId) {
          layer.copy(
            name = "${layer.id} - ${preset.name.take(18)}",
            mode = preset.recommendedMode,
            speedMmMin = preset.speedMmMin,
            powerMaxPercent = preset.powerPercent,
            powerMinPercent = (preset.powerPercent * 0.4f).coerceAtLeast(10f),
            passes = preset.passes,
            airAssist = preset.airAssist,
            lineIntervalMm = preset.lineIntervalMm
          )
        } else layer
      }
      state.copy(layers = newLayers, showMaterialsSheet = false)
    }
    appendConsoleOutput("Applied preset '${preset.name}' to Layer $targetLayerId")
    regenerateToolpath()
  }

  // --- Real-time Simulation & Preview ---
  fun regenerateToolpath() {
    val state = _uiState.value
    val plan = LaserGCodeGenerator.generate(
      shapes = state.shapes,
      layers = state.layers,
      bedWidthMm = state.bedWidthMm,
      bedHeightMm = state.bedHeightMm
    )
    _uiState.update {
      it.copy(
        gcodeText = plan.gcode,
        simulation = plan.simulation
      )
    }
  }

  fun startSimulation() {
    if (_uiState.value.isSimulating) return
    val segments = _uiState.value.simulation.segments
    if (segments.isEmpty()) {
      regenerateToolpath()
    }
    if (_uiState.value.simulation.segments.isEmpty()) return

    _uiState.update { it.copy(isSimulating = true) }
    simulationJob?.cancel()
    simulationJob = viewModelScope.launch {
      val totalSec = _uiState.value.simulation.totalDurationSeconds.coerceAtLeast(1.0)
      var currentSec = (_uiState.value.simProgress * totalSec)
      val fps = 30
      val tickIntervalMs = 1000L / fps

      while (_uiState.value.isSimulating && currentSec < totalSec) {
        val speedMultiplier = _uiState.value.simPlaybackSpeed
        val deltaSec = (tickIntervalMs / 1000.0) * speedMultiplier
        currentSec += deltaSec
        val progress = (currentSec / totalSec).toFloat().coerceIn(0f, 1f)

        // Find current segment in simulation
        val (curSeg, segX, segY) = evaluateSimulationAtProgress(progress)

        _uiState.update {
          it.copy(
            simProgress = progress,
            simElapsedSeconds = currentSec,
            simLaserX = segX,
            simLaserY = segY,
            simIsFiring = curSeg != null && !curSeg.isRapid,
            simCurrentPower = curSeg?.powerPercent ?: 0f,
            simCurrentSpeed = curSeg?.speedMmMin ?: 0.0,
            simCurrentLayerColor = curSeg?.layerColor ?: Color.Transparent
          )
        }

        delay(tickIntervalMs)
      }

      _uiState.update {
        it.copy(
          isSimulating = false,
          simProgress = if (currentSec >= totalSec) 1f else it.simProgress,
          simIsFiring = false
        )
      }
    }
  }

  fun pauseSimulation() {
    simulationJob?.cancel()
    _uiState.update { it.copy(isSimulating = false, simIsFiring = false) }
  }

  fun resetSimulation() {
    simulationJob?.cancel()
    val (seg, sx, sy) = evaluateSimulationAtProgress(0f)
    _uiState.update {
      it.copy(
        isSimulating = false,
        simProgress = 0f,
        simElapsedSeconds = 0.0,
        simLaserX = sx,
        simLaserY = sy,
        simIsFiring = false
      )
    }
  }

  fun seekSimulation(progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    val totalSec = _uiState.value.simulation.totalDurationSeconds
    val (seg, sx, sy) = evaluateSimulationAtProgress(clamped)
    _uiState.update {
      it.copy(
        simProgress = clamped,
        simElapsedSeconds = clamped * totalSec,
        simLaserX = sx,
        simLaserY = sy,
        simIsFiring = seg != null && !seg.isRapid,
        simCurrentPower = seg?.powerPercent ?: 0f,
        simCurrentSpeed = seg?.speedMmMin ?: 0.0,
        simCurrentLayerColor = seg?.layerColor ?: Color.Transparent
      )
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    _uiState.update { it.copy(simPlaybackSpeed = speed) }
  }

  private fun evaluateSimulationAtProgress(progress: Float): Triple<ToolpathSegment?, Float, Float> {
    val segments = _uiState.value.simulation.segments
    if (segments.isEmpty()) return Triple(null, 0f, 0f)

    val totalDuration = _uiState.value.simulation.totalDurationSeconds.coerceAtLeast(0.001)
    val targetTime = progress * totalDuration

    var accumulated = 0.0
    for (seg in segments) {
      val segDur = seg.durationSeconds
      if (accumulated + segDur >= targetTime) {
        val fraction = if (segDur > 0) ((targetTime - accumulated) / segDur).toFloat().coerceIn(0f, 1f) else 1f
        val x = seg.startX + (seg.endX - seg.startX) * fraction
        val y = seg.startY + (seg.endY - seg.startY) * fraction
        return Triple(seg, x, y)
      }
      accumulated += segDur
    }

    val last = segments.last()
    return Triple(last, last.endX, last.endY)
  }

  // --- Machine Jog & Control ---
  fun setJogStep(stepMm: Float) {
    _uiState.update { it.copy(machineState = it.machineState.copy(jogStepMm = stepMm)) }
  }

  fun jog(deltaX: Float, deltaY: Float, deltaZ: Float = 0f) {
    val state = _uiState.value
    val cur = state.machineState
    val nx = (cur.posX + deltaX).coerceIn(0f, state.bedWidthMm)
    val ny = (cur.posY + deltaY).coerceIn(0f, state.bedHeightMm)
    val nz = (cur.posZ + deltaZ).coerceIn(-10f, 50f)

    val cmd = String.format(Locale.US, "\$J=G91 G21 X%.2f Y%.2f F%.0f", deltaX, deltaY, cur.jogSpeedMmMin)
    appendConsoleOutput(cmd, isOutgoing = true)
    appendConsoleOutput("ok")

    _uiState.update {
      it.copy(machineState = cur.copy(posX = nx, posY = ny, posZ = nz))
    }
  }

  fun homeMachine() {
    appendConsoleOutput("\$H", isOutgoing = true)
    viewModelScope.launch {
      appendConsoleOutput("[MSG:Homing cycle initiated...]")
      delay(600)
      _uiState.update {
        it.copy(machineState = it.machineState.copy(posX = 0f, posY = 0f, posZ = 0f, connectionStatus = ConnectionStatus.CONNECTED))
      }
      appendConsoleOutput("ok: Homing complete. Position: X0.000 Y0.000 Z0.000")
    }
  }

  fun setOrigin() {
    val cur = _uiState.value.machineState
    appendConsoleOutput("G92 X0 Y0 ; Set current position as Work Origin", isOutgoing = true)
    appendConsoleOutput("ok: Work Origin set to (${cur.posX}, ${cur.posY})")
    _uiState.update {
      it.copy(machineState = cur.copy(originX = cur.posX, originY = cur.posY))
    }
  }

  fun goToOrigin() {
    val cur = _uiState.value.machineState
    appendConsoleOutput("G0 X0 Y0 ; Move to Work Origin", isOutgoing = true)
    appendConsoleOutput("ok")
    _uiState.update {
      it.copy(machineState = cur.copy(posX = cur.originX, posY = cur.originY))
    }
  }

  fun frameWorkpiece() {
    if (framingJob?.isActive == true) {
      framingJob?.cancel()
      _uiState.update { it.copy(machineState = it.machineState.copy(isFraming = false)) }
      appendConsoleOutput("; Framing aborted.")
      return
    }

    val bounds = _uiState.value.simulation
    val minX = bounds.boundsMinX
    val minY = bounds.boundsMinY
    val maxX = bounds.boundsMaxX
    val maxY = bounds.boundsMaxY

    appendConsoleOutput(String.format(Locale.US, "; Framing bounding box: (%.1f, %.1f) to (%.1f, %.1f)", minX, minY, maxX, maxY))
    appendConsoleOutput("M03 S5 ; Pilot laser 0.5% for framing", isOutgoing = true)

    _uiState.update { it.copy(machineState = it.machineState.copy(isFraming = true)) }

    framingJob = viewModelScope.launch {
      val pts = listOf(
        Pair(minX, minY),
        Pair(maxX, minY),
        Pair(maxX, maxY),
        Pair(minX, maxY),
        Pair(minX, minY)
      )
      for (pt in pts) {
        _uiState.update {
          it.copy(machineState = it.machineState.copy(posX = pt.first, posY = pt.second))
        }
        delay(400)
      }
      appendConsoleOutput("M05 ; Laser OFF")
      _uiState.update { it.copy(machineState = it.machineState.copy(isFraming = false)) }
    }
  }

  fun testFireLaser(durationMs: Long = 600) {
    viewModelScope.launch {
      appendConsoleOutput("M03 S15 ; Test fire pulse 1.5% focus beam", isOutgoing = true)
      _uiState.update { it.copy(machineState = it.machineState.copy(isLaserFiring = true)) }
      delay(durationMs)
      appendConsoleOutput("M05 ; Laser OFF", isOutgoing = true)
      _uiState.update { it.copy(machineState = it.machineState.copy(isLaserFiring = false)) }
    }
  }

  fun emergencyStop() {
    simulationJob?.cancel()
    framingJob?.cancel()
    appendConsoleOutput("! ; IMMEDIATE HOLD / FEED STOP", isOutgoing = true)
    appendConsoleOutput("M05 ; Laser OFF", isOutgoing = true)
    appendConsoleOutput("Alarm: Machine halted by user.")
    _uiState.update {
      it.copy(
        isSimulating = false,
        machineState = it.machineState.copy(
          isLaserFiring = false,
          isFraming = false,
          isJobRunning = false,
          connectionStatus = ConnectionStatus.ALARM
        )
      )
    }
  }

  fun clearAlarm() {
    appendConsoleOutput("\$X ; Clear alarm lock", isOutgoing = true)
    appendConsoleOutput("[MSG:Caution: Unlocked]")
    appendConsoleOutput("ok")
    _uiState.update {
      it.copy(machineState = it.machineState.copy(connectionStatus = ConnectionStatus.CONNECTED))
    }
  }

  fun sendConsoleCommand(cmd: String) {
    if (cmd.isBlank()) return
    appendConsoleOutput(cmd, isOutgoing = true)
    val upper = cmd.trim().uppercase()
    when {
      upper == "?" -> {
        val s = _uiState.value.machineState
        appendConsoleOutput(String.format(Locale.US, "<Idle|WPos:%.3f,%.3f,%.3f|FS:0,0|Pn:P|WCO:%.3f,%.3f,0.000>", s.posX, s.posY, s.posZ, s.originX, s.originY))
      }
      upper == "\$H" -> homeMachine()
      upper == "\$X" -> clearAlarm()
      upper == "\$\$" -> {
        appendConsoleOutput("\$30=1000 (Max spindle/laser RPM)")
        appendConsoleOutput("\$31=0 (Min spindle/laser RPM)")
        appendConsoleOutput("\$32=1 (Laser-mode enable)")
        appendConsoleOutput("\$100=80.000 (x:step/mm)")
        appendConsoleOutput("\$101=80.000 (y:step/mm)")
        appendConsoleOutput("\$110=6000.000 (x:max rate mm/min)")
        appendConsoleOutput("\$111=6000.000 (y:max rate mm/min)")
        appendConsoleOutput("ok")
      }
      upper.startsWith("G0") || upper.startsWith("G1") -> {
        appendConsoleOutput("ok: Executed motion.")
      }
      else -> {
        appendConsoleOutput("ok")
      }
    }
  }

  private fun appendConsoleOutput(text: String, isOutgoing: Boolean = false) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
    val time = sdf.format(Date())
    val msg = ConsoleMessage(text = text, isOutgoing = isOutgoing, timestamp = time)
    _uiState.update { state ->
      val newHistory = (state.machineState.consoleHistory + msg).takeLast(80)
      state.copy(machineState = state.machineState.copy(consoleHistory = newHistory))
    }
  }

  // --- Preloaded Project Templates ---
  fun loadProjectTemplate(name: String) {
    simulationJob?.cancel()
    framingJob?.cancel()

    val shapes = when (name) {
      "Mandala Coaster" -> listOf(
        LaserShape(
          name = "Coaster Outer Cut",
          type = ShapeType.CIRCLE,
          layerId = "C00",
          x = 40f,
          y = 40f,
          width = 100f,
          height = 100f
        ),
        LaserShape(
          name = "Mandala Star Score",
          type = ShapeType.STAR,
          layerId = "C01",
          x = 55f,
          y = 55f,
          width = 70f,
          height = 70f,
          polygonPoints = 8
        ),
        LaserShape(
          name = "Center Monogram",
          type = ShapeType.TEXT,
          layerId = "C02",
          x = 65f,
          y = 80f,
          width = 50f,
          height = 20f,
          text = "LIGHT"
        )
      )

      "Living Hinge Box" -> listOf(
        LaserShape(
          name = "Box Perimeter Cut",
          type = ShapeType.RECTANGLE,
          layerId = "C00",
          x = 30f,
          y = 30f,
          width = 150f,
          height = 110f,
          cornerRadius = 2f
        ),
        LaserShape(
          name = "Flexible Kerf Hinge",
          type = ShapeType.LIVING_HINGE,
          layerId = "C00",
          x = 75f,
          y = 35f,
          width = 60f,
          height = 100f
        ),
        LaserShape(
          name = "Brand Stamp",
          type = ShapeType.TEXT,
          layerId = "C02",
          x = 38f,
          y = 75f,
          width = 30f,
          height = 16f,
          text = "BURN"
        )
      )

      "Laser Calibration Grid" -> listOf(
        LaserShape(
          name = "Speed-Power Matrix",
          type = ShapeType.CALIBRATION_GRID,
          layerId = "C02",
          x = 30f,
          y = 30f,
          width = 120f,
          height = 120f
        ),
        LaserShape(
          name = "Matrix Cut Boundary",
          type = ShapeType.RECTANGLE,
          layerId = "C00",
          x = 25f,
          y = 25f,
          width = 130f,
          height = 130f
        )
      )

      "Leather Keyring Tag" -> listOf(
        LaserShape(
          name = "Keyring Body Cut",
          type = ShapeType.RECTANGLE,
          layerId = "C00",
          x = 40f,
          y = 40f,
          width = 110f,
          height = 36f,
          cornerRadius = 18f
        ),
        LaserShape(
          name = "Rivet Hole Cut",
          type = ShapeType.CIRCLE,
          layerId = "C00",
          x = 48f,
          y = 53f,
          width = 10f,
          height = 10f
        ),
        LaserShape(
          name = "Custom Name Engrave",
          type = ShapeType.TEXT,
          layerId = "C02",
          x = 65f,
          y = 48f,
          width = 75f,
          height = 20f,
          text = "MAKER"
        )
      )

      else -> emptyList()
    }

    _uiState.update {
      it.copy(
        activeProjectName = name,
        shapes = shapes,
        selectedShapeId = shapes.firstOrNull()?.id,
        simProgress = 0f,
        isSimulating = false
      )
    }
    regenerateToolpath()
  }

  fun setAddShapeDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showAddShapeDialog = visible) }
  }

  fun setMaterialsSheetVisible(visible: Boolean) {
    _uiState.update { it.copy(showMaterialsSheet = visible) }
  }

  fun setLayerSettingsDialog(layerId: String?, visible: Boolean) {
    _uiState.update { it.copy(editingLayerId = layerId, showLayerSettingsDialog = visible) }
  }

  fun setGCodeInputDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showGCodeInputDialog = visible) }
  }

  fun importGCode(rawGCode: String) {
    simulationJob?.cancel()
    framingJob?.cancel()
    val plan = com.example.engine.LaserGCodeParser.parse(rawGCode)
    _uiState.update {
      it.copy(
        gcodeText = plan.gcode,
        simulation = plan.simulation,
        simProgress = 0f,
        isSimulating = false,
        currentTab = StudioTab.PREVIEW,
        showGCodeInputDialog = false,
        activeProjectName = "Imported G-Code (${plan.simulation.segments.size} moves)"
      )
    }
    appendConsoleOutput("Imported G-Code program: ${plan.simulation.segments.size} segments parsed.")
    appendConsoleOutput("Est. Cut Dist: ${String.format(Locale.US, "%.1f", plan.simulation.totalCutDistanceMm)}mm | Rapid: ${String.format(Locale.US, "%.1f", plan.simulation.totalRapidDistanceMm)}mm")
  }
}
