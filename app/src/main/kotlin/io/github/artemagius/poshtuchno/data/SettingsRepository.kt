package io.github.artemagius.poshtuchno.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    /** Следовать системной теме. */
    Auto("Авто"),
    Light("Светлая"),
    Dark("Тёмная"),
    ;

    companion object {
        fun parse(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: Auto
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.Auto,
    val palette: String = "Violet",
    /** Обои-цвета Android 12+. Выключено по умолчанию: фирменный фиолетовый важнее. */
    val dynamicColor: Boolean = false,
    /** Закрывать приложение сразу после сохранения траты. */
    val closeAfterSave: Boolean = false,
    val showKopecks: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val palette = stringPreferencesKey("palette")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val closeAfterSave = booleanPreferencesKey("close_after_save")
        val showKopecks = booleanPreferencesKey("show_kopecks")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.parse(prefs[Keys.themeMode]),
            palette = prefs[Keys.palette] ?: "Violet",
            dynamicColor = prefs[Keys.dynamicColor] ?: false,
            closeAfterSave = prefs[Keys.closeAfterSave] ?: false,
            showKopecks = prefs[Keys.showKopecks] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setPalette(name: String) {
        context.dataStore.edit { it[Keys.palette] = name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setCloseAfterSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.closeAfterSave] = enabled }
    }

    suspend fun setShowKopecks(enabled: Boolean) {
        context.dataStore.edit { it[Keys.showKopecks] = enabled }
    }
}
