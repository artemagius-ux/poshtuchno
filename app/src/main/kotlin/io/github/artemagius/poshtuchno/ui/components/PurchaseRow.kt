package io.github.artemagius.poshtuchno.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.rememberDateFormatter
import java.time.LocalDate

/**
 * Строка траты. Одна на все экраны, чтобы вид и поведение не расходились.
 */
@Composable
fun PurchaseRow(
    purchase: PurchaseListItem,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { CategoryAvatar(icon = purchase.topCategoryIcon) },
            headlineContent = {
                Text(
                    text = purchaseTitle(purchase),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = purchaseSubtitle(purchase, showDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Text(
                    text = money(purchase.totalKopecks),
                    style = MaterialTheme.typography.titleMedium,
                )
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

@Composable
fun purchaseSubtitle(purchase: PurchaseListItem, showDate: Boolean = true): String {
    val dateTime = Periods.toLocalDateTime(purchase.purchasedAt)
    val time = rememberDateFormatter("HH:mm").format(dateTime)
    val dateText = rememberDateFormatter("d MMMM").format(dateTime)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    val whenText = when {
        !showDate -> time
        date == today -> "сегодня, $time"
        date == today.minusDays(1) -> "вчера, $time"
        else -> "$dateText, $time"
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = money(totalKopecks),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
