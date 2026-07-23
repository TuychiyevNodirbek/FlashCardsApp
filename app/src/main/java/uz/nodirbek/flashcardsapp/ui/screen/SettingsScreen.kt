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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler
import uz.nodirbek.flashcardsapp.ui.theme.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.components.SimpleAppBar

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit = {},
    onImportClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationScheduler = remember { NotificationScheduler(context) }

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
        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) FdDarkBackground else FdBackground,
        topBar = {
            SimpleAppBar(
                title = "Настройки",
                onBackClick = onBackClick,
                showDivider = true
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {

            item { Spacer(Modifier.height(16.dp)) }

            // Theme section
            item {
                SettingsSection(title = "Внешний вид") {
                    SettingsLabel("Тема оформления")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption("☀️", "Светлая", "light", uiState.theme, Modifier.weight(1f)) {
                            viewModel.setTheme(it)
                        }
                        ThemeOption("🌙", "Тёмная", "dark", uiState.theme, Modifier.weight(1f)) {
                            viewModel.setTheme(it)
                        }
                        ThemeOption("🌗", "Системная", "system", uiState.theme, Modifier.weight(1f)) {
                            viewModel.setTheme(it)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Daily goal
            item {
                SettingsSection(title = "Цель на день") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Карточек в день", fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FdText)
                            Text("Ваша ежедневная цель", fontSize = 12.sp, color = FdTextSub)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 20, 30).forEach { count ->
                                val sel = uiState.dailyGoal == count
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (sel) FdPrimary else FdSurface2)
                                        .border(1.5.dp, if (sel) FdPrimaryDark else FdBorder, CircleShape)
                                        .clickable { viewModel.setDailyGoal(count) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$count",
                                        fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        color = if (sel) Color.White else FdText
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Notifications
            item {
                SettingsSection(title = "Уведомления") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ежедневные напоминания", fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FdText)
                            Text("Напомним повторить карточки", fontSize = 12.sp, color = FdTextSub)
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
                                checkedTrackColor = FdPrimary,
                                uncheckedThumbColor = FdTextSub,
                                uncheckedTrackColor = FdBorder
                            )
                        )
                    }
                    if (uiState.reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        val timeLabel = uiState.reminderTime
                        val timeParts = timeLabel.split(":").let {
                            if (it.size == 2) it[0].toIntOrNull() to it[1].toIntOrNull()
                            else 9 to 0
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FdSurface2)
                                .border(1.5.dp, FdBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    TimePickerDialog(context, { _, h, m ->
                                        val newTime = "%02d:%02d".format(h, m)
                                        viewModel.setReminderTime(newTime)
                                        if (uiState.reminderEnabled && permissionGranted) {
                                            notificationScheduler.scheduleReminder(newTime)
                                        }
                                    }, timeParts.first ?: 9, timeParts.second ?: 0, true).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🕐", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Время:", fontSize = 13.sp, color = FdTextSub, modifier = Modifier.weight(1f))
                            Text(timeLabel, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FdPrimary)
                        }
                        if (!permissionGranted) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FdOrangeLight)
                                    .border(1.5.dp, FdOrange.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("⚠️ Уведомления не разрешены", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdOrange)
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
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
                Spacer(Modifier.height(16.dp))
            }

            // Data
            item {
                SettingsSection(title = "Данные") {
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FdPrimaryLight)
                            .border(1.5.dp, FdPrimary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onImportClick)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📂", fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Импортировать CSV", fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FdPrimary)
                                Text("Загрузить карточки из файла", fontSize = 12.sp, color = FdTextSub)
                            }
                        }
                    }
                }
            }

            // App version
            item {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("FlashDeck v2.0", fontSize = 12.sp, color = FdBorder, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Text(title, fontFamily = OutfitFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FdTextSub)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FdSurface)
                .border(1.5.dp, FdBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FdText)
}

@Composable
private fun ThemeOption(
    emoji: String,
    label: String,
    value: String,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) FdPrimary else FdSurface2)
            .border(2.dp, if (isSelected) FdPrimaryDark else FdBorder, RoundedCornerShape(12.dp))
            .clickable { onSelect(value) }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSelected) Color.White else FdText
            )
        }
    }
}
