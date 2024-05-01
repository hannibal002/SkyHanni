package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object HoppityEggPlayerOpacityChanger {
    private val config get() = HoppityEggsManager.config
    private var armor = mapOf<Int, ItemStack>()

    private fun hideNearbyPlayer(entity: EntityPlayer, location: LorenzVec) {
        if (entity.distanceTo(location) < 4.0) {
            GlStateManager.enableBlend()
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.color(1.0f, 1.0f, 1.0f, config.playerOpacity / 100f)
            val armorInventory = entity.getArmorInventory() ?: return

            armor = buildMap {
                for ((i, stack) in armorInventory.withIndex()) {
                    stack?.let {
                        this[i] = it.copy()
                        armorInventory[i] = null
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPreRenderPlayer(event: SkyHanniRenderEntityEvent.Pre<EntityLivingBase>) {
        if (!isEnabled()) return
        if (event.entity !is EntityPlayer) return
        if (event.entity.name == LorenzUtils.getPlayerName()) return
        HoppityEggLocator.sharedEggLocation?.let { hideNearbyPlayer(event.entity, it) }
        HoppityEggLocator.possibleEggLocations.forEach { hideNearbyPlayer(event.entity, it) }
    }

    @SubscribeEvent
    fun onPostRenderPlayer(event: SkyHanniRenderEntityEvent.Post<EntityLivingBase>) {
        if (!isEnabled()) return
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.disableBlend()
        val armorInventory = event.entity.getArmorInventory() ?: return

        for ((index, stack) in armor) { // restore armor after players leave the area
            armorInventory[index] = stack
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.waypoints
        && ChocolateFactoryAPI.isHoppityEvent() && config.playerOpacity != 100
}
