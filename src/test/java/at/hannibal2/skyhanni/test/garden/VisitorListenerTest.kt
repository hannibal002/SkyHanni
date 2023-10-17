package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorListener
import at.hannibal2.skyhanni.utils.LorenzUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VisitorListenerTest {
    private lateinit var listener: VisitorListener

    @BeforeEach
    fun setUp() {
        mockkObject(GardenAPI)
        every { GardenAPI.inGarden() } returns true

        mockkObject(LorenzUtils)
        every { LorenzUtils.getPlayerName() } returns "ThePlayerName"

        mockkObject(VisitorAPI)
        every { VisitorAPI.addVisitor(any()) } returns true

        listener = VisitorListener()
    }

    @Test
    fun `onTablistUpdate it should add new visitors to the list`() {
        listener.onTabListUpdate(
            TabListUpdateEvent(
                mutableListOf(
                    "§b§lVisitors:",
                    "§cSpaceman",
                    "§cGrandma Wolf",
                    "ThePlayerName",
                    "Jacob",
                    "",
                )
            )
        )

        verify { VisitorAPI.addVisitor("§fJacob") }
        verify { VisitorAPI.addVisitor("§cSpaceman") }
        verify { VisitorAPI.addVisitor("§cGrandma Wolf") }
    }

    @Test
    fun `onTablistUpdate it should remove visitors from the list`() {
        every { VisitorAPI.getVisitors() } returns listOf(
            mockk { every { visitorName } returns "§fJacob" },
        )

        listener.onTabListUpdate(
            TabListUpdateEvent(
                mutableListOf("§b§lVisitors:", "")
            )
        )

        verify { VisitorAPI.removeVisitor("§fJacob") }
    }

    @Test
    fun `onTablistUpdate it should not remove visitors if the timeout is not hit`() {
        every { VisitorAPI.getVisitors() } returns listOf(
            mockk { every { visitorName } returns "§fJacob" },
        )

        every { LorenzUtils.lastWorldSwitch } returns System.currentTimeMillis()

        listener.onTabListUpdate(
            TabListUpdateEvent(
                mutableListOf("§b§lVisitors:", "")
            )
        )

        verify(exactly = 0) { VisitorAPI.removeVisitor("§fJacob") }
    }
}