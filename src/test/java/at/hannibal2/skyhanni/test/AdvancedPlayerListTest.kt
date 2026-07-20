package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList.CrimsonIsleFaction
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import com.google.gson.TypeAdapter
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
            @Suppress("UNCHECKED_CAST")
            adapter = SkyHanniTypeAdapters.COMPONENT.adapter as TypeAdapter<Component>
            createData()
        }

        private var inputData: List<Component> = emptyList()

        lateinit var adapter: TypeAdapter<Component>

        private var playerData: List<AdvancedPlayerList.PlayerData> = emptyList()

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
                """{"text":"","extra":["§8[§a137§8] §7","Swaimz"," §7§l⚝"],"color":"gray"}"""
            )
            playerData = listOf(
                createPlayerData(
                    Component.literal("§b").append(Component.literal("LoooonZ")),
                    Component.literal("§7🐸"),
                    Component.literal("§d333"),
                    333
                ),

                createPlayerData(
                    Component.literal("§b").append(Component.literal("IntenseEnderman")),
                    Component.literal("§b🐸§7♲"),
                    Component.literal("§c464"),
                    464,
                    ironman = true
                ),

                createPlayerData(
                    Component.literal("§b").append(Component.literal("kaifktx")),
                    Component.literal("§6§lᛝ"),
                    Component.literal("§2199"),
                    199
                ),

                createPlayerData(
                    Component.literal("§b").append(Component.literal("Scoobag_")),
                    Component.literal("§b♔"),
                    Component.literal("§5377"),
                    377
                ),

                createPlayerData(
                    Component.literal("§b").append(Component.literal("Swedosh")),
                    Component.literal(""),
                    Component.literal("§f72"),
                    72
                ),

                createPlayerData(
                    Component.literal("§b").append(Component.literal("Namaser")),
                    Component.literal("§7Σ"),
                    Component.literal("§9314"),
                    314
                ),

                createPlayerData(
                    Component.literal("§a").append(Component.literal("valnus")),
                    Component.literal(""),
                    Component.literal("§e114"),
                    114
                ),

                createPlayerData(
                    Component.literal("§a").append(Component.literal("liron150")),
                    Component.literal("§b§lᛝ§7♲"),
                    Component.literal("§6400"),
                    400,
                    ironman = true
                ),

                createPlayerData(
                    Component.literal("§a").append(Component.literal("gouroumaster")),
                    Component.literal("§7☃"),
                    Component.literal("§a135"),
                    135
                ),

                createPlayerData(
                    Component.literal("§7").append(Component.literal("angelstuff")),
                    Component.literal(""),
                    Component.literal("§a143"),
                    143
                ),

                createPlayerData(
                    Component.literal("§7").append(Component.literal("Swaimz")),
                    Component.literal("§7§l⚝"),
                    Component.literal("§a137"),
                    137
                )
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
