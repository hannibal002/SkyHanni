package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.misc.HideArmorConfig
import at.hannibal2.hanni.config.features.misc.HideArmorConfig.ModeEntry
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.hanni.utils.EntityUtils.isNpc
import at.hannibal2.hanni.utils.FakePlayer
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.EffectsCompat
import at.hannibal2.hanni.utils.compat.EffectsCompat.Companion.hasPotionEffect
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

@HanniModule
object HideArmor {

    val config: HideArmorConfig get() = HanniMod.feature.misc.hideArmor
    private var armor = mapOf<Int, ItemStack>()

    fun shouldHideArmor(entity: EntityPlayer): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (entity is FakePlayer) return false
        if (entity.hasPotionEffect(EffectsCompat.INVISIBILITY)) return false
        if (entity.isNpc()) return false

        return when (config.mode) {
            ModeEntry.ALL -> true

            ModeEntry.OWN -> entity is EntityPlayerSP
            ModeEntry.OTHERS -> entity !is EntityPlayerSP

            else -> false
        }
    }

    @HandleEvent
    fun onRenderLivingPre(event: HanniRenderEntityEvent.Pre<EntityPlayer>) {
        val entity = event.entity
        if (!shouldHideArmor(entity)) return
        val armorInventory = entity.getArmorInventory() ?: return

        armor = buildMap {
            for ((i, stack) in armorInventory.withIndex()) {
                stack?.let {
                    if (!config.onlyHelmet || i == 3) {
                        this[i] = it.copy()
                        armorInventory[i] = null
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onRenderLivingPost(event: HanniRenderEntityEvent.Post<EntityPlayer>) {
        val entity = event.entity
        if (!shouldHideArmor(entity)) return
        val armorInventory = entity.getArmorInventory() ?: return

        for ((index, stack) in armor) {
            armorInventory[index] = stack
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(91, "misc.hideArmor2", "misc.hideArmor")

    }
}
