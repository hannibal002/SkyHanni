package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.blendRGB
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object MobHealthDisplay {

    private val config get() = SkyHanniMod.feature.fishing.healthDisplay

    private val healthMap = mutableMapOf<LivingSeaCreatureData, Int>()

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = healthMap.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        for (seaCreature in healthMap.keys) {
            if (!seaCreature.exists() || !seaCreature.canBeSeen()) continue
            val health = seaCreature.health ?: continue
            if (health == -1) continue
            healthMap[seaCreature] = health
        }
    }

    private fun formatHealth(mob: Mob): Component {
        val health = mob.health
        val maxHealth = mob.maxHealth
        val percentage = (1 - (health / maxHealth)).toDouble()
        val componentColor = blendRGB(LorenzColor.GREEN, LorenzColor.RED, percentage)
        return componentBuilder {
            appendWithColor(health.shortFormat(), componentColor.rgb)
            appendWithColor("/", ChatFormatting.WHITE)
            appendWithColor(maxHealth.shortFormat(), ChatFormatting.GREEN)
            appendWithColor(" ❤", ChatFormatting.RED)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (healthMap.isEmpty()) return
        val strings = buildList {
            for ((seaCreature, health) in healthMap) {
                if (health == -1) continue
                val mob = seaCreature.mob ?: continue
                val color = if (seaCreature.isOwn) ChatFormatting.GREEN else ChatFormatting.RED
                val guiComponent = componentBuilder {
                    appendWithColor(seaCreature.name, color)
                    append(" ")
                    append(formatHealth(mob))
                }
                add(Renderable.text(guiComponent))
                if (size >= config.limit) break
            }
        }

        config.pos.renderRenderables(strings, posLabel = "Mob Health Display")
    }

    private fun addMob(seaCreature: LivingSeaCreatureData) {
        if (SeaCreatureSettings.getConfig(seaCreature)?.shouldShowHealthOverlay == true) {
            val value = if (seaCreature.canBeSeen()) seaCreature.health else null
            healthMap[seaCreature] = value ?: -1
        }
    }

}
