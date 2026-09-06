package com.example.engine

import com.example.model.BuildErrorDiagnosis
import com.example.model.ErrorCategory
import com.example.model.PlatformTarget
import java.util.UUID

object ErrorAnalyzerEngine {

    fun analyzeLog(rawLog: String): BuildErrorDiagnosis {
        val lower = rawLog.lowercase()

        // 1. Genuine Application Test Failure (CRITICAL - DO NOT MASK)
        if (rawLog.contains("FAILED (failures=") ||
            rawLog.contains("test result: FAILED") ||
            rawLog.contains("Tests run: ") && (rawLog.contains("Failures: ") || rawLog.contains("Errors: ")) ||
            rawLog.contains("FAIL src/") ||
            rawLog.contains("AssertionError") ||
            rawLog.contains("--- FAIL:") ||
            rawLog.contains("Test suite failed") ||
            rawLog.contains("1 failed,") ||
            rawLog.contains("FAILURES!")
        ) {
            val failedLine = rawLog.lines().find {
                it.contains("FAILED") || it.contains("AssertionError") || it.contains("--- FAIL:")
            } ?: "Test suite assertion failure"

            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.REAL_APPLICATION_TEST_FAILURE,
                title = "Real Application Test Failure",
                summary = "Application tests failed assertion. This is a legitimate app error that MUST NOT be masked.",
                affectedPlatform = PlatformTarget.BOTH,
                exitCode = 1,
                failingCommand = "test runner (pytest / cargo test / npm test / gradlew test)",
                logSnippet = extractContext(rawLog, failedLine),
                explanation = "A unit or integration test assertion did not pass. In naive build scripts, people sometimes append `|| true` to suppress CI red flags. Build-Bridge's unified layer strictly preserves this exit code (1) so developers and CI workflows catch regressions immediately.",
                fixScript = "# Do NOT mask this in shell! Fix the underlying code assertion in your tests:\n# Inspect test failure details above and update application logic.",
                isRealAppError = true,
                isRiskOfBeingMasked = true
            )
        }

