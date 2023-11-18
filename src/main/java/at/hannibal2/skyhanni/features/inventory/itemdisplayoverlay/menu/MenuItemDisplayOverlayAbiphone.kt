package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayAbiphone {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.abiphone.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.abiphone
        val chestName = InventoryUtils.openInventoryName()

        (("(.*A.iphone.*|Contacts Directory)").toPattern()).matchMatcher(chestName) {
            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.CONTACTS_DIRECTORY)) && (itemName == ("Contacts Directory"))) {
                for (line in item.getLore()) {
                    ("(§.)?Your contacts: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*".toPattern()).matchMatcher(line) {
                        return group("useful")
                    }
                }
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.DO_NOT_DISTURB))) {
                val nameWithColor = item.name ?: return ""
                ((".*§f§.*").toPattern()).matchMatcher(nameWithColor) {
                    val lore = item.getLore()
                    for (line in lore) {
                        if (line == ("§cDo Not Disturb enabled!")) {
                            return "§c§l✖"
                        }
                    }
                }
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.RELAYS_COMPLETED)) && (itemName == ("9f™ Operator Chip"))) {
                val maxRelays = "9" //edit this line whenever they add more relays
                //§7Upgraded Relays: §e1§7/§59
                //Upgraded Relays: 1/9
                for (line in item.getLore()) {
                    ("(§.)?Upgraded Relays: (§.).*ALL!.*".toPattern()).matchMatcher(line) { return maxRelays }
                    ("(§.)?Upgraded Relays: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*".toPattern()).matchMatcher(line) { return group("useful") }
                }
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.SELECTED_RINGTONE)) && (itemName == ("Ringtones"))) {
                for (line in item.getLore()) {
                    (("(§.)*Selected Ringtone: (§.)*(?<ringtone>.+)").toPattern()).matchMatcher(line) {
                        return when (group("ringtone").split(" ").last()) {
                            "Default" -> "Def"
                            "Entertainer" -> "Ent"
                            "Notkia" -> "Nka"
                            "Techy" -> "Tec"
                            "Scrapper" -> "Scr"
                            "Elise" -> "WTF" //intentional. do not edit.
                            "Bells" -> "Jbl"
                            "Vibrate" -> "Vib"
                            else -> "?"
                        }
                    }
                }
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.TIC_TAC_TOE)) && (itemName == ("Tic Tac Toe"))) {
                var finalString = ""
                for (line in item.getLore()) {
                    (("(§.)*(?<type>.+): (§.)*(?<count>[\\w]+)").toPattern()).matchMatcher(line) {
                        val colorCode = when (group("type")) {
                            "Wins" -> "§a"
                            "Draws" -> "§e"
                            "Losses" -> "§c"
                            else -> "§0"
                        }
                        finalString = "$finalString$colorCode${group("count")}"
                    }
                }
                return finalString
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.SNAKE)) && (itemName == ("Snake"))) {
                for (line in item.getLore()) {
                    (("(§.)*(?<type>.+): (§.)*(?<count>[\\w]+)").toPattern()).matchMatcher(line) {
                        return group("count")
                    }
                }
            }

            if ((stackSizeConfig.contains(StackSizeMenuConfig.Abiphone.NAVIGATION)) && ((itemName == ("Filter")) || itemName == ("Sort"))) {
                for (line in item.getLore()) {
                    ((".*(?<colorCode>§.)*▶.?(?<category>[\\w ]+).*").toPattern()).matchMatcher(line) {
                        return when (val placeholder = group("category").replace(" ", "").lowercase()) {
                            "alphabetical" -> "ABC"
                            "donotdisturbfirst" -> "§cDND"
                            "difficulty" -> "§aE§eM§cH"
                            "usuallocation" -> "Loc"
                            "notadded" -> "§cQA"
                            "completedquestbutnotadded" -> "§aQ§cA"
                            else -> placeholder.take(3).firstLetterUppercase()
                        }
                    }
                }
            }
        }

        return ""
    }
}
