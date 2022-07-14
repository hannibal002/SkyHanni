package at.hannibal2.skyhanni.utils

object LorenzDebug {

    private val logger = LorenzLogger("debug")

    fun log(text: String) {
        logger.log(text)
        println("debug logger: $text")
    }

    fun writeAndLog(text: String) {
        LorenzUtils.debug(text)
        log(text)
    }
}