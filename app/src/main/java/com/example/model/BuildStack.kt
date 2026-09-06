package com.example.model

enum class StackType(
    val id: String,
    val displayName: String,
    val iconName: String,
    val description: String,
    val defaultPackageManager: String,
    val availablePackageManagers: List<String>,
    val termuxPackageDependencies: List<String>,
    val ciSetupAction: String,
    val termuxQuirk: String
) {
    PYTHON(
        id = "python",
        displayName = "Python",
        iconName = "Terminal",
        description = "CPython 3.10+, pip, poetry, or uv with C-extension build-from-source guards",
        defaultPackageManager = "pip",
        availablePackageManagers = listOf("pip", "poetry", "uv", "pipenv"),
        termuxPackageDependencies = listOf("python", "clang", "make", "libffi", "openssl"),
        ciSetupAction = "actions/setup-python@v5",
        termuxQuirk = "Many PyPI manylinux wheels fail on Termux Bionic libc; native build-from-source flags (CFLAGS/LDFLAGS) are required."
    ),
    NODEJS(
        id = "nodejs",
        displayName = "Node.js",
        iconName = "Javascript",
        description = "Node.js (npm, pnpm, yarn, bun) with prebuilt binary fallback for Bionic libc",
        defaultPackageManager = "npm",
        availablePackageManagers = listOf("npm", "pnpm", "yarn", "bun"),
        termuxPackageDependencies = listOf("nodejs-lts", "python", "make", "clang"),
        ciSetupAction = "actions/setup-node@v4",
        termuxQuirk = "Tools like esbuild, swc, sharp ship glibc binaries by default that crash on Termux; requires npm rebuild from source."
    ),
    RUST(
        id = "rust",
        displayName = "Rust",
        iconName = "Build",
        description = "Cargo & rustc with aarch64-linux-android target & dynamic linker shims",
        defaultPackageManager = "cargo",
        availablePackageManagers = listOf("cargo"),
        termuxPackageDependencies = listOf("rust", "binutils", "clang"),
        ciSetupAction = "dtolnay/rust-toolchain@stable",
        termuxQuirk = "Requires explicit target `aarch64-linux-android` with clang as linker instead of default gcc."
    ),
    GO(
        id = "go",
        displayName = "Go (Golang)",
        iconName = "Code",
        description = "Go 1.21+ with CGO_ENABLED adaptation between mobile Bionic and Linux CI",
        defaultPackageManager = "go modules",
        availablePackageManagers = listOf("go modules"),
        termuxPackageDependencies = listOf("golang"),
        ciSetupAction = "actions/setup-go@v5",
        termuxQuirk = "CGO requires Termux clang toolchain; pure Go works natively without external libc shims."
    ),
    JAVA_GRADLE(
        id = "java_gradle",
        displayName = "Java / Gradle",
        iconName = "Coffee",
        description = "OpenJDK 17/21 & Gradle wrapper with Termux daemon and memory safeguards",
        defaultPackageManager = "gradlew",
        availablePackageManagers = listOf("gradlew", "gradle", "maven"),
        termuxPackageDependencies = listOf("openjdk-17", "binutils"),
        ciSetupAction = "actions/setup-java@v4",
        termuxQuirk = "Gradle daemon crashes or gets killed by Android LMK; mandatory `--no-daemon` and heap limit on Termux."
    ),
    CPP_CMAKE(
        id = "cpp_cmake",
        displayName = "C / C++ (CMake)",
        iconName = "Memory",
        description = "Clang/GCC with CMake and Ninja, sysroot and compiler flag unification",
        defaultPackageManager = "cmake",
        availablePackageManagers = listOf("cmake", "make", "ninja"),
        termuxPackageDependencies = listOf("clang", "cmake", "ninja", "pkg-config"),
        ciSetupAction = "run: sudo apt-get install -y cmake ninja-build build-essential",
        termuxQuirk = "Header paths under \$PREFIX/include must be explicitly passed to CMake CMAKE_INCLUDE_PATH."
    ),
    MIXED(
        id = "mixed",
        displayName = "Polyglot / Mixed",
        iconName = "Layers",
        description = "Composite project (e.g. Node + Rust, Python + C++, Gradle + Go) with unified execution order",
        defaultPackageManager = "multi-tool",
        availablePackageManagers = listOf("composite"),
        termuxPackageDependencies = listOf("build-essential", "clang", "pkg-config"),
        ciSetupAction = "multiple setup steps",
        termuxQuirk = "Multi-step build order must guarantee dependency binaries are in PATH before downstream compilers run."
    );

    companion object {
        fun fromId(id: String): StackType = entries.find { it.id == id } ?: PYTHON
    }
}
