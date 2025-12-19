package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import net.minecraft.client.Minecraft

@SkyHanniModule
object BossbarData {
    private var bossbar: String? = null
    private var previousServerBossbar = ""

    fun getBossbar() = bossbar.orEmpty()

    @HandleEvent
    fun onWorldChange() {
        val oldBossbar = bossbar ?: return
        previousServerBossbar = oldBossbar
        bossbar = null
    }

    @HandleEvent
    fun onTick() {
        var multipleBossBars = false
        for (bossBar in Minecraft.getInstance().gui.bossOverlay.events.values) {
            if (multipleBossBars) {
                return
            }
            multipleBossBars = true
            val bossbarLine = bossBar.name.unformattedTextCompat()
            if (bossbarLine.isBlank() || bossbarLine.isEmpty()) continue
            if (bossbarLine == bossbar) continue
            if (bossbarLine == previousServerBossbar) continue
            if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""
            bossbar = bossbarLine
            BossbarUpdateEvent(bossbarLine).post()
        }
    }
}
