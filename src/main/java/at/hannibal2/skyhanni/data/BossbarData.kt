package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import net.minecraft.entity.boss.BossStatus
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BossbarData {
    private var bossbar: String? = null
    private var previousServerBossbar = ""

    fun getBossbar() = bossbar ?: ""

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        val oldBossbar = bossbar ?: return
        previousServerBossbar = oldBossbar
        bossbar = null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val bossbarLine = BossStatus.bossName ?: return
        if (bossbarLine.isBlank() || bossbarLine.isEmpty()) return
        if (bossbarLine == bossbar) return
        if (bossbarLine == previousServerBossbar) return
        if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""

        bossbar = bossbarLine
        BossbarUpdateEvent(bossbarLine).postAndCatch()
    }
}