        // 2. Genuine Application Syntax / Compiler Error (CRITICAL - DO NOT MASK)
        if (rawLog.contains("error[E0") || // Rust compiler error code
            rawLog.contains("SyntaxError:") ||
            rawLog.contains("error: cannot find symbol") ||
            rawLog.contains("undefined reference to `") ||
            rawLog.contains("TS2304:") || // TypeScript compiler error
            rawLog.contains("error: expected ") ||
            rawLog.contains("IndentationError:")
        ) {
            val errorLine = rawLog.lines().find {
                it.contains("error[E0") || it.contains("SyntaxError") || it.contains("TS2") || it.contains("cannot find symbol")
            } ?: "Source code compilation error"

            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.REAL_APPLICATION_SYNTAX_ERROR,
                title = "Real Application Syntax / Compiler Error",
                summary = "The compiler encountered an invalid statement or type mismatch in your source files.",
                affectedPlatform = PlatformTarget.BOTH,
                exitCode = 2,
                failingCommand = "compiler (rustc / tsc / clang / javac / python)",
                logSnippet = extractContext(rawLog, errorLine),
                explanation = "Your source code contains a syntax error, unhandled borrow, or missing symbol. The unified abstraction layer isolates this from environment setup failures and halts immediately with full exit code fidelity.",
                fixScript = "# Inspect the source file line indicated above and fix code syntax.\n# Check import statements and type definitions.",
                isRealAppError = true,
                isRiskOfBeingMasked = false
            )
        }

        // 3. Termux Bionic vs Glibc Dynamic Linker Conflict
        if (rawLog.contains("dlopen failed") ||
            rawLog.contains("cannot locate symbol") ||
            rawLog.contains("/lib/ld-linux-") ||
            rawLog.contains("ld-linux-x86-64.so") ||
            rawLog.contains("ld-linux-aarch64.so") ||
            (lower.contains("no such file or directory") && (lower.contains("node") || lower.contains("cargo") || lower.contains("esbuild")))
        ) {
            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.TERMUX_BIONIC_GLIBC_MISMATCH,
                title = "Termux Bionic vs Glibc ABI Conflict",
                summary = "Precompiled binary depends on GNU glibc (standard Linux), but Android uses Bionic libc.",
                affectedPlatform = PlatformTarget.TERMUX_ONLY,
                exitCode = 127,
                failingCommand = "dynamic linker / ELF loader",
                logSnippet = extractContext(rawLog, "dlopen") ?: rawLog.take(300),
                explanation = "Standard packages on npm/PyPI ship precompiled binaries linked against standard Linux glibc. On Android Termux, Bionic libc is used. Running glibc binaries results in 'cannot locate symbol' or 'No such file or directory'.",
                fixScript = "# In Termux, force building native addons from source or use Termux packages:\nexport CFLAGS=\"-I\$PREFIX/include\"\nexport LDFLAGS=\"-L\$PREFIX/lib\"\nnpm rebuild --build-from-source\n# Or for python:\npip install --no-binary :all: <package>",
                isRealAppError = false,
                isRiskOfBeingMasked = true
            )
        }

        // 4. Missing Native C/C++ Header or Toolchain
        if (rawLog.contains("Python.h: No such file") ||
            rawLog.contains("jni.h: No such file") ||
            rawLog.contains("make: not found") ||
            rawLog.contains("clang: not found") ||
            rawLog.contains("pkg-config: not found") ||
            rawLog.contains("fatal error:") && rawLog.contains(".h")
        ) {
            val headerLine = rawLog.lines().find { it.contains("fatal error:") || it.contains("not found") } ?: "Header missing"
            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.MISSING_NATIVE_HEADER_OR_TOOL,
                title = "Missing Native Compiler / C Header",
                summary = "A native dependency requires C headers or build utilities that are not installed.",
                affectedPlatform = PlatformTarget.TERMUX_ONLY,
                exitCode = 1,
                failingCommand = "native extension build (clang/make/cmake)",
                logSnippet = extractContext(rawLog, headerLine),
                explanation = "Native wheels or npm packages with native bindings (e.g. node-gyp, pyyaml, cryptography) require native headers and clang in Termux.",
                fixScript = "# In Termux, install the required toolchains:\npkg install -y build-essential clang python-dev libffi openssl pkg-config",
                isRealAppError = false,
                isRiskOfBeingMasked = true
            )
        }

        // 5. Out Of Memory / Android LMK Signal 9
        if (rawLog.contains("Killed") ||
            rawLog.contains("Signal 9") ||
            rawLog.contains("SIGKILL") ||
            rawLog.contains("exit status 137") ||
            rawLog.contains("java.lang.OutOfMemoryError")
        ) {
            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.MEMORY_EXHAUSTION_LMK,
                title = "Out Of Memory / Android LMK SIGKILL (Signal 9)",
                summary = "Android's Low Memory Killer terminated the compiler because RAM usage exceeded limits.",
                affectedPlatform = PlatformTarget.TERMUX_ONLY,
                exitCode = 137,
                failingCommand = "compiler / JVM daemon",
                logSnippet = extractContext(rawLog, "Killed") ?: extractContext(rawLog, "137") ?: rawLog.take(300),
                explanation = "Android aggressively reclaims memory by sending SIGKILL (Signal 9 / Exit code 137) to heavy background processes like Gradle daemons, rustc, or webpack. In GitHub Actions CI this rarely happens due to 7GB+ swap.",
                fixScript = "# Reduce parallelism and disable persistent daemons:\nexport CARGO_BUILD_JOBS=1\nexport GRADLE_OPTS=\"-Dorg.gradle.jvmargs='-Xmx768m' --no-daemon\"\n./build-bridge.sh build --throttle",
                isRealAppError = false,
                isRiskOfBeingMasked = false
            )
        }

        // 6. Bad Shebang / Path Mismatch
        if (rawLog.contains("/bin/bash: bad interpreter") ||
            rawLog.contains("/usr/bin/env: bad interpreter") ||
            rawLog.contains("/bin/sh: No such file")
        ) {
            return BuildErrorDiagnosis(
                id = UUID.randomUUID().toString(),
                category = ErrorCategory.SHEBANG_PATH_ERROR,
                title = "Termux Shebang & Path Mismatch",
                summary = "Script references /bin/bash which does not exist in standard Android Termux filesystem.",
                affectedPlatform = PlatformTarget.TERMUX_ONLY,
                exitCode = 127,
                failingCommand = "shell script interpreter",
                logSnippet = extractContext(rawLog, "bad interpreter") ?: rawLog.take(300),
                explanation = "Android does not have /bin or /usr/bin. Termux stores utilities under \$PREFIX/bin (/data/data/com.termux/files/usr/bin). Scripts with '#!/bin/bash' fail immediately unless patched.",
                fixScript = "# Fix shebangs automatically using termux-fix-shebang or use build-bridge.sh:\ntermux-fix-shebang *.sh\n# Or invoke via:\nbash ./your-script.sh",
                isRealAppError = false,
                isRiskOfBeingMasked = false
            )
        }

        // Default Fallback
        return BuildErrorDiagnosis(
            id = UUID.randomUUID().toString(),
            category = ErrorCategory.UNKNOWN_ERROR,
            title = "Unclassified Build Process Exit",
            summary = "The build process failed with non-zero status. Inspect stdout/stderr trace.",
            affectedPlatform = PlatformTarget.BOTH,
            exitCode = 1,
            failingCommand = "shell execution",
            logSnippet = rawLog.lines().takeLast(10).joinToString("\n"),
            explanation = "The execution returned an error code. Check the full log trace below to determine whether it is an application bug or an environment misconfiguration.",
            fixScript = "# Run with debug tracing enabled:\n./build-bridge.sh --verbose",
            isRealAppError = false,
            isRiskOfBeingMasked = false
        )
    }

    private fun extractContext(log: String, targetSubstring: String): String {
        val lines = log.lines()
        val index = lines.indexOfFirst { it.contains(targetSubstring, ignoreCase = true) }
        if (index == -1) return lines.takeLast(10).joinToString("\n")
        val start = (index - 4).coerceAtLeast(0)
        val end = (index + 6).coerceAtMost(lines.size)
        return lines.subList(start, end).joinToString("\n")
    }

    fun getSampleLogs(): List<Pair<String, String>> {
        return listOf(
            "Real Pytest Failure (Exit 1 - DO NOT MASK)" to """
============================= test session starts ==============================
platform linux -- Python 3.11.8, pytest-8.1.1, pluggy-1.4.0
collected 4 items

tests/test_auth.py ..F.                                                  [100%]

=================================== FAILURES ===================================
___________________________ test_token_expiration ____________________________
tests/test_auth.py:28: in test_token_expiration
    assert token.is_valid() is False
E   AssertionError: assert True is False
E    +  where True = <bound method Token.is_valid of <Token exp=1741258490>>()

=========================== short test summary info ============================
FAILED tests/test_auth.py::test_token_expiration - AssertionError: assert True is False
1 failed, 3 passed in 0.42s
            """.trimIndent(),

            "Rust Real Syntax Error (E0382)" to """
   Compiling termux-bridge v0.1.0 (/data/data/com.termux/files/home/project)
error[E0382]: use of moved value: `config`
  --> src/main.rs:42:19
   |
39 |     let config = parse_args()?;
   |         ------ move occurs because `config` has type `Config`, which does not implement `Copy`
40 |     validate_environment(config);
   |                          ------ value moved here
41 |     // Later in the function
42 |     let run_mode = config.mode;
   |                    ^^^^^^^^^^^ value used here after move
   |
help: consider cloning the value if the performance cost is acceptable
   |
40 |     validate_environment(config.clone());
   |                                ++++++++

For more information about this error, try `rustc --explain E0382`.
error: could not compile `termux-bridge` (bin "termux-bridge") due to 1 previous error
            """.trimIndent(),

            "Termux Bionic Glibc dlopen Error" to """
> node index.js
node:internal/modules/cjs/loader:1183
  return process.dlopen(module, path.toNamespacedPath(filename));
                 ^
Error: dlopen failed: cannot locate symbol "__libc_current_sigrtmin" referenced by "/data/data/com.termux/files/home/project/node_modules/@swc/core-linux-arm64-gnu/swc.android-arm64.node"...
    at Module._extensions..node (node:internal/modules/cjs/loader:1183:18)
    at Module.load (node:internal/modules/cjs/loader:981:32)
    at Function.Module._load (node:internal/modules/cjs/loader:822:12)
    at Module.require (node:internal/modules/cjs/loader:1005:19)
    at require (node:internal/modules/cjs/helpers:102:18) {
  code: 'ERR_DLOPEN_FAILED'
}
            """.trimIndent(),

            "Missing Native C Header (Python.h)" to """
building 'cryptography.hazmat.bindings._rust' extension
creating build/temp.linux-aarch64-cpython-311
clang -Wsign-compare -DNDEBUG -g -fwrapv -O3 -Wall -fPIC -I/data/data/com.termux/files/usr/include -c build/temp.c -o build/temp.o
src/_cffi_src/openssl/err.c:14:10: fatal error: 'Python.h' file not found
#include <Python.h>
         ^~~~~~~~~~
1 error generated.
error: command 'clang' failed with exit status 1
[end of output]
note: This error originates from a subprocess, and is likely not a problem with pip.
error: subprocess-exited-with-error
            """.trimIndent(),

            "Android LMK Out-Of-Memory (Signal 9)" to """
> Task :app:compileReleaseKotlin
e: Out of memory error during Kotlin compilation.
Gradle daemon process died unexpectedly!
Process 'Gradle Daemon' finished with non-zero exit value 137
Killed
Signal 9 received from operating system kernel.
LowMemoryKiller triggered by kernel to free 512MB RAM.
            """.trimIndent(),

            "Termux Shebang Missing /bin/bash" to """
$ ./run-pipeline.sh
sh: ./run-pipeline.sh: /bin/bash: bad interpreter: No such file or directory
exit code 127
            """.trimIndent()
        )
    }
}
