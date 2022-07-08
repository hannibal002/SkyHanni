package at.lorenz.mod

import at.lorenz.mod.utils.LorenzLogger

object LorenzDebug {

    private val logger = LorenzLogger("debug")

    fun log(text: String) {
        logger.log(text)
        println("debug logger: $text")
    }
}