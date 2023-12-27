package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MaxwellAPI {
    var currentPower: MaxwellPowers? = null

    private val pattern by RepoPattern.pattern(
        "data.maxwell.chat.power",
        "§eYou selected the §a(?<power>.*) §efor your §aAccessory Bag§e!"
    )

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentPower = config.currentPower ?: return
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        pattern.matchMatcher(message) {
            val power = group("power")
            currentPower = MaxwellPowers.entries.find { power.contains(it.power) } ?: MaxwellPowers.UNKNOWN
            savePower(currentPower)
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.inventoryName.contains("Accessory Bag Thaumaturgy")) return

        val stacks = event.inventoryItems
        val selectedPower =
            stacks.values.find { it.getLore().isNotEmpty() && it.getLore().last() == "§aPower is selected!" } ?: return

        currentPower = MaxwellPowers.entries.find { selectedPower.displayName.contains(it.power) }
        savePower(currentPower)
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentPower = config.currentPower ?: return
    }

    private fun savePower(power: MaxwellPowers?) {
        if (power == null) return
        val config = ProfileStorageData.profileSpecific ?: return
        config.currentPower = power
    }
}
