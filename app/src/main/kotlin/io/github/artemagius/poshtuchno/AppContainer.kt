package io.github.artemagius.poshtuchno

import android.content.Context
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.db.PoshtuchnoDatabase

/**
 * Ручной контейнер зависимостей. Пока графа маленькая, Hilt избыточен:
 * одна база, один репозиторий. Если графа разрастётся — заменим.
 */
class AppContainer(context: Context) {
    private val database: PoshtuchnoDatabase = PoshtuchnoDatabase.get(context)

    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database) }
}
