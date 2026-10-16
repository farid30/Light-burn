package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class LaserMode(val label: String, val description: String) {
  LINE("Line (Cut)", "Vector perimeter cut or shallow score"),
  FILL("Fill (Engrave)", "Bi-directional raster scanline engraving"),
  FILL_AND_LINE("Fill + Line", "Raster engrave interior followed by perimeter cut"),
  OFFSET_FILL("Offset Fill", "Inward concentric pathing for rapid engraving")
}

data class LaserLayer(
  val id: String,                 // "C00", "C01", etc.
  val name: String,               // "C00 - Line / Cut"
  val color: Color,
  val colorHex: String,
  val mode: LaserMode = LaserMode.LINE,
  val speedMmMin: Double = 600.0,
  val powerMaxPercent: Float = 80f,
  val powerMinPercent: Float = 20f,
  val passes: Int = 1,
  val airAssist: Boolean = true,
  val outputEnabled: Boolean = true,
  val isVisible: Boolean = true,
  val kerfOffsetMm: Double = 0.0,
  val lineIntervalMm: Double = 0.08, // raster scanline density
  val zOffsetMm: Double = 0.0
) {
  companion object {
    val DefaultLayers = listOf(
      LaserLayer(
        id = "C00",
        name = "C00 - Cut",
        color = LayerC00Cut,
        colorHex = "#22C55E",
        mode = LaserMode.LINE,
        speedMmMin = 500.0,
        powerMaxPercent = 90f,
        powerMinPercent = 90f,
        passes = 2,
        airAssist = true
      ),
      LaserLayer(
        id = "C01",
        name = "C01 - Score",
        color = LayerC01Score,
        colorHex = "#06B6D4",
        mode = LaserMode.LINE,
        speedMmMin = 2000.0,
        powerMaxPercent = 35f,
        powerMinPercent = 20f,
        passes = 1,
        airAssist = true
      ),
      LaserLayer(
        id = "C02",
        name = "C02 - Engrave",
        color = LayerC02Engrave,
        colorHex = "#F43F5E",
        mode = LaserMode.FILL,
        speedMmMin = 3500.0,
        powerMaxPercent = 45f,
        powerMinPercent = 15f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.08
      ),
      LaserLayer(
        id = "C03",
        name = "C03 - Fill+Cut",
        color = LayerC03FillLine,
        colorHex = "#A855F7",
        mode = LaserMode.FILL_AND_LINE,
        speedMmMin = 1800.0,
        powerMaxPercent = 65f,
        powerMinPercent = 40f,
        passes = 1,
        airAssist = true
      ),
      LaserLayer(
        id = "C04",
        name = "C04 - Offset",
        color = LayerC04Offset,
        colorHex = "#EAB308",
        mode = LaserMode.OFFSET_FILL,
        speedMmMin = 2800.0,
        powerMaxPercent = 50f,
        powerMinPercent = 30f,
        passes = 1,
        airAssist = false
      )
    )
  }
}
