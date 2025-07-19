package at.hannibal2.skyhanni.features.misc


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.HideArmorConfig
import at.hannibal2.skyhanni.config.features.misc.HideArmorConfig.ModeEntry
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.hasPotionEffect
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack


@SkyHanniModule
object HideArmor {

    val config: HideArmorConfig get() = SkyHanniMod.feature.misc.hideArmor
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
    fun onRenderLivingPre(event: SkyHanniRenderEntityEvent.Pre<EntityPlayer>) {
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
    fun onRenderLivingPost(event: SkyHanniRenderEntityEvent.Post<EntityPlayer>) {
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

    private val CURRENT_RENDERED_ENTITY: ThreadLocal<Entity> = ThreadLocal<Entity>()

    @JvmStatic
    fun setCurrentEntity(entity: Entity) {
        CURRENT_RENDERED_ENTITY.set(entity)
    }

    @JvmStatic
    fun getCurrentEntity(): Entity? {
        return CURRENT_RENDERED_ENTITY.get()
    }

    @JvmStatic
    fun clearCurrentEntity() {
        CURRENT_RENDERED_ENTITY.remove()
    }
}
