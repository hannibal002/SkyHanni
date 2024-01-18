package at.hannibal2.skyhanni.test.hotswap

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import at.hannibal2.skyhanni.utils.LorenzUtils.removeFinal
import moe.nea.hotswapagentforge.forge.ClassDefinitionEvent
import moe.nea.hotswapagentforge.forge.HotswapEvent
import moe.nea.hotswapagentforge.forge.HotswapFinishedEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HotswapSupportImpl : HotswapSupportHandle {
    override fun load() {
        MinecraftForge.EVENT_BUS.register(this)
        println("Hotswap Client in Skyhanni loaded")
    }

    @SubscribeEvent
    fun onHotswapClass(event: ClassDefinitionEvent.Redefinition) {
        val instance = SkyHanniMod.modules.find { it.javaClass.name == event.fullyQualifiedName } ?: return
        val primaryConstructor = runCatching { instance.javaClass.getDeclaredConstructor() }.getOrNull()
        Minecraft.getMinecraft().addScheduledTask {
            LorenzUtils.chat("Refreshing event subscriptions for module $instance!")
            MinecraftForge.EVENT_BUS.unregister(instance)
            if (primaryConstructor == null) {
                MinecraftForge.EVENT_BUS.register(instance)
            } else {
                SkyHanniMod.modules.remove(instance)
                primaryConstructor.isAccessible = true
                val newInstance = primaryConstructor.newInstance()
                LorenzUtils.chat("Reconstructing $instance -> $newInstance!")
                val instanceField = runCatching { instance.javaClass.getDeclaredField("INSTANCE") }.getOrNull()
                    ?.takeIf { it.type == instance.javaClass }
                    ?.makeAccessible()
                    ?.removeFinal()
                if (instanceField != null) {
                    LorenzUtils.chat("Reinjected static instance $newInstance!")
                    instanceField.set(null, newInstance)
                }
                SkyHanniMod.modules.add(newInstance)
                MinecraftForge.EVENT_BUS.register(newInstance)
            }
        }
    }

    @SubscribeEvent
    fun onHotswapDetected(event: HotswapFinishedEvent) {
        LorenzUtils.chat("Hotswap finished!")
    }

    override fun isLoaded(): Boolean {
        return HotswapEvent.isReady()
    }
}
