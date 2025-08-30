package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.DynamicTextLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawMultiLineDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereInWorld
import java.awt.Color

@SkyHanniModule
object TestDynamicText {

    private val texts: MutableList<DynamicText> = mutableListOf()

//     private const val ANGLETHRESHOLD: Int = 30

    private interface DynamicText {
        fun draw(event: SkyHanniRenderWorldEvent)
    }

    private class SingleDynamicText(
        val location: LorenzVec,
        val text: String,
        val scaleMultiplier: Double,
    ) : DynamicText {
        override fun draw(event: SkyHanniRenderWorldEvent) {
            event.drawDynamicText(
                location,
                text,
                scaleMultiplier,
            )
        }
    }

    private class MultiLineDynamicText(
        val location: LorenzVec,
        val lines: List<DynamicTextLine>,
    ) : DynamicText {
        override fun draw(event: SkyHanniRenderWorldEvent) {
            event.drawMultiLineDynamicText(
                location,
                lines,
                anchoredLineIndex = 0,
                blockCenter = true,
            )
            event.drawSphereInWorld(
                Color.RED,
                location + LorenzVec(0.5, 0.5, 0.5),
                0.02F,
            )
//             event.drawWaypointFilled(
//                 location,
//                 Color.YELLOW,
//             )
        }
    }

    private fun testSingleLine() {
        texts.add(
            SingleDynamicText(GetCoordinates.inFront(), "Test in Front x1.0", 1.0),
        )
    }

    private fun testMultiLine() {
        // TODO: make sure it turns around the right point
        texts.add(
            MultiLineDynamicText(
                GetCoordinates.inFront(),
                listOf(
                    DynamicTextLine("topline x1.5", 1.5),
                    DynamicTextLine("second line x1.0", 1.0),
                    DynamicTextLine("second line x1.0", 1.0),
                    DynamicTextLine("topline x1.5", 1.5),
                    DynamicTextLine("second line x1.0", 1.0),
                    DynamicTextLine("second line x1.0", 1.0),
                ),
            ),
        )
    }

    private fun test1() {
        println("test1 called")
        testSingleLine()
    }

    private fun test2() {
        println("test2 called")
        testMultiLine()
    }

    private fun test3() {
        println("test3 called")
        texts.add(
            MultiLineDynamicText(
                GetCoordinates.inFront(),
                buildList {
                    add(DynamicTextLine("topline x1.5", 1.5))
                    for (i in 0..10) add(DynamicTextLine("second line x1.0", 1.0))
                }.toList(),
            ),
        )
    }

    private fun test4() {
        println("test4 called")

    }

    private fun test5() {
        println("test5 called")

    }

    private fun clearTests() {
        println("${texts.size} tests cleared")
        texts.clear()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        texts.forEach { it.draw(event) }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shtestdynamictext1") {
            description = "test dynamic text 1"
            category = CommandCategory.DEVELOPER_TEST
            callback { test1() }
        }
        event.register("shtestdynamictext2") {
            description = "test dynamic text 2"
            category = CommandCategory.DEVELOPER_TEST
            callback { test2() }
        }
        event.register("shtestdynamictext3") {
            description = "test dynamic text 3"
            category = CommandCategory.DEVELOPER_TEST
            callback { test3() }
        }
        event.register("shtestdynamictext4") {
            description = "test dynamic text 4"
            category = CommandCategory.DEVELOPER_TEST
            callback { test4() }
        }
        event.register("shtestdynamictext5") {
            description = "test dynamic text 5"
            category = CommandCategory.DEVELOPER_TEST
            callback { test5() }
        }
        event.register("shtestdynamictextclear") {
            description = "clear all dynamic text tests"
            category = CommandCategory.DEVELOPER_TEST
            callback { clearTests() }
        }
    }

    object GetCoordinates {
        fun inFront(): LorenzVec {
            val location = LocationUtils.playerLocation() + LorenzVec(5, 0, 0)
            println("returned $location")
            return location
        }

        fun down(): LorenzVec {
            val location = LocationUtils.playerLocation() + LorenzVec(0, -10, 0)
            println("returned $location")
            return location
        }
    }
}
