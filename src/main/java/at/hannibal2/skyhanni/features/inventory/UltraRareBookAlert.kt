package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object UltraRareBookAlert {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting
    private val chargeSound by lazy { createSound("item.fireCharge.use", 1f) }

    private val superpairsGui by RepoPattern.pattern(
        "inventory.experimentstable.gui",
        "Superpairs.*"
    )

    private val ultraRarePattern by RepoPattern.pattern(
        "inventory.experimentstable.ultrarare",
        "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX"
    )

    private val bookPattern by RepoPattern.pattern(
        "inventory.experimentstable.book",
        "§9(?<enchant>.*)"
    )

    private val enchantsFound = mutableListOf<Int>()

    fun notification(enchantsName: String) {
        chargeSound.playSound()
        DelayedRun.runDelayed(0.5.seconds) {
            chargeSound.playSound()
        }
        DelayedRun.runDelayed(1.seconds) {
            chargeSound.playSound()
        }
        ChatUtils.chat("You have uncovered a §d§kXX§5 ULTRA-RARE §d§kXX§r §ebook! You found: §9$enchantsName§.")
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.ultraRareBookAlert) return
        if (!superpairsGui.matches(event.inventoryName)) return

        for ((slotId, item) in event.inventoryItems) {
            val firstLine = item.getLore().firstOrNull() ?: continue
            if (slotId in enchantsFound) continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = item.getLore().getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine){
                val enchantsName = group ("enchant")
                notification(enchantsName)
                enchantsFound.add(slotId)

            }
        }

    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        enchantsFound.clear()
    }
}
