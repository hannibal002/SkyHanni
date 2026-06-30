package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.overrideId
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.world.item.Items
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private fun createPlayerHead(): SafeItemStack = SafeItemStack(Items.PLAYER_HEAD)

// Todo de-duplicate with MiningEventType in data
enum class MiningEventType(
    val eventName: String,
    shortName: String,
    val defaultLength: Duration,
    color: LorenzColor,
    val dwarvenSpecific: Boolean,
    iconInput: Renderable,
    var itemStack: SafeItemStack? = null,
) {
    GONE_WITH_THE_WIND(
        "GONE WITH THE WIND", "Wind", 18.minutes, LorenzColor.BLUE, false,
        object : Renderable {
            override val width = 10
            override val height = 10
            override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
            override val verticalAlign = RenderUtils.VerticalAlignment.CENTER

            val compass by lazy {
                Renderable.item(SafeItemStack(Items.COMPASS)) { scale = 0.45 }
            }
            val wind = Renderable.text("§9≈", scale = 0.75)

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                DrawContextUtils.translate(1f, 1f)
                compass.render(mouseOffsetX, mouseOffsetY)
                DrawContextUtils.translate(-1f, -2f)
                wind.render(mouseOffsetX, mouseOffsetY)
                DrawContextUtils.translate(0f, 1f)
            }
        },
    ),
    DOUBLE_POWDER(
        "2X POWDER", "2x", 15.minutes, LorenzColor.AQUA, false,
        object : Renderable {
            override val width = 10
            override val height = 10
            override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
            override val verticalAlign = RenderUtils.VerticalAlignment.CENTER

            val dyeGreen by lazy {
                Renderable.item(DyeCompat.LIME.createStack()) { scale = 0.45 }
            }
            val dyePink by lazy {
                Renderable.item(DyeCompat.PINK.createStack()) { scale = 0.45 }
            }

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                DrawContextUtils.translate(1f, 0f)
                dyePink.render(mouseOffsetX + 1, mouseOffsetY - 1)
                DrawContextUtils.translate(-2f, 1.5f)
                dyeGreen.render(mouseOffsetX, mouseOffsetY)
                DrawContextUtils.translate(1f, -1.5f)
            }

        },
    ),

    GOBLIN_RAID(
        "GOBLIN RAID", "Raid", 5.minutes, LorenzColor.RED, true,
        Renderable.item(
            ItemUtils.repoSkullProvider(
                displayName = "Goblin",
                uuid = "32518c29-6127-3c71-b2a7-be4c3251e76f",
                repoSkullId = "GOBLIN_RAID",
            ),
        ) { scale = 0.36 },
    ),

    BETTER_TOGETHER(
        "BETTER TOGETHER", "Better", 18.minutes, LorenzColor.LIGHT_PURPLE, false,
        object : Renderable {
            override val width = 10
            override val height = 10
            override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
            override val verticalAlign = RenderUtils.VerticalAlignment.CENTER

            val steveHead by lazy {
                Renderable.item(createPlayerHead()) { scale = 0.36 }
            }
            val alexHeadProvider = ItemUtils.repoSkullProvider(
                displayName = "Alex",
                uuid = "6ab43178-89fd-4905-97f6-0f67d9d76fd9",
                repoSkullId = "ALEX_SKIN_TEXTURE",
            )
            val alexHead by lazy { Renderable.item(alexHeadProvider) { scale = 0.36 } }

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                DrawContextUtils.translate(-1f, 0f)
                alexHead.render(mouseOffsetX, mouseOffsetY)
                DrawContextUtils.translate(+4f, +3f)
                steveHead.render(mouseOffsetX, mouseOffsetY)
                DrawContextUtils.translate(-3f, -3f)
            }

        },
    ),
    RAFFLE(
        "RAFFLE",
        "Raffle",
        160.seconds,
        color = LorenzColor.GOLD,
        dwarvenSpecific = true,
        iconInput = SafeItemStack(Items.NAME_TAG).overrideId("MINING_RAFFLE_TICKET"),
    ),
    MITHRIL_GOURMAND(
        "MITHRIL GOURMAND",
        "Gourmand", 10.minutes,
        color = LorenzColor.AQUA,
        dwarvenSpecific = true,
        iconInput = DyeCompat.CYAN.createStack().overrideId("MITHRIL_GOURMAND"),
    ),
    ;

    constructor(
        eventName: String,
        shortName: String,
        defaultLength: Duration,
        color: LorenzColor,
        dwarvenSpecific: Boolean,
        iconInput: SafeItemStack,
    ) : this(
        eventName, shortName, defaultLength, color, dwarvenSpecific,
        Renderable.item(iconInput) { xSpacing = 0 },
        iconInput,
    )

    private val icon = Renderable.hoverTips(iconInput, listOf(eventName))
    private val compactText = Renderable.text("${color.getChatColor()}$shortName")
    private val normalText = Renderable.text("${color.getChatColor()}$eventName")

    private val compactTextWithIcon = Renderable.horizontal(icon, compactText, spacing = 0)
    private val normalTextWithIcon = Renderable.horizontal(icon, normalText, spacing = 0)

    fun getRenderable(): Renderable = when (config.compressedFormat) {
        CompressFormat.COMPACT_TEXT -> compactTextWithIcon
        CompressFormat.ICON_ONLY -> icon
        CompressFormat.TEXT_WITHOUT_ICON -> normalText
        CompressFormat.COMPACT_TEXT_WITHOUT_ICON -> compactText
        CompressFormat.DEFAULT -> normalTextWithIcon
    }

    // TODO on 1.8 this used to make it darker, the shader we had for that is gone now so idk
    fun getRenderableAsPast(): Renderable = getRenderable()

    @SkyHanniModule
    companion object {

        private val config get() = SkyHanniMod.feature.mining.miningEvent

        enum class CompressFormat {
            DEFAULT,
            COMPACT_TEXT,
            ICON_ONLY,
            TEXT_WITHOUT_ICON,
            COMPACT_TEXT_WITHOUT_ICON;

            override fun toString(): String {
                return name.lowercase().allLettersFirstUppercase()
            }
        }

        fun fromEventName(bossbarName: String): MiningEventType? {
            return MiningEventType.entries.find { it.eventName == bossbarName.removeColor() }
        }
    }
}
