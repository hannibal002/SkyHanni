package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetNametag {

    private val config get() = SkyHanniMod.feature.misc.pets.petNametag

    /**
     * REGEX-TEST: §8[§7Lv99§8] §6Skirtwearer's Ammonite
     * REGEX-TEST: §8[§7Lv100§8] §dSkirtwearer's Endermite§5 ✦
     */
    private val pattern by RepoPattern.pattern(
        "feature.pet.nametag",
        "(?<start>§8\\[§7Lv(?<lvl>\\d+)§8\\]) (?<rarity>§.)(?<player>[\\w_]+'s?) (?<pet>[\\w\\s]+)(?<skin>§5 ✦)?"
    )

    @SubscribeEvent
    fun onNameTagRender(event: EntityDisplayNameEvent) {
        if (event.entity !is EntityArmorStand) return

        pattern.matchMatcher(event.chatComponent.unformattedText) {
            val start = group("start")
            val lvl = group("lvl").formatInt()
            val rarity = group("rarity")
            val player = group("player")
            val pet = group("pet")
            val skin = group("skin") ?: ""

            val component = ChatComponentText("")

            if (config.hidePetLevel || config.hideMaxPetLevel) {
                if (config.hideMaxPetLevel && !(lvl == 100 || lvl == 200)) {
                    component.appendText("$start ")
                }
            } else {
                component.appendText("$start ")
            }

            if (!config.hidePlayerName) {
                component.appendText(player)
            }

            component.appendText("$rarity$pet $skin")

            ChatUtils.chat(component)

            event.chatComponent = component
        }
    }

}
