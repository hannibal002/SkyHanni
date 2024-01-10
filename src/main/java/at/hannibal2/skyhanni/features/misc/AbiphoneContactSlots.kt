package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AbiphoneContactSlots {
    private val allPossibleUpgrades: List<Int> = listOf<Int>(36, 2, 2) //36: Contacts Trio, 2: GG Abicase, 2: Abiphone relays. see https://antifandom.com/hypixel-skyblock/wiki/Abiphones#Contact_Slots
    private val couldNotCalculateNotice: String = " §8Could not calculate contact slots. [SkyHanni]"
    private val abiphoneContactSlotsGroup = RepoPattern.group("abiphone.contact.slots")
    private val isAbiphoneInternalNamePattern by abiphoneContactSlotsGroup.pattern(("isabiphone.internalname"), ("ABIPHONE_.*")) //is it even worth it (assuming, well, *possible at all*) to increase contact slots on the abingohpone?
    private val maximumContactSlotsLoreLinePattern by abiphoneContactSlotsGroup.pattern(("maximumcontactslots.loreline"), (" ?§7Maximum Contacts: (?:§.)*(?<first>\\d+)(?: |§.)*(?:[(\\[]\\+?(?<second>\\d+)[)\\]])?(?: |§.)*(?:[(\\[]\\+?(?<third>\\d+)[)\\]])?(?: |§.)*(?:[(\\[]\\+?(?<fourth>\\d+)[)\\]])?(?: |§.)*(?:[(\\[]\\+?(?<fifth>\\d+)[)\\]])?")) // adapted from nea's https://regex101.com/r/aCVGHN/1 -ery

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.abiphoneContactsProgress) return
        val currScreen = Minecraft.getMinecraft().currentScreen ?: return
        if (currScreen is GuiEditSign) return
        val itemStack = event.itemStack
        if (!(isAbiphoneInternalNamePattern.matches(itemStack.getInternalName().asString()))) return
        val itemLore = itemStack.getLore()
        if (itemLore.none { maximumContactSlotsLoreLinePattern.matches(it) }) return
        // "mc.currentScreen !is AccessorGuiEditSign" is to prevent such calculations happening with NEU custom auction house search
        // §7Maximum Contacts: §b7 §c(+15) §5(+1)
        // §7Maximum Contacts: §b7 §c(+15) §5(+1) §f(+1)
        // 7 15 1 1
        var total = 0
        val trueIndex = itemLore.indexOfFirst { maximumContactSlotsLoreLinePattern.matches(it) }
        if (trueIndex == -1) return
        val index = trueIndex + 2
        maximumContactSlotsLoreLinePattern.matchMatcher(itemLore[trueIndex]) {
            val contactsBeforeSplit = "${group("first") ?: 0} ${group("second") ?: 0} ${group("third") ?: 0} ${group("fourth") ?: 0}" // ${group("fifth") ?: 0}"
            val contacts = contactsBeforeSplit.split(" ")
            for (eachInt in contacts) if (eachInt.toIntOrNull() != null) total += eachInt.toInt()
            if (total > 0) {
                event.toolTip.add(
                    index,
                    " §7Contacts Progress: §b$total§7/§b${(allPossibleUpgrades.sum() + contacts.first().toInt())}"
                )
            } else { //this is in case hypixel changes formatting but still keeps spaces somehow
                event.toolTip.add(index, couldNotCalculateNotice)
            }
        }
    }
}
