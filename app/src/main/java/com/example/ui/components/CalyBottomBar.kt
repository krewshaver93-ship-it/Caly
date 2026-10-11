package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageManager
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.viewmodel.AppScreen

@Composable
fun CalyBottomBar(
    currentScreen: AppScreen,
    languageCode: String,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Today
            BottomNavItem(
                icon = Icons.Default.CalendarToday,
                label = LanguageManager.getString("today", languageCode),
                isSelected = currentScreen == AppScreen.TODAY,
                tag = "tab_today",
                onClick = { onNavigate(AppScreen.TODAY) }
            )

            // 2. Insights
            BottomNavItem(
                icon = Icons.Default.Insights,
                label = LanguageManager.getString("insights", languageCode),
                isSelected = currentScreen == AppScreen.INSIGHTS,
                tag = "tab_insights",
                onClick = { onNavigate(AppScreen.INSIGHTS) }
            )

            // 3. Camera / Scanner (Center Elevated CTA)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldLight, EmeraldDark)
                        )
                    )
                    .clickable { onNavigate(AppScreen.CAMERA) }
                    .testTag("tab_camera_center"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = LanguageManager.getString("camera", languageCode),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 4. Recipes
            BottomNavItem(
                icon = Icons.Default.RestaurantMenu,
                label = LanguageManager.getString("recipes", languageCode),
                isSelected = currentScreen == AppScreen.RECIPES,
                tag = "tab_recipes",
                onClick = { onNavigate(AppScreen.RECIPES) }
            )

            // 5. Caly AI Agent
            BottomNavItem(
                icon = Icons.Default.AutoAwesome,
                label = LanguageManager.getString("caly", languageCode),
                isSelected = currentScreen == AppScreen.CALY_CHAT,
                tag = "tab_caly",
                onClick = { onNavigate(AppScreen.CALY_CHAT) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val color by animateColorAsState(targetValue = if (isSelected) activeColor else inactiveColor)

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1
        )
    }
}
