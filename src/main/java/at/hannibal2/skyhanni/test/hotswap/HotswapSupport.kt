package at.hannibal2.skyhanni.test.hotswap

import java.util.function.Supplier

object HotswapSupport {

    private val isForgeSidePresent =
        runCatching { Class.forName("moe.nea.hotswapagentforge.forge.HotswapEvent") }.isSuccess
    private val obj = if (isForgeSidePresent) {
        Supplier<HotswapSupportHandle?> { HotswapSupportImpl() }
    } else {
        Supplier<HotswapSupportHandle?> { null }
    }.get()

    fun isLoaded(): Boolean {
        return obj?.isLoaded() ?: false
    }

    fun load() {
        obj?.load()
    }
}

