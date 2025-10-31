package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.GetFromSackApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.hanni.utils.RegexUtils.matchMatchers
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object GFSPiggyBank {

    private val ENCHANTED_PORK by lazy { "ENCHANTED_PORK".toInternalName().makePrimitiveStack(8) }

    private val group = RepoPattern.group("misc.piggybank")

    /**
     * REGEX-TEST: §cYou died and your piggy bank cracked!
     */
    private val crackedPattern by group.pattern(
        "cracked",
        "§cYou died and your piggy bank cracked!",
    )

    /**
     * REGEX-TEST: §cYou died, lost 50,000 coins and your piggy bank broke!
     */
    private val brokePattern by group.pattern(
        "broke",
        "§cYou died, lost [\\d.,]* coins and your piggy bank broke!",
    )

    private val patternList = listOf(crackedPattern, brokePattern)

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        patternList.matchMatchers(event.message) {
            GetFromSackApi.getFromChatMessageSackItems(ENCHANTED_PORK)
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HanniMod.feature.misc.gfsPiggyBank
}
