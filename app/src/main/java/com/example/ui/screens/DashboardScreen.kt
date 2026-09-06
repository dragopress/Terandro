package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectConfig
import com.example.model.StackType
import com.example.ui.components.MetricCard
import com.example.ui.components.PillBadge
import com.example.ui.theme.CleanMinimalBorderLight
import com.example.ui.theme.CleanMinimalOnPrimaryContainer
import com.example.ui.theme.CleanMinimalPrimaryContainer
import com.example.ui.theme.CleanMinimalTerminalAccent
import com.example.ui.theme.CleanMinimalTerminalBg
import com.example.ui.theme.CleanMinimalTerminalBlue
import com.example.ui.theme.CleanMinimalTerminalDot
import com.example.ui.theme.CleanMinimalTerminalText
import com.example.ui.theme.CleanMinimalTextSecondary
import com.example.ui.theme.CleanStatusPass
import com.example.ui.theme.CodeAmber
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.ScreenTab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentProject: ProjectConfig,
    onStackSelected: (StackType) -> Unit,
    onNavigateTab: (ScreenTab) -> Unit,
    onRunDiagnostics: () -> Unit,
    diagnosticScore: Int?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clean Minimalism Twin Environment Status Cards (Termux & GitHub CI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Termux Local Dev Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp)
                    .clickable { onNavigateTab(ScreenTab.DIAGNOSTICS) },
                shape = RoundedCornerShape(24.dp),
                color = CleanMinimalPrimaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Termux",
                        tint = CleanMinimalOnPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Termux",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanMinimalOnPrimaryContainer
                        )
                        Text(
                            text = "Local Dev Active",
                            fontSize = 11.sp,
                            color = CleanMinimalOnPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // GitHub CI Workflow Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp)
                    .clickable { onNavigateTab(ScreenTab.SCRIPTS) },
                shape = RoundedCornerShape(24.dp),
                color = CleanMinimalBorderLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "GitHub CI",
                        tint = CleanMinimalTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "GitHub CI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanMinimalTextSecondary
                        )
                        Text(
                            text = "Awaiting Workflow",
                            fontSize = 11.sp,
                            color = CleanMinimalTextSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // Clean Minimalism Git CI/CD Development Loop Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab(ScreenTab.GIT) }
                .testTag("dashboard_git_card"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Commit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Git CI/CD Development Loop",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fetch origin, inspect tree status & commit changes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Git",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Clean Minimalism Detected Stack Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DETECTED STACK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StackType.entries.forEach { stack ->
                        val isSelected = currentProject.primaryStack == stack
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
                                .clickable { onStackSelected(stack) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("stack_selector_${stack.id}")
                        ) {
                            Text(
                                text = stack.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Clean Minimalism Shell Output Preview (from Design HTML)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab(ScreenTab.SCRIPTS) },
            shape = RoundedCornerShape(24.dp),
            color = CleanMinimalTerminalBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CleanMinimalTerminalDot, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shell Output • production-build",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFC4C6D0)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$ unified-build --env termux --stack ${currentProject.primaryStack.id}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CleanMinimalTerminalAccent
                    )
                    Text(
                        text = "[INFO] Resolving cross-platform toolchains (${currentProject.packageManager})...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CleanMinimalTerminalText
                    )
                    Text(
                        text = "[INFO] Setting up sandbox environment (strict pipefail enabled)...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CleanMinimalTerminalText
                    )
                    Text(
                        text = "[SYNC] Symlinking local Termux libs to CI path...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CleanMinimalTerminalBlue
                    )
                    Text(
                        text = "[DONE] Abstraction layer ready. Exit code: 0",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CleanMinimalTerminalText
                    )
                }
            }
        }

        // Hero Summary Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_hero_card"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Termux CI Bridge",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Unified Shell Abstraction Layer",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Readiness Score Pill
                    val score = diagnosticScore ?: 95
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$score% Ready",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Build seamlessly on both Android Termux and GitHub Actions Linux CI without masking real application errors. Preserves exit codes, resolves Bionic libc differences, and throttles parallel workers safely.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateTab(ScreenTab.SCRIPTS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_scripts_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "View Scripts", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onNavigateTab(ScreenTab.ERROR_ANALYZER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audit_errors_button"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Error Audit")
                    }
                }
            }
        }

        // Active Stack Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Stack Profile: ${currentProject.primaryStack.displayName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PillBadge(
                        text = currentProject.packageManager,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentProject.primaryStack.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Termux Quirk: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentProject.primaryStack.termuxQuirk,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }

        // Metrics / Safeguards Grid
        Text(
            text = "Active Protection Guardrails",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        MetricCard(
            title = "Error Propagation Fidelity",
            value = "Strict Unmasked",
            subtitle = "Zero error suppression: Real unit test / compiler errors never masked by || true",
            icon = Icons.Default.Security,
            accentColor = CleanStatusPass
        )

        MetricCard(
            title = "Termux Resource Safeguards",
            value = "${currentProject.termuxCpuLimit} Cores / ${currentProject.termuxMaxMemoryMb} MB",
            subtitle = "Guards against Android LMK SIGKILL (Signal 9) with throttled parallel jobs",
            icon = Icons.Default.Memory,
            accentColor = MaterialTheme.colorScheme.primary
        )

        MetricCard(
            title = "C Runtime / Dynamic Linker",
            value = "Bionic & glibc Dual Target",
            subtitle = "Automated fallback to build-from-source when prebuilt glibc binaries fail",
            icon = Icons.Default.Build,
            accentColor = CodeAmber
        )

        // Navigation Quick Action Banners
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTab(ScreenTab.DIAGNOSTICS) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preflight Checks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Device readiness report",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTab(ScreenTab.CONFIG) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configure Pipeline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tune steps & limits",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

