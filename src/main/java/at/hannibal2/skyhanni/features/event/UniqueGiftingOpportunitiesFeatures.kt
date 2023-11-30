package at.hannibal2.skyhanni.features.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
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
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object UniqueGiftingOpportunitiesFeatures {
    private val playerList: MutableSet<String>?
        get() = ProfileStorageData.playerSpecific?.winter?.playersThatHaveBeenGifted

    private val pattern = "§6\\+1 Unique Gift given! To ([^§]+)§r§6!".toPattern()

    private fun hasGiftedPlayer(player: EntityPlayer) = playerList?.contains(player.name) == true

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
        if (entity is EntityPlayer && !entity.isNPC() && !isIronman(entity) && !isBingo(entity) && !hasGiftedPlayer(entity))
            event.color = LorenzColor.DARK_GREEN.toColor().withAlpha(127)
    }

    private fun isBingo(entity: EntityLivingBase) =
        !HypixelData.bingo && entity.displayName.formattedText.endsWith("Ⓑ§r")

    private fun isIronman(entity: EntityLivingBase) =
        !LorenzUtils.noTradeMode && entity.displayName.formattedText.endsWith("♲§r")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        pattern.matchMatcher(event.message) {
            addGiftedPlayer(group(1))
            UniqueGiftCounter.addUniqueGift()
        }
    }
}
