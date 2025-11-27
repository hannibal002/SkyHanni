package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFungiCutterMode
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object WrongFungiCutterWarning {

    private var mode = FungiMode.UNKNOWN
    private var lastPlaySoundTime = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("garden.fungicutter")

    /**
     * REGEX-TEST: §eFungi Cutter Mode: §r§cRed Mushrooms
     * REGEX-TEST: §eFungi Cutter Mode: §r§6Brown Mushrooms
     */
    private val modePattern by patternGroup.pattern(
        "mode",
        "§eFungi Cutter Mode: (?:§.)*(?<color>\\w+) Mushrooms",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        modePattern.matchMatcher(event.message) {
            mode = FungiMode.from(group("color"))
        }
    }

    @HandleEvent
    fun onCropClick(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.crop != CropType.MUSHROOM) return
        if (mode == FungiMode.UNKNOWN) return

        if (event.blockState.block != mode.block) {
            notifyWrong()
        }
    }

    private fun notifyWrong() {
        if (!GardenApi.config.fungiCutterWarn) return

        TitleManager.sendTitle("§cWrong Fungi Cutter Mode!", duration = 2.seconds)
        if (lastPlaySoundTime.passedSince() > 300.milliseconds) {
            lastPlaySoundTime = SimpleTimeMark.now()
            SoundUtils.playBeepSound()
        }
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        if (event.crop == CropType.MUSHROOM) {
            readItem(event.toolItem ?: error("Tool item is null"))
        } else {
            mode = FungiMode.UNKNOWN
        }
    }

    private fun readItem(item: ItemStack) {
        // The fungi cutter mode is not set into the item nbt data immediately after purchasing it.
        val rawMode = item.getFungiCutterMode() ?: return
        mode = FungiMode.from(rawMode)
    }

    enum class FungiMode(val block: Block? = null) {
        RED(Blocks.red_mushroom),
        BROWN(Blocks.brown_mushroom),
        UNKNOWN(),
        ;

        companion object {
            fun from(name: String): FungiMode = entries.firstOrNull {
                it.name.equals(name, ignoreCase = true)
            } ?: error("Unknown Fungi Cutter mode: '$name'")
        }
    }
}
