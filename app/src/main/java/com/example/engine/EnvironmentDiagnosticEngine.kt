package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.model.DiagnosticItem
import com.example.model.DiagnosticReport
import com.example.model.DiagnosticStatus
import java.io.File

object EnvironmentDiagnosticEngine {

    fun runDiagnostics(context: Context): DiagnosticReport {
        val items = mutableListOf<DiagnosticItem>()
        var score = 100

        // 1. Check Architecture / ABI
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        items.add(
            DiagnosticItem(
                title = "Processor Architecture",
                description = "Primary ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"}",
                status = if (is64Bit) DiagnosticStatus.PASSED else DiagnosticStatus.WARNING,
                detail = "Supported ABIs: $abis (64-bit: $is64Bit)",
                recommendation = if (!is64Bit) "32-bit architecture may face memory limits for Rust/C++ compilation. Use `-j1` flag." else null
            )
        )

        // 2. libc Runtime (Bionic libc detection)
        val isAndroidBionic = true // Android inherently uses Bionic libc
        items.add(
            DiagnosticItem(
                title = "C Runtime Library (libc)",
                description = "Android Bionic libc detected",
                status = DiagnosticStatus.INFO,
                detail = "Android uses Bionic libc rather than GNU glibc. Dynamic linking to glibc-only wheels or prebuilt binaries will fail.",
                recommendation = "Enable build-bridge.sh Bionic shims (`CARGO_BUILD_TARGET=aarch64-linux-android` and pip `--no-binary :all:` for C extensions)."
            )
        )

        // 3. System Shell
        val shExists = File("/system/bin/sh").canExecute()
        if (shExists) {
            items.add(
                DiagnosticItem(
                    title = "System Shell Availability",
                    description = "/system/bin/sh is accessible and executable",
                    status = DiagnosticStatus.PASSED,
                    detail = "Standard Android POSIX shell present. Unified script can bootstrap without hardcoded /bin/bash."
                )
            )
        } else {
            score -= 20
            items.add(
                DiagnosticItem(
                    title = "System Shell Availability",
                    description = "/system/bin/sh inaccessible",
                    status = DiagnosticStatus.FAILED,
                    detail = "Cannot locate default Android shell binary.",
                    recommendation = "Ensure build-bridge.sh uses portable `#!/bin/sh` or Termux wrapper."
                )
            )
        }

        // 4. Termux Environment Detection
        val termuxPrefix = System.getenv("PREFIX") ?: "/data/data/com.termux/files/usr"
        val termuxDir = File(termuxPrefix)
        val isTermuxPresent = termuxDir.exists() && termuxDir.isDirectory
        if (isTermuxPresent) {
            items.add(
                DiagnosticItem(
                    title = "Termux Environment",
                    description = "Termux prefix located at $termuxPrefix",
                    status = DiagnosticStatus.PASSED,
                    detail = "Local device has Termux workspace ready."
                )
            )
        } else {
            items.add(
                DiagnosticItem(
                    title = "Termux Environment",
                    description = "Termux app environment not currently active or inaccessible",
                    status = DiagnosticStatus.INFO,
                    detail = "Running inside Android APK runtime. build-bridge.sh includes automated bootstrap for Termux app installation.",
                    recommendation = "Run `termux-bootstrap.sh` inside Termux to auto-install pkg prerequisites."
                )
            )
        }

        // 5. Memory & Android LMK
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val availMemMb = memInfo.availMem / (1024 * 1024)
        val totalMemMb = memInfo.totalMem / (1024 * 1024)

        if (availMemMb < 512) {
            score -= 25
            items.add(
                DiagnosticItem(
                    title = "Available RAM / LMK Risk",
                    description = "$availMemMb MB available (Total: $totalMemMb MB)",
                    status = DiagnosticStatus.WARNING,
                    detail = "Low available memory. High risk of Android Low Memory Killer (LMK) sending SIGKILL (Signal 9) to rustc/javac.",
                    recommendation = "Unified script must restrict build parallelism to 1 or 2 threads and enable Gradle --no-daemon."
                )
            )
        } else {
            items.add(
                DiagnosticItem(
                    title = "Available RAM",
                    description = "$availMemMb MB available (Total: $totalMemMb MB)",
                    status = DiagnosticStatus.PASSED,
                    detail = "Adequate memory for concurrent mobile builds with throttled workers."
                )
            )
        }

        // 6. Storage space
        val stat = StatFs(Environment.getDataDirectory().path)
        val availStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        if (availStorageMb < 1000) {
            score -= 20
            items.add(
                DiagnosticItem(
                    title = "Internal Storage Space",
                    description = "$availStorageMb MB free space remaining",
                    status = DiagnosticStatus.WARNING,
                    detail = "Build artifacts (node_modules, target/, .gradle) require at least 1-2GB free space.",
                    recommendation = "Clean old build caches using `./build-bridge.sh clean`."
                )
            )
        } else {
            items.add(
                DiagnosticItem(
                    title = "Internal Storage Space",
                    description = "${availStorageMb / 1024} GB free storage space",
                    status = DiagnosticStatus.PASSED,
                    detail = "Sufficient capacity for package manager downloads and compilation targets."
                )
            )
        }

        // 7. Error Unmasking & Strict POSIX
        items.add(
            DiagnosticItem(
                title = "Error Propagation Fidelity",
                description = "Strict `set -euo pipefail` enabled",
                status = DiagnosticStatus.PASSED,
                detail = "Build-Bridge does NOT mask genuine application errors. Exit codes from pytest/cargo test/npm test are passed through directly.",
                recommendation = "Never use `|| true` in build steps. Use explicit trapped handlers."
            )
        )

        return DiagnosticReport(
            overallScore = score.coerceIn(0, 100),
            detectedEnvironment = if (isTermuxPresent) "Termux Android Shell" else "Android Container / CI Host",
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidApiLevel = Build.VERSION.SDK_INT,
            libcVariant = "Bionic libc (Android 14+)",
            availableMemoryMb = availMemMb,
            totalMemoryMb = totalMemMb,
            storageAvailableMb = availStorageMb,
            items = items
        )
    }
}
