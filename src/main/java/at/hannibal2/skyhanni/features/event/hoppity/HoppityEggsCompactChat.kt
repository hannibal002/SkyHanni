package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsManager.getEggType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object HoppityEggsCompactChat {

    private var hoppityEggChat = mutableListOf<String>()
    private var duplicate = false
    private var lastRarity = ""
    private var lastName = ""
    private var lastProfit = ""
    private var newRabbit = false
    private var lastChatMeal: HoppityEggType? = null
    private var lastDuplicateAmount: Long? = null

    fun compactChat(event: LorenzChatEvent, lastDuplicateAmount: Long? = null) {
        lastDuplicateAmount?.let {
            this.lastDuplicateAmount = it
        }
        if (!HoppityEggsManager.config.compactChat) return
        event.blockedReason = "compact_hoppity"
        hoppityEggChat.add(event.message)
        if (lastDuplicateAmount != null) {
            this.duplicate = true
        }
        if (hoppityEggChat.size == 3) {
            DelayedRun.runDelayed(200.milliseconds) {
                sendCompact()
            }
        }
    }

    private fun sendCompact() {
        if (hoppityEggChat.isNotEmpty()) {
            ChatUtils.hoverableChat(createCompactMessage(), hover = hoppityEggChat, prefix = false)
        }

        this.duplicate = false
        this.newRabbit = false
        lastRarity = ""
        lastName = ""
        lastProfit = ""
        lastChatMeal = null
        lastDuplicateAmount = null
    }

    private fun createCompactMessage(): String {
        val mealName = lastChatMeal?.coloredName ?: ""

        return if (duplicate) {
            val format = lastDuplicateAmount?.let { NumberUtil.format(it) } ?: "?"
            "$mealName Egg! §7Duplicate $lastName §7(§6+$format Chocolate§7)"
        } else if (newRabbit) {
            "$mealName Egg! §d§lNEW $lastName §7(${lastProfit}§7)"
        } else "?"
    }

    fun handleChat(event: LorenzChatEvent) {
        HoppityEggsManager.eggFoundPattern.matchMatcher(event.message) {
            hoppityEggChat = mutableListOf()
            lastChatMeal = getEggType(event)
            compactChat(event)
        }

        HoppityEggsManager.rabbitFoundPatttern.matchMatcher(event.message) {
            lastName = group("name")
            lastRarity = group("rarity")
            compactChat(event)
        }
        HoppityEggsManager.newRabbitFound.matchMatcher(event.message) {
            val chocolate = group("chocolate")
            val perSecond = group("perSecond")
            newRabbit = true
            lastProfit = "§6+$chocolate §7and §6+${perSecond}x c/s!"
            compactChat(event)
        }
    }

    fun clickableCompact(onClick: () -> Unit): Boolean = if (hoppityEggChat.size > 0) {
        val hover = hoppityEggChat.joinToString("\n") +
            " \n§eClick here to share the location of this chocolate egg with the server!"
        hoppityEggChat.clear()
        ChatUtils.clickableChat(
            createCompactMessage(),
            hover = hover,
            onClick = onClick,
            expireAt = 30.seconds.fromNow(),
            oneTimeClick = true,
            prefix = false
        )
        true
    } else false
}
