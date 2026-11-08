package pro.web4u.app.garage.voice.feature.garage.domain

enum class LightStatus(val raw: String) {
    ON("on"),
    OFF("off"),
    UNKNOWN("unknown");

    companion object {
        fun fromRaw(raw: String): LightStatus =
            entries.firstOrNull { it.raw == raw } ?: UNKNOWN
    }
}