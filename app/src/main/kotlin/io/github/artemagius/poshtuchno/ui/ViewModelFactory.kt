package io.github.artemagius.poshtuchno.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Фабрика ViewModel без DI-фреймворка: конструктор передаётся лямбдой.
 */
@Suppress("UNCHECKED_CAST")
fun <VM : ViewModel> viewModelFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
