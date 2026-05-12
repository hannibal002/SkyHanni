package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
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

    private val TURBOMAX_VACUUM = "TURBOMAX_VACUUM".toInternalName()
    private val VERMIN_FLY_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_FLY") }
    private val VERMIN_SPIDER_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_SPIDER") }

    @HandleEvent
    fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (shouldDiscover()) tryAdd(event.entity)
    }

    @HandleEvent
    fun onEntityMaxHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (shouldDiscover()) tryAdd(event.entity)
    }

    @HandleEvent
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<LivingEntity>) {
        if (shouldDiscover()) tryAdd(event.entity)
    }

    @HandleEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (event.newItem == TURBOMAX_VACUUM) refreshLoadedEntities()
    }

    @HandleEvent
    fun onAreaChange(event: ScoreboardAreaChangeEvent) {
        if (event.area == "West Village" || event.area == "Infested House") refreshLoadedEntities()
    }

    fun tryAdd(entity: LivingEntity) {
        if (!isVermin(entity)) return
        val color = config.color.get().toColor().addAlpha(60)
        RenderLivingEntityHelper.setEntityColor(entity, color) { isEnabled() }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.color) {
            refreshLoadedEntities()
        }
    }

    // This only gets called for explicit one-shot refreshes, so the performance impact is minimal.
    @OptIn(AllEntitiesGetter::class)
    private fun refreshLoadedEntities() {
        if (!shouldDiscover()) return
        EntityUtils.getEntities<LivingEntity>().forEach(::tryAdd)
    }

    private fun isVermin(entity: LivingEntity): Boolean = when (entity) {
        is ArmorStand -> entity.wearingSkullTexture(VERMIN_FLY_TEXTURE) || entity.wearingSkullTexture(VERMIN_SPIDER_TEXTURE)
        is Silverfish -> entity.baseMaxHealth == 8

        else -> false
    }

    private fun hasItemInHand() = InventoryUtils.itemInHandId == TURBOMAX_VACUUM

    private fun shouldDiscover() = RiftApi.inRift() && RiftApi.inWestVillage() && config.enabled

    fun isEnabled() = shouldDiscover() && hasItemInHand()

}
