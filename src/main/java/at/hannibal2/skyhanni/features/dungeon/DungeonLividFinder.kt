package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object DungeonLividFinder {
    var livid: EntityOtherPlayerMP? = null
    private var gotBlinded = false
    private val blockLocation = LorenzVec(6, 109, 43)
    private val lividColor = Color(191, 0, 255).rgb

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inDungeons || !DungeonAPI.inBossRoom) return
        if (DungeonAPI.dungeonFloor != "F5" && DungeonAPI.dungeonFloor != "M5") return
        if (DungeonAPI.dungeonFloor == "F5" && livid != null) return
        if (!event.repeatSeconds(2)) return
        if (!gotBlinded) {
            gotBlinded = Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.blindness)
            return
        } else if (Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.blindness)) return

        val dyeColor = blockLocation.getBlockStateAt().getValue(BlockStainedGlass.COLOR)
        val chatColor = dyeColor.toLorenzColor()?.getChatColor() ?: return

        val lividEntity = EntityUtils.getEntities<EntityArmorStand>()
            .firstOrNull { it.name.startsWith("${chatColor}﴾ ${chatColor}§lLivid") } ?: return

        val aabb = with(lividEntity) {
            AxisAlignedBB(
                posX - 0.5,
                posY - 2,
                posZ - 0.5,
                posX + 0.5,
                posY,
                posZ + 0.5
            )
        }
        val world = Minecraft.getMinecraft().theWorld
        livid = world.getEntitiesWithinAABB(EntityOtherPlayerMP::class.java, aabb)
            .takeIf { it.size == 1 }?.firstOrNull() ?: return
        livid?.let {
            RenderLivingEntityHelper.setEntityColor(it, lividColor) { true }
            LorenzUtils.debug("Livid found!")
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        livid = null
        gotBlinded = false
    }
}