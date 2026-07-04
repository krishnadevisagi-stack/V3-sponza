package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun SponzaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Color.Black)
            .border(2.dp, Color(0xFFFF9800).copy(alpha = 0.6f), RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sponza_logo),
            contentDescription = "Sponza Logo",
            modifier = Modifier.size(size),
            contentScale = ContentScale.Crop
        )
    }
}
