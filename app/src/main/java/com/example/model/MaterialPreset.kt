package com.example.model

enum class MaterialCategory(val label: String) {
  WOOD("Wood & Timber"),
  ACRYLIC("Acrylic & Plastics"),
  LEATHER("Leather & Fabric"),
  METAL("Metals"),
  STONE("Stone & Ceramics"),
  PAPER("Paper & Board")
}

data class MaterialPreset(
  val id: String,
  val name: String,
  val category: MaterialCategory,
  val thicknessMm: Double,
  val recommendedMode: LaserMode,
  val speedMmMin: Double,
  val powerPercent: Float,
  val passes: Int = 1,
  val airAssist: Boolean = true,
  val lineIntervalMm: Double = 0.08,
  val tips: String
) {
  companion object {
    val Presets = listOf(
      MaterialPreset(
        id = "birch_3mm_cut",
        name = "3mm Baltic Birch Plywood - Cut",
        category = MaterialCategory.WOOD,
        thicknessMm = 3.0,
        recommendedMode = LaserMode.LINE,
        speedMmMin = 500.0,
        powerPercent = 90f,
        passes = 2,
        airAssist = true,
        tips = "High air assist required to avoid charring. Check focus height on flat surface."
      ),
      MaterialPreset(
        id = "birch_engrave",
        name = "Wood Surface Photo / Logo Engrave",
        category = MaterialCategory.WOOD,
        thicknessMm = 3.0,
        recommendedMode = LaserMode.FILL,
        speedMmMin = 3500.0,
        powerPercent = 35f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.08,
        tips = "Low air assist prevents soot from embedding into wood pores."
      ),
      MaterialPreset(
        id = "acrylic_5mm_cut",
        name = "5mm Cast Acrylic - Cut",
        category = MaterialCategory.ACRYLIC,
        thicknessMm = 5.0,
        recommendedMode = LaserMode.LINE,
        speedMmMin = 350.0,
        powerPercent = 95f,
        passes = 2,
        airAssist = true,
        tips = "Use paper masking to prevent flashback marks on honeycomb bed."
      ),
      MaterialPreset(
        id = "acrylic_engrave",
        name = "Acrylic Frost Engrave (Mirror)",
        category = MaterialCategory.ACRYLIC,
        thicknessMm = 3.0,
        recommendedMode = LaserMode.FILL,
        speedMmMin = 4000.0,
        powerPercent = 25f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.07,
        tips = "Engrave on reverse side mirrored for glass-like front finish."
      ),
      MaterialPreset(
        id = "leather_cut",
        name = "Vegetable-Tanned Leather 2.5mm - Cut",
        category = MaterialCategory.LEATHER,
        thicknessMm = 2.5,
        recommendedMode = LaserMode.LINE,
        speedMmMin = 850.0,
        powerPercent = 75f,
        passes = 1,
        airAssist = true,
        tips = "Ventilation is critical. Clean edges with damp microfiber."
      ),
      MaterialPreset(
        id = "leather_engrave",
        name = "Leather Branding / Dark Stamp",
        category = MaterialCategory.LEATHER,
        thicknessMm = 2.5,
        recommendedMode = LaserMode.FILL,
        speedMmMin = 3000.0,
        powerPercent = 28f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.10,
        tips = "Slightly out of focus (+1mm Z) creates darker, embossed vintage look."
      ),
      MaterialPreset(
        id = "slate_coaster",
        name = "Natural Slate Coaster - Engrave",
        category = MaterialCategory.STONE,
        thicknessMm = 6.0,
        recommendedMode = LaserMode.FILL,
        speedMmMin = 4200.0,
        powerPercent = 50f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.065,
        tips = "Produces vivid white micro-fracture mark. Seal with mineral oil after."
      ),
      MaterialPreset(
        id = "anodized_alum",
        name = "Anodized Aluminum - Laser Bleach",
        category = MaterialCategory.METAL,
        thicknessMm = 1.0,
        recommendedMode = LaserMode.FILL,
        speedMmMin = 5000.0,
        powerPercent = 60f,
        passes = 1,
        airAssist = false,
        lineIntervalMm = 0.05,
        tips = "Removes dye without ablating base metal. Extremely crisp resolution."
      ),
      MaterialPreset(
        id = "cardboard_cut",
        name = "Corrugated Cardboard 4mm - Cut",
        category = MaterialCategory.PAPER,
        thicknessMm = 4.0,
        recommendedMode = LaserMode.LINE,
        speedMmMin = 1400.0,
        powerPercent = 45f,
        passes = 1,
        airAssist = true,
        tips = "Fast pass prevents internal fluting fire hazard. Never leave unattended."
      )
    )
  }
}
