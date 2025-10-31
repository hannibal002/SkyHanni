package at.hannibal2.hanni.features.rift.area.westvillage

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.collection.TimeLimitedSet
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntitySilverfish
import kotlin.time.Duration.Companion.minutes

@HanniModule
object VerminHighlighter {
    private val config get() = RiftApi.config.area.westVillage.verminHighlight

    private val checkedEntities = TimeLimitedSet<Int>(1.minutes)

    private val VERMIN_FLY_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_FLY") }
    private val VERMIN_SPIDER_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_SPIDER") }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        for (entity in EntityUtils.getEntities<EntityLivingBase>()) {
            val id = entity.entityId
            if (id in checkedEntities) continue
            checkedEntities.add(id)

            if (!isVermin(entity)) continue
            val color = config.color.get().toColor().addAlpha(60)
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, color) { isEnabled() }
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.color) {
            // running setEntityColorWithNoHurtTime() again
            checkedEntities.clear()
        }
    }

    private fun isVermin(entity: EntityLivingBase): Boolean = when (entity) {
        is EntityArmorStand -> entity.wearingSkullTexture(VERMIN_FLY_TEXTURE) || entity.wearingSkullTexture(VERMIN_SPIDER_TEXTURE)
        is EntitySilverfish -> entity.baseMaxHealth == 8

        else -> false
    }

    private fun hasItemInHand() = InventoryUtils.itemInHandId == "TURBOMAX_VACUUM".toInternalName()

    fun isEnabled() = RiftApi.inRift() && RiftApi.inWestVillage() && config.enabled && hasItemInHand()

}
