package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class BaitChangeWarning {
    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarning
    private var bobber: EntityFishHook? = null
    private var lastBait: String? = null
    private var timeLastCast: Long = 0L
    private var isUsingBait: Boolean = false

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent){
        if(!isEnabled()) return
        val entity = event.entity ?: return
        if(entity !is EntityFishHook) return
        if(entity.angler != Minecraft.getMinecraft().thePlayer) return

        bobber = entity;
        timeLastCast = System.currentTimeMillis()
        isUsingBait = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent){
        if(!isEnabled() || bobber == null) return
        //Is there a way to get event sent time to be more accurate?
        if(System.currentTimeMillis() - timeLastCast < 1000L) return

        if(!isUsingBait && config.noBaitWarning) showNoBaitWarning()
        reset()
    }

    fun reset(){
        bobber = null
        isUsingBait = false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent){
        if(!isEnabled() || !config.baitChangeWarning) return
        if(bobber == null) return
        for(entityItem in EntityUtils.getEntitiesNearby<EntityItem>(bobber!!.getLorenzVec(), 1.5)){
            val itemStack = entityItem.entityItem
            var name = itemStack.name ?: continue
            name = name.removeColor()

            if((!name.endsWith(" Bait") && !name.startsWith("Obfuscated"))
                || itemStack.stackSize != 1) continue

            isUsingBait = true
            if(lastBait == null){
                lastBait = name.removeColor()
                continue
            }
            if(name.removeColor() == lastBait) continue
            showBaitChangeWarning(lastBait!!, name.removeColor())
            lastBait = name.removeColor()
        }
    }

    fun showBaitChangeWarning(before: String, after: String){
        SoundUtils.playClickSound()
        LorenzUtils.sendTitle("§eBait changed!", 2.seconds)
        LorenzUtils.chat("§e" + before + " -> " + after)
    }

    fun showNoBaitWarning(){
        SoundUtils.playErrorSound()
        LorenzUtils.sendTitle("§cNo bait is used!", 2.seconds)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && FishingAPI.hasFishingRodInHand()
}
