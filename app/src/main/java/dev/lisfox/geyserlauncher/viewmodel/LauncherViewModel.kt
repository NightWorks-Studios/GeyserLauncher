package dev.lisfox.geyserlauncher.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dev.lisfox.geyserlauncher.GeyserService
import dev.lisfox.geyserlauncher.runtime.GeyserRuntime
import dev.lisfox.geyserlauncher.runtime.RuntimePhase

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val phase = GeyserRuntime.phase
    val logs = GeyserRuntime.logs
    val lastMessage = GeyserRuntime.lastMessage
    val error = GeyserRuntime.error

    fun toggle() {
        val action = if (phase.value == RuntimePhase.STARTING || phase.value == RuntimePhase.RUNNING) {
            GeyserService.STOP
        } else {
            GeyserRuntime.beginStart()
            GeyserService.START
        }
        val intent = Intent(getApplication(), GeyserService::class.java).setAction(action)
        if (action == GeyserService.START) {
            ContextCompat.startForegroundService(getApplication(), intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun clearLogs() = GeyserRuntime.clearLogs()
    fun consumeError() = GeyserRuntime.consumeError()

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        val intent = Intent(getApplication(), GeyserService::class.java)
            .setAction(GeyserService.COMMAND)
            .putExtra(GeyserService.EXTRA_COMMAND, command)
        getApplication<Application>().startService(intent)
    }
}
