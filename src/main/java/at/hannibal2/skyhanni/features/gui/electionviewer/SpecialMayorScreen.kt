package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.MayorAPI.ELECTION_END_DAY
import at.hannibal2.skyhanni.data.MayorAPI.ELECTION_END_MONTH
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.ZoneId

@SkyHanniModule
object SpecialMayorScreen : ElectionViewerScreen() {

    override val posLabel = "Election Viewer - Special Mayor"

    @SubscribeEvent
    override fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return

        ElectionViewerUtils.getNextSpecialMayors(SkyBlockTime.now().year).let { nextMayors ->
            display = Renderable.verticalContainer(
                listOf(
                    Renderable.string("Next Mayors", horizontalAlign = HorizontalAlignment.CENTER),
                    Renderable.string(
                        "The next three special mayors and the year they will (most likely) be elected",
                        horizontalAlign = HorizontalAlignment.CENTER,
                    ),
                    Renderable.horizontalContainer(
                        nextMayors.map { (name, year) ->
                            val electionTime = SkyBlockTime(year, ELECTION_END_MONTH, day = ELECTION_END_DAY).asTimeMark()
                            val color = MayorAPI.mayorNameToColorCode(name)
                            Renderable.horizontalContainer(
                                listOf(
                                    ElectionViewerUtils.getFakeMayor(Mayor.getMayorFromName(name) ?: Mayor.UNKNOWN),
                                    Renderable.verticalContainer(
                                        listOf(
                                            "$color$name",
                                            "in year $color$year",
                                            "Election in: $color${electionTime.timeUntil().format(maxUnits = 2)}",
                                            "Election at: $color${electionTime.formattedDate("EEEE, MMM d h:mm a")}",
                                        ).map { Renderable.wrappedString(it, 130) },
                                        spacing = 5,
                                        verticalAlign = VerticalAlignment.CENTER,
                                    ),
                                ),
                                spacing = 10,
                            )
                        },
                        horizontalAlign = HorizontalAlignment.CENTER,
                        spacing = 10,
                    ),
                ),
                spacing = 20,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
        }

    }

    override fun isInGui() = Minecraft.getMinecraft().currentScreen is SpecialMayorScreen
}
