package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.SimulationResult
import com.example.model.ToolpathSegment
import com.example.ui.theme.LayerC00Cut
import com.example.ui.theme.LayerC01Score
import com.example.ui.theme.RapidTravelColor
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

object LaserGCodeParser {

  private val WORD_PATTERN = Pattern.compile("([A-Za-z])([-+]?[0-9]*\\.?[0-9]+)")

  fun parse(rawGCode: String): LaserGCodeGenerator.ToolpathPlan {
    val segments = mutableListOf<ToolpathSegment>()
    var currentX = 0f
    var currentY = 0f
    var currentSpeed = 1000.0
    var currentPower = 0f // 0..100%
    var isLaserActive = false
    var isAbsolute = true

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    var totalCutDist = 0f
    var totalRapidDist = 0f
    var totalDurationSec = 0.0

    val lines = rawGCode.lines()
    for (line in lines) {
      // Strip comments (; or (...))
      val cleaned = line.replace(Regex(";.*$"), "")
        .replace(Regex("\\(.*\\)"), "")
        .trim()
        .uppercase(Locale.US)

      if (cleaned.isEmpty()) continue

      val matcher = WORD_PATTERN.matcher(cleaned)
      var targetX: Float? = null
      var targetY: Float? = null
      var isRapid = false
      var isLinearCut = false

      while (matcher.find()) {
        val letter = matcher.group(1)?.firstOrNull() ?: continue
        val value = matcher.group(2)?.toFloatOrNull() ?: continue

        when (letter) {
          'G' -> {
            when (value.toInt()) {
              0 -> isRapid = true
              1 -> isLinearCut = true
              90 -> isAbsolute = true
              91 -> isAbsolute = false
            }
          }
          'M' -> {
            when (value.toInt()) {
              3, 4 -> isLaserActive = true
              5 -> isLaserActive = false
            }
          }
          'X' -> {
            targetX = if (isAbsolute) value else currentX + value
          }
          'Y' -> {
            targetY = if (isAbsolute) value else currentY + value
          }
          'F' -> {
            currentSpeed = value.toDouble().coerceAtLeast(60.0)
          }
          'S' -> {
            // S value typically 0..1000 or 0..255
            currentPower = if (value <= 255f) (value / 255f * 100f) else (value / 1000f * 100f).coerceIn(0f, 100f)
            if (currentPower > 0f) isLaserActive = true
          }
        }
      }

      val endX = targetX ?: currentX
      val endY = targetY ?: currentY

      if (endX != currentX || endY != currentY) {
        minX = min(minX, min(currentX, endX))
        minY = min(minY, min(currentY, endY))
        maxX = max(maxX, max(currentX, endX))
        maxY = max(maxY, max(currentY, endY))

        val rapidMove = isRapid || !isLaserActive || currentPower <= 0f
        val seg = ToolpathSegment(
          startX = currentX,
          startY = currentY,
          endX = endX,
          endY = endY,
          isRapid = rapidMove,
          powerPercent = if (rapidMove) 0f else currentPower,
          speedMmMin = if (rapidMove) 6000.0 else currentSpeed,
          layerColor = if (rapidMove) RapidTravelColor else LayerC00Cut,
          layerId = if (rapidMove) "RAPID" else "IMPORTED",
          passIndex = 1
        )

        segments.add(seg)
        if (rapidMove) {
          totalRapidDist += seg.lengthMm
        } else {
          totalCutDist += seg.lengthMm
        }
        totalDurationSec += seg.durationSeconds

        currentX = endX
        currentY = endY
      }
    }

    if (minX == Float.MAX_VALUE) {
      minX = 0f
      minY = 0f
      maxX = 100f
      maxY = 100f
    }

    val sim = SimulationResult(
      segments = segments,
      totalCutDistanceMm = totalCutDist,
      totalRapidDistanceMm = totalRapidDist,
      totalDurationSeconds = totalDurationSec.coerceAtLeast(1.0),
      boundsMinX = minX,
      boundsMinY = minY,
      boundsMaxX = maxX,
      boundsMaxY = maxY
    )

    return LaserGCodeGenerator.ToolpathPlan(
      gcode = rawGCode,
      simulation = sim
    )
  }
}
