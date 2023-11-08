package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FishingAPI {
    private val lavaBlocks = listOf(Blocks.lava, Blocks.flowing_lava)
    private val waterBlocks = listOf(Blocks.water, Blocks.flowing_water)

    var lastCastTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!LorenzUtils.inSkyBlock || !hasFishingRodInHand()) return
        val entity = event.entity ?: return
        if (entity !is EntityFishHook) return
        if (entity.angler != Minecraft.getMinecraft().thePlayer) return

        lastCastTime = SimpleTimeMark.now()
        FishingBobberCastEvent(entity).postAndCatch()
    }

    fun hasFishingRodInHand() = InventoryUtils.itemInHandId.asString().contains("ROD")

    fun ItemStack.isBait(): Boolean {
        val name = name ?: return false
        return stackSize == 1 && (name.removeColor().startsWith("Obfuscated") || name.endsWith(" Bait"))
    }

    fun isLavaRod() = InventoryUtils.getItemInHand()?.getLore()?.any { it.contains("Lava Rod") } ?: false

    fun getAllowedBlocks() = if (isLavaRod()) lavaBlocks else waterBlocks

}
