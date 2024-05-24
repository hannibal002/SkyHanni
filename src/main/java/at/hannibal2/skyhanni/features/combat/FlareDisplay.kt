package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.combat.FlareConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FlareDisplay {

    private val config get() = SkyHanniMod.feature.combat.flare
    private var display = emptyList<Renderable>()
    private var flareList = mutableListOf<Flare>()

    class Flare(val type: FlareType, val entity: EntityArmorStand, val location: LorenzVec = entity.getLorenzVec())

    private val MAX_FLARE_TIME = 3.minutes

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
        flareList.removeIf { !it.entity.isEntityAlive }
        for (entity in EntityUtils.getAllEntities().filterIsInstance<EntityArmorStand>()) {
            if (entity.ticksExisted.ticks > MAX_FLARE_TIME) continue
            if (isAlreadyKnownFlare(entity)) continue
            getFlareTypeForTexuture(entity)?.let {
                flareList.add(Flare(it, entity))
            }
        }
        var newDisplay: List<Renderable>? = null
        for (type in FlareType.entries) {
            val flare = getFlareForType(type) ?: continue
            val entity = flare.entity
            val aliveTime = entity.ticksExisted.ticks
            val remainingTime = (MAX_FLARE_TIME - aliveTime)

            val name = type.displayName
            if (newDisplay == null) {
                newDisplay = buildList {
                    add(Renderable.string("$name: §b${remainingTime.format()}"))
                    if (config.showManaBuff) {
                        type.manaBuff?.let {
                            add(Renderable.string(" §b$it §7mana regen"))
                        }
                    }
                }
            }
            if (remainingTime > 5.seconds) continue
            val message = "$name §eexpires in: §b${remainingTime.inWholeSeconds}s"
            when (config.alertType) {
                FlareConfig.AlertType.CHAT -> {
                    ChatUtils.chat(message)
                }

                FlareConfig.AlertType.TITLE -> {
                    LorenzUtils.sendTitle(message, 1.seconds)
                }

                FlareConfig.AlertType.CHAT_TITLE -> {
                    ChatUtils.chat(message)
                    LorenzUtils.sendTitle(message, 1.seconds)
                }

                else -> {}
            }
        }
        display = newDisplay ?: emptyList()
    }

    private fun getFlareForType(type: FlareType): Flare? = flareList.firstOrNull { it.type == type }

    private fun getFlareTypeForTexuture(entity: EntityArmorStand): FlareType? =
        flares.entries.firstOrNull { entity.hasSkullTexture(it.key) }?.value

    private fun isAlreadyKnownFlare(entity: EntityArmorStand): Boolean =
        flareList.any { it.entity.entityId == entity.entityId }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        flareList.clear()
        display = emptyList()
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (config.outlineType == FlareConfig.OutlineType.NONE) return

        for (flare in flareList) {
            val entity = flare.entity
            val location = flare.location

            val color = when (flare.type) {
                FlareType.WARNING -> config.warningColor
                FlareType.ALERT -> config.alertColor
                FlareType.SOS -> config.sosColor
            }.toChromaColor()

            when (config.outlineType) {
                FlareConfig.OutlineType.FILLED -> {
                    event.drawSphereInWorld(color, location, 40f)
                }

                FlareConfig.OutlineType.WIREFRAME -> {
                    event.drawSphereWireframeInWorld(color, location, 40f)
                }

                FlareConfig.OutlineType.CIRCLE -> {
                    RenderUtils.drawCircle(entity, event.partialTicks, 40.0, color)
                }

                else -> {}
            }
        }
    }

    enum class FlareType(val displayName: String, val manaBuff: String?) {
        SOS("§5SOS Flare", "+125%"),
        ALERT("§9Alert Flare", "+50%"),
        WARNING("§aWarning Flare", null),
        ;
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
