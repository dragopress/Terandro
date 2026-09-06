package com.example.model

enum class ErrorCategory(val displayName: String, val severity: String) {
    REAL_APPLICATION_TEST_FAILURE("Real Test Suite Failure", "CRITICAL - NEVER MASK"),
    REAL_APPLICATION_SYNTAX_ERROR("Real Code / Compilation Error", "CRITICAL - NEVER MASK"),
    TERMUX_BIONIC_GLIBC_MISMATCH("Bionic vs Glibc Linkage Conflict", "ENVIRONMENT"),
    MISSING_NATIVE_HEADER_OR_TOOL("Missing C/Native Toolchain", "DEPENDENCY"),
    MEMORY_EXHAUSTION_LMK("Memory Exhaustion / LMK Kill (Signal 9)", "RESOURCE"),
    SHEBANG_PATH_ERROR("Bad Shebang / Path Mismatch", "ENVIRONMENT"),
    PERMISSION_DENIED("Permission Denied (chmod/exec)", "ENVIRONMENT"),
    DEPENDENCY_RESOLUTION_FAILURE("Dependency Resolution Failure", "PACKAGE_MANAGER"),
    UNKNOWN_ERROR("Unclassified Process Error", "DIAGNOSTIC")
}

enum class PlatformTarget(val label: String) {
    TERMUX_ONLY("Termux Only"),
    CI_ONLY("GitHub Actions CI Only"),
    BOTH("Both Termux & CI")
}

data class BuildErrorDiagnosis(
    val id: String,
    val category: ErrorCategory,
    val title: String,
    val summary: String,
    val affectedPlatform: PlatformTarget,
    val exitCode: Int,
    val failingCommand: String,
    val logSnippet: String,
    val explanation: String,
    val fixScript: String,
    val isRealAppError: Boolean,
    val isRiskOfBeingMasked: Boolean
)
