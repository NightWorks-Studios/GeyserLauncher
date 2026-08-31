package dev.lisfox.geyserlauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lisfox.geyserlauncher.GeyserService
import dev.lisfox.geyserlauncher.data.GeyserDistribution
import dev.lisfox.geyserlauncher.runtime.GeyserRuntime
import dev.lisfox.geyserlauncher.runtime.RuntimePhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeyserUpdateViewModel(application: Application) : AndroidViewModel(application) {
    val state = GeyserDistribution.state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            GeyserDistribution.refreshInstalled(getApplication())
        }
    }

    fun check() {
        if (state.value.operation != dev.lisfox.geyserlauncher.data.DistributionOperation.IDLE) return
        viewModelScope.launch(Dispatchers.IO) { runCatching { GeyserDistribution.checkLatest() } }
    }

    fun update() {
        if (state.value.operation != dev.lisfox.geyserlauncher.data.DistributionOperation.IDLE) return
        if (GeyserService.isProcessActive() ||
            GeyserRuntime.phase.value in setOf(RuntimePhase.STARTING, RuntimePhase.RUNNING)
        ) return
        viewModelScope.launch(Dispatchers.IO) { runCatching { GeyserDistribution.installLatest(getApplication()) } }
    }
}
