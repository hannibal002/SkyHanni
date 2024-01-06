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
    private val abiphoneContactSlotsGroup = RepoPattern.group("abiphone.contact.slots")
    private val allPossibleUpgrades: List<Int> = listOf<Int>(36, 2, 2) //36: Contacts Trio, 2: GG Abicase, 2: Abiphone relays. see https://antifandom.com/hypixel-skyblock/wiki/Abiphones#A%E2%92%B7iphone
    private val isAbiphoneInternalNamePattern by abiphoneContactSlotsGroup.pattern(("isabiphone.internalname"), ("ABIPHONE_.*"))
    private val maximumContactSlotsLoreLinePattern by abiphoneContactSlotsGroup.pattern(("maximumcontactslots.loreline"), ("(?: )?(?:§.)Maximum Contacts: (?:(?:§.)(?<base>[\\d]+)) ?(?:(?:§.)\\(\\+(?<one>[\\d]+)\\))? ?(?:(?:§.)\\(\\+(?<two>[\\d]+)\\))? ?(?:(?:§.)\\(\\+(?<three>[\\d]+)\\))? ?")) // https://regex101.com/r/NsPIm2/1 -ery

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.abiphoneContactsProgress) return
        if (Minecraft.getMinecraft().currentScreen == null) return
        if (Minecraft.getMinecraft().currentScreen is GuiEditSign) return
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
            val contactsBeforeSplit = "${group("base") ?: 0} ${group("one") ?: 0} ${group("two") ?: 0} ${group("three") ?: 0}"
            val contacts = contactsBeforeSplit.split(" ")
            if (contacts.first() == contactsBeforeSplit) {
                //this is in case hypixel changes their formatting of the maximum contacts lore line and things go terribly wrong
                event.toolTip.add(index, " §8Could not calculate contact slots. [SkyHanni]")
            } else {
                for (eachInt in contacts) if (eachInt.toIntOrNull() != null) total += eachInt.toInt()
                if (total > -1) {
                    event.toolTip.add(
                        index,
                        " §7Contacts Progress: §b$total§7/§b${(allPossibleUpgrades.sum() + contacts.first().toInt())}"
                    )
                } else { //this is in case hypixel changes formatting but still keeps spaces somehow
                    event.toolTip.add(index, " §8Could not calculate contact slots. [SkyHanni]")
                }
            }
        }
    }
}
