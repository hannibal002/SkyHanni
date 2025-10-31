package at.hannibal2.hanni.features.misc.teleportpad

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object TeleportPadCompactName {
    private val patternGroup = RepoPattern.group("misc.teleportpad")

    /**
     * REGEX-TEST: §a✦ §aWarp To §9Blue
     */
    private val namePattern by patternGroup.pattern(
        "name",
        "§.✦ §aWarp To (?<name>.*)"
    )

    /**
     * REGEX-TEST: §c✦ §cNo Destination
     */
    private val noNamePattern by patternGroup.pattern(
        "noname",
        "§.✦ §cNo Destination"
    )

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!HanniMod.feature.misc.teleportPad.compactName) return
        val entity = event.entity

        val name = entity.name

        noNamePattern.matchMatcher(name) {
            event.cancel()
        }

        namePattern.matchMatcher(name) {
            //#if MC < 1.21
            entity.customNameTag = group("name")
            //#else
            //$$ entity.setCustomName(net.minecraft.text.Text.of(group("name")))
            //#endif
        }
    }
}
