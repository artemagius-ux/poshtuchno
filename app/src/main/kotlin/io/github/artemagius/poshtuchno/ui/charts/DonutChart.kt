package io.github.artemagius.poshtuchno.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

data class DonutSlice(
    val label: String,
    val amountKopecks: Long,
    val color: Color,
)

/**
 * Кольцевая диаграмма категорий с суммой в центре.
 *
 * Рисуется на Canvas, без внешних библиотек: график простой, а зависимость
 * на charting-библиотеку добавила бы к APK больше, чем стоит эта функция.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    totalKopecks: Long,
    modifier: Modifier = Modifier,
    centerLabel: String = "за период",
) {
    val total = slices.sumOf { it.amountKopecks }.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = if (slices.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 550),
        label = "donut",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val description = donutDescription(slices, totalKopecks)

    Box(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Кольцо всегда круглое: берём наименьшую сторону и центруем.
            val diameter = size.minDimension
            val strokeWidth = diameter * 0.14f
            val arcSide = diameter - strokeWidth
            val topLeft = Offset(
                x = (size.width - arcSide) / 2f,
                y = (size.height - arcSide) / 2f,
            )
            val arcSize = Size(arcSide, arcSide)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )

            val gap = if (slices.size > 1) 3f else 0f
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.amountKopecks / total * progress
                if (sweep > gap) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + gap / 2,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                }
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = money(totalKopecks),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun donutDescription(slices: List<DonutSlice>, totalKopecks: Long): String =
    if (slices.isEmpty()) {
        "Диаграмма категорий пуста"
    } else {
        val parts = slices.take(3).joinToString(", ") { "${it.label} ${Money.format(it.amountKopecks)}" }
        "Категории: $parts. Всего ${Money.format(totalKopecks)}"
    }

/** Легенда к кольцу: цвет, название, доля и сумма. */
@Composable
fun DonutLegend(
    slices: List<DonutSlice>,
    shares: List<Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        slices.forEachIndexed { index, slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = slice.color, shape = CircleShape),
                )
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                )
                Text(
                    text = "${shares.getOrNull(index) ?: 0}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = money(slice.amountKopecks),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DonutPreview() {
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        Column(Modifier.padding(16.dp)) {
            DonutChart(
                slices = listOf(
                    DonutSlice("Продукты", 430_000, Color(0xFF6750A4)),
                    DonutSlice("Кафе", 180_000, Color(0xFF9A82DB)),
                    DonutSlice("Транспорт", 90_000, Color(0xFFC5B4F0)),
                ),
                totalKopecks = 700_000,
                modifier = Modifier.height(180.dp),
            )
        }
    }
}
