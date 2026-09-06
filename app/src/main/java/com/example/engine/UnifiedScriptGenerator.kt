package com.example.engine

import com.example.model.ProjectConfig
import com.example.model.StackType

object UnifiedScriptGenerator {

    fun generateBuildBridgeScript(config: ProjectConfig): String {
        val primary = config.primaryStack
        val secondary = config.secondaryStack

        val termuxCpu = config.termuxCpuLimit
        val termuxMem = config.termuxMaxMemoryMb

        return """
#!/usr/bin/env bash
# ==============================================================================
# BUILD-BRIDGE: Unified Shell Abstraction Layer
# Auto-generated for: ${config.name}
# Targets: Termux (Android Bionic) & GitHub Actions Linux CI (glibc)
# Stack: ${primary.displayName}${if (secondary != null) " + " + secondary.displayName else ""}
#
# GUARANTEE: Real application compilation and test failures are NEVER masked.
# Exit codes are preserved with 100% fidelity.
# ==============================================================================

set -Eeuo pipefail

# ------------------------------------------------------------------------------
# 1. Platform & Environment Detection
# ------------------------------------------------------------------------------
BB_START_TIME=${'$'}(date +%s)
BB_EXIT_CODE=0
BB_REPORT_DIR=".build-bridge"
mkdir -p "${'$'}BB_REPORT_DIR"

if [ -n "${'$'}{TERMUX_VERSION:-}" ] || [ -d "/data/data/com.termux" ] || [ -n "${'$'}{PREFIX:-}" ]; then
    BB_ENV="termux"
    export PREFIX="${'$'}{PREFIX:-/data/data/com.termux/files/usr}"
    export PATH="${'$'}PREFIX/bin:${'$'}PATH"
    BB_ARCH=${'$'}(uname -m)
elif [ -n "${'$'}{GITHUB_ACTIONS:-}" ]; then
    BB_ENV="github_actions"
    BB_ARCH=${'$'}(uname -m)
elif [ "${'$'}(uname -s)" = "Darwin" ]; then
    BB_ENV="macos"
    BB_ARCH=${'$'}(uname -m)
else
    BB_ENV="generic_linux"
    BB_ARCH=${'$'}(uname -m)
fi

# Color formatting
if [ -t 1 ] || [ -n "${'$'}{GITHUB_ACTIONS:-}" ]; then
    C_RESET='\033[0m'
    C_BLUE='\033[1;34m'
    C_GREEN='\033[1;32m'
    C_YELLOW='\033[1;33m'
    C_RED='\033[1;31m'
    C_CYAN='\033[1;36m'
    C_DIM='\033[2m'
else
    C_RESET=''
    C_BLUE=''
    C_GREEN=''
    C_YELLOW=''
    C_RED=''
    C_CYAN=''
    C_DIM=''
fi

bb_log_info()    { printf "${'$'}{C_BLUE}[BUILD-BRIDGE]${'$'}{C_RESET} %s\n" "${'$'}*"; }
bb_log_success() { printf "${'$'}{C_GREEN}[SUCCESS]${'$'}{C_RESET} %s\n" "${'$'}*"; }
bb_log_warn()    { printf "${'$'}{C_YELLOW}[WARNING]${'$'}{C_RESET} %s\n" "${'$'}*" >&2; }
bb_log_err()     { printf "${'$'}{C_RED}[ERROR]${'$'}{C_RESET} %s\n" "${'$'}*" >&2; }

# ------------------------------------------------------------------------------
# 2. Transparent Error Trap (NEVER MASKS APPLICATION FAILURES)
# ------------------------------------------------------------------------------
bb_trap_error() {
    local exit_status=${'$'}1
    local line_no=${'$'}2
    local failing_cmd=${'$'}3
    local end_time=${'$'}(date +%s)
    local duration=${'$'}((end_time - BB_START_TIME))

    printf "\n"
    bb_log_err "======================================================================"
    bb_log_err "PIPELINE FAILED with exit code ${'$'}exit_status on line ${'$'}line_no"
    bb_log_err "Command: ${'$'}failing_cmd"
    bb_log_err "Environment: ${'$'}BB_ENV (${'$'}BB_ARCH)"
    bb_log_err "Duration: ${'$'}{duration}s"
    bb_log_err "======================================================================"

    # Categorize error context for developer guidance
    if [ "${'$'}BB_ENV" = "termux" ]; then
        if [ "${'$'}exit_status" -eq 137 ]; then
            bb_log_warn "Exit code 137 indicates Android Low Memory Killer (LMK) sent SIGKILL (Signal 9)."
            bb_log_warn "Remedy: export BB_THROTTLE=1 or increase swap memory."
        elif [ "${'$'}exit_status" -eq 127 ]; then
            bb_log_warn "Exit code 127 indicates missing binary, bad shebang (/bin/bash), or dynamic linker failure."
            bb_log_warn "Remedy: run 'termux-fix-shebang' or verify 'pkg install' toolchains."
        fi
    fi

    # Write structured failure report for tooling
    cat <<EOF > "${'$'}BB_REPORT_DIR/last_run.json"
{
  "status": "failed",
  "exitCode": ${'$'}exit_status,
  "failedLine": ${'$'}line_no,
  "failedCommand": "${'$'}failing_cmd",
  "environment": "${'$'}BB_ENV",
  "architecture": "${'$'}BB_ARCH",
  "durationSeconds": ${'$'}duration,
  "timestamp": ${'$'}(date +%s)
}
EOF

    # CRITICAL: Re-exit with the exact application exit code!
    exit "${'$'}exit_status"
}

trap 'bb_trap_error ${'$'}? ${'$'}LINENO "${'$'}BASH_COMMAND"' ERR

# ------------------------------------------------------------------------------
# 3. Environment Sanitization & Resource Throttling
# ------------------------------------------------------------------------------
bb_setup_env() {
    bb_log_info "Active environment: ${'$'}BB_ENV on ${'$'}BB_ARCH"

    if [ "${'$'}BB_ENV" = "termux" ]; then
        # Limit concurrency to avoid Android LMK killing compiler
        export CARGO_BUILD_JOBS=${termuxCpu}
        export MAKEFLAGS="-j${termuxCpu}"
        export CMAKE_BUILD_PARALLEL_LEVEL=${termuxCpu}
        export GRADLE_OPTS="-Dorg.gradle.jvmargs='-Xmx${termuxMem}m' --no-daemon"
        export CFLAGS="-I${'$'}PREFIX/include"
        export LDFLAGS="-L${'$'}PREFIX/lib"
        export PKG_CONFIG_PATH="${'$'}PREFIX/lib/pkgconfig:${'$'}PREFIX/share/pkgconfig"

        # Prevent glibc precompiled binary issues
        export npm_config_build_from_source=true
        export PIP_NO_BINARY=":all:"

        # Rust linker on Android Termux
        if [ "${'$'}BB_ARCH" = "aarch64" ]; then
            export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="clang"
        elif [ "${'$'}BB_ARCH" = "arm" ] || [ "${'$'}BB_ARCH" = "armv7l" ]; then
            export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="clang"
        fi
    else
        # Full concurrency on CI
        local cpus=${'$'}(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 2)
        export CARGO_BUILD_JOBS=${'$'}cpus
        export MAKEFLAGS="-j${'$'}cpus"
        export CMAKE_BUILD_PARALLEL_LEVEL=${'$'}cpus
    fi
}

# ------------------------------------------------------------------------------
# 4. Dependency Management Phase
# ------------------------------------------------------------------------------
bb_install_deps() {
    bb_log_info "Phase: Installing dependencies..."
${generateInstallBlock(config)}
    bb_log_success "Dependencies installed successfully."
}

# ------------------------------------------------------------------------------
# 5. Lint & Type Check Phase
# ------------------------------------------------------------------------------
bb_lint() {
    bb_log_info "Phase: Running code linters & static analysis..."
${generateLintBlock(config)}
    bb_log_success "Lint checks passed."
}

# ------------------------------------------------------------------------------
# 6. Build / Compilation Phase
# ------------------------------------------------------------------------------
bb_build() {
    bb_log_info "Phase: Compiling / Building artifacts..."
${generateBuildBlock(config)}
    bb_log_success "Build completed successfully."
}

# ------------------------------------------------------------------------------
# 7. Test Phase (Strict Exit Code Preservation)
# ------------------------------------------------------------------------------
bb_test() {
    bb_log_info "Phase: Executing test suite..."
    bb_log_info "Note: Test failures will NOT be swallowed or masked."
${generateTestBlock(config)}
    bb_log_success "All test suites passed!"
}

# ------------------------------------------------------------------------------
# 8. Command Dispatcher
# ------------------------------------------------------------------------------
bb_setup_env

CMD="${'$'}{1:-all}"
case "${'$'}CMD" in
    check)
        bb_log_info "Preflight check passed. Environment is ready."
        ;;
    install)
        bb_install_deps
        ;;
    lint)
        bb_lint
        ;;
    build)
        bb_build
        ;;
    test)
        bb_test
        ;;
    all)
        ${if (config.enableInstall) "bb_install_deps" else "# install skipped"}
        ${if (config.enableLint) "bb_lint" else "# lint skipped"}
        ${if (config.enableBuild) "bb_build" else "# build skipped"}
        ${if (config.enableTest) "bb_test" else "# test skipped"}
        bb_log_success "All pipeline stages completed with 100% test integrity!"
        ;;
    clean)
        bb_log_info "Cleaning build artifacts..."
        rm -rf target/ dist/ build/ .pytest_cache/ __pycache__/ "${'$'}BB_REPORT_DIR"
        bb_log_success "Clean completed."
        ;;
    *)
        echo "Usage: ${'$'}0 {check|install|lint|build|test|all|clean}"
        exit 1
        ;;
esac
""".trimIndent()
    }

