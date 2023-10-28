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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CurrentPetDisplay {

    // TODO USE SH-REPO
    private val inventoryNamePattern = "(?:\\(\\d+/\\d+\\))? Pets".toPattern()
    private val autoPetEquippedPattern = "§cAutopet §eequipped your §7(.*)§e! §a§lVIEW RULE".toPattern()
    private val petSummonedPattern = "§aYou summoned your §r(.*)§r§a!".toPattern()
    private val petDespawnedPattern = "§aYou despawned your §r(.*)§r§a!".toPattern()
    private val selectedPetPattern = "§7§7Selected pet: (?<pet>.*)".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val config = ProfileStorageData.profileSpecific ?: return
        val message = event.message
        var blocked = false

        autoPetEquippedPattern.matchMatcher(message) {
            config.currentPet = message.between("] ", "§e!")
            blocked = true
        }

        petSummonedPattern.matchMatcher(message) {
            config.currentPet = message.between("your §r", "§r§a")
            blocked = true
        }

        petDespawnedPattern.matchMatcher(message) {
            config.currentPet = ""
            blocked = true
        }

        if (blocked && SkyHanniMod.feature.misc.pets.display) {
            event.blockedReason = "pets"
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        if (!inventoryNamePattern.matcher(event.inventoryName).matches()) return

        val lore = event.inventoryItems[4]?.getLore() ?: return
        for (line in lore) {
            selectedPetPattern.matchMatcher(line) {
                val newPet = group("pet")
                config.currentPet = if (newPet != "§cNone") newPet else ""
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (RiftAPI.inRift()) return

        if (!SkyHanniMod.feature.misc.pets.display) return
        val config = ProfileStorageData.profileSpecific ?: return

        SkyHanniMod.feature.misc.petDisplayPos.renderString(config.currentPet, posLabel = "Current Pet")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
    }
}
