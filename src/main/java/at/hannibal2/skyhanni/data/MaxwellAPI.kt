package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MaxwellAPI {
    val config = ProfileStorageData.profileSpecific
    var currentPower: MaxwellPowers? = config?.currentPower

    private val pattern by RepoPattern.pattern(
        "data.maxwell.chat.power",
        "§eYou selected the §a(?<power>.*) §efor your §aAccessory Bag§e!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        pattern.matchMatcher(event.message) {
            currentPower = MaxwellPowers.entries.find { group("power").contains(it.power) }
                ?: MaxwellPowers.UNKNOWN
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
        currentPower = config?.currentPower ?: return
    }

    private fun savePower(power: MaxwellPowers?) {
        if (power == null) return
        config?.currentPower = power
    }
}
