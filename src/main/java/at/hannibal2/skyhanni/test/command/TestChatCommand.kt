package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils

object TestChatCommand {
    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            LorenzUtils.chat("§c[SkyHanni] Specify a chat message to test")
            return
        }
        val resultList = mutableListOf<String>()
        for (arg in args) {
            resultList.add(arg)
        }
        val string = resultList.joinToString(" ")
        LorenzUtils.chat("§a[SkyHanni] testing message: §7$string")
        LorenzUtils.chat(string.replace("&", "§"))
    }
}