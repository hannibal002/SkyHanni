package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

// https://wiki.hypixel.net/Pablo
@SkyHanniModule
object PabloHelper {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    private val patterns = listOf(
        "\\[NPC] Pablo: Could you bring me an (?<flower>[\\w ]+).*".toPattern(),
        "\\[NPC] Pablo: Bring me that (?<flower>[\\w ]+) as soon as you can!".toPattern()
    )
    private var lastSentMessage = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (lastSentMessage.passedSince() < 5.minutes) return
        val itemName = patterns.matchMatchers(event.message.removeColor()) {
            group("flower")
        } ?: return

        if (InventoryUtils.countItemsInLowerInventory { it.name.contains(itemName) } > 0) return

        GetFromSackAPI.getFromChatMessageSackItems(
            itemName.asInternalName().makePrimitiveStack(),
            "Click here to grab an $itemName from sacks!"
        )

        lastSentMessage = SimpleTimeMark.now()
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.pabloHelper
}
