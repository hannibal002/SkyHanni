//
// https://discord.com/channels/997079228510117908/1167847565564334090
//

package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private val config get() = SkyHanniMod.feature.fishing.totemOfCorruption
private var display = emptyList<String>()
private var totems: List<Totem> = emptyList()

class TotemOfCorruption {
    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!enabled()) return
        if (display.isEmpty()) return

        LorenzUtils.chat("totems: ${totems.size}")
        LorenzUtils.chat("display: ${display.size}")

        config.position.renderStrings(display, posLabel = "Totem of Corruption")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!enabled()) return

        if (getTotems().isEmpty()) {
            clearData()
            return
        }

        LorenzUtils.chat("amount: ${getTotems().size}")

        for (totem in getTotems()) {
            if (totem == null) continue
            val timeRemainingEntity = EntityUtils.getEntitiesNearby<EntityArmorStand>(
                totem.getLorenzVec(),
                2.0
            ).filter { it.name.startsWith("§7Time Remaining: §e") }.firstOrNull()
            val ownerEntity = EntityUtils.getEntitiesNearby<EntityArmorStand>(
                totem.getLorenzVec(),
                2.0
            ).filter { it.name.startsWith("§7Owner: §e") }.firstOrNull()
            if (timeRemainingEntity != null && ownerEntity != null) {
                totems += Totem(
                    totem,
                    timeRemainingEntity.nameToSeconds(),
                    ownerEntity.name.substring(11)
                )
            }
        }

        display = createLines()
    }

    private fun createLines(): List<String> {
        val totem = getTotemToShow() ?: return emptyList()
        LorenzUtils.chat("building string")
        val lines = mutableListOf<String>()
        lines.add("§5§lTotem of Corruption")
        if (totem.timeRemainingSeconds < 60) {
            lines.add("§7Remaining: §e${totem.timeRemainingSeconds % 60}s")
        } else {
            lines.add("§7Remaining: §e${totem.timeRemainingSeconds / 60}min ${totem.timeRemainingSeconds % 60}s")
        }
        lines.add("§7Owner: §e${totem.ownerName}")
        return lines
    }

    private fun getTotemToShow(): Totem? {
        return totems.maxByOrNull { it.timeRemainingSeconds }
    }

    private fun getTotems(): List<EntityArmorStand?> {
        return EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(20.0)
            .filter { it.inventory?.get(4)?.name == "§aTotem of Corruption" }.toList()
    }

    private fun clearData() {
        totems = emptyList()
        display = emptyList()
    }

    private fun Entity.nameToSeconds(): Int {
        return name.substring(15).replace("m", "").replace("s", "").split(" ").let {
            if (it.size == 2) {
                it[0].toInt() * 60 + it[1].substring(0, it[1].length).toInt()
            } else {
                it[0].substring(0, it[0].length).toInt()
            }
        }
    }

    private fun enabled() = config.enabled && LorenzUtils.inSkyBlock
}

class Totem(
    val totemEntity: EntityArmorStand,
    val timeRemainingSeconds: Int,
    val ownerName: String
)
