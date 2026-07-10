package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Predefined Activity Categories supporting both formal backend names and simplified short names.
 */
enum class PredefinedCategory(
    val id: String,
    val displayName: String,
    val shortName: String,
    val emoji: String,
    val icon: ImageVector,
    val baseColor: Color
) {
    FOOD("Food & Cafés", "Food & Cafés", "Food", "🍽", Icons.Default.Restaurant, Color(0xFFE65100)),
    DRINKS("Drinks & Nightlife", "Drinks & Nightlife", "Drinks", "🍻", Icons.Default.LocalBar, Color(0xFFFFB300)),
    EVENTS("Events", "Events", "Events", "🎉", Icons.Default.Event, Color(0xFF6A1B9A)),
    SPORTS("Sports", "Sports", "Sports", "⚽", Icons.Default.SportsSoccer, Color(0xFF2E7D32)),
    OUTDOOR("Outdoor", "Outdoor", "Outdoor", "🏔", Icons.Default.Terrain, Color(0xFF1565C0)),
    MEET_PEOPLE("Meet People", "Meet People", "Meet People", "👥", Icons.Default.Groups, Color(0xFF00838F));

    companion object {
        fun fromString(value: String): PredefinedCategory? {
            val normalized = value.lowercase().trim()
            return values().firstOrNull { 
                it.id.lowercase() == normalized || 
                it.displayName.lowercase() == normalized || 
                it.shortName.lowercase() == normalized ||
                normalized.contains(it.shortName.lowercase())
            }
        }
    }
}

/**
 * Reusable CategoryIcon Widget.
 * Displays a highly polished, consistent icon and label for the six predefined categories.
 * Can be used as a filter button in a categories bar, or a compact static tag in a card or details screen.
 */
@Composable
fun CategoryIcon(
    categoryName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    isChipStyle: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val category = PredefinedCategory.fromString(categoryName)
    
    // Fallback if category name is not predefined
    val displayName = category?.shortName ?: categoryName
    val emoji = category?.emoji ?: "✨"
    val icon = category?.icon ?: Icons.Default.Explore
    val accentColor = category?.baseColor ?: MaterialTheme.colorScheme.primary

    if (isChipStyle) {
        // Compact Chip Style (useful for category tags in cards or details)
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) accentColor.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) accentColor.copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            if (showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    } else {
        // Vertical Icon + Label Column Style (perfect for Home screen categories filter bar)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .width(68.dp)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = if (isSelected) accentColor.copy(alpha = 0.15f) 
                                else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor.copy(alpha = 0.7f) 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display both vector icon & subtle emoji overlay/visual depth
                    Icon(
                        imageVector = icon,
                        contentDescription = displayName,
                        tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            if (showLabel) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
