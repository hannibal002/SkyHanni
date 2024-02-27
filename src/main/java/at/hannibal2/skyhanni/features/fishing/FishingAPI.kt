package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.FishingBobberInWaterEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getFilletValue
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FishingAPI {

    val lavaBlocks = listOf(Blocks.lava, Blocks.flowing_lava)
    private val waterBlocks = listOf(Blocks.water, Blocks.flowing_water)

    var lastCastTime = SimpleTimeMark.farPast()
    var holdingRod = false
    var holdingLavaRod = false
    var holdingWaterRod = false

    private var lavaRods = listOf<NEUInternalName>()
    private var waterRods = listOf<NEUInternalName>()

    var bobber: EntityFishHook? = null
    var bobberHasTouchedWater = false

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!LorenzUtils.inSkyBlock || !holdingRod) return
        val entity = event.entity ?: return
        if (entity !is EntityFishHook) return
        if (entity.angler != Minecraft.getMinecraft().thePlayer) return

        lastCastTime = SimpleTimeMark.now()
        bobber = entity
        bobberHasTouchedWater = false
        FishingBobberCastEvent(entity).postAndCatch()
    }

    private fun resetBobber() {
        bobber = null
        bobberHasTouchedWater = false
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        resetBobber()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val bobber = bobber ?: return
        if (bobber.isDead) {
            resetBobber()
        } else {
            if (!bobberHasTouchedWater) {
                val block = bobber.getLorenzVec().getBlockAt()
                if (block in getAllowedBlocks()) {
                    bobberHasTouchedWater = true
                    FishingBobberInWaterEvent().postAndCatch()
                }
            }
        }
    }

    private fun NEUInternalName.isFishingRod() = contains("ROD")

    fun ItemStack.isBait(): Boolean {
        val name = name ?: return false
        return stackSize == 1 && (name.removeColor().startsWith("Obfuscated") || name.endsWith(" Bait"))
    }

    @SubscribeEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        // TODO correct rod type per island water/lava
        holdingRod = event.newItem.isFishingRod()
        holdingLavaRod = event.newItem in lavaRods
        holdingWaterRod = event.newItem in waterRods
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        lavaRods = data.lava_fishing_rods ?: error("§clava_fishing_rods is missing from repo.")
        waterRods = data.water_fishing_rods ?: error("§cwater_fishing_rods is missing from repo.")
    }

    fun isLavaRod() = InventoryUtils.getItemInHand()?.getLore()?.any { it.contains("Lava Rod") } ?: false

    private fun getAllowedBlocks() = if (isLavaRod()) lavaBlocks else waterBlocks

    fun getFilletPerTrophy(internalName: NEUInternalName): Int {
        val internal = internalName.asString()
        val trophyFishName = internal.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internal.substringAfterLast("_")
        val info = TrophyFishManager.getInfo(trophyFishName)
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: TrophyRarity.BRONZE
        return info?.getFilletValue(rarity) ?: 0
    }

    fun isFishing(checkRodInHand: Boolean = true) = IsFishingDetection.isFishing || (checkRodInHand && holdingRod)

    fun seaCreatureCount(entity: EntityArmorStand): Int {
        val name = entity.name
        // a dragon, will always be fought
        if (name == "Reindrake") return 0

        // a npc shop
        if (name == "§5Frosty the Snow Blaster") return 0

        if (name == "Frosty") {
            val npcLocation = LorenzVec(-1.5, 76.0, 92.5)
            if (entity.getLorenzVec().distance(npcLocation) < 1) {
                return 0
            }
        }

        val isSummonedSoul = name.contains("'")
        val hasFishingMobName = SeaCreatureManager.allFishingMobs.keys.any { name.contains(it) }
        if (!hasFishingMobName || isSummonedSoul) return 0

        if (name == "Sea Emperor" || name == "Rider of the Deep") {
            return 2
        }
        return 1
    }
}
