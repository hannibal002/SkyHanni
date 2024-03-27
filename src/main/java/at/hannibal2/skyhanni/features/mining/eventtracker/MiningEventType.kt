package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private fun Item.toItemStack(meta: Int = 0): ItemStack = ItemStack(this, 1, meta)

enum class MiningEventType(
    val eventName: String,
    private val shortName: String,
    val defaultLength: Duration,
    private val colourCode: Char,
    val dwarvenSpecific: Boolean,
    iconInput: Renderable
) {
    GONE_WITH_THE_WIND(
        "GONE WITH THE WIND", "Wind", 18.minutes, '9', false, Items.feather.toItemStack()
    ),
    DOUBLE_POWDER("2X POWDER", "2x", 15.minutes, 'b', false,
        object : Renderable {
            override val width = 12
            override val height = 10
            override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
            override val verticalAlign = RenderUtils.VerticalAlignment.TOP

            val dyeGreen = Renderable.itemStack(Items.dye.toItemStack(10), 10.0 / 12.0)
            val dyePink = Renderable.itemStack(Items.dye.toItemStack(9), 10.0 / 12.0)

            override fun render(posX: Int, posY: Int) {
                GlStateManager.translate(1f, -1.5f, 0f)
                dyePink.render(posX + 1, posY - 1)
                GlStateManager.translate(-2f, 1.5f, 0f)
                dyeGreen.render(posX, posY)
                GlStateManager.translate(1f, 0f, 0f)
            }

        }
    ),
    GOBLIN_RAID(
        "GOBLIN RAID",
        "Raid",
        5.minutes,
        'c',
        true,
        ItemUtils.createSkull(
            "Goblin",
            "32518c29-6127-3c71-b2a7-be4c3251e76f",
            "ewogICJ0aW1lc3RhbXAiIDogMTYwNzQ2NDg4MTMwOCwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTcyODUwOTA2YjdmMGQ5NTJjMGU1MDgwNzNjYzQzOWZkMzM3NGNjZjViODg5YzA2ZjdlOGQ5MGNjMGNjMjU1YyIKICAgIH0KICB9Cn0="
        )
    ),
    BETTER_TOGETHER("BETTER TOGETHER", "Better", 18.minutes, 'd', false, object : Renderable {
        override val width = 12
        override val height = 10
        override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
        override val verticalAlign = RenderUtils.VerticalAlignment.TOP

        val steveHead = Renderable.itemStack(Items.skull.toItemStack(3), 8.0 / 12.0)
        val pickaxe = Renderable.itemStack(Items.iron_pickaxe.toItemStack(9), 12.0 / 12.0)

        override fun render(posX: Int, posY: Int) {
            GlStateManager.translate(-2f, -1f, 0f)
            steveHead.render(posX, posY)
            GlStateManager.translate(+4f, +3f, 0f)
            steveHead.render(posX, posY)

            GlStateManager.translate(-2f, -2f, 2f)
            pickaxe.render(posX, posY)
            GlStateManager.translate(0f, 0f, -2f)
        }

    }),
    RAFFLE(
        "RAFFLE",
        "Raffle",
        160.seconds,
        '6',
        true,
        Items.name_tag.toItemStack()
    ),
    MITHRIL_GOURMAND("MITHRIL GOURMAND", "Gourmand", 10.minutes, 'b', true, Items.dye.toItemStack(6)), ;

    constructor(
        eventName: String,
        shortName: String,
        defaultLength: Duration,
        colourCode: Char,
        dwarvenSpecific: Boolean,
        iconInput: ItemStack
    ) : this(
        eventName, shortName, defaultLength, colourCode, dwarvenSpecific, Renderable.itemStack(
            iconInput
        )
    )

    val icon = Renderable.hoverTips(iconInput, listOf(eventName))
    val compactText = Renderable.string("§$colourCode$shortName")
    val normalText = Renderable.string("§$colourCode$eventName")

    override fun toString() = when (config.compressedFormat) {
        CompressFormat.COMPACT_TEXT -> "§$colourCode$shortName"
        CompressFormat.ICONS -> "WRONG VALUE"
        else -> "§$colourCode$eventName"
    }

    fun toPastString() = when (config.compressedFormat) {
        CompressFormat.COMPACT_TEXT -> "§7$shortName"
        CompressFormat.ICONS -> TODO()
        else -> "§7$eventName"
    }

    companion object {
        private val config get() = SkyHanniMod.feature.mining.miningEvent

        enum class CompressFormat {
            NONE, COMPACT_TEXT, ICONS;

            override fun toString(): String {
                return name.lowercase().allLettersFirstUppercase()
            }
        }

        fun fromEventName(bossbarName: String): MiningEventType? {
            return MiningEventType.entries.find { it.eventName == bossbarName.removeColor() }
        }
    }
}
