package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object FrogMaskDisplay {

    val config get() = SkyHanniMod.feature.misc

    private var display = Renderable.verticalContainer(listOf())

    private var region: Pair<String, SimpleTimeMark> = "" to SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("misc.frogmask")
    private val activeRegionPattern by patternGroup.pattern(
        "description.active",
        "Today's region: (?<region>.+)",
    )

    private const val FROG_MASK_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxNDI0NjI0NzQ1NywKICAicHJvZmlsZUlkIiA6ICJmOTEyM2ZjOGQzMzg0OWMwOWFlM" +
        "zk5YjQ2NDljZDRjZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJab2VfX01heSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAg" +
        "ICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84NzUyMmExZWFkZjg5OTZmNDJhOGQyZTVmYjBjYTl" +
        "iZmIzYWI0ODNkM2ZiYWY2OGVmMjlhNDJkZThjZWY3NGM1IgogICAgfQogIH0KfQ"

    private val frogMaskSkull = ItemUtils.createSkull(
        "§5Frog Mask",
        "0e9936a2-5609-385f-a5ef-ce38dfee8c91",
        FROG_MASK_TEXTURE,
    )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.frogMaskDisplayPos.renderRenderable(display, posLabel = "Frog Mask Display")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val now = SkyBlockTime.now()
        if (!isEnabled()) return

        if (event.isMod(2)) display = if (region.first != "") updateDisplay() else Renderable.verticalContainer(listOf())

        if (region.second.isInFuture()) return

        val helmet = InventoryUtils.getHelmet() ?: return
        if (helmet.displayName.removeColor() != "Frog Mask") return

        activeRegionPattern.matchMatcher(helmet.getLore()[11].removeColor()) {
            val nextRegion = group("region")
            val nextRegionTime = SkyBlockTime(year = now.year, month = now.month, day = now.day + 1).asTimeMark()

            region = nextRegion to nextRegionTime
        }
    }


    private fun updateDisplay(): Renderable {
        val until = region.second.timeUntil()
        val timeString = "${until.inWholeMinutes}m ${(until.inPartialSeconds % 60).toInt()}s"

        return Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(frogMaskSkull),
                Renderable.string(
                    "§5Frog Mask§6 - §a${region.first} §6for §e$timeString",
                ),
            ),
            spacing = 1,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    private fun isEnabled() = config.frogMaskDisplay && LorenzUtils.skyBlockIsland == IslandType.THE_PARK
}
