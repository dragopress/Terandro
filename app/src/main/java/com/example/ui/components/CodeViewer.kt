package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanMinimalTerminalAccent
import com.example.ui.theme.CleanMinimalTerminalBg
import com.example.ui.theme.CleanMinimalTerminalBlue
import com.example.ui.theme.CleanMinimalTerminalDot
import com.example.ui.theme.CleanMinimalTerminalText

@Composable
fun CodeViewer(
    code: String,
    title: String,
    modifier: Modifier = Modifier,
    maxHeightDp: Int = 420
) {
    val context = LocalContext.current
    val lines = code.lines()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("code_viewer_surface"),
        shape = RoundedCornerShape(24.dp),
        color = CleanMinimalTerminalBg,
        tonalElevation = 2.dp
    ) {
        Column {
            // Header Bar with Clean Minimalism dot and divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF44474E).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CleanMinimalTerminalDot, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC4C6D0)
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(title, code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied $title to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = CleanMinimalTerminalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Monospace code area with line numbers
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeightDp.dp)
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row {
                    // Line numbers
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        lines.indices.forEach { index ->
                            Text(
                                text = (index + 1).toString().padStart(3, ' '),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF5B5D72),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Code content with Clean Minimal syntax coloring
                    Column {
                        lines.forEach { line ->
                            val color = when {
                                line.trimStart().startsWith("#") -> Color(0xFF74777F) // Subtle comment
                                line.contains("set -Eeuo pipefail") || line.contains("trap") -> CleanMinimalTerminalAccent
                                line.contains("exit ") || line.contains("ERR") -> CleanMinimalTerminalDot
                                line.trimStart().startsWith("export ") -> CleanMinimalTerminalBlue
                                line.trimStart().startsWith("bb_") -> CleanMinimalTerminalBlue
                                line.trimStart().startsWith("$") -> CleanMinimalTerminalAccent
                                else -> CleanMinimalTerminalText
                            }
                            Text(
                                text = line.ifEmpty { " " },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = color,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

