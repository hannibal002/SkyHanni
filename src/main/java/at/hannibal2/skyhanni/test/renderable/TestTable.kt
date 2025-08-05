package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.ScrollTable.Companion.scrollTable
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableTable.Companion.searchableTable
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color

@SkyHanniModule(devOnly = true)
object TestTable : RenderableTestSuite.TestRenderable("table") {

    private val scrollValues = mutableMapOf<Int, ScrollValue>()
    private val textInput = TextInput()

    @Suppress("LongMethod")
    override fun renderable(): Renderable = with(Renderable) {
        val header = listOf(
            text("I'm a Header", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
            text("Test Column", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
            text("Search Colum", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
            text("Last Column", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
        )
        val input = mapOf<List<Renderable>, String>(
            listOf(
                text("Normal", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
                empty(),
                text("not searchable").renderBounds(),
                item(
                    "BOX_OF_SEEDS".toInternalName(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ).renderBounds(),
            ) to "",
            listOf(
                text(
                    "Funky Spaces",
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.TOP,
                ).renderBounds(),
                vertical(text("Sub Row 1"), text("Sub Row 2"), text("Sub Row 3")).renderBounds(),
                text("searchable: Spaces", verticalAlign = RenderUtils.VerticalAlignment.BOTTOM).renderBounds(),
                item("BOX_OF_SEEDS".toInternalName(), verticalAlign = RenderUtils.VerticalAlignment.CENTER).renderBounds(),
            ) to "Spaces",
            listOf(
                text("Hoverable", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
                hoverTips("Hover Here!", listOf("You see me!")).renderBounds(),
                text("searchable: Hover").renderBounds(),
                hoverTips(
                    item(
                        "BOX_OF_SEEDS".toInternalName(),
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
                    ),
                    listOf("I'm a secret"),
                ).renderBounds(),
            ) to "Hover",
            listOf(
                text(
                    "Reverse Funky Spaces",
                    horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
                    verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
                ).renderBounds(),
                vertical(
                    text("Sub Row 1"),
                    text("Sub Row 2"),
                    text("Sub Row 3"),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ).renderBounds(),
                text("searchable: Spaces", verticalAlign = RenderUtils.VerticalAlignment.TOP).renderBounds(),
                item("BOX_OF_SEEDS".toInternalName(), verticalAlign = RenderUtils.VerticalAlignment.CENTER).renderBounds(),
            ) to "Spaces",
            listOf(
                text(
                    "Even more Funky Spaces",
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ).renderBounds(),
                vertical(
                    text("Sub Row 1 ____________"),
                    text("Sub Row 2", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                    text("Sub Row 3", horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT),
                    horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
                ).renderBounds(),
                text("searchable: Spaces", verticalAlign = RenderUtils.VerticalAlignment.TOP).renderBounds(),
                item(
                    "BOX_OF_SEEDS".toInternalName(),
                    verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
                    horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
                ).renderBounds(),
            ) to "Spaces",
            listOf(
                text("Again Normal", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
                empty(),
                text("not searchable").renderBounds(),
                item(
                    "BOX_OF_SEEDS".toInternalName(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ).renderBounds(),
            ) to "",
            listOf(
                text("Searchable", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).renderBounds(),
                text("I'm full right!", horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT).renderBounds(),
                text("searchable: Search", verticalAlign = RenderUtils.VerticalAlignment.BOTTOM).renderBounds(),
                item("BOX_OF_SEEDS".toInternalName()).renderBounds(),
            ) to "Search",
        )
        val inputWithHeader = listOf(header) + input.keys

        val tables = mapOf(
            "Normal" to table(
                content = inputWithHeader,
                xSpacing = 8,
                ySpacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ),
            "Normal with use Space" to table(
                content = inputWithHeader,
                xSpacing = 8,
                ySpacing = 2,
                useEmptySpace = true,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ),
            "Searchable" to searchableTable(
                header = header,
                content = input,
                textInput = textInput,
                key = 1,
                xSpacing = 8,
                ySpacing = 2,
            ),
            "Searchable with use Space" to searchableTable(
                header = header,
                content = input,
                textInput = textInput,
                key = 2,
                xSpacing = 8,
                ySpacing = 2,
                useEmptySpace = true,
            ),
            "Scroll without Header" to scrollTable(
                content = input.keys.toList(),
                height = 50,
                scrollValue = scrollValues.getOrPut(1) { ScrollValue() },
                xSpacing = 8,
                ySpacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ),
            "Scroll with Header" to scrollTable(
                content = input.keys.toList(),
                height = 50,
                scrollValue = scrollValues.getOrPut(2) { ScrollValue() },
                header = header,
                xSpacing = 8,
                ySpacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ),
            "Searchable Scroll" to searchableScrollTable(
                content = input,
                height = 50,
                scrollValue = scrollValues.getOrPut(3) { ScrollValue() },
                key = 3,
                textInput = textInput,
                xSpacing = 8,
                ySpacing = 2,
            ),
            "Searchable Scroll with Header" to searchableScrollTable(
                content = input,
                height = 50,
                scrollValue = scrollValues.getOrPut(4) { ScrollValue() },
                key = 4,
                textInput = textInput,
                header = header,
                xSpacing = 8,
                ySpacing = 2,
            ),
            "Searchable Scroll and hints" to searchableScrollTable(
                content = input,
                height = 60,
                scrollValue = scrollValues.getOrPut(5) { ScrollValue() },
                key = 5,
                textInput = textInput,
                xSpacing = 8,
                ySpacing = 2,
                showScrollableTipsInList = true
            ),
            "Searchable Scroll with Header and hints" to searchableScrollTable(
                content = input,
                height = 70,
                scrollValue = scrollValues.getOrPut(6) { ScrollValue() },
                key = 6,
                textInput = textInput,
                header = header,
                xSpacing = 8,
                ySpacing = 2,
                showScrollableTipsInList = true
            ),
        )


        searchBox(
            content = table(
                tables.map {
                    vertical(
                        text("§l§6" + it.key, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                        it.value.renderBounds(Color.WHITE.addAlpha(50)),
                    )
                }.chunked(2),
                xSpacing = 6,
                ySpacing = 2,
            ),
            onUpdateSize = {},
            textInput = textInput,
            hideIfNoText = false,
            searchPrefix = "Type anywhere for Search in all tables, search: ",
            scale = 1.1,
        )
    }
}
