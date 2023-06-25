package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object DungeonLividFinder {
    var livid : EntityOtherPlayerMP? = null
    private var gotBlinded = false

    @SubscribeEvent
    fun onTick(tickEvent: LorenzTickEvent) {
        if(!LorenzUtils.inDungeons || !DungeonData.inBossRoom) return
        if(DungeonData.dungeonFloor != "F5" && DungeonData.dungeonFloor != "M5") return
        if(DungeonData.dungeonFloor == "F5" && livid != null) return
        if(tickEvent.isMod(100)) return
        if(!gotBlinded) {
            gotBlinded = Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.blindness)
            return
        } else if (Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.blindness)) return


        val blockStateAt = LorenzVec(6, 109, 43).getBlockStateAt()
        val color = LorenzColor.getMatchingColor(blockStateAt.getValue(BlockStainedGlass.COLOR)) ?: return

        for(entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if(entity is EntityArmorStand && entity.hasCustomName()) {
                if(entity.name.startsWith("${color.getChatColor()}﴾ ${color.getChatColor()}§lLivid")) {
                    val aabb =  AxisAlignedBB(
                        entity.posX - 0.5,
                        entity.posY - 2,
                        entity.posZ - 0.5,
                        entity.posX + 0.5,
                        entity.posY,
                        entity.posZ + 0.5
                    )
                    val entities = Minecraft.getMinecraft().theWorld.getEntitiesWithinAABB(EntityOtherPlayerMP::class.java, aabb)
                    livid = entities.takeIf { it.size == 1 }?.firstOrNull() ?: return
                    livid?.let {
                        RenderLivingEntityHelper.setEntityColor(it, Color(0xBF00FF).rgb) { true }
                        LorenzUtils.debug("Livid found!")
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        livid = null
        gotBlinded = false
    }
}