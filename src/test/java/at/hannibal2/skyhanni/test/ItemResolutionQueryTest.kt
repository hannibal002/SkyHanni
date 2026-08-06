package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.TreeMap

class ItemResolutionQueryTest {
    companion object {
        /**
         * Mimics how [EnoughUpdatesManager] fills its item map and title word map while reading the NEU repo.
         */
        private fun registerItem(internalName: String, displayName: String) {
            val name = internalName.toInternalName()
            EnoughUpdatesManager.getItemInformation()[name] = NeuItemJson(
                itemId = "minecraft:enchanted_book",
                displayName = displayName,
                nbtTagAny = "",
                internalName = name,
            )
            for ((index, part) in displayName.split(" ").withIndex()) {
                EnoughUpdatesManager.titleWordMap.getOrPut(part.cleanString()) { TreeMap() }
                    .getOrPut(name.asString()) { mutableListOf() }.add(index)
            }
        }

        @JvmStatic
        @BeforeAll
        fun registerItems() {
            // Every enchanted book in the NEU repo shares the same display name
            registerItem("KARMA;1", "§fEnchanted Book")
            registerItem("PALEONTOLOGIST;5", "§fEnchanted Book")
            registerItem("HYPERION", "§dHyperion")
        }
    }

    /** All arguments have to be matched explicitly, otherwise the default arguments are matched by value. */
    private fun MockKMatcherScope.anyErrorState() = ErrorManager.logErrorStateWithData(
        any(),
        any(),
        *anyVararg(),
        ignoreErrorCache = any(),
        noStackTrace = any(),
        betaOnly = any(),
        condition = any(),
    )

    @BeforeEach
    fun setUp() {
        // Logging an error needs a running Minecraft client
        mockkObject(ErrorManager)
        every { anyErrorState() } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(ErrorManager)
    }

    @Test
    fun testAmbiguousDisplayNameIsNotResolved() {
        assertNull(ItemResolutionQuery.findInternalNameByDisplayName("§fEnchanted Book"))
        assertNull(ItemResolutionQuery.findInternalNameByDisplayName("§fEnchanted Book", mayBeMangled = true))
        verify { anyErrorState() }
    }

    @Test
    fun testUniqueDisplayNameIsResolved() {
        assertEquals(
            "HYPERION".toInternalName(),
            ItemResolutionQuery.findInternalNameByDisplayName("§dHyperion"),
        )
    }
}
