package at.hannibal2.skyhanni.test.features.minion

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.SkyblockItemsDataJson
import at.hannibal2.skyhanni.features.inventory.bazaar.HypixelItemApi
import at.hannibal2.skyhanni.features.minion.MinionXP
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.json.fromJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HypixelItemApiTest {
    @Test
    fun `extracts minion storage xp from Hypixel item data`() {
        val itemsData = ConfigManager.gson.fromJson<SkyblockItemsDataJson>(ITEMS_RESPONSE)
        HypixelItemApi.processItemData(itemsData)

        assertEquals(
            mapOf("fishing" to 20480.0),
            HypixelItemApi.getMinionStorageXP("CONDENSED_WATER_LILY".toInternalName()),
        )
        assertNull(HypixelItemApi.getMinionStorageXP("NO_EXPERIENCE".toInternalName()))
        assertNull(HypixelItemApi.getMinionStorageXP("NO_MINION_STORAGE".toInternalName()))
        assertEquals(
            mapOf("farming" to 2.0, "mining" to 3.0),
            HypixelItemApi.getMinionStorageXP("MULTI_SKILL".toInternalName()),
        )
        assertEquals(
            mapOf("mystery" to 4.0),
            HypixelItemApi.getMinionStorageXP("UNKNOWN_SKILL".toInternalName()),
        )
    }

    @Test
    fun `calculates every known skill and ignores unknown skills`() {
        assertEquals(
            listOf(
                MinionXP.XPInfo(SkillType.FARMING, 6.0),
                MinionXP.XPInfo(SkillType.MINING, 9.0),
            ),
            MinionXP.calculateXPInfo(
                mapOf("farming" to 2.0, "mining" to 3.0, "mystery" to 4.0),
                itemAmount = 2,
                multiplier = 1.5,
            ),
        )
    }

    companion object {
        private val ITEMS_RESPONSE = """
            {
              "items": [
                {
                  "material": "PAPER",
                  "item_model": "hypixel_skyblock:item/collections/lily_pad/condensed_lily_pad",
                  "name": "Condensed Lily Pad",
                  "tier": "RARE",
                  "npc_sell_price": 256000,
                  "description": "%%gray%%A truly massive amount of Lily Pads mashed into a large ball.",
                  "components": [
                    {
                      "type": "RECIPE_INGREDIENT"
                    }
                  ],
                  "experience": {
                    "fishing": {
                      "MINION_STORAGE": 20480.0
                    }
                  },
                  "id": "CONDENSED_WATER_LILY"
                },
                {
                  "id": "NO_EXPERIENCE"
                },
                {
                  "experience": {
                    "fishing": {
                      "OTHER_SOURCE": 10.0
                    }
                  },
                  "id": "NO_MINION_STORAGE"
                },
                {
                  "experience": {
                    "farming": {
                      "MINION_STORAGE": 2.0
                    },
                    "mining": {
                      "MINION_STORAGE": 3.0
                    }
                  },
                  "id": "MULTI_SKILL"
                },
                {
                  "experience": {
                    "mystery": {
                      "MINION_STORAGE": 4.0
                    }
                  },
                  "id": "UNKNOWN_SKILL"
                }
              ]
            }
        """.trimIndent()
    }
}
