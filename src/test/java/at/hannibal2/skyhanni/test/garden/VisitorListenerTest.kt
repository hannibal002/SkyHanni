package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.garden.visitor.VisitorListener
import at.hannibal2.skyhanni.test.BootstrapExtension
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BootstrapExtension::class)
class VisitorListenerTest {
    private lateinit var listener: VisitorListener

    @BeforeEach
    fun setUp() {
        mockkObject(PlayerUtils)
        mockkObject(SkyBlockUtils)
        every { PlayerUtils.getName() } returns "ThePlayerName"

        mockkObject(VisitorApi)
        every { VisitorApi.addVisitor(any()) } returns true
        every { VisitorApi.visitorsInTabList(any()) } returns emptyList()

        listener = VisitorListener
    }

    @Test
    fun `onTablistUpdate it should add new visitors to the list`() {
        every { VisitorApi.visitorsInTabList(any()) } returns listOf("§cSpaceman", "§6Madame Eleanor Q. Goldsworth III", "§fJacob")

        listener.onWidgetUpdate(fakeTabWidget(mutableListOf("")))

        verify { VisitorApi.addVisitor("§fJacob") }
        verify { VisitorApi.addVisitor("§cSpaceman") }
        verify { VisitorApi.addVisitor("§6Madame Eleanor Q. Goldsworth III") }
    }

    @Test
    fun `onTablistUpdate it should remove visitors from the list`() {
        every { VisitorApi.getVisitors() } returns listOf(
            mockk { every { visitorName } returns "§fJacob" },
        )

        listener.onWidgetUpdate(
            fakeTabWidget(
                mutableListOf("§b§lVisitors: §r§f(0)", ""),
            ),
        )

        verify { VisitorApi.removeVisitor("§fJacob") }
    }

    @Test
    fun `onTablistUpdate it should not remove visitors if the timeout is not hit`() {
        every { VisitorApi.getVisitors() } returns listOf(
            mockk { every { visitorName } returns "§fJacob" },
        )

        every { SkyBlockUtils.lastWorldSwitch } returns SimpleTimeMark.now()

        listener.onWidgetUpdate(
            fakeTabWidget(
                mutableListOf("§b§lVisitors: §r§f(0)", ""),
            ),
        )

        verify(exactly = 0) { VisitorApi.removeVisitor("§fJacob") }
    }

    private fun fakeTabWidget(lines: List<String>): WidgetUpdateEvent {
        return WidgetUpdateEvent(TabWidget.VISITORS, lines.map { Component.literal(it) })
    }
}
