package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.hoppity.RabbitFoundEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsManager.getEggType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyblockSeason
import net.minecraft.util.ChatComponentText
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityAPI {
    private var sideDishActive = false
    fun setSideDishActive() {
        sideDishActive = true
        LorenzChatEvent("§d§lHOPPITY'S HUNT §r§dYou found a §r§6§lSide Dish §r§6Egg §r§din the Chocolate Factory§r§d!", ChatComponentText("")).postAndCatch()
        DelayedRun.runDelayed(7.seconds) {
            sideDishActive = false
        }
    }

    fun isHoppityEvent() = (SkyblockSeason.currentSeason == SkyblockSeason.SPRING || SkyHanniMod.feature.dev.debug.alwaysHoppitys)

    private var hoppityEggChat = mutableListOf<String>()
    private var duplicate = false
    private var lastRarity = ""
    private var lastName = ""
    private var newRabbit = false
    private var lastChatMeal: HoppityEggType? = null
    private var lastDuplicateAmount: Long? = null
    private var rabbitBought = false
    private var sideDishEgg = false

    private fun resetChatData() {
        this.hoppityEggChat = mutableListOf()
        this.duplicate = false
        this.newRabbit = false
        this.lastRarity = ""
        this.lastName = ""
        this.lastChatMeal = null
        this.lastDuplicateAmount = null
        this.rabbitBought = false
        this.sideDishEgg = false
    }

    // Dumbed down version of the Compact Chat for Hoppity's,
    // with the additional native context of side dishes
    fun handleChat(event: LorenzChatEvent) {
        HoppityEggsManager.eggFoundPattern.matchMatcher(event.message) {
            resetChatData()
            lastChatMeal = getEggType(event)
            if (lastChatMeal == HoppityEggType.SIDE_DISH) sideDishEgg = true
            attemptFire(event)
        }

        HoppityEggsManager.eggBoughtPattern.matchMatcher(event.message) {
            if (group("rabbitname").equals(lastName)) {
                rabbitBought = true
                lastChatMeal = HoppityEggType.BOUGHT
                attemptFire(event)
            }
        }

        HoppityEggsManager.rabbitFoundPattern.matchMatcher(event.message) {
            // The only cases where "You found ..." will come in with more than 1 message,
            // or empty for hoppityEggChat, is where the rabbit was purchased from hoppity,
            // or when a Side Dish golden stray was found in the Chocolate Factory.
            // In the case of buying, we want to reset variables to a clean state during this capture,
            // as the important capture for the purchased message is the final message in
            // the chain; "You found [rabbit]" -> "Dupe/New Rabbit" -> "You bought [rabbit]"
            if ((hoppityEggChat.isEmpty() || hoppityEggChat.size > 1) && !sideDishEgg) {
                resetChatData()
            }

            lastName = group("name")
            lastRarity = group("rarity")
            attemptFire(event)
        }

        HoppityEggsManager.newRabbitFound.matchMatcher(event.message) {
            newRabbit = true
            groupOrNull("other")?.let {
                attemptFire(event)
                return
            }
            attemptFire(event)
        }
    }

    fun attemptFire(event: LorenzChatEvent, lastDuplicateAmount: Long? = null) {
        lastDuplicateAmount?.let {
            this.lastDuplicateAmount = it
        }
        hoppityEggChat.add(event.message)
        if (lastDuplicateAmount != null) {
            this.duplicate = true
        }
        if (lastChatMeal == null) return
        if (hoppityEggChat.size == 3) {
            RabbitFoundEvent(lastChatMeal!!, duplicate, lastName, lastDuplicateAmount ?: 0).post()
        }
    }
}
