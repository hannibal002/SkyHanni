package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CrystalHollowsProfessorRobot {

    private val config get() = SkyHanniMod.feature.mining.professorRobot;

    private val pattern = "\\[NPC\\] Professor Robot: That's not one of the components I need! Bring me one of the missing components:".toRegex()

    private var robotMessage = false
    private val robotParts = listOf(
        "Electron Transmitter",
        "FTX 3070",
        "Robotron Reflector",
        "Superlite Motor",
        "Control Switch",
        "Synthetic Heart"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (event.message.removeColor().matches(pattern)) {
            robotMessage = true;
            return
        }
        if (!robotMessage) return

        val itemName = event.message.removeColor().removePrefix("  ")

        if (InventoryUtils.countItemsInLowerInventory { it.name.contains(itemName) } > 0 || !robotParts.contains(itemName)) return

        ChatUtils.clickableChat("Click here to grab an §r§9$itemName!", onClick = {
            if(config.sack) HypixelCommands.getFromSacks(itemName.replace(' ', '_'), 1)
            else HypixelCommands.bazaar(itemName)
        })

        robotMessage = false
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.enabled
}
