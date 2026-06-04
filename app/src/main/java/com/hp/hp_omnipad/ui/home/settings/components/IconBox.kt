package com.hp.hp_omnipad.ui.home.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hp.hp_omnipad.ui.theme.LightGreyButton
import com.hp.hp_omnipad.ui.theme.PrimaryBlue

@Composable
fun IconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(LightGreyButton, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue)
    }
}