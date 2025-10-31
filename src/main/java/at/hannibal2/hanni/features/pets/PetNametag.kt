package at.hannibal2.hanni.features.pets

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
//#if MC > 1.16
//$$ import at.hannibal2.hanni.utils.compat.formattedTextCompatLessResets
//#endif

@HanniModule
object PetNametag {

    private val config get() = HanniMod.feature.misc.pets.nametag

    /**
     * REGEX-TEST: §8[§7Lv99§8] §6Ammonite
     * REGEX-TEST: §8[§7Lv100§8] §dEndermite§5 ✦
     */
    private val petNametagPattern by RepoPattern.pattern(
        "pet.nametag",
        "(?<start>§8\\[§7Lv(?<lvl>\\d+)§8]) (?<rarity>§.)(?<pet>[\\w\\s]+)(?<skin>§. ✦)?",
    )

    @HandleEvent
    fun onNameTagRender(event: EntityDisplayNameEvent<EntityArmorStand>) {
        if (!isEnabled()) return

        val standName: String =
            //#if MC < 1.16
            event.chatComponent.unformattedText
        //#else
        //$$ event.chatComponent.formattedTextCompatLessResets()
        //#endif

        petNametagPattern.matchMatcher(standName) {
            val start = group("start")
            val lvl = group("lvl").formatInt()
            val rarity = group("rarity")
            val pet = group("pet")
            val skin = groupOrEmpty("skin")

            val hideLevel = config.hidePetLevel
            val hideMaxLevel = config.hideMaxPetLevel && (lvl == 100 || lvl == 200)

            val text = buildString {
                if (!hideLevel && !hideMaxLevel) {
                    append(start)
                }
                append(rarity + pet + skin)
            }

            event.chatComponent = text.asComponent()
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && (config.hidePetLevel || config.hideMaxPetLevel)
}
