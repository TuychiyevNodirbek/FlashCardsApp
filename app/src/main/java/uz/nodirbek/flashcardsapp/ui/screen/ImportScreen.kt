package uz.nodirbek.flashcardsapp.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.domain.model.Card
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.ui.components.PressButton
import uz.nodirbek.flashcardsapp.ui.theme.*

@Composable
fun ImportScreen(
    onCardsImported: (List<Card>) -> Unit,
    onBackClick: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    message = "Не удалось открыть файл"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val content = inputStream.bufferedReader().readText()
                inputStream.close()

                if (content.isBlank()) {
                    message = "Файл пустой"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val lines = content.lines().filter { it.isNotBlank() }
                if (lines.size > 5000) {
                    message = "Файл слишком большой (макс. 5000 строк)"
                    isError = true
                    isLoading = false
                    return@rememberLauncherForActivityResult
                }

                val delimiter = detectDelimiter(lines)
                val cards = mutableListOf<Card>()
                val today = RateCardUseCase.getTodayDate()

                for (line in lines) {
                    val parts = line.split(delimiter).map { it.trim() }
                    if (parts.size >= 2) {
                        val front = parts[0]
                        val back = parts.drop(1).joinToString(", ")
                        if (front.isNotBlank() && back.isNotBlank()) {
                            cards.add(
                                Card(
                                    id = java.util.UUID.randomUUID().toString(),
                                    front = front,
                                    back = back,
                                    dueDate = today,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                if (cards.isEmpty()) {
                    message = "Не найдено подходящих пар в файле"
                    isError = true
                    isSuccess = false
                } else {
                    onCardsImported(cards)
                    importedCount = cards.size
                    isSuccess = true
                    isError = false
                    message = ""
                }
            } catch (e: Exception) {
                message = "Ошибка: ${e.message}"
                isError = true
                isSuccess = false
            }
            isLoading = false
        }
    }

    Scaffold(
        containerColor = FdBackground,
        topBar = {
            Surface(color = FdSurface, shadowElevation = 0.dp) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, null, tint = FdText)
                        }
                        Text(
                            "Импорт карточек",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = FdText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Divider(color = FdBorder, thickness = 1.5.dp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drop zone
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FdPrimaryLight)
                        .border(2.dp, FdPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoading) { fileLauncher.launch("text/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = FdPrimary, modifier = Modifier.size(36.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📂", fontSize = 40.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Нажмите, чтобы выбрать файл",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FdPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "CSV, TSV — до 5000 строк",
                                fontSize = 12.sp,
                                color = FdTextSub
                            )
                        }
                    }
                }
            }

            // Format instructions
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FdSurface)
                        .border(1.5.dp, FdBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "📋 Формат файла",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = FdText
                        )
                        Spacer(Modifier.height(10.dp))
                        FormatRow("Разделитель", "Tab (\\t), точка с запятой (;) или запятая (,)")
                        Spacer(Modifier.height(8.dp))
                        FormatRow("Структура", "лицевая сторона [разделитель] оборотная сторона")
                        Spacer(Modifier.height(12.dp))
                        // Example box
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(FdSurface2)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "Пример CSV:",
                                    fontSize = 11.sp,
                                    color = FdTextSub,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    "apple,яблоко\nbanana,банан\nhello,привет",
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = FdText
                                )
                            }
                        }
                    }
                }
            }

            // Result / error
            if (isSuccess) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FdGreenLight)
                            .border(1.5.dp, FdGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "✅ Импорт успешен!",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = FdGreenDark
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Добавлено $importedCount карточек",
                                fontSize = 13.sp,
                                color = FdTextSub
                            )
                            Spacer(Modifier.height(14.dp))
                            PressButton(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                color = FdGreen,
                                shadowColor = FdGreenDark,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Готово",
                                    fontFamily = OutfitFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (isError) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FdRedLight)
                            .border(1.5.dp, FdRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "⚠️ Ошибка",
                                fontFamily = OutfitFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = FdRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(message, fontSize = 13.sp, color = FdTextSub)
                        }
                    }
                }
            }

            // Main action button
            if (!isSuccess) {
                item {
                    PressButton(
                        onClick = { fileLauncher.launch("text/*") },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        color = FdPrimary,
                        shadowColor = FdPrimaryDark,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading
                    ) {
                        Text(
                            if (isLoading) "Загрузка..." else "Выбрать файл",
                            fontFamily = OutfitFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 12.sp,
            color = FdTextSub,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(90.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 12.sp, color = FdText, modifier = Modifier.weight(1f))
    }
}

private fun detectDelimiter(lines: List<String>): Char {
    var tabCount = 0
    var semicolonCount = 0
    var commaCount = 0

    for (line in lines.take(10)) {
        tabCount += line.count { it == '\t' }
        semicolonCount += line.count { it == ';' }
        commaCount += line.count { it == ',' }
    }

    return when {
        tabCount > semicolonCount && tabCount > commaCount -> '\t'
        semicolonCount > commaCount -> ';'
        else -> ','
    }
}
