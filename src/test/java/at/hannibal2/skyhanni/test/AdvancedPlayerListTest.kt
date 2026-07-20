package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.hypixelapi.HypixelLocationApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList.CrimsonIsleFaction
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import com.google.gson.TypeAdapter
import io.mockk.every
import io.mockk.mockkObject
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AdvancedPlayerListTest {

    @Test
    fun checkPlayerDataMatches() {
        AdvancedPlayerList.newSorting(inputData)
        val generatedData = AdvancedPlayerList.playerData.values.toList()
        for (i in generatedData.indices) {
            val expected = playerData[i]
            val actual = generatedData[i]
            Assertions.assertEquals(expected.toString(), actual.toString(), "Mismatch at index $i")
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            mockkObject(SkyBlockUtils)
            every { SkyBlockUtils.inSkyBlock } returns true
            every { SkyBlockUtils.onHypixel } returns true
            every { SkyBlockUtils.currentIsland } returns IslandType.CRIMSON_ISLE

            mockkObject(HypixelLocationApi)
            every { HypixelLocationApi.inSkyblock } returns true

            @Suppress("UNCHECKED_CAST")
            adapter = SkyHanniTypeAdapters.COMPONENT.adapter as TypeAdapter<Component>
            createData()
        }

        private var inputData: List<Component> = emptyList()

        lateinit var adapter: TypeAdapter<Component>

        private var playerData: List<AdvancedPlayerList.PlayerData> = emptyList()

        @Suppress("LongMethod", "MaxLineLength")
        fun createData() {
            val rawData = listOf(
                """{"text":"","extra":["§8[§d333§8] §b","LoooonZ"," §7🐸"],"color":"aqua"}""",
                """{"text":"","extra":["§8[§c464§8] §b","IntenseEnderman"," §b🐸§7♲"],"color":"aqua"}""",
                """{"text":"","extra":["§8[§2199§8] §b","kaifktx"," §6§lᛝ"],"color":"aqua"}""",
                """{"text":"","extra":["§8[§5377§8] §b","Scoobag_"," §b♔"],"color":"aqua"}""",
                """{"text":"","extra":["§8[§f72§8] §b","Swedosh",{"text":"","color":"white"}],"color":"aqua"}""",
                """{"text":"","extra":["§8[§9314§8] §b","Namaser"," §7Σ"],"color":"aqua"}""",
                """{"text":"","extra":["§8[§e114§8] §a","valnus",{"text":"","color":"white"}],"color":"green"}""",
                """{"text":"","extra":["§8[§6400§8] §a","liron150"," §b§lᛝ§7♲"],"color":"green"}""",
                """{"text":"","extra":["§8[§a135§8] §a","gouroumaster"," §7☃"],"color":"green"}""",
                """{"text":"","extra":["§8[§a143§8] §7","angelstuff",{"text":"","color":"white"}],"color":"gray"}""",
                """{"text":"","extra":["§8[§a137§8] §7","Swaimz"," §7§l⚝"],"color":"gray"}""",

                """{"text":"","extra":[{"text":"[","color":"dark_gray"},{"text":"446","color":"red"},{"text":"] ","color":"dark_gray"},{"text":"Anrok ","color":"aqua"},{"text":"♫","color":"gold"},{"text":"ቾ","color":"dark_purple"}],"italic":false}""",
                """{"text":"","extra":[{"text":"[","color":"dark_gray"},{"text":"345","color":"light_purple"},{"text":"] ","color":"dark_gray"},{"text":"__Leafs__ ","color":"aqua"},{"text":"⸕","color":"gold","bold":true},{"text":"⚒","color":"red"}],"italic":false}""",
                """{"text":"","extra":[{"text":"[","color":"dark_gray"},{"text":"369","color":"dark_purple"},{"text":"] ","color":"dark_gray"},{"text":"Ant_e ","color":"gray"},{"text":"⛂","color":"gold"},{"text":"⚒","color":"red"}],"italic":false}"""
            )
            playerData = listOf(
                createPlayerData(
                    "§b".asComponent { append("LoooonZ") },
                    "§7🐸".asComponent(),
                    "§d333".asComponent(),
                    333
                ),

                createPlayerData(
                    "§b".asComponent { append("IntenseEnderman") },
                    "§b🐸§7♲".asComponent(),
                    "§c464".asComponent(),
                    464,
                    ironman = true
                ),

                createPlayerData(
                    "§b".asComponent { append("kaifktx") },
                    "§6§lᛝ".asComponent(),
                    "§2199".asComponent(),
                    199
                ),

                createPlayerData(
                    "§b".asComponent { append("Scoobag_") },
                    "§b♔".asComponent(),
                    "§5377".asComponent(),
                    377
                ),

                createPlayerData(
                    "§b".asComponent { append("Swedosh") },
                    "".asComponent(),
                    "§f72".asComponent(),
                    72
                ),

                createPlayerData(
                    "§b".asComponent { append("Namaser") },
                    "§7Σ".asComponent(),
                    "§9314".asComponent(),
                    314
                ),

                createPlayerData(
                    "§a".asComponent { append("valnus") },
                    "".asComponent(),
                    "§e114".asComponent(),
                    114
                ),

                createPlayerData(
                    "§a".asComponent { append("liron150") },
                    "§b§lᛝ§7♲".asComponent(),
                    "§6400".asComponent(),
                    400,
                    ironman = true
                ),

                createPlayerData(
                    "§a".asComponent { append("gouroumaster") },
                    "§7☃".asComponent(),
                    "§a135".asComponent(),
                    135
                ),

                createPlayerData(
                    "§7".asComponent { append("angelstuff") },
                    "".asComponent(),
                    "§a143".asComponent(),
                    143
                ),

                createPlayerData(
                    "§7".asComponent { append("Swaimz") },
                    "§7§l⚝".asComponent(),
                    "§a137".asComponent(),
                    137
                ),

                createPlayerData(
                    componentBuilder { appendWithColor("Anrok", ChatFormatting.AQUA) },
                    componentBuilder { appendWithColor("♫", ChatFormatting.GOLD) },
                    componentBuilder { appendWithColor("446", ChatFormatting.RED) },
                    446,
                    faction = CrimsonIsleFaction.MAGE
                ),

                createPlayerData(
                    componentBuilder { appendWithColor("__Leafs__", ChatFormatting.AQUA) },
                    componentBuilder { appendWithColor("⸕", ChatFormatting.GOLD) { bold = true } },
                    componentBuilder { appendWithColor("345", ChatFormatting.LIGHT_PURPLE) },
                    345,
                    faction = CrimsonIsleFaction.BARBARIAN
                ),

                createPlayerData(
                    componentBuilder { appendWithColor("Ant_e", ChatFormatting.GRAY) },
                    componentBuilder { appendWithColor("⛂", ChatFormatting.GOLD) },
                    componentBuilder { appendWithColor("369", ChatFormatting.DARK_PURPLE) },
                    369,
                    faction = CrimsonIsleFaction.BARBARIAN
                ),
            )
            val componentData: MutableList<Component> = mutableListOf()
            componentData.add(Component.literal(""))
            componentData.addAll(rawData.map { adapter.fromJson(it) ?: Component.literal("Invalid") })
            inputData = componentData
        }

        fun createPlayerData(
            coloredName: Component,
            nameSuffix: Component,
            levelText: Component,
            sbLevel: Int,
            ironman: Boolean = false,
            bingoLevel: Int? = null,
            faction: CrimsonIsleFaction = CrimsonIsleFaction.NONE,
        ): AdvancedPlayerList.PlayerData {

            val data = AdvancedPlayerList.PlayerData(sbLevel)
            data.name = coloredName.string.removeColor()
            data.coloredName = coloredName.intoSpan()
            data.nameSuffix = nameSuffix.intoSpan()
            data.levelText = levelText.intoSpan()
            data.ironman = ironman
            data.bingoLevel = bingoLevel
            data.faction = faction
            return data
        }
    }
}
