package uz.nodirbek.flashcardsapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.ui.components.UnifiedAppBar

data class DeckSource(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val badge: String,
    val badgeColor: Color
)

private val sources = listOf(
    DeckSource(
        id = "ankiweb",
        emoji = "🗂️",
        name = "AnkiWeb Shared Decks",
        description = "Огромная база от сообщества Anki — языки, медицина, история и многое другое. Файлы .apkg импортируются напрямую.",
        tags = listOf("Английский", "Японский", "Медицина", "История"),
        badge = "2000+ колод",
        badgeColor = Color(0xFF4F8EF7)
    ),
    DeckSource(
        id = "opentdb",
        emoji = "🧠",
        name = "Open Trivia DB",
        description = "Бесплатная база вопросов и ответов без ограничений. Отлично подходит для общих знаний и подготовки к тестам.",
        tags = listOf("Наука", "История", "География", "Компьютеры"),
        badge = "Без лимитов",
        badgeColor = Color(0xFF4CAF50)
    ),
    DeckSource(
        id = "github",
        emoji = "🐙",
        name = "GitHub Decks",
        description = "Поиск .apkg файлов в открытых репозиториях GitHub. Тысячи готовых колод от разработчиков со всего мира.",
        tags = listOf("Языки", "Программирование", "Математика", "Химия"),
        badge = "Открытый код",
        badgeColor = Color(0xFF9C27B0)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSourceScreen(
    onBackClick: () -> Unit,
    onSelectAnkiWeb: () -> Unit,
    onSelectOpenTDB: () -> Unit,
    onSelectGitHub: () -> Unit
) {
    Scaffold(
        topBar = {
            UnifiedAppBar(
                title = "Найти колоды",
                onBackClick = onBackClick,
                showBackButton = true,
                showDivider = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Выберите источник",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            sources.forEach { source ->
                SourceCard(
                    source = source,
                    onClick = {
                        when (source.id) {
                            "ankiweb" -> onSelectAnkiWeb()
                            "opentdb" -> onSelectOpenTDB()
                            "github" -> onSelectGitHub()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SourceCard(source: DeckSource, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(source.emoji, fontSize = 26.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = source.badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = source.badge,
                            color = source.badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = source.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                source.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
