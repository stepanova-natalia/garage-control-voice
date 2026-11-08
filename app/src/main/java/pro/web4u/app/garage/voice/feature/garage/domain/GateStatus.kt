package pro.web4u.app.garage.voice.feature.garage.domain

enum class GateStatus(val raw: String) {
    OPEN("open"),
    CLOSE("close"),
    MOVE("move"),
    STOP("stop"),
    UNKNOWN("unknown");

    companion object {
        fun fromRaw(raw: String): GateStatus =
            entries.firstOrNull { it.raw == raw } ?: UNKNOWN
    }
}