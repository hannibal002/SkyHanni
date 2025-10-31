package at.hannibal2.hanni.utils

object LorenzDebug {

    private val logger = LorenzLogger("debug")

    fun log(text: String) {
        logger.log(text)
        ChatUtils.consoleLog("debug logger: $text")
    }

    fun chatAndLog(text: String) {
        ChatUtils.debug(text)
        log(text)
    }
}
