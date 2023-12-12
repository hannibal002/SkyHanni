package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TotemOfCorruption {
    private var totemEntity: EntityArmorStand? = null
    private var timeRemainingEntity: EntityArmorStand? = null
    private var ownerEntity: EntityArmorStand? = null
    private var timeRemainingSeconds: Int? = null
    private var ownerName: String? = null

    private val config get() = SkyHanniMod.feature.fishing.totemOfCorruption
    private var display = emptyList<String>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!enabled()) return

        totemEntity = EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(40.0)
            .firstOrNull { it.inventory?.get(4)?.name == "§aTotem of Corruption" } ?: return clearData()
        timeRemainingEntity = EntityUtils.getEntitiesNearby<EntityArmorStand>(totemEntity!!.getLorenzVec(), 2.0)
            .firstOrNull { it.name.startsWith("§7Remaining: §e") } ?: return clearData()
        ownerEntity = EntityUtils.getEntitiesNearby<EntityArmorStand>(totemEntity!!.getLorenzVec(), 2.0)
            .firstOrNull { it.name.startsWith("§7Owner: §e") } ?: return clearData()

        // time format: 1min 23s AND 24s, returns seconds
        timeRemainingSeconds =
            timeRemainingEntity!!.name.substring(15).replace("m", "").replace("s", "").split(" ").let {
                if (it.size == 2) {
                    it[0].toInt() * 60 + it[1].substring(0, it[1].length).toInt()
                } else {
                    it[0].substring(0, it[0].length).toInt()
                }
            }

        ownerName = ownerEntity!!.name.substring(11)

        display = createLines()
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!enabled()) return
        if (display.isEmpty()) return

        config.position.renderStrings(display, posLabel = "Totem of Corruption")
    }

    private fun createLines(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("§5§lTotem of Corruption")
        if (timeRemainingSeconds!! < 60) {
            lines.add("§7Remaining: §e${timeRemainingSeconds!! % 60}s")
        } else {
            lines.add("§7Remaining: §e${timeRemainingSeconds!! / 60}min ${timeRemainingSeconds!! % 60}s")
        }
        lines.add("§7Owner: §e$ownerName")
        return lines
    }

    private fun enabled() = config.enabled && LorenzUtils.inSkyBlock

    private fun clearData() {
        totemEntity = null
        timeRemainingEntity = null
        ownerEntity = null
        timeRemainingSeconds = null
        display = emptyList()
    }
}
