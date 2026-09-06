package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagnosticStatus
import com.example.ui.theme.CleanStatusFail
import com.example.ui.theme.CleanStatusFailBg
import com.example.ui.theme.CleanStatusInfo
import com.example.ui.theme.CleanStatusInfoBg
import com.example.ui.theme.CleanStatusPass
import com.example.ui.theme.CleanStatusPassBg
import com.example.ui.theme.CleanStatusWarn
import com.example.ui.theme.CleanStatusWarnBg

@Composable
fun StatusBadge(
    status: DiagnosticStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        DiagnosticStatus.PASSED -> Triple(CleanStatusPassBg, CleanStatusPass, "PASS")
        DiagnosticStatus.WARNING -> Triple(CleanStatusWarnBg, CleanStatusWarn, "WARN")
        DiagnosticStatus.FAILED -> Triple(CleanStatusFailBg, CleanStatusFail, "FAIL")
        DiagnosticStatus.INFO -> Triple(CleanStatusInfoBg, CleanStatusInfo, "INFO")
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(50))
            .border(1.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(textColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

