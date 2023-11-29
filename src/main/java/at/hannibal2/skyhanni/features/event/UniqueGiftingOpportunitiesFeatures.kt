package at.hannibal2.skyhanni.features.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.event.winter.UniqueGiftCounter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object UniqueGiftingOpportunitiesFeatures {
    private val playerList: MutableSet<String>?
        get() = ProfileStorageData.playerSpecific?.winter?.playersThatHaveBeenGifted

    private val pattern = "§6\\+1 Unique Gift given! To ([^§]+)§r§6!".toPattern()

    private fun hasGiftedPlayer(player: EntityPlayer) = playerList?.contains(player.name) == true

    private var excludedPlayer = setOf<String>()
    private fun excludedPlayer(player: EntityPlayer) = excludedPlayer.contains(player.name)

    private fun addGiftedPlayer(playerName: String) {
        playerList?.add(playerName)
    }

    private val config get() = SkyHanniMod.feature.event.winter.giftingOpportunities

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled &&
        (InventoryUtils.itemInHandId.endsWith("_GIFT")
            || !config.highlighWithGiftOnly)

    private val hasNotGiftedNametag = "§a§lꤥ"
    private val hasGiftedNametag = "§c§lꤥ"

    private fun analyzeArmorStand(entity: EntityArmorStand) {
        if (!config.useArmorStandDetection) return
        if (entity.name != hasGiftedNametag) return

        val matchedPlayer = EntityUtils.getEntitiesNearby<EntityPlayer>(entity.getLorenzVec(), 2.0)
            .singleOrNull { !it.isNPC() } ?: return
        addGiftedPlayer(matchedPlayer.name)

    }

    @SubscribeEvent
    fun onEntityChangeName(event: EntityCustomNameUpdateEvent) {
        val entity = event.entity as? EntityArmorStand ?: return
        analyzeArmorStand(entity)
    }

    @SubscribeEvent
    fun onEntityJoinWorldEvent(event: EntityJoinWorldEvent) {
        val entity = event.entity as? EntityArmorStand ?: return
        analyzeArmorStand(entity)
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity is EntityPlayerSP) return
        if (entity is EntityPlayer && !entity.isNPC() && !excludedPlayer(entity) && !hasGiftedPlayer(entity))
            event.color = LorenzColor.DARK_GREEN.toColor().withAlpha(127)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        pattern.matchMatcher(event.message) {
            addGiftedPlayer(group(1))
            UniqueGiftCounter.addUniqueGift()
        }
    }

    private val playerNameTabListRegex = "\\].{5}(\\S+)".toRegex()

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        // TODO move this somewhere else
        if (HypixelData.bingo || HypixelData.stranded) return
        val playerList = event.tabList.takeWhile { it != "§r§3§lServer Info" }.filterNot { it == " " || it.startsWith("§r§a§lPlayers") }
        val bingoList = playerList.filter { it.endsWith("Ⓑ") }
        if (HypixelData.ironman) {
            excludedPlayer = bingoList.mapNotNull { playerNameTabListRegex.find(it)?.groupValues?.get(1) }.toSet()
            return
        }
        val ironmanList = playerList.filter { it.endsWith("♲") }
        excludedPlayer = (bingoList + ironmanList).mapNotNull { playerNameTabListRegex.find(it)?.groupValues?.get(1) }.toSet()
    }

}
