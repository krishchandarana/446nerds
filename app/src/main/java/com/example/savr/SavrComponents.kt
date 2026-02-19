package com.savr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.theme.SavrColors


@Composable
fun ExpiryBadge(label: String, status: ExpiryStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        ExpiryStatus.URGENT  -> SavrColors.TerraTint to SavrColors.Terra
        ExpiryStatus.WARNING -> SavrColors.AmberTint  to SavrColors.Amber
        ExpiryStatus.FRESH   -> SavrColors.SageTint   to SavrColors.Sage
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text  = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}


enum class TagStyle { TERRA, AMBER, SAGE, GRAY }

@Composable
fun TagChip(text: String, style: TagStyle = TagStyle.GRAY, modifier: Modifier = Modifier) {
    val (bg, fg) = when (style) {
        TagStyle.TERRA -> SavrColors.TerraTint to SavrColors.Terra
        TagStyle.AMBER -> SavrColors.AmberTint  to SavrColors.Amber
        TagStyle.SAGE  -> SavrColors.SageTint   to SavrColors.Sage
        TagStyle.GRAY  -> SavrColors.CreamMid   to SavrColors.TextMid
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun FilterPill(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isActive) SavrColors.Dark else SavrColors.White
    val fg = if (isActive) SavrColors.White else SavrColors.TextMid

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text       = text,
            color      = fg,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text       = text,
        color      = SavrColors.TextMid,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier   = modifier.padding(horizontal = 20.dp, vertical = 5.dp)
    )
}


@Composable
fun PageHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp)) {
        Text(
            text  = title,
            style = MaterialTheme.typography.displayMedium,
            color = SavrColors.Dark
        )
        Text(
            text     = subtitle,
            color    = SavrColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun MetaPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SavrColors.CreamMid)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = text,
            color      = SavrColors.TextMid,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
