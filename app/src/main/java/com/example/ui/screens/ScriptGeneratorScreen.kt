package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.UnifiedScriptGenerator
import com.example.model.ProjectConfig
import com.example.ui.components.CodeViewer
import com.example.ui.theme.CodeAmber
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.ScriptTab

@Composable
fun ScriptGeneratorScreen(
    config: ProjectConfig,
    selectedTab: ScriptTab,
    onTabSelected: (ScriptTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val scriptCode = when (selectedTab) {
        ScriptTab.BUILD_BRIDGE -> UnifiedScriptGenerator.generateBuildBridgeScript(config)
        ScriptTab.CI_WORKFLOW -> UnifiedScriptGenerator.generateGitHubActionsWorkflow(config)
        ScriptTab.TERMUX_BOOTSTRAP -> UnifiedScriptGenerator.generateTermuxBootstrapScript(config)
        ScriptTab.VERIFY_UNMASKED -> UnifiedScriptGenerator.generateErrorVerificationScript()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Generated Abstraction Layer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Portable shell engine for ${config.primaryStack.displayName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, scriptCode)
                        putExtra(Intent.EXTRA_TITLE, selectedTab.filename)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share ${selectedTab.filename}"))
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .testTag("share_script_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share script",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Script Tabs Row
        val tabScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScriptTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(50)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("script_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Code Viewer
        CodeViewer(
            code = scriptCode,
            title = selectedTab.filename,
            maxHeightDp = 380
        )

        // Safety Guardrails Explained
        Text(
            text = "Architecture & Safety Guarantees",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        SafetyExplanationCard(
            title = "Zero Error Masking Guarantee",
            description = "Traditional shell scripts often do `test || true` or ignore pipeline exits. Build-Bridge uses `set -Eeuo pipefail` with a trapped error callback. If a pytest assertion, cargo test, or gradlew test fails, Build-Bridge aborts immediately and preserves the EXACT non-zero exit code ($?).",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Security
        )

        SafetyExplanationCard(
            title = "Termux Bionic libc vs Linux glibc",
            description = "Termux binaries link against Android Bionic libc, lacking GNU dynamic linkers like /lib64/ld-linux-x86-64.so.2. Build-Bridge automatically detects Termux via \$PREFIX, injects include/lib headers, and forces `--build-from-source` on npm and pip so native extensions compile cleanly.",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Code
        )

        SafetyExplanationCard(
            title = "Android LMK (Low Memory Killer) Protection",
            description = "Android mobile kernels kill compilers via SIGKILL (Exit status 137) when memory spikes. Build-Bridge restricts parallel compilation on Termux to ${config.termuxCpuLimit} cores, limits Gradle JVM heap to ${config.termuxMaxMemoryMb}MB, and passes `--no-daemon`.",
            color = CodeAmber,
            icon = Icons.Default.Info
        )
    }
}

@Composable
private fun SafetyExplanationCard(
    title: String,
    description: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
