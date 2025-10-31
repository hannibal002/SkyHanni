package at.hannibal2.hanni.features.itemabilities

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.itemability.FireVeilWandConfig.DisplayEntry
import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.events.ItemClickEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.nether.ashfang.AshfangFreezeCooldown
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawCircleWireframe
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object FireVeilWandParticles {

    private val config get() = HanniMod.feature.inventory.itemAbilities.fireVeilWands
    private val item = "FIRE_VEIL_WAND".toInternalName()

    private var lastClick = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (config.display == DisplayEntry.PARTICLES) return
        if (lastClick.passedSince() > 5.5.seconds) return
        if (event.type == EnumParticleTypes.FLAME && event.speed == 0.55f) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemClick(event: ItemClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val internalName = event.itemInHand?.getInternalName()

        if (AshfangFreezeCooldown.isCurrentlyFrozen()) return

        if (internalName == item) {
            lastClick = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (config.display != DisplayEntry.LINE) return
        if (lastClick.passedSince() > 5.5.seconds) return

        val color = config.displayColor.toColor()
        event.drawCircleWireframe(MinecraftCompat.localPlayer, rad = 3.5, color)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "itemAbilities.fireVeilWandDisplayColor", "itemAbilities.fireVeilWands.displayColor")
        event.move(3, "itemAbilities.fireVeilWandDisplay", "itemAbilities.fireVeilWands.display")
    }
}