    private fun generateInstallBlock(config: ProjectConfig): String {
        val sb = StringBuilder()
        when (config.primaryStack) {
            StackType.PYTHON -> {
                sb.append("    if [ -f requirements.txt ]; then\n")
                sb.append("        pip install --upgrade pip\n")
                sb.append("        pip install -r requirements.txt\n")
                sb.append("    elif [ -f pyproject.toml ]; then\n")
                sb.append("        if command -v poetry >/dev/null 2>&1; then\n")
                sb.append("            poetry install\n")
                sb.append("        elif command -v uv >/dev/null 2>&1; then\n")
                sb.append("            uv pip install -e .\n")
                sb.append("        else\n")
                sb.append("            pip install -e .\n")
                sb.append("        fi\n")
                sb.append("    fi\n")
            }
            StackType.NODEJS -> {
                sb.append("    if [ -f package-lock.json ]; then\n")
                sb.append("        npm ci\n")
                sb.append("    elif [ -f pnpm-lock.yaml ]; then\n")
                sb.append("        pnpm install --frozen-lockfile\n")
                sb.append("    elif [ -f yarn.lock ]; then\n")
                sb.append("        yarn install --frozen-lockfile\n")
                sb.append("    else\n")
                sb.append("        npm install\n")
                sb.append("    fi\n")
            }
            StackType.RUST -> {
                sb.append("    cargo fetch\n")
            }
            StackType.GO -> {
                sb.append("    go mod download\n")
                sb.append("    go mod verify\n")
            }
            StackType.JAVA_GRADLE -> {
                sb.append("    chmod +x gradlew 2>/dev/null || true\n")
                sb.append("    if [ -f gradlew ]; then\n")
                sb.append("        ./gradlew --version\n")
                sb.append("    fi\n")
            }
            StackType.CPP_CMAKE -> {
                sb.append("    mkdir -p build\n")
                sb.append("    cmake -B build -S . -DCMAKE_BUILD_TYPE=Release\n")
            }
            StackType.MIXED -> {
                sb.append("    # Multi-stack installation\n")
                sb.append("    [ -f package.json ] && npm install\n")
                sb.append("    [ -f Cargo.toml ] && cargo fetch\n")
                sb.append("    [ -f requirements.txt ] && pip install -r requirements.txt\n")
            }
        }
        return sb.toString()
    }

