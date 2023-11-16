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

    // TODO USE SH-REPO
    private val inventoryNamePattern = "(?:\\(\\d+/\\d+\\))? Pets".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val message = event.message
        val config = ProfileStorageData.profileSpecific ?: return
        var blocked = false
        if (message.matchRegex("§cAutopet §eequipped your §7(.*)§e! §a§lVIEW RULE")) {
            config.currentPet = message.between("] ", "§e!")
            blocked = true
        }

        if (!LorenzUtils.inSkyBlock) return

        if (message.matchRegex("§aYou summoned your §r(.*)§r§a!")) {
            config.currentPet = message.between("your §r", "§r§a")
            blocked = true
        }
        if (message.matchRegex("§aYou despawned your §r(.*)§r§a!")) {
            config.currentPet = ""
            blocked = true
        }

        if (blocked && SkyHanniMod.feature.misc.pets.display) {
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

        if (!SkyHanniMod.feature.misc.pets.display) return
        val storage = ProfileStorageData.profileSpecific ?: return

        SkyHanniMod.feature.misc.pets.displayPos.renderString(storage.currentPet, posLabel = "Current Pet")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
    }
}
