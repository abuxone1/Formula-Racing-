package com.example.model

enum class RadioMessageType(val title: String, val badgeColorHex: Long) {
    STRATEGY("PIT WALL STRATEGY", 0xFF00E5FF),
    FLAG("RACE CONTROL", 0xFFFFB800),
    PACE("RACE ENGINEER", 0xFF00E676),
    ALERT("SYSTEM ALERT", 0xFFFF1801)
}

data class RadioMessage(
    val id: String,
    val type: RadioMessageType,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 4500L
)

class RaceEngineerRadio {
    var activeMessage: RadioMessage? = null
        private set
    private var messageExpiryTimeMs: Long = 0L

    private val radioHistory = mutableListOf<RadioMessage>()

    fun triggerMessage(type: RadioMessageType, text: String) {
        val now = System.currentTimeMillis()
        val msg = RadioMessage(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            text = text,
            timestampMs = now
        )
        activeMessage = msg
        messageExpiryTimeMs = now + msg.durationMs
        radioHistory.add(msg)
    }

    fun update() {
        val now = System.currentTimeMillis()
        if (activeMessage != null && now > messageExpiryTimeMs) {
            activeMessage = null
        }
    }

    fun getHistory(): List<RadioMessage> = radioHistory.takeLast(10)
}
