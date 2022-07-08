package at.lorenz.mod

import at.lorenz.mod.utils.LorenzLogger
import at.lorenz.mod.utils.LorenzUtils

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