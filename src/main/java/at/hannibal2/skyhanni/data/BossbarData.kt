package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.entity.boss.BossStatus

@SkyHanniModule
object BossbarData {
    private var bossbar: String? = null
    private var previousServerBossbar = ""

    fun getBossbar() = bossbar.orEmpty()

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        val oldBossbar = bossbar ?: return
        previousServerBossbar = oldBossbar
        bossbar = null
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        val bossbarLine = BossStatus.bossName ?: return
        if (bossbarLine.isBlank() || bossbarLine.isEmpty()) return
        if (bossbarLine == bossbar) return
        if (bossbarLine == previousServerBossbar) return
        if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""

        bossbar = bossbarLine
        BossbarUpdateEvent(bossbarLine).post()
    }
}
