package com.example.model

enum class ConnectionStatus(val label: String, val isReady: Boolean) {
  CONNECTED("GRBL 1.1h (Laser Mode On)", true),
  CONNECTING("Connecting via USB/Serial...", false),
  DISCONNECTED("Offline / Standby", false),
  ALARM("Machine Alarm (\$H Needed)", false),
  BUSY("Job In Progress", false)
}

data class ConsoleMessage(
  val id: Long = System.currentTimeMillis() + (0..1000).random(),
  val text: String,
  val isOutgoing: Boolean,
  val timestamp: String
)

data class MachineState(
  val posX: Float = 0f,
  val posY: Float = 0f,
  val posZ: Float = 0f,
  val originX: Float = 0f,
  val originY: Float = 0f,
  val jogStepMm: Float = 10f,
  val jogSpeedMmMin: Float = 2500f,
  val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
  val isLaserFiring: Boolean = false,
  val isFraming: Boolean = false,
  val isJobRunning: Boolean = false,
  val jobProgressPercent: Float = 0f,
  val airAssistActive: Boolean = false,
  val doorClosed: Boolean = true,
  val consoleHistory: List<ConsoleMessage> = emptyList()
)
