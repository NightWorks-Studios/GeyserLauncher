package dev.lisfox.geyserlauncher.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeyserRuntimeTest {
    @Test
    fun remainsStartingUntilDoneLineArrives() {
        GeyserRuntime.beginStart()
        GeyserRuntime.emit("Geyser 进程已启动", true)
        GeyserRuntime.emit("[INFO] Loading extensions...", true)

        assertEquals(RuntimePhase.STARTING, GeyserRuntime.phase.value)

        GeyserRuntime.emit("[INFO] Done (1.42s)! Run /geyser help for help!", true)

        assertEquals(RuntimePhase.RUNNING, GeyserRuntime.phase.value)
        assertNull(GeyserRuntime.error.value)
    }

    @Test
    fun startupExitBecomesVisibleError() {
        GeyserRuntime.beginStart()
        GeyserRuntime.emit("Geyser 已退出，代码: 1", false)

        assertEquals(RuntimePhase.ERROR, GeyserRuntime.phase.value)
        assertTrue(GeyserRuntime.error.value.orEmpty().contains("代码: 1"))

        GeyserRuntime.consumeError()
    }

    @Test
    fun normalStopReturnsToStopped() {
        GeyserRuntime.beginStart()
        GeyserRuntime.emit("[INFO] Done (1.0s)!", true)
        GeyserRuntime.emit("Geyser 已停止", false)

        assertEquals(RuntimePhase.STOPPED, GeyserRuntime.phase.value)
    }
}
