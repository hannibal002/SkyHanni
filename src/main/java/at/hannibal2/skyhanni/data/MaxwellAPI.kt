package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern


// TODO USE SH-REPO
enum class Powers (val power: String, val pattern: Pattern) {
    // Standard
    FORTUITOUS("Fortuitous", "§r§eYou(?:r selected power was set to)? §r§aFortuitous§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    PRETTY("Pretty", "§r§eYou(?:r selected power was set to)? §r§aPretty§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    PROTECTED("Protected", "§r§eYou(?:r selected power was set to)? §r§aProtected§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SIMPLE("Simple", "§r§eYou(?:r selected power was set to)? §r§aSimple§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    WARRIOR("Warrior", "§r§eYou(?:r selected power was set to)? §r§aWarrior§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    COMMANDO("Commando", "§r§eYou(?:r selected power was set to)? §r§aCommando§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    DISCIPLINED("Disciplined", "§r§eYou(?:r selected power was set to)? §r§aDisciplined§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    INSPIRED("Inspired", "§r§eYou(?:r selected power was set to)? §r§aInspired§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    OMINOUS("Ominous", "§r§eYou(?:r selected power was set to)? §r§aOminous§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    PREPARED("Prepared", "§r§eYou(?:r selected power was set to)? §r§aPrepared§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),

    // Unlockable
    SILKY("Silky", "§r§eYou(?:r selected power was set to)? §r§aSilky§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SWEET("Sweet", "§r§eYou(?:r selected power was set to)? §r§aSweet§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    BLOODY("Bloody", "§r§eYou(?:r selected power was set to)? §r§aBloody§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    ITCHY("Itchy", "§r§eYou(?:r selected power was set to)? §r§aItchy§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SIGHTED("Sighted", "§r§eYou(?:r selected power was set to)? §r§aSighted§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    ADEPT("Adept", "§r§eYou(?:r selected power was set to)? §r§aAdept§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    MYTHICAL("Mythical", "§r§eYou(?:r selected power was set to)? §r§aMythical§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    FORCEFUL("Forceful", "§r§eYou(?:r selected power was set to)? §r§aForceful§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SHADED("Shaded", "§r§eYou(?:r selected power was set to)? §r§aShaded§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    STRONG("Strong", "§r§eYou(?:r selected power was set to)? §r§aStrong§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    DEMONIC("Demonic", "§r§eYou(?:r selected power was set to)? §r§aDemonic§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    PLEASANT("Pleasant", "§r§eYou(?:r selected power was set to)? §r§aPleasant§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    HURTFUL("Hurtful", "§r§eYou(?:r selected power was set to)? §r§aHurtful§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    BIZARRE("Bizarre", "§r§eYou(?:r selected power was set to)? §r§aBizarre§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    HEALTHY("Healthy", "§r§eYou(?:r selected power was set to)? §r§aHealthy§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SLENDER("Slender", "§r§eYou(?:r selected power was set to)? §r§aSlender§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SCORCHING("Scorching", "§r§eYou(?:r selected power was set to)? §r§aScorching§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    CRUMBLY("Crumbly", "§r§eYou(?:r selected power was set to)? §r§aCrumbly§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    BUBBA("Bubba", "§r§eYou(?:r selected power was set to)? §r§aBubba§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() ),
    SANGUISUGE("Sanguisuge", "§r§eYou(?:r selected power was set to)? §r§aSanguisuge§r§e(?:!§r| power for your §r§aAccessory Bag§r§e!§r)".toPattern() );
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
        for (power in Powers.entries) {
            if (power.pattern.matcher(event.message).matches()) {
                currentPower = power
                savePower(power)
            }
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!InventoryUtils.openInventoryName().contains("Accessory Bag Thaumaturgy")) return

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
