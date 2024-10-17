package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.SkyhanniTickEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.entity.boss.BossStatus
import at.hannibal2.skyhanni.api.event.HandleEvent

@SkyHanniModule
object BossbarData {
    private var bossbar: String? = null
    private var previousServerBossbar = ""

    fun getBossbar() = bossbar ?: ""

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        val oldBossbar = bossbar ?: return
        previousServerBossbar = oldBossbar
        bossbar = null
    }

    @HandleEvent
    fun onTick(event: SkyhanniTickEvent) {
        val bossbarLine = BossStatus.bossName ?: return
        if (bossbarLine.isBlank() || bossbarLine.isEmpty()) return
        if (bossbarLine == bossbar) return
        if (bossbarLine == previousServerBossbar) return
        if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""

        bossbar = bossbarLine
        BossbarUpdateEvent(bossbarLine).post()
    }
}
