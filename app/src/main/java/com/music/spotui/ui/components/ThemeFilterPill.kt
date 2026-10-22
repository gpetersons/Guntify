package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.theme.*

@Composable
fun ThemeFilterPill(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(label, color = if (selected) Color.Black else AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(if (selected) Color(0xFF1ED760) else AppRaised)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp))
}
