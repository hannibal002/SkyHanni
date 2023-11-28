package at.hannibal2.skyhanni.features.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SkyhanniPlayersWhenTheySeeUngiftedPlayers {
    val playerList: MutableSet<String>?
        get() = ProfileStorageData.playerSpecific?.playersThatHaveBeenGifted

    val pattern = "§6\\+1 Unique Gift given! To ([^§]+)§r§6!".toPattern()
    fun hasGiftedPlayer(player: EntityPlayer): Boolean =
        playerList?.contains(player.name) == true

    fun addGiftedPlayer(playerName: String): Unit {
        playerList?.add(playerName)
    }

    val config get() = SkyHanniMod.feature.event.winter.giftingOpportunities

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.highlightSpecialNeedsPlayers &&
            (Minecraft.getMinecraft().thePlayer?.heldItem?.getInternalName()?.endsWith("_GIFT") == true
                || (!config.highlighWithGiftOnly))
    }

    val hasNotGiftedNametag = "§a§lꤥ"
    val hasGiftedNametag = "§c§lꤥ"

    fun analyzeArmorStand(entity: EntityArmorStand) {
        if (!config.useArmorStandDetection) return
        val world = Minecraft.getMinecraft().theWorld ?: return
        if (entity.name != hasGiftedNametag) return
        val matchedPlayer =
            world.playerEntities.singleOrNull {
                !it.isNPC() && it.getLorenzVec().distance(entity.getLorenzVec()) < 2
            } ?: return
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
        if (entity is EntityPlayer && !entity.isNPC() && !hasGiftedPlayer(entity))
            event.color = LorenzColor.DARK_GREEN.toColor().withAlpha(127)
    }


    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        pattern.matchMatcher(event.message) {
            addGiftedPlayer(group(1))
        }
    }

}
