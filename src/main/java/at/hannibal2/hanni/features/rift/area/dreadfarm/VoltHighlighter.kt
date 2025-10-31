package at.hannibal2.hanni.features.rift.area.dreadfarm

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils.getEntities
import at.hannibal2.hanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.CollectionUtils.editCopy
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.compat.getStandHelmet
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawCylinderInWorld
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.exactLocation
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HanniModule
object VoltHighlighter {

    private val config get() = RiftApi.config.area.dreadfarm.voltCrux

    private val VOLT_DOING_LIGHTNING by lazy { SkullTextureHolder.getTexture("VOLT_DOING_LIGHTNING") }
    private val VOLT_FRIENDLY by lazy { SkullTextureHolder.getTexture("VOLT_FRIENDLY") }
    private val VOLT_HOSTILE by lazy { SkullTextureHolder.getTexture("VOLT_HOSTILE") }

    private const val LIGHTNING_DISTANCE = 7F
    private val CHARGE_TIME = 12.seconds
    private var chargingSince = mapOf<Entity, SimpleTimeMark>()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onArmorChange(event: EntityEquipmentChangeEvent<Entity>) {
        if (!config.voltWarning) return
        val player = MinecraftCompat.localPlayerOrNull ?: return
        if (event.isHead && getVoltState(event.entity) == VoltState.DOING_LIGHTNING &&
            event.entity.positionVector.squareDistanceTo(player.positionVector) <= LIGHTNING_DISTANCE * LIGHTNING_DISTANCE
        ) {
            chargingSince = chargingSince.editCopy {
                this[event.entity] = SimpleTimeMark.now()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!(config.voltRange || config.voltMoodMeter)) return
        for (entity in getEntities<EntityLivingBase>()) {
            val state = getVoltState(entity).takeIf { it != VoltState.NO_VOLT } ?: continue

            if (config.voltMoodMeter) RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                entity,
                state.color.toColor()
            ) { config.voltMoodMeter }

            if (state == VoltState.DOING_LIGHTNING && config.voltRange) {
                event.drawCylinderInWorld(
                    config.voltColor.toColor(),
                    entity.posX,
                    entity.posY - 4f,
                    entity.posZ,
                    radius = LIGHTNING_DISTANCE,
                    height = 20F,
                )
                val dischargingSince = chargingSince.getOrDefault(entity, SimpleTimeMark.farPast())
                val dischargeTimeLeft = CHARGE_TIME - dischargingSince.passedSince()
                if (dischargeTimeLeft > Duration.ZERO) {
                    event.drawDynamicText(
                        event.exactLocation(entity).up(2.5),
                        "§eLightning: ${dischargeTimeLeft.format(showMilliSeconds = true)}",
                        2.5,
                    )
                }
            }
        }
    }

    enum class VoltState(val color: ChromaColour) {
        NO_VOLT(ChromaColour.fromStaticRGB(0, 0, 0, 0)),
        FRIENDLY(ChromaColour.fromStaticRGB(0, 255, 0, 128)),
        HOSTILE(ChromaColour.fromStaticRGB(255, 0, 0, 128)),
        DOING_LIGHTNING(ChromaColour.fromStaticRGB(0, 0, 255, 128)),
    }

    private fun getVoltState(itemStack: ItemStack): VoltState {
        return when (itemStack.getSkullTexture()) {
            VOLT_DOING_LIGHTNING -> VoltState.DOING_LIGHTNING
            VOLT_FRIENDLY -> VoltState.FRIENDLY
            VOLT_HOSTILE -> VoltState.HOSTILE
            else -> VoltState.NO_VOLT
        }
    }

    private fun getVoltState(entity: Entity): VoltState {
        if (entity !is EntityArmorStand) return VoltState.NO_VOLT
        val helmet = entity.getStandHelmet() ?: return VoltState.NO_VOLT
        return getVoltState(helmet)
    }

    @HandleEvent
    @Suppress("AvoidBritishSpelling")
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(82, "rift.area.dreadfarm.voltCrux.voltColour", "rift.area.dreadfarm.voltCrux.voltColor")
    }
}
