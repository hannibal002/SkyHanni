package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeExp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeLevel
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object HoeLevelDisplay {

    val pos = Position(100, 100)
    var hoeLevels: List<Int>? = null
    var hoeOverflow = 200000
    var display: List<Renderable>? = null

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        display = null
        val list = mutableListOf<Renderable>()
        list.add(Renderable.text("§6Hoe Levels"))
        val hoeExp = InventoryUtils.getItemInHand()?.getHoeExp() ?: return
        val hoeLevel = InventoryUtils.getItemInHand()?.getHoeLevel() ?: return
        var next = hoeOverflow
        if (hoeLevel < 50) {
            list.add(Renderable.text("§7Level §8$hoeLevel➜§3${hoeLevel+1}"))
            next = hoeLevels?.let { it[hoeLevel-1] } ?: return
        }
        hoeLevels ?: return
        var colorPrefix = "§e"
        if (hoeExp > next) {
            colorPrefix = "§c§l"
            if (hoeLevel >= 40) list.add(Renderable.text("§3§lOVERCLOCK REQUIRED!"))
            else list.add(Renderable.text("§c§lUPGRADE REQUIRED!"))
        }
        val formattedXp = hoeExp.addSeparators()
        val formattedXpToNext = next.addSeparators()
        list.add(Renderable.text("$colorPrefix$formattedXp§8/§e$formattedXpToNext"))


        display = list
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        val renderable = display ?: return
        pos.renderRenderables(renderable, posLabel = "amazing")
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        hoeLevels = event.getConstant<GardenJson>("Garden").hoeExpLevels
        hoeOverflow = event.getConstant<GardenJson>("Garden").hoeExpOverflow
    }
}