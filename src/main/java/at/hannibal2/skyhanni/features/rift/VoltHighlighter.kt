package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTUtil
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VoltHighlighter {
    val config get() = SkyHanniMod.feature.rift.crux

    val LIGHTNING_DISTANCE = 7F
    val ARMOR_SLOT_HEAD = 3
    val CHARGE_TIME = 12.seconds
    var dischargingSince = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onArmorChange(event: EntityEquipmentChangeEvent) {
        if (!RiftAPI.inRift() || !config.voltWarning) return
        val player = Minecraft.getMinecraft().thePlayer ?: return
        if (EntityEquipmentChangeEvent.EQUIPMENT_SLOT_HEAD == event.equipmentSlot
            && getVoltState(event.entity) == VoltState.AVoltThatIsCurrentlyAtThisMomentActivelySpawningLightning
            && event.entity.positionVector.squareDistanceTo(player.positionVector) <= LIGHTNING_DISTANCE * LIGHTNING_DISTANCE
        ) {
            dischargingSince = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!RiftAPI.inRift() || !(config.voltRange || config.voltMoodMeter)) return
        val dischargeTimeLeft = CHARGE_TIME - dischargingSince.passedSince()
        if (dischargeTimeLeft > Duration.ZERO) {
            TitleUtils.sendTitle(
                "§eLightning: ${TimeUtils.formatDuration(dischargeTimeLeft, showMilliSeconds = true)}",
                50
            )
        }
        val e = Minecraft.getMinecraft().theWorld?.loadedEntityList ?: return
        for (e in e) {
            if (e !is EntityLivingBase) continue
            val s = getVoltState(e)
            if (s == VoltState.NotAVolt) continue

            if (config.voltMoodMeter)
                RenderLivingEntityHelper.setEntityColor(
                    e, when (s) {
                        VoltState.AVoltThatIsFriendly -> 0x8000FF00.toInt()
                        VoltState.AVoltThatIsCurrentlyAtThisMomentActivelySpawningLightning -> 0x800000FF.toInt()
                        VoltState.AVoltThatIsAggressiveButDoesNotDischargeAtmosphericElectricity -> 0x80FF0000.toInt()
                        else -> 0
                    }
                ) { true }
            if (s == VoltState.AVoltThatIsCurrentlyAtThisMomentActivelySpawningLightning && config.voltRange) {
                RenderUtils.drawCylinderInWorld(
                    Color(SpecialColour.specialToChromaRGB(config.voltColour), true),
                    e.posX,
                    e.posY - 4f,
                    e.posZ,
                    radius = LIGHTNING_DISTANCE,
                    partialTicks = event.partialTicks,
                    height = 20F
                )
            }
        }
    }

    enum class VoltState {
        NotAVolt,
        AVoltThatIsFriendly,
        AVoltThatIsAggressiveButDoesNotDischargeAtmosphericElectricity,
        AVoltThatIsCurrentlyAtThisMomentActivelySpawningLightning, ;
    }

    private fun getVoltState(itemStack: ItemStack): VoltState {
        if (itemStack.item !is ItemSkull) return VoltState.NotAVolt
        val skullOwnerNbt = itemStack.getSubCompound("SkullOwner", false) ?: return VoltState.NotAVolt
        val profile = NBTUtil.readGameProfileFromNBT(skullOwnerNbt) ?: return VoltState.NotAVolt
        val textures = profile.properties["textures"].singleOrNull() ?: return VoltState.NotAVolt
        return when (textures.value) {
            "ewogICJ0aW1lc3RhbXAiIDogMTY3Mzg4MzU3MjAzNSwKICAicHJvZmlsZUlkIiA6ICI0MDU4NDhjMmJjNTE0ZDhkOThkOTJkMGIwYzhiZDQ0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJMb3ZlT3dPIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2UxYjFiZmI1MzZiNjQxNmIyNmEyODNkMmQ4YWQ0YjE3NzFiYmU1Yjc2ODk2ZTI3MjdkNWU4MzNiYzg5NDk4MmQiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==" -> {
                VoltState.AVoltThatIsCurrentlyAtThisMomentActivelySpawningLightning
            }

            "ewogICJ0aW1lc3RhbXAiIDogMTY3MzYzNzQ1OTAwOCwKICAicHJvZmlsZUlkIiA6ICJmMTA0NzMxZjljYTU0NmI0OTkzNjM4NTlkZWY5N2NjNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJ6aWFkODciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2FlYTYyNTFlNThlM2QyMDU1MmEwMzVkNDI0NTYxZWFlZTA4M2ZlYWNkMWU2Y2IzYzJhNWNmOTQ1Y2U2ZDc2ZSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9" -> {
                VoltState.AVoltThatIsFriendly
            }

            "ewogICJ0aW1lc3RhbXAiIDogMTY3Mzg4MzEwNjAwMywKICAicHJvZmlsZUlkIiA6ICI5NTE3OTkxNjljYzE0MGY1OGM2MmRjOGZmZTU3NjBiZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJZdWFyaWciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY5ZWRlOTcwZDQwYzViMjQ1Y2JkNjUxMzQ5ZWUxNjZmNjk1ZDI1MDM0NWY4ZjBlNjNmY2IxMGNmYjVhMmI3OCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9" -> {
                VoltState.AVoltThatIsAggressiveButDoesNotDischargeAtmosphericElectricity
            }

            else -> VoltState.NotAVolt
        }

    }

    private fun getVoltState(entity: net.minecraft.entity.Entity): VoltState {
        if (entity !is EntityArmorStand) return VoltState.NotAVolt
        val helmet = entity.getCurrentArmor(ARMOR_SLOT_HEAD) ?: return VoltState.NotAVolt
        return getVoltState(helmet)
    }
}