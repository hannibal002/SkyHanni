package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CurrentPetDisplay {
    private val config get() = SkyHanniMod.feature.misc.pets

    // TODO USE SH-REPO
    private val inventoryNamePattern = "(?:\\(\\d+/\\d+\\))? Pets".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val message = event.message
        val storage = ProfileStorageData.profileSpecific ?: return

        var blocked = false
        if (message.matchRegex("§cAutopet §eequipped your §7(.*)§e! §a§lVIEW RULE")) {
            storage.currentPet = message.between("] ", "§e!")
            blocked = true
        }

        if (!LorenzUtils.inSkyBlock) return

        if (message.matchRegex("§aYou summoned your §r(.*)§r§a!")) {
            storage.currentPet = message.between("your §r", "§r§a")
            blocked = true
        }
        if (message.matchRegex("§aYou despawned your §r(.*)§r§a!")) {
            storage.currentPet = ""
            blocked = true
        }

        if (blocked && config.display && config.hideAutopet) {
            event.blockedReason = "pets"
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val storage = ProfileStorageData.profileSpecific ?: return
        if (!inventoryNamePattern.matcher(event.inventoryName).matches()) return

        val lore = event.inventoryItems[4]?.getLore() ?: return
        val selectedPetPattern = "§7§7Selected pet: (?<pet>.*)".toPattern()
        for (line in lore) {
            selectedPetPattern.matchMatcher(line) {
                val newPet = group("pet")
                storage.currentPet = if (newPet != "§cNone") newPet else ""
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (RiftAPI.inRift()) return

        if (!config.display) return
        val storage = ProfileStorageData.profileSpecific ?: return

        config.petDisplayPos.renderString(storage.currentPet, posLabel = "Current Pet")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(7, "misc.petDisplayPos", "misc.pets.petDisplayPos")
    }
}
