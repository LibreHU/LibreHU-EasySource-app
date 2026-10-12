package org.librehu.easysource.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librehu.easysource.R
import org.librehu.easysource.SourceSwitcher
import org.librehu.easysource.data.Source
import org.librehu.easysource.data.SourceStore
import org.librehu.easysource.data.SourceType

@Composable
fun SettingsScreen(
    openSwitcher: () -> Unit,
    openNotificationAccess: () -> Unit,
) {
    val context = LocalContext.current
    val store = SourceStore.get(context)
    val sources by store.sources.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    val access = remember { SourceSwitcher.hasSessionAccess(context) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CarColors.Background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    color = CarColors.Text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Pill(stringResource(R.string.try_switcher), selected = true, icon = Icons.Default.SwapHoriz, onClick = openSwitcher)
            }
        }
        item {
            Card(stringResource(R.string.sources)) {
                Text(stringResource(R.string.sources_hint), color = CarColors.TextDim, fontSize = 15.sp)
            }
        }
        items(sources, key = { it.key }) { s ->
            SourceRow(
                s,
                first = s == sources.first(),
                last = s == sources.last(),
                onToggle = { store.setEnabled(s.key, it) },
                onUp = { store.move(s.key, -1) },
                onDown = { store.move(s.key, 1) },
                onRemove = { store.remove(s.key) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(stringResource(R.string.add_source), icon = Icons.Default.Add) { adding = true }
                if (sources.none { it.type == SourceType.AUX }) {
                    Pill(stringResource(R.string.add_aux), icon = Icons.AutoMirrored.Filled.Input) {
                        store.add(Source(SourceType.AUX, label = "AUX"))
                    }
                }
            }
        }
        item {
            Card(stringResource(R.string.switching)) {
                Text(stringResource(R.string.confirm_delay, settings.confirmDelayS), color = CarColors.Text, fontSize = 18.sp)
                Slider(
                    value = settings.confirmDelayS.toFloat(),
                    onValueChange = { v -> store.updateSettings { it.copy(confirmDelayS = v.toInt()) } },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = CarColors.Accent, activeTrackColor = CarColors.Accent),
                )
                SwitchRow(stringResource(R.string.open_app), stringResource(R.string.open_app_hint), settings.openApp) { on ->
                    store.updateSettings { it.copy(openApp = on) }
                }
                SwitchRow(stringResource(R.string.resume_boot), null, settings.resumeAtBoot) { on ->
                    store.updateSettings { it.copy(resumeAtBoot = on) }
                }
                if (!access) {
                    Text(stringResource(R.string.access_missing), color = CarColors.TextDim, fontSize = 15.sp)
                    Pill(stringResource(R.string.access_open), onClick = openNotificationAccess)
                }
            }
        }
        item {
            Card(stringResource(R.string.wheel)) {
                Text(stringResource(R.string.wheel_hint), color = CarColors.TextDim, fontSize = 15.sp)
                Text(
                    "org.librehu.easysource/.SwitcherActivity\norg.librehu.easysource.action.NEXT · .PREVIOUS",
                    color = CarColors.Text,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
    if (adding) {
        AddSourceDialog(store, onDismiss = { adding = false }) { s ->
            store.add(s)
            adding = false
        }
    }
}

@Composable
private fun SourceRow(
    s: Source,
    first: Boolean,
    last: Boolean,
    onToggle: (Boolean) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CarColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceIcon(s)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.label, color = CarColors.Text, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (s.type == SourceType.AUX) stringResource(R.string.aux_hint) else s.packageName,
                color = CarColors.TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RoundButton(Icons.Default.KeyboardArrowUp, enabled = !first, onClick = onUp)
        RoundButton(Icons.Default.KeyboardArrowDown, enabled = !last, onClick = onDown)
        Switch(
            checked = s.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = CarColors.Accent, checkedThumbColor = CarColors.OnAccent),
        )
        RoundButton(Icons.Default.Close, onClick = onRemove)
    }
}

@Composable
private fun AddSourceDialog(
    store: SourceStore,
    onDismiss: () -> Unit,
    onAdd: (Source) -> Unit,
) {
    var all by remember { mutableStateOf(false) }
    val existing =
        store.sources.value
            .map { it.key }
            .toSet()
    val apps = remember(all) { (if (all) store.launchableApps() else store.mediaApps()).filter { it.key !in existing } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_source)) },
        text = {
            LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Pill(stringResource(if (all) R.string.media_apps else R.string.all_apps)) { all = !all }
                }
                if (apps.isEmpty()) item { Text(stringResource(R.string.no_apps), color = CarColors.TextDim) }
                items(apps, key = { it.key }) { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAdd(s) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SourceIcon(s)
                        Spacer(Modifier.width(12.dp))
                        Text(s.label, color = CarColors.Text, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun SourceIcon(s: Source) {
    val context = LocalContext.current
    val icon =
        remember(s.key) {
            if (s.type == SourceType.APP) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(s.packageName)
                        .toBitmap(96, 96)
                        .asImageBitmap()
                }.getOrNull()
            } else {
                null
            }
        }
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(CarColors.SurfaceHigh), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(icon, null, Modifier.size(38.dp))
        } else {
            Icon(
                if (s.type == SourceType.AUX) Icons.AutoMirrored.Filled.Input else Icons.Default.MusicNote,
                null,
                tint = CarColors.Text,
            )
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) CarColors.Text else CarColors.SurfaceHigh)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = CarColors.Text, fontSize = 18.sp)
            if (subtitle != null) Text(subtitle, color = CarColors.TextDim, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = CarColors.Accent, checkedThumbColor = CarColors.OnAccent),
        )
    }
}

@Composable
private fun Card(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CarColors.Surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = CarColors.Accent, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun Pill(
    label: String,
    selected: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) CarColors.Accent else CarColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (selected) CarColors.OnAccent else CarColors.Text
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}
