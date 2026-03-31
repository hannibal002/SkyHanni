package at.hannibal2.skyhanni.utils

object LorenzDebug {

    private val logger = SkyHanniLogger("debug")

    fun log(text: String) {
        logger.log(text)
        ChatUtils.consoleLog("debug logger: $text")
    }

    fun chatAndLog(text: String) {
        ChatUtils.debug(text)
        log(text)
    }
}
