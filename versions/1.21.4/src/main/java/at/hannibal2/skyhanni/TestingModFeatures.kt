package at.hannibal2.skyhanni

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

object TestingModFeatures {

    init {
        println("TestingModFeatures loaded")


        ClientTickEvents.START_WORLD_TICK.register(
            ClientTickEvents.StartWorldTick {
                MinecraftClient.getInstance().player ?: return@StartWorldTick

                //println("screen class: ${MinecraftClient.getInstance().currentScreen?.javaClass?.name}")
                //println("screen title: ${MinecraftClient.getInstance().currentScreen?.title}")

                // gets inventory stacks
//                 val size = MinecraftClient.getInstance().player?.currentScreenHandler?.slots?.size ?: 0
//                 for (i in 0 until size) {
//                     val slot = MinecraftClient.getInstance().player?.currentScreenHandler?.slots?.get(i)
//                     println(slot?.stack?.name.formattedTextCompat())
//                 }
            },
        )


    }


}
