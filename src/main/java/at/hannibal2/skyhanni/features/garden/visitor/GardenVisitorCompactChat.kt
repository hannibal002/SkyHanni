package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import kotlin.time.Duration.Companion.milliseconds

object GardenVisitorCompactChat {

    private var visitorAcceptedChat = mutableListOf<String>()
    private var visitorNameFormatted = "";
    private var rewardsList = mutableListOf<String>()

    private fun compactChat(event: LorenzChatEvent) {
        if(!VisitorAPI.config.compactVisitorRewardChat) return
        event.blockedReason = "compact_visitor"
        visitorAcceptedChat.add(event.message)
        if(visitorAcceptedChat.size == 3){
            DelayedRun.runDelayed(200.milliseconds){
                sendCompact()
            }
        }
    }

    private fun sendCompact() {
        if(visitorAcceptedChat.isNotEmpty()) {
            ChatUtils.hoverableChat(createCompactVisitorMessage(), hover = visitorAcceptedChat, prefix = false)
        }

        this.visitorNameFormatted = ""
        this.rewardsList.clear()
    }

    private fun createCompactVisitorMessage(): String {
        val rewardsFormattedList = mutableListOf<String>()
        if(rewardsList.isNotEmpty()) rewardsFormattedList.addAll(rewardsList)

        val rewardsFormatted = rewardsFormattedList.joinToString(separator = "§7, ", prefix = " ")

        return "§6§lOFFER ACCEPTED §7w/§r$visitorNameFormatted§6§l:$rewardsFormatted"
    }

    fun handleChat(event: LorenzChatEvent) {
        val transformedMessage = event.message.removeResets()

        GardenVisitorFeatures.fullyAcceptedPattern.matchMatcher(transformedMessage){
            visitorAcceptedChat = mutableListOf()
            val visitorColor = groupOrNull("color") ?: "§7"
            val visitorName = group("name")
            visitorNameFormatted = "$visitorColor$visitorName"
        }

        //Match rewards and transform
        GardenVisitorFeatures.visitorRewardPattern.matchMatcher(transformedMessage) {
            val rewardColor = groupOrNull("rewardcolor")
            val amountColor = groupOrNull("amountcolor")
            val amount = groupOrNull("amount")
            var altAmount = groupOrNull("altamount")
            val reward = group("reward")

            val fullTextColor = if(rewardColor.isNullOrBlank() || rewardColor == "§7"){
                if(amountColor.isNullOrBlank()) "§f"
                else amountColor
            } else rewardColor

            val amountString = if(amount != null) {
                if(GardenVisitorFeatures.discardRewardNamePattern.matcher(reward).matches()) "$amount"
                else "$amount "
            } else {
                altAmount ?: ""
            }

            //Don't add name for copper, farming XP, garden XP, or bits
            val rewardString = if(GardenVisitorFeatures.discardRewardNamePattern.matcher(reward).matches()) "" else reward

            rewardsList.add(
                "$fullTextColor$amountString$rewardString"
            )
        }

        compactChat(event)
    }
}
