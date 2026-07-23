package uz.nodirbek.flashcardsapp.ui.screen

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler
import uz.nodirbek.flashcardsapp.ui.components.SimpleAppBar
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

private val TTS_LANGS = listOf(
    "en" to "English (US)",
    "en-gb" to "English (UK)",
    "ru" to "Русский",
    "de" to "Deutsch"
)

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit = {},
    onImportClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationScheduler = remember { NotificationScheduler(context) }
    var showResetConfirm by remember { mutableStateOf(false) }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted && uiState.reminderEnabled) {
            notificationScheduler.scheduleReminder(uiState.reminderTime)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SimpleAppBar(
                title = "Настройки",
                onBackClick = onBackClick,
                showDivider = true
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {

            // ── ТЕМА ─────────────────────────────────────────────────────
            item {
                SectionLabel("ТЕМА")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                ) {
                    listOf("light" to "Светлая", "dark" to "Тёмная", "system" to "Система").forEach { (value, label) ->
                        val sel = uiState.theme == value
                        Box(
                            Modifier.weight(1f)
                                .background(if (sel) FdPrimary else Color.Transparent)
                                .clickable { viewModel.setTheme(value) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── ЕЖЕДНЕВНЫЕ ЛИМИТЫ ────────────────────────────────────────
            item {
                SectionLabel("ЕЖЕДНЕВНЫЕ ЛИМИТЫ")
                SettingsGroup {
                    NumberSettingRow(
                        label = "Новые карточки в день",
                        value = uiState.dailyNewLimit,
                        onCommit = { viewModel.setDailyNewLimit(it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    NumberSettingRow(
                        label = "Повторения в день",
                        value = uiState.dailyReviewLimit,
                        onCommit = { viewModel.setDailyReviewLimit(it) }
                    )
                }
            }

            // ── НАПОМИНАНИЯ ──────────────────────────────────────────────
            item {
                SectionLabel("НАПОМИНАНИЯ")
                SettingsGroup {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ежедневное напоминание", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Уведомление о времени повторения", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                        Switch(
                            checked = uiState.reminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setReminderEnabled(enabled)
                                if (enabled) {
                                    if (permissionGranted) {
                                        notificationScheduler.scheduleReminder(uiState.reminderTime)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    notificationScheduler.cancelReminder()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FdGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    if (uiState.reminderEnabled) {
                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                        val timeParts = uiState.reminderTime.split(":").let {
                            if (it.size == 2) (it[0].toIntOrNull() ?: 9) to (it[1].toIntOrNull() ?: 0) else 9 to 0
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text("Время", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        TimePickerDialog(context, { _, h, m ->
                                            val newTime = "%02d:%02d".format(h, m)
                                            viewModel.setReminderTime(newTime)
                                            if (uiState.reminderEnabled && permissionGranted) {
                                                notificationScheduler.scheduleReminder(newTime)
                                            }
                                        }, timeParts.first, timeParts.second, true).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🕐", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(uiState.reminderTime, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FdPrimary)
                            }
                            if (!permissionGranted) {
                                Spacer(Modifier.height(10.dp))
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FdOrangeLight)
                                        .border(1.5.dp, FdOrange.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("⚠️ Уведомления не разрешены", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdOrange)
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(FdOrange)
                                            .clickable {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                else permissionGranted = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Разрешить", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, tint = FdGreen, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Уведомления включены", fontSize = 12.sp, color = FdGreen)
                                }
                            }
                        }
                    }
                }
            }

            // ── ОЗВУЧКА (TTS) ────────────────────────────────────────────
            item {
                SectionLabel("ОЗВУЧКА (TTS)")
                SettingsGroup {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("Язык", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        TtsLangSelector(
                            selected = uiState.ttsLang,
                            onSelect = { viewModel.setTtsLang(it) }
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        val speedLabel = if (uiState.ttsSpeed % 1f == 0f) "${uiState.ttsSpeed.toInt()}×" else "${uiState.ttsSpeed}×"
                        Text("Скорость $speedLabel", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = uiState.ttsSpeed,
                            onValueChange = { viewModel.setTtsSpeed(it) },
                            valueRange = 0.5f..2f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = FdPrimary,
                                activeTrackColor = FdPrimary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // ── ДАННЫЕ ───────────────────────────────────────────────────
            item {
                SectionLabel("ДАННЫЕ")
                SettingsGroup {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(onClick = onImportClick)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📂", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Импортировать CSV", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdPrimary)
                            Text("Загрузить карточки из файла", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.padding(horizontal = 14.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(FdRedLight)
                            .border(1.5.dp, FdRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { showResetConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("🗑  Сбросить прогресс", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdRed)
                    }
                }
            }

            // App version
            item {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("FlashDeck v2.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Сбросить прогресс?", fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Серия, XP, статистика и прогресс всех карточек будут удалены. Это действие необратимо.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProgress()
                    showResetConfirm = false
                }) { Text("Сбросить", color = FdRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) { content() }
}

@Composable
private fun NumberSettingRow(label: String, value: Int, onCommit: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new.filter { it.isDigit() }.take(3)
                text.toIntOrNull()?.let(onCommit)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FdPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun TtsLangSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                TTS_LANGS.firstOrNull { it.first == selected }?.second ?: selected,
                fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text("▾", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TTS_LANGS.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name, fontFamily = OutfitFamily, fontWeight = if (code == selected) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
