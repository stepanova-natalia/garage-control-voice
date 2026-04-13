package pro.web4u.app.garage.voice.feature.voice

enum class Command {
    GATE_OPEN,
    GATE_CLOSE,
    GATE_STATUS,
    LIGHT_ON,
    LIGHT_OFF,
    LIGHT_STATUS,
    UNKNOWN
}

object CommandParser {
    fun parse(text: String, assistantName: String): Command {
        val t = text.lowercase().trim()
        val name = assistantName.lowercase().trim()

        if(!t.contains(name)) return Command.UNKNOWN

        return when {
            t.contains("открой ворота") || t.contains("открыть ворота") -> Command.GATE_OPEN
            t.contains("закрой ворота") || t.contains("закрыть ворота") -> Command.GATE_CLOSE
            t.contains("статус ворот") -> Command.GATE_STATUS
            t.contains("включи свет") || t.contains("включить свет") -> Command.LIGHT_ON
            t.contains("выключи свет") || t.contains("выключить свет") -> Command.LIGHT_OFF
            t.contains("статус света") -> Command.LIGHT_STATUS
            else -> Command.UNKNOWN
        }
    }
}