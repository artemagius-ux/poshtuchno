package io.github.artemagius.poshtuchno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.artemagius.poshtuchno.ui.home.HomeScreen
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PoshtuchnoTheme {
                // P0: экран-скелет на статических данных.
                // Подключение к Room и быстрый ввод — следующий этап (P1).
                HomeScreen(
                    monthTotalKopecks = 0,
                    monthLimitKopecks = null,
                    recent = emptyList(),
                    onAddClick = {},
                )
            }
        }
    }
}
