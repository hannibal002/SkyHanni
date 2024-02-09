package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets

    // TODO USE SH-REPO
    private val inventorySelectedPetPattern = "§7§7Selected pet: (?<pet>.*)".toPattern()
    private val chatSpawnPattern = "§aYou summoned your §r(?<pet>.*)§r§a!".toPattern()
    private val chatDespawnPattern = "§aYou despawned your §r.*§r§a!".toPattern()
    private val chatPetRulePattern = "§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)! §a§lVIEW RULE".toPattern()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        findPetInChat(event.message)?.let {
            PetAPI.currentPet = it
            if (config.hideAutopet) {
                event.blockedReason = "pets"
            }
        }
    }

    private fun findPetInChat(message: String): String? {
        chatSpawnPattern.matchMatcher(message) {
            return group("pet")
        }
        if (chatDespawnPattern.matches(message)) {
            return ""
        }
        chatPetRulePattern.matchMatcher(message) {
            return group("pet")
        }

        return null
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!PetAPI.isPetMenu(event.inventoryName)) return

        val lore = event.inventoryItems[4]?.getLore() ?: return
        for (line in lore) {
            inventorySelectedPetPattern.matchMatcher(line) {
                val newPet = group("pet")
                PetAPI.currentPet = if (newPet != "§cNone") newPet else ""
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (RiftAPI.inRift()) return

        if (!config.display) return

        config.displayPos.renderString(PetAPI.currentPet, posLabel = "Current Pet")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
    }
}
