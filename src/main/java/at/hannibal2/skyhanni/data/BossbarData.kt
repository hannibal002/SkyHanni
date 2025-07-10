package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
//#if MC < 1.21
import net.minecraft.entity.boss.BossStatus
//#else
//$$ import at.hannibal2.skyhanni.test.command.ErrorManager
//$$ import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
//$$ import net.minecraft.client.MinecraftClient
//#endif

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
        //#if MC < 1.21
        val bossbarLine = BossStatus.bossName ?: return
        if (bossbarLine.isBlank() || bossbarLine.isEmpty()) return
        if (bossbarLine == bossbar) return
        if (bossbarLine == previousServerBossbar) return
        if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""

        bossbar = bossbarLine
        BossbarUpdateEvent(bossbarLine).post()
        //#else
        //$$ var multipleBossBars = false
        //$$ for (bossBar in MinecraftClient.getInstance().inGameHud.bossBarHud.bossBars.values) {
        //$$     if (multipleBossBars) {
        //$$         ErrorManager.skyHanniError("Multiple Bossbars")
        //$$     }
        //$$     multipleBossBars = true
        //$$     val bossbarLine = bossBar.name.unformattedTextCompat()
        //$$     if (bossbarLine.isBlank() || bossbarLine.isEmpty()) continue
        //$$     if (bossbarLine == bossbar) continue
        //$$     if (bossbarLine == previousServerBossbar) continue
        //$$     if (previousServerBossbar.isNotEmpty()) previousServerBossbar = ""
        //$$     bossbar = bossbarLine
        //$$     BossbarUpdateEvent(bossbarLine).post()
        //$$ }
        //#endif
    }
}
