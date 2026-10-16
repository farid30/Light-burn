package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LaserGCodeGenerator
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun LaserCanvas(
  bedWidthMm: Float,
  bedHeightMm: Float,
  shapes: List<LaserShape>,
  layers: List<LaserLayer>,
  selectedShapeId: String?,
  laserHeadX: Float,
  laserHeadY: Float,
  isLaserFiring: Boolean,
  isGridSnapEnabled: Boolean,
  gridSnapMm: Float,
  onSelectShape: (String?) -> Unit,
  onMoveShape: (String, Float, Float) -> Unit,
  onResetView: () -> Unit,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1.8f) }
  var panOffset by remember { mutableStateOf(Offset(40f, 40f)) }

  val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(0.5f, 6.0f)
    panOffset += offsetChange
  }

  val layerMap = remember(layers) { layers.associateBy { it.id } }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BedBackground)
      .transformable(state = transformState)
      .pointerInput(shapes, scale, panOffset) {
        detectDragGestures(
          onDragStart = { startOffset ->
            // Convert screen tap to bed mm coordinates
            val bedX = (startOffset.x - panOffset.x) / scale
            val bedY = (startOffset.y - panOffset.y) / scale
            // Check if any shape hit
            val hit = shapes.findLast { it.containsPoint(bedX, bedY) }
            onSelectShape(hit?.id)
          },
          onDrag = { change, dragAmount ->
            change.consume()
            selectedShapeId?.let { id ->
              val dMmX = dragAmount.x / scale
              val dMmY = dragAmount.y / scale
              onMoveShape(id, dMmX, dMmY)
            } ?: run {
              // Pan if no shape selected
              panOffset += dragAmount
            }
          }
        )
      }
      .testTag("laser_bed_canvas")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasW = size.width
      val canvasH = size.height

      // 1. Draw Bed Surface Grid in mm
      val bedPixelW = bedWidthMm * scale
      val bedPixelH = bedHeightMm * scale

      // Bed boundary rectangle
      drawRect(
        color = SurfaceDark,
        topLeft = panOffset,
        size = Size(bedPixelW, bedPixelH)
      )

      // Draw minor grid (10mm) and major grid (50mm)
      val step10 = 10f * scale
      val numCols = (bedWidthMm / 10f).toInt()
      val numRows = (bedHeightMm / 10f).toInt()

      // Vertical grid lines
      for (i in 0..numCols) {
        val x = panOffset.x + i * step10
        val isMajor = (i % 5 == 0)
        drawLine(
          color = if (isMajor) BedGridMajor else BedGridMinor,
          start = Offset(x, panOffset.y),
          end = Offset(x, panOffset.y + bedPixelH),
          strokeWidth = if (isMajor) 1.5f else 0.75f
        )
      }

      // Horizontal grid lines
      for (j in 0..numRows) {
        val y = panOffset.y + j * step10
        val isMajor = (j % 5 == 0)
        drawLine(
          color = if (isMajor) BedGridMajor else BedGridMinor,
          start = Offset(panOffset.x, y),
          end = Offset(panOffset.x + bedPixelW, y),
          strokeWidth = if (isMajor) 1.5f else 0.75f
        )
      }

      // Bed outer frame
      drawRect(
        color = BorderDark,
        topLeft = panOffset,
        size = Size(bedPixelW, bedPixelH),
        style = Stroke(width = 2f)
      )

      // Origin Axes (Bottom-Left / Top-Left) - standard CNC Top-Left (0,0) with X right, Y down
      drawLine(
        color = BedOriginAxisX,
        start = panOffset,
        end = Offset(panOffset.x + 40f * scale, panOffset.y),
        strokeWidth = 3f
      )
      drawLine(
        color = BedOriginAxisY,
        start = panOffset,
        end = Offset(panOffset.x, panOffset.y + 40f * scale),
        strokeWidth = 3f
      )

      // 2. Draw Shapes
      for (shape in shapes) {
        val layer = layerMap[shape.layerId] ?: LaserLayer.DefaultLayers.first()
        if (!layer.isVisible) continue

        val shapeColor = layer.color
        val sx = panOffset.x + shape.x * scale
        val sy = panOffset.y + shape.y * scale
        val sw = shape.width * scale
        val sh = shape.height * scale
        val isSelected = shape.id == selectedShapeId

        // Render shape contours
        drawShapeContent(
          shape = shape,
          layer = layer,
          color = shapeColor,
          topLeft = Offset(sx, sy),
          size = Size(sw, sh),
          scale = scale,
          isSelected = isSelected
        )

        // Draw Selection Bounding Box & Handles
        if (isSelected) {
          drawRect(
            color = LaserAmber,
            topLeft = Offset(sx - 4f, sy - 4f),
            size = Size(sw + 8f, sh + 8f),
            style = Stroke(
              width = 2f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )
          )

          // Corner handles
          val handleRadius = 6f
          val corners = listOf(
            Offset(sx - 4f, sy - 4f),
            Offset(sx + sw + 4f, sy - 4f),
            Offset(sx + sw + 4f, sy + sh + 4f),
            Offset(sx - 4f, sy + sh + 4f)
          )
          for (c in corners) {
            drawCircle(color = Color.White, radius = handleRadius, center = c)
            drawCircle(color = LaserAmber, radius = handleRadius, center = c, style = Stroke(2f))
          }

          // Center coordinate crosshair
          val center = Offset(sx + sw / 2f, sy + sh / 2f)
          drawLine(LaserAmber, Offset(center.x - 8f, center.y), Offset(center.x + 8f, center.y), 1.5f)
          drawLine(LaserAmber, Offset(center.x, center.y - 8f), Offset(center.x, center.y + 8f), 1.5f)
        }
      }

      // 3. Draw Laser Head Crosshair Pointer
      val headPx = panOffset.x + laserHeadX * scale
      val headPy = panOffset.y + laserHeadY * scale

      // Red laser beam dot & glow
      if (isLaserFiring) {
        drawCircle(
          color = LaserAmberGlow.copy(alpha = 0.4f),
          radius = 16f,
          center = Offset(headPx, headPy)
        )
        drawCircle(
          color = LaserRed,
          radius = 5f,
          center = Offset(headPx, headPy)
        )
      } else {
        drawCircle(
          color = LaserRed,
          radius = 3.5f,
          center = Offset(headPx, headPy)
        )
      }

      // Crosshair lines
      drawLine(
        color = LaserRed.copy(alpha = 0.8f),
        start = Offset(headPx - 18f, headPy),
        end = Offset(headPx + 18f, headPy),
        strokeWidth = 1.5f
      )
      drawLine(
        color = LaserRed.copy(alpha = 0.8f),
        start = Offset(headPx, headPy - 18f),
        end = Offset(headPx, headPy + 18f),
        strokeWidth = 1.5f
      )
    }

    // Top-Left Bed Coordinates HUD
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = SurfaceElevatedDark.copy(alpha = 0.9f),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Column {
          Text(
            text = "BED: ${bedWidthMm.toInt()}x${bedHeightMm.toInt()} mm",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
          )
          Text(
            text = "HEAD: X${String.format("%.1f", laserHeadX)} Y${String.format("%.1f", laserHeadY)}",
            style = MaterialTheme.typography.labelLarge,
            color = LaserAmber
          )
        }

        VerticalDivider(modifier = Modifier.height(24.dp), color = BorderDark)

        // Grid Snap Indicator
        AssistChip(
          onClick = { /* handled by parent */ },
          label = { Text("Snap ${gridSnapMm.toInt()}mm", fontSize = 11.sp) },
          leadingIcon = {
            Icon(
              if (isGridSnapEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
              contentDescription = "Snap",
              modifier = Modifier.size(14.dp),
              tint = if (isGridSnapEnabled) LaserAmber else TextTertiary
            )
          },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isGridSnapEnabled) SurfaceVariantDark else Color.Transparent
          ),
          modifier = Modifier.height(28.dp)
        )
      }
    }

    // Bottom-Right Zoom & Pan Controls
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = SurfaceElevatedDark.copy(alpha = 0.9f),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { scale = (scale * 1.25f).coerceAtMost(6.0f) },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextPrimary)
        }
        Text(
          text = "${(scale / 1.8f * 100).toInt()}%",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(
          onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextPrimary)
        }
        IconButton(
          onClick = {
            scale = 1.8f
            panOffset = Offset(40f, 40f)
            onResetView()
          },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View", tint = LaserAmber)
        }
      }
    }
  }
}