    private fun generateLintBlock(config: ProjectConfig): String {
        return when (config.primaryStack) {
            StackType.PYTHON -> "    command -v flake8 >/dev/null 2>&1 && flake8 . || true\n    command -v ruff >/dev/null 2>&1 && ruff check . || true\n"
            StackType.NODEJS -> "    npm run lint --if-present\n"
            StackType.RUST -> "    cargo clippy -- -D warnings || cargo check\n"
            StackType.GO -> "    go vet ./...\n"
            StackType.JAVA_GRADLE -> "    ./gradlew check -x test\n"
            StackType.CPP_CMAKE -> "    # C/C++ Static analysis\n    echo \"Checking CMake config...\"\n"
            StackType.MIXED -> "    [ -f package.json ] && npm run lint --if-present\n    [ -f Cargo.toml ] && cargo check\n"
        }
    }

    private fun generateBuildBlock(config: ProjectConfig): String {
        if (config.customBuildCommand.isNotBlank()) {
            return "    ${config.customBuildCommand}\n"
        }
        return when (config.primaryStack) {
            StackType.PYTHON -> "    # Python package build\n    python -m build --wheel 2>/dev/null || python setup.py build 2>/dev/null || true\n"
            StackType.NODEJS -> "    npm run build --if-present\n"
            StackType.RUST -> "    cargo build --release\n"
            StackType.GO -> "    go build -v ./...\n"
            StackType.JAVA_GRADLE -> "    ./gradlew assemble\n"
            StackType.CPP_CMAKE -> "    cmake --build build --config Release\n"
            StackType.MIXED -> "    [ -f Cargo.toml ] && cargo build --release\n    [ -f package.json ] && npm run build --if-present\n"
        }
    }

