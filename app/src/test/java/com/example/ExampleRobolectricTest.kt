package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.LaserGCodeGenerator
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LightBurn", appName)
  }

  @Test
  fun `laser gcode generator produces valid gcode and simulation`() {
    val shape = LaserShape(
      name = "Test Cut",
      type = ShapeType.RECTANGLE,
      layerId = "C00",
      x = 10f,
      y = 10f,
      width = 50f,
      height = 50f
    )
    val plan = LaserGCodeGenerator.generate(
      shapes = listOf(shape),
      layers = LaserLayer.DefaultLayers
    )

    assertTrue(plan.gcode.contains("G21"))
    assertTrue(plan.gcode.contains("G90"))
    assertTrue(plan.gcode.contains("M03"))
    assertTrue(plan.gcode.contains("M05"))
    assertTrue(plan.simulation.segments.isNotEmpty())
    assertTrue(plan.simulation.totalCutDistanceMm > 0f)
  }

  @Test
  fun `laser gcode parser extracts moves and simulation segments`() {
    val sampleGcode = """
      G21
      G90
      G0 X10.0 Y10.0
      M03 S800
      G1 X60.0 Y10.0 F1200
      G1 X60.0 Y60.0
      M05
    """.trimIndent()

    val parsed = com.example.engine.LaserGCodeParser.parse(sampleGcode)
    assertEquals(3, parsed.simulation.segments.size)
    assertTrue(parsed.simulation.segments[0].isRapid)
    assertTrue(!parsed.simulation.segments[1].isRapid)
    assertEquals(80f, parsed.simulation.segments[1].powerPercent, 0.1f)
  }
}
