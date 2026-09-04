package io.github.artemagius.poshtuchno.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")

/**
 * Строка траты. Используется на всех вкладках, поэтому вынесена отдельно:
 * так вид и поведение не расходятся между экранами.
 */
@Composable
fun PurchaseRow(
    purchase: PurchaseListItem,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: String? = null,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { CategoryAvatar(icon = icon, tint = accent) },
            headlineContent = {
                Text(
                    text = purchaseTitle(purchase),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                Text(
                    text = purchaseSubtitle(purchase, showDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Money.format(purchase.totalKopecks),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.size(2.dp))
                }
            },
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        )
    }
}

fun purchaseTitle(purchase: PurchaseListItem): String = when {
    !purchase.note.isNullOrBlank() -> purchase.note
    !purchase.shopName.isNullOrBlank() -> purchase.shopName
    !purchase.topCategoryName.isNullOrBlank() -> purchase.topCategoryName
    else -> "Трата"
}

fun purchaseSubtitle(purchase: PurchaseListItem, showDate: Boolean = true): String {
    val dateTime = Periods.toLocalDateTime(purchase.purchasedAt)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    val time = timeFormatter.format(dateTime)
    val whenText = when {
        !showDate -> time
        date == today -> "сегодня, $time"
        date == today.minusDays(1) -> "вчера, $time"
        else -> "${dateFormatter.format(date)}, $time"
    }
    val category = purchase.topCategoryName
    return if (category.isNullOrBlank()) whenText else "$category · $whenText"
}

/** Разделитель дня в истории: дата слева, сумма за день справа. */
@Composable
fun DayHeader(
    title: String,
    totalKopecks: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.format(totalKopecks),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