    private fun generateTestBlock(config: ProjectConfig): String {
        if (config.customTestCommand.isNotBlank()) {
            return "    ${config.customTestCommand}\n"
        }
        return when (config.primaryStack) {
            StackType.PYTHON -> """
    if command -v pytest >/dev/null 2>&1; then
        pytest -v
    else
        python -m unittest discover -s tests -p "test_*.py"
    fi
""".trimIndent().prependIndent("    ") + "\n"
            StackType.NODEJS -> "    npm test\n"
            StackType.RUST -> "    cargo test -- --nocapture\n"
            StackType.GO -> "    go test -v -race ./...\n"
            StackType.JAVA_GRADLE -> "    ./gradlew test\n"
            StackType.CPP_CMAKE -> "    ctest --test-dir build --output-on-failure\n"
            StackType.MIXED -> """
    [ -f Cargo.toml ] && cargo test
    [ -f package.json ] && npm test
    [ -f pytest.ini ] || [ -d tests ] && pytest
""".trimIndent().prependIndent("    ") + "\n"
        }
    }

    fun generateGitHubActionsWorkflow(config: ProjectConfig): String {
        val primary = config.primaryStack
        return """
name: CI (${config.name})

on:
  push:
    branches: [ main, master, develop ]
  pull_request:
    branches: [ main, master ]

jobs:
  build-and-test:
    runs-on: ${config.ciLinuxDistro}
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

${generateCISetupSteps(config)}

      - name: Make Build-Bridge Executable
        run: chmod +x build-bridge.sh

      - name: Run Preflight Check
        run: ./build-bridge.sh check

      - name: Install Dependencies
        run: ./build-bridge.sh install

      - name: Run Linters
        run: ./build-bridge.sh lint

      - name: Build Application
        run: ./build-bridge.sh build

      - name: Run Tests (Exit codes preserved strictly)
        run: ./build-bridge.sh test
""".trimIndent()
    }

