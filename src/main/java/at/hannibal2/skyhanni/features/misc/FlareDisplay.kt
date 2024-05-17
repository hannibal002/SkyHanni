package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.FlareConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object FlareDisplay {

    private val config get() = SkyHanniMod.feature.misc.flareConfig
    private var display = emptyList<Renderable>()
    private var flareList = mutableMapOf<FlareType, EntityArmorStand>()
    private const val FLARE_TIME = 180_000

    private val flares = mapOf(
        "ewogICJ0aW1lc3RhbXAiIDogMTY0NjY4NzMwNjIyMywKICAicHJvZmlsZUlkIiA6ICI0MWQzYWJjMmQ3NDk0MDBjOTA5MGQ1NDM0ZDAzODMxYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWdha2xvb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjJlMmJmNmMxZWMzMzAyNDc5MjdiYTYzNDc5ZTU4NzJhYzY2YjA2OTAzYzg2YzgyYjUyZGFjOWYxYzk3MTQ1OCIKICAgIH0KICB9Cn0="
            to FlareType.WARNING,
        "ewogICJ0aW1lc3RhbXAiIDogMTY0NjY4NzMyNjQzMiwKICAicHJvZmlsZUlkIiA6ICI0MWQzYWJjMmQ3NDk0MDBjOTA5MGQ1NDM0ZDAzODMxYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWdha2xvb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQyYmY5ODY0NzIwZDg3ZmQwNmI4NGVmYTgwYjc5NWM0OGVkNTM5YjE2NTIzYzNiMWYxOTkwYjQwYzAwM2Y2YiIKICAgIH0KICB9Cn0="
            to FlareType.ALERT,
        "ewogICJ0aW1lc3RhbXAiIDogMTY0NjY4NzM0NzQ4OSwKICAicHJvZmlsZUlkIiA6ICI0MWQzYWJjMmQ3NDk0MDBjOTA5MGQ1NDM0ZDAzODMxYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWdha2xvb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzAwNjJjYzk4ZWJkYTcyYTZhNGI4OTc4M2FkY2VmMjgxNWI0ODNhMDFkNzNlYTg3YjNkZjc2MDcyYTg5ZDEzYiIKICAgIH0KICB9Cn0="
            to FlareType.SOS
    )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderRenderables(display, posLabel = "Flare Timer")
    }

    @SubscribeEvent
    fun onSecondsPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        flareList.values.removeIf { !it.isEntityAlive }
        EntityUtils.getAllEntities().filterIsInstance<EntityArmorStand>().forEach { entity ->
            if (entity.ticksExisted > FLARE_TIME) return@forEach
            for ((texture, flareType) in flares) {
                if (flareList.contains(flareType)) return@forEach
                if (entity.hasSkullTexture(texture)) {
                    flareList[flareType] = entity
                }
            }
        }
        display = buildList {
            for ((flare, entity) in flareList) {
                val time = (FLARE_TIME - entity.ticksExisted / 20 * 1000).milliseconds

                add(Renderable.string("§6${flare.displayName}: §e$time §b${flare.buff}"))
                if (time <= 5.seconds) {
                    when (config.alertType) {
                        FlareConfig.AlertType.CHAT -> {
                            ChatUtils.chat("§6${flare.displayName} expire in b${time.inWholeSeconds}")
                        }

                        FlareConfig.AlertType.TITLE -> {
                            LorenzUtils.sendTitle("§6${flare.displayName} expire in §b${time.inWholeSeconds}", 1.seconds)
                        }

                        FlareConfig.AlertType.CHAT_TITLE -> {
                            ChatUtils.chat("§6${flare.displayName} expire in §b${time.inWholeSeconds}")
                            LorenzUtils.sendTitle("§6${flare.displayName} expire in §b${time.inWholeSeconds}", 1.seconds)
                        }

                        else -> return
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        for ((flare, entity) in flareList) {

            val color = when (flare) {
                FlareType.WARNING -> config.warningColor
                FlareType.ALERT -> config.alertColor
                FlareType.SOS -> config.sosColor
            }.toChromaColor()

            when (config.outlineType) {
                FlareConfig.OutlineType.FILLED -> {
                    event.drawSphereInWorld(color, entity.getLorenzVec(), 40f)
                }

                FlareConfig.OutlineType.WIREFRAME -> {
                    event.drawSphereWireframeInWorld(color, entity.getLorenzVec(), 40f)
                }

                FlareConfig.OutlineType.CIRCLE -> {
                    RenderUtils.drawCircle(entity, event.partialTicks, 40.0, color)
                }

                else -> return
            }
        }
    }

    enum class FlareType(val displayName: String, val buff: String) {
        WARNING("Warning Flare", ""),
        ALERT("Alert Flare", "+50%"),
        SOS("SOS Flare", "+150%")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.overlayEnabled
}
