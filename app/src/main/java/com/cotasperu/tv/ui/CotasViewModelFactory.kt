package com.cotasperu.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cotasperu.tv.di.AppGraph

class CotasViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            GateViewModel::class.java -> GateViewModel(graph) as T
            HomeViewModel::class.java -> HomeViewModel(graph) as T
            else -> error("Modelo no registrado")
        }
    }
}
