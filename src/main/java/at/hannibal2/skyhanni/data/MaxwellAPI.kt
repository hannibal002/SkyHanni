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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


// TODO USE SH-REPO
private val pattern = "§eYou selected the §a(?<power>.*) §efor your §aAccessory Bag§e!".toPattern()
enum class Powers (val power: String) {
    // Standard
    NO_POWER("No Power"),
    FORTUITOUS("Fortuitous"),
    PRETTY("Pretty"),
    PROTECTED("Protected"),
    SIMPLE("Simple"),
    WARRIOR("Warrior"),
    COMMANDO("Commando"),
    DISCIPLINED("Disciplined"),
    INSPIRED("Inspired"),
    OMINOUS("Ominous"),
    PREPARED("Prepared"),

    // Unlockable
    SILKY("Silky"),
    SWEET("Sweet"),
    BLOODY("Bloody"),
    ITCHY("Itchy"),
    SIGHTED("Sighted"),
    ADEPT("Adept"),
    MYTHICAL("Mythical"),
    FORCEFUL("Forceful"),
    SHADED("Shaded"),
    STRONG("Strong"),
    DEMONIC("Demonic"),
    PLEASANT("Pleasant"),
    HURTFUL("Hurtful"),
    BIZARRE("Bizarre"),
    HEALTHY("Healthy"),
    SLENDER("Slender"),
    SCORCHING("Scorching"),
    CRUMBLY("Crumbly"),
    BUBBA("Bubba"),
    SANGUISUGE("Sanguisuge"),

    UNKNOWN("Unknown"),
    ;
}
object MaxwellAPI {
    var currentPower : Powers? = null

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
            currentPower = Powers.entries.find { power.contains(it.power) } ?: Powers.UNKNOWN
            savePower(currentPower!!)
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.inventoryName.contains("Accessory Bag Thaumaturgy")) return

        val stacks = event.inventoryItems
        val selectedPower = stacks.values.find { it.getLore().isNotEmpty() && it.getLore().last() == "§aPower is selected!" } ?: return

        currentPower = Powers.entries.find { selectedPower.displayName.contains(it.power) }
        savePower(currentPower!!)
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentPower = config.currentPower ?: return
    }

    private fun savePower(power: Powers) {
        val config = ProfileStorageData.profileSpecific ?: return
        config.currentPower = power
    }
}
