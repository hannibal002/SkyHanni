package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Silverfish

@SkyHanniModule
object VerminHighlighter {
    private val config get() = RiftApi.config.area.westVillage.verminHighlight

    private val VERMIN_FLY_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_FLY") }
    private val VERMIN_SPIDER_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_SPIDER") }

    @HandleEvent
    fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (isEnabled()) tryAdd(event.entity)
    }

    @HandleEvent
    fun onEntityMaxHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (isEnabled()) tryAdd(event.entity)
    }

    fun tryAdd(entity: LivingEntity) {
        if (!isVermin(entity)) return
        val color = config.color.get().toColor().addAlpha(60)
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, color) { isEnabled() }
    }

    // This only gets called on config change, so the performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.color) {
            EntityUtils.getEntities<LivingEntity>().forEach(::tryAdd)
        }
    }

    private fun isVermin(entity: LivingEntity): Boolean = when (entity) {
        is ArmorStand -> entity.wearingSkullTexture(VERMIN_FLY_TEXTURE) || entity.wearingSkullTexture(VERMIN_SPIDER_TEXTURE)
        is Silverfish -> entity.baseMaxHealth == 8

        else -> false
    }

    private fun hasItemInHand() = InventoryUtils.itemInHandId == "TURBOMAX_VACUUM".toInternalName()

    fun isEnabled() = RiftApi.inRift() && RiftApi.inWestVillage() && config.enabled && hasItemInHand()

}