/**
 * Draws the vector paths or raster fill for a laser shape
 */
private fun DrawScope.drawShapeContent(
  shape: LaserShape,
  layer: LaserLayer,
  color: Color,
  topLeft: Offset,
  size: Size,
  scale: Float,
  isSelected: Boolean
) {
  val isFillMode = layer.mode == LaserMode.FILL || layer.mode == LaserMode.FILL_AND_LINE

  // If fill mode, draw semi-transparent hatch / fill
  if (isFillMode) {
    when (shape.type) {
      ShapeType.RECTANGLE -> {
        drawRoundRect(
          color = color.copy(alpha = 0.25f),
          topLeft = topLeft,
          size = size,
          cornerRadius = CornerRadius(shape.cornerRadius * scale, shape.cornerRadius * scale)
        )
      }
      ShapeType.CIRCLE -> {
        drawOval(
          color = color.copy(alpha = 0.25f),
          topLeft = topLeft,
          size = size
        )
      }
      else -> {
        drawRect(
          color = color.copy(alpha = 0.20f),
          topLeft = topLeft,
          size = size
        )
      }
    }
  }

  // Draw contours (perimeter stroke)
  val contours = LaserGCodeGenerator.getContoursForShape(shape)
  for (contour in contours) {
    if (contour.size < 2) continue
    val path = Path()
    val first = contour.first()
    path.moveTo(topLeft.x + (first.first - shape.x) * scale, topLeft.y + (first.second - shape.y) * scale)
    for (i in 1 until contour.size) {
      val pt = contour[i]
      path.lineTo(topLeft.x + (pt.first - shape.x) * scale, topLeft.y + (pt.second - shape.y) * scale)
    }

    drawPath(
      path = path,
      color = color,
      style = Stroke(
        width = if (layer.mode == LaserMode.LINE) 2.2f else 1.2f
      )
    )
  }

  // Draw label tag near shape
  val label = "${layer.id} | ${shape.name}"
  drawContext.canvas.nativeCanvas.apply {
    val paint = android.graphics.Paint().apply {
      this.color = android.graphics.Color.argb(180, 200, 210, 225)
      this.textSize = 10f * scale.coerceIn(0.8f, 1.4f)
      this.isAntiAlias = true
    }
    drawText(label, topLeft.x, (topLeft.y - 4f).coerceAtLeast(14f), paint)
  }
}
