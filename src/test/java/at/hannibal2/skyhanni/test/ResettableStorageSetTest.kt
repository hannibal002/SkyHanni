package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ResettableStorageSetTest {

    data class TestStorage(
        var string: String = "default",
        var int: Int = 42,
        val staticString: String = "static",
        val staticInt: Int = 100,
        var nullString: String? = null,
        var nullInt: Int? = null,
        @Transient var transientString: String = "transient",
        val defaultBool: Boolean = false,
    ) : ResettableStorageSet() {
        val list: MutableList<String> = mutableListOf()
        val map: MutableMap<String, Int> = mutableMapOf()
        @Transient val transientList: MutableList<String> = mutableListOf("transientListItem")

        val intGetter: Int get() = staticInt + 1
    }

    @Test
    fun testAbstractSet() {
        val storage = TestStorage(
            string = "changed",
            int = 100,
            nullString = "notNull",
            nullInt = 99,
            transientString = "transient_changed",
            defaultBool = true,
        ).apply {
            list.add("item1")
            list.add("item2")
            map["key1"] = 1
            map["key2"] = 2
        }

        storage.reset()

        Assertions.assertEquals(
            "default",
            storage.string,
            "String property should reset to default value"
        )

        Assertions.assertEquals(
            42,
            storage.int,
            "Int property should reset to default value"
        )

        Assertions.assertEquals(
            "static",
            storage.staticString,
            "Static String property should not change"
        )

        Assertions.assertEquals(
            100,
            storage.staticInt,
            "Static Int property should not change"
        )

        Assertions.assertNull(
            storage.nullString,
            "Nullable String property should be reset to null"
        )

        Assertions.assertNull(
            storage.nullInt,
            "Nullable Int property should be reset to null"
        )

        Assertions.assertEquals(
            "transient_changed",
            storage.transientString,
            "Transient property should not be reset"
        )

        Assertions.assertTrue(
            storage.defaultBool,
            "Default boolean property should not be reset"
        )

        Assertions.assertTrue(
            storage.list.isEmpty(),
            "List property should be cleared"
        )

        Assertions.assertTrue(
            storage.map.isEmpty(),
            "Map property should be cleared"
        )

        Assertions.assertTrue(
            storage.transientList.isNotEmpty(),
            "Transient list should not be cleared"
        )

    }

    @Test
    fun testRealSet() {
        mockkObject(ChatUtils)
        every { ChatUtils.chat(any<String>()) } just Runs
        every { ChatUtils.chat(any<String>(), any()) } just Runs

        val h = HoppityApi.HoppityStateDataSet()
        h.hoppityMessages.add("item1")
        h.hoppityMessages.add("item2")
        h.duplicate = true
        h.lastRarity = LorenzRarity.MYTHIC
        h.lastName = "TestHoppity"
        h.lastProfit = "test string"
        h.lastMeal = HoppityEggType.BREAKFAST
        h.lastDuplicateAmount = 5

        h.reset()


        Assertions.assertTrue(
            h.hoppityMessages.isEmpty(),
            "Hoppity messages should be cleared"
        )

        Assertions.assertFalse(
            h.duplicate,
            "Duplicate flag should be reset to false"
        )

        Assertions.assertNull(
            h.lastRarity,
            "Last rarity should be reset to null"
        )

        Assertions.assertEquals(
            "",
            h.lastName,
            "Last name should be reset to empty string"
        )

        Assertions.assertEquals(
            "",
            h.lastProfit,
            "Last profit should be reset to empty string"
        )

        Assertions.assertNull(
            h.lastMeal,
            "Last meal should be reset to null"
        )

        Assertions.assertNull(
            h.lastDuplicateAmount,
            "Last duplicate amount should be reset to null"
        )
    }
}
