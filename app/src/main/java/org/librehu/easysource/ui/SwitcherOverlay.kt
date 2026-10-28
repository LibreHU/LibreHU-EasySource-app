package org.librehu.easysource.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import org.librehu.easysource.R
import org.librehu.easysource.data.Source
import org.librehu.easysource.data.SourceType

/** Big tiles in a row, the selected one in the accent colour, a countdown before it is activated. */
@Composable
fun SwitcherOverlay(
    sources: List<Source>,
    selected: Int,
    remaining: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val list = rememberLazyListState()
    LaunchedEffect(selected) { list.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .clip(if (carStyled) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(32.dp))
                .background(if (RetroLook.ambient) Color(0xF00E1626) else CarColors.Background)
                .then(
                    when {
                        RetroLook.factory -> Modifier.border(1.5.dp, Color.White)
                        RetroLook.ambient -> Modifier.border(1.dp, CarColors.Accent)
                        else -> Modifier
                    },
                )
                // Taps on the panel itself do not close it (only outside).
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.switcher_title), color = CarColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            LazyRow(
                state = list,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(sources, key = { _, s -> s.key }) { i, s -> Tile(s, i == selected) { onSelect(i) } }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.switcher_hint, remaining),
                color = CarColors.TextDim,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun Tile(
    source: Source,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val icon =
        remember(source.key) {
            if (source.type == SourceType.APP) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(source.packageName)
                        .toBitmap(128, 128)
                        .asImageBitmap()
                }.getOrNull()
            } else {
                null
            }
        }
    if (carStyled) return StyledTile(source, icon, selected, onClick)
    Column(
        Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) CarColors.Accent else CarColors.Surface)
            .border(if (selected) 0.dp else 1.dp, CarColors.SurfaceHigh, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 22.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(CarColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(icon, null, Modifier.size(72.dp))
            } else {
                Icon(
                    if (source.type == SourceType.AUX) Icons.AutoMirrored.Filled.Input else Icons.Default.MusicNote,
                    null,
                    tint = CarColors.Text,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            source.label,
            color = if (selected) CarColors.OnAccent else CarColors.Text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A source in LibreHU Launcher's car styles: a nearly black tile with the accent line under it, the chosen one framed
 * in the accent colour (factory); a framed tile lit from below when chosen (ambient).
 */
@Composable
private fun StyledTile(
    source: Source,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = CarColors.Accent
    Column(Modifier.width(170.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .background(
                    if (RetroLook.factory) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1D1E22), Color(0xFF0B0B0D)))
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Black,
                            0.6f to Color.Black,
                            1f to if (selected) deep(accent) else Color.Black,
                        )
                    },
                ).border(if (selected) 3.dp else 1.dp, if (RetroLook.factory && !selected) Color(0xFF2C2D31) else accent),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(icon, null, Modifier.size(72.dp))
            } else {
                MetalIcon(if (source.type == SourceType.AUX) Icons.AutoMirrored.Filled.Input else Icons.Default.MusicNote, 60.dp)
            }
        }
        if (RetroLook.factory) Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
        Text(
            source.label,
            color = if (RetroLook.factory && selected) accent else Color.White,
            fontSize = 20.sp,
            fontWeight = if (RetroLook.ambient) FontWeight.Light else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
