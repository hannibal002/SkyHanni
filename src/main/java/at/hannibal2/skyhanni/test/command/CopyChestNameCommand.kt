package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils

object CopyChestNameCommand {
    fun command() {
        copyChestName()
    }
    fun copyChestName() {
        OSUtils.copyToClipboard(InventoryUtils.openInventoryName())
        LorenzUtils.chat("Chest name copied into your clipboard!")
    }
}