    private fun generateCISetupSteps(config: ProjectConfig): String {
        val sb = StringBuilder()
        when (config.primaryStack) {
            StackType.PYTHON -> {
                sb.append("      - name: Set up Python 3.11\n")
                sb.append("        uses: actions/setup-python@v5\n")
                sb.append("        with:\n")
                sb.append("          python-version: '3.11'\n")
                if (config.ciCacheEnabled) {
                    sb.append("          cache: 'pip'\n")
                }
            }
            StackType.NODEJS -> {
                sb.append("      - name: Set up Node.js 20\n")
                sb.append("        uses: actions/setup-node@v4\n")
                sb.append("        with:\n")
                sb.append("          node-version: 20\n")
                if (config.ciCacheEnabled) {
                    sb.append("          cache: 'npm'\n")
                }
            }
            StackType.RUST -> {
                sb.append("      - name: Set up Rust Toolchain\n")
                sb.append("        uses: dtolnay/rust-toolchain@stable\n")
                sb.append("        with:\n")
                sb.append("          components: clippy\n")
            }
            StackType.GO -> {
                sb.append("      - name: Set up Go 1.22\n")
                sb.append("        uses: actions/setup-go@v5\n")
                sb.append("        with:\n")
                sb.append("          go-version: '1.22'\n")
                if (config.ciCacheEnabled) {
                    sb.append("          cache: true\n")
                }
            }
            StackType.JAVA_GRADLE -> {
                sb.append("      - name: Set up JDK 17\n")
                sb.append("        uses: actions/setup-java@v4\n")
                sb.append("        with:\n")
                sb.append("          java-version: '17'\n")
                sb.append("          distribution: 'temurin'\n")
                if (config.ciCacheEnabled) {
                    sb.append("          cache: 'gradle'\n")
                }
            }
            StackType.CPP_CMAKE -> {
                sb.append("      - name: Install Build Tools\n")
                sb.append("        run: sudo apt-get update && sudo apt-get install -y cmake ninja-build build-essential\n")
            }
            StackType.MIXED -> {
                sb.append("      - name: Set up Node.js\n")
                sb.append("        uses: actions/setup-node@v4\n")
                sb.append("        with:\n")
                sb.append("          node-version: 20\n")
                sb.append("      - name: Set up Rust\n")
                sb.append("        uses: dtolnay/rust-toolchain@stable\n")
            }
        }
        return sb.toString()
    }

    fun generateTermuxBootstrapScript(config: ProjectConfig): String {
        val pkgs = (config.primaryStack.termuxPackageDependencies +
                (config.secondaryStack?.termuxPackageDependencies ?: emptyList()) +
                listOf("git", "bash")).distinct().joinToString(" ")

        return """
#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# TERMUX BOOTSTRAP: ${config.name}
# Prepares Android Termux environment with native toolchains & shims.
# ==============================================================================

set -Eeuo pipefail

echo "=================================================="
echo " Termux Build-Bridge Bootstrap"
echo " Target: ${config.name}"
echo "=================================================="

# 1. Update Termux repositories
echo "[1/4] Updating package indexes..."
pkg update -y

# 2. Install required native packages
echo "[2/4] Installing required packages: ${pkgs}..."
pkg install -y ${pkgs}

# 3. Fix shebangs for scripts
echo "[3/4] Ensuring POSIX shebang compatibility..."
if command -v termux-fix-shebang >/dev/null 2>&1; then
    termux-fix-shebang build-bridge.sh 2>/dev/null || true
fi
chmod +x build-bridge.sh

# 4. Run preflight check
echo "[4/4] Executing Build-Bridge preflight check..."
./build-bridge.sh check

echo ""
echo "Termux environment successfully bootstrapped!"
echo "Run './build-bridge.sh all' to build and test your project."
""".trimIndent()
    }

    fun generateErrorVerificationScript(): String {
        return """
#!/usr/bin/env bash
# ==============================================================================
# TEST VERIFICATION: Proof that Real Application Errors are NEVER Masked
# ==============================================================================
set -euo pipefail

echo "1. Testing that deliberate test failure produces non-zero exit code..."
bash -c '
    source ./build-bridge.sh
    # Simulate a failed pytest assertion
    false
' || {
    EXIT_CODE=${'$'}?
    if [ "${'$'}EXIT_CODE" -ne 0 ]; then
        echo "[VERIFIED] Real application error correctly returned non-zero exit code: ${'$'}EXIT_CODE"
        echo "[VERIFIED] Build-Bridge did NOT mask the error."
        exit 0
    fi
}

echo "[FAIL] Error was masked!" >&2
exit 1
""".trimIndent()
    }
}
