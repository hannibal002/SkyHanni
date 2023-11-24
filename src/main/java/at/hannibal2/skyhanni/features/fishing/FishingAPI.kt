package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getFilletValue
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object FishingAPI {
    private val lavaBlocks = listOf(Blocks.lava, Blocks.flowing_lava)
    private val waterBlocks = listOf(Blocks.water, Blocks.flowing_water)

    var lastCastTime = SimpleTimeMark.farPast()
    var lastActiveFishingTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!LorenzUtils.inSkyBlock || !hasFishingRodInHand()) return
        val entity = event.entity ?: return
        if (entity !is EntityFishHook) return
        if (entity.angler != Minecraft.getMinecraft().thePlayer) return

        lastCastTime = SimpleTimeMark.now()
        FishingBobberCastEvent(entity).postAndCatch()
    }

    @SubscribeEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (event.oldItem.isFishingRod()) {
            lastActiveFishingTime = SimpleTimeMark.now()
        }
        if (event.newItem.isFishingRod()) {
            DelayedRun.runDelayed(1.seconds) {
                lastActiveFishingTime = SimpleTimeMark.now()
            }
        }
    }

    @SubscribeEvent
    fun onSkillExpGain(event: SkillExpGainEvent) {
        val skill = event.skill
        if (FishingProfitTracker.isEnabled()) {
            if (skill != "fishing") {
                lastActiveFishingTime = SimpleTimeMark.farPast()
            }
        }
    }

    fun hasFishingRodInHand() = InventoryUtils.itemInHandId.isFishingRod()

    fun NEUInternalName.isFishingRod() = contains("ROD")

    fun ItemStack.isBait(): Boolean {
        val name = name ?: return false
        return stackSize == 1 && (name.removeColor().startsWith("Obfuscated") || name.endsWith(" Bait"))
    }

    fun isLavaRod() = InventoryUtils.getItemInHand()?.getLore()?.any { it.contains("Lava Rod") } ?: false

    fun getAllowedBlocks() = if (isLavaRod()) lavaBlocks else waterBlocks

    fun getFilletPerTrophy(internalName: NEUInternalName): Int {
        val internal = internalName.asString()
        val trophyFishName = internal.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internal.substringAfterLast("_")
        val info = TrophyFishManager.getInfo(trophyFishName)
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: TrophyRarity.BRONZE
        return info?.getFilletValue(rarity) ?: 0
    }

}
