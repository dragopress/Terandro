package com.example.model

data class DiagnosticItem(
    val title: String,
    val description: String,
    val status: DiagnosticStatus,
    val detail: String,
    val recommendation: String? = null
)

enum class DiagnosticStatus {
    PASSED,
    WARNING,
    FAILED,
    INFO
}

data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val overallScore: Int, // 0 - 100
    val detectedEnvironment: String, // "Termux on Android", "Standard Linux / CI", etc.
    val architecture: String, // e.g. "arm64-v8a" or "x86_64"
    val androidApiLevel: Int,
    val libcVariant: String, // "Bionic libc (Android)" or "GNU libc (glibc)"
    val availableMemoryMb: Long,
    val totalMemoryMb: Long,
    val storageAvailableMb: Long,
    val items: List<DiagnosticItem>
)
