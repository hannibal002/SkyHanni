package at.hannibal2.hanni.features.gifting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.WinterApi
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.hanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.isNpc
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer

@HanniModule
object UniqueGiftingOpportunitiesFeatures {

    private val playerList: MutableSet<String>?
        get() = ProfileStorageData.playerSpecific?.winter?.playersThatHaveBeenGifted

    private val patternGroup = RepoPattern.group("event.winter.uniquegifts")

    /**
     * REGEX-TEST: §6+1 Unique Gift given! To oBlazin§r§6!
     */
    private val giftedPattern by patternGroup.pattern(
        "gifted",
        "§6\\+1 Unique Gift given! To (?<player>[^§]+)§r§6!",
    )

    private fun hasGiftedPlayer(player: EntityPlayer) = playerList?.contains(player.name) == true

    private fun addGiftedPlayer(playerName: String) {
        playerList?.add(playerName)
    }

    private val config get() = HanniMod.feature.event.gifting.giftingOpportunities

    private fun isHoldingGift() = !config.highlighWithGiftOnly || GiftApi.isHoldingGift()
    private fun isEnabled() = isHoldingGift() && config.enabled && WinterApi.isDecember()

    @Suppress("UnusedPrivateProperty")
    private const val HAS_NOT_GIFTED_NAMETAG = "§a§lꤥ"
    private const val HAS_GIFTED_NAMETAG = "§c§lꤥ"

    private fun analyzeArmorStand(entity: EntityArmorStand) {
        if (!config.useArmorStandDetection) return
        if (entity.name != HAS_GIFTED_NAMETAG) return

        val matchedPlayer = EntityUtils.getEntitiesNearby<EntityPlayer>(entity.getLorenzVec(), 2.0)
            .singleOrNull { !it.isNpc() } ?: return
        addGiftedPlayer(matchedPlayer.name)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityChangeName(event: EntityCustomNameUpdateEvent<EntityArmorStand>) {
        analyzeArmorStand(event.entity)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityJoinWorld(event: EntityEnterWorldEvent<Entity>) {
        playerColor(event)
        val entity = event.entity as? EntityArmorStand ?: return
        analyzeArmorStand(entity)
    }

    private fun playerColor(event: EntityEnterWorldEvent<Entity>) {
        if (event.entity is EntityOtherPlayerMP) {
            val entity = event.entity
            if (entity.isNpc() || isIronman(entity) || isBingo(entity)) return

            RenderLivingEntityHelper.setEntityColor(
                entity,
                LorenzColor.DARK_GREEN.toColor().addAlpha(127),
            ) { isEnabled() && !hasGiftedPlayer(entity) }
        }
    }

    private fun isBingo(entity: EntityLivingBase) =
        !SkyBlockUtils.isBingoProfile && entity.displayName.formattedText.endsWith("Ⓑ§r")

    private fun isIronman(entity: EntityLivingBase) =
        !SkyBlockUtils.noTradeMode && entity.displayName.formattedText.endsWith("♲§r")

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        giftedPattern.matchMatcher(event.message) {
            addGiftedPlayer(group("player"))
            UniqueGiftCounter.addUniqueGift()
        }
    }
}
