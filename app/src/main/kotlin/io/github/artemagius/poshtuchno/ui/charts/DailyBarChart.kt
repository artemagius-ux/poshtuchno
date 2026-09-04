package io.github.artemagius.poshtuchno.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate

/**
 * Столбики по дням. Тап по столбику выделяет день — так можно посмотреть
 * конкретную дату, не открывая историю.
 */
@Composable
fun DailyBarChart(
    days: List<DayAmount>,
    modifier: Modifier = Modifier,
    highlighted: LocalDate? = null,
    onDaySelected: (LocalDate?) -> Unit = {},
    labelEvery: Int = 5,
) {
    if (days.isEmpty()) return

    val maxAmount = days.maxOf { it.totalKopecks }.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "bars",
    )

    val barColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val description = "График трат по дням, максимум за день ${Money.format(maxAmount)}"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .pointerInput(days.size) {
                detectTapGestures { offset ->
                    val slot = size.width.toFloat() / days.size
                    val index = (offset.x / slot).toInt().coerceIn(0, days.lastIndex)
                    val date = days[index].date
                    onDaySelected(if (date == highlighted) null else date)
                }
            },
    ) {
        val labelHeight = 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val slot = size.width / days.size
        val barWidth = (slot * 0.56f).coerceAtMost(18.dp.toPx())
        val radius = barWidth / 2

        drawLine(
            color = gridColor,
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx(),
        )

        days.forEachIndexed { index, day ->
            val centerX = slot * index + slot / 2
            val isEmpty = day.totalKopecks == 0L
            val color = when {
                isEmpty -> emptyColor
                highlighted == null || highlighted == day.date -> barColor
                else -> dimColor
            }

            if (isEmpty) {
                // Пустой день показываем точкой на базовой линии, а не нулевым столбиком.
                drawCircle(
                    color = color,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, chartHeight - 2.dp.toPx()),
                )
            } else {
                val fraction = day.totalKopecks.toFloat() / maxAmount
                val barHeight = ((chartHeight - radius) * fraction * progress)
                    .coerceAtLeast(barWidth * 0.6f)
                val top = chartHeight - barHeight
                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerX - barWidth / 2, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }

            val showLabel = index % labelEvery == 0 || index == days.lastIndex || day.date == highlighted
            if (showLabel) {
                val measured = textMeasurer.measure(day.date.dayOfMonth.toString(), labelStyle)
                val x = (centerX - measured.size.width / 2f)
                    .coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f))
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(x, chartHeight + 3.dp.toPx()),
                )
            }
        }

        if (highlighted != null) {
            val index = days.indexOfFirst { it.date == highlighted }
            if (index >= 0) {
                val centerX = slot * index + slot / 2
                drawLine(
                    color = barColor.copy(alpha = 0.4f),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                    ),
                )
            }
        }
    }
}

/** Компактный спарклайн: тренд за последние дни без осей и подписей. */
@Composable
fun Sparkline(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.size < 2) return
    val max = values.max().coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxSize()) {
        val stepX = size.width / (values.size - 1)
        val line = Path()
        val fill = Path()

        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height * (1f - value.toFloat() / max)
            if (index == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()

        drawPath(path = fill, color = color.copy(alpha = 0.15f))
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BarsPreview() {
    val base = LocalDate.of(2026, 9, 1)
    val days = (0..29).map {
        DayAmount(base.plusDays(it.toLong()), if (it % 4 == 0) 0 else (30_000L + it * 4_500))
    }
    PoshtuchnoTheme(dynamicColor = false) {
        Column(Modifier.padding(16.dp)) {
            Text("Траты по дням", fontWeight = FontWeight.SemiBold)
            DailyBarChart(
                days = days,
                highlighted = base.plusDays(9),
                modifier = Modifier.height(160.dp),
            )
        }
    }
}
