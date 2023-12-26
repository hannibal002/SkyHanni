package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

object MinecraftExecutor {

    @JvmField
    val OnThread = Executor {
        val mc = Minecraft.getMinecraft()
        if (mc.isCallingFromMinecraftThread) {
            it.run()
        } else {
            Minecraft.getMinecraft().addScheduledTask(it)
        }
    }

    @JvmField
    val OffThread = Executor {
        val mc = Minecraft.getMinecraft()
        if (mc.isCallingFromMinecraftThread) {
            ForkJoinPool.commonPool().execute(it)
        } else {
            it.run()
        }
    }
}
