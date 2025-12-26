package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TemplateUtil
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

// Taken and modified from Not Enough Updates https://github.com/NotEnoughUpdates/NotEnoughUpdates
@KSerializable
data class CustomTodo(
    @Expose var label: String,
    @Expose var timer: Int,
    @Expose var trigger: String,
    @Expose var icon: String,
    @Expose var isResetOffset: Boolean,
    @Expose var showWhen: Int = 0,
    @Expose var showOnlyWhenReady: Boolean = false,
    @Expose var triggerTarget: TriggerTarget = TriggerTarget.CHAT,
    @Expose var triggerMatcher: TriggerMatcher = TriggerMatcher.CONTAINS,
    @Expose var readyAt: MutableMap<String, SimpleTimeMark> = mutableMapOf(),
    @Expose var isEnabled: Boolean = true,
    @Expose var ignoreColorCodes: Boolean = true,
    @Expose var position: Position = Position(10, 10),
    @Expose var totalTriggers: Int = 1,
    @Expose var triggersLeft: MutableMap<String, Int> = mutableMapOf(),
) {
    enum class TriggerMatcher {
        REGEX,
        STARTS_WITH,
        CONTAINS,
        EQUALS
    }

    enum class TriggerTarget {
        CHAT,
        ACTION_BAR,
        TAB_LIST,
        SIDEBAR
    }

    fun isValid(): Boolean {
        return timer >= 0 && trigger.isNotBlank()
    }

    fun setDoneNow() {
        if (!SkyBlockUtils.inSkyBlock) return
        val now = SimpleTimeMark.now()
        val triggersLeft = this.triggersLeftOnCurrentProfile ?: 1
        if (triggersLeft > 1) {
            this.triggersLeftOnCurrentProfile = triggersLeft - 1
            return
        } else {
            this.triggersLeftOnCurrentProfile = this.totalTriggers
        }
        readyAt[HypixelData.profileName] =
            if (isResetOffset) {
                val asTimeMark = (now.toMillis() - now.toMillis() % MS_IN_A_DAY + timer * 1000L).asTimeMark()
                if (asTimeMark.isInPast()) asTimeMark + 1.days else asTimeMark
            } else {
                now + timer.seconds
            }
        CustomTodos.save()
    }

    var readyAtOnCurrentProfile: SimpleTimeMark?
        get() {
            if (!SkyBlockUtils.inSkyBlock) return null
            return readyAt[HypixelData.profileName]
        }
        set(value) {
            if (!SkyBlockUtils.inSkyBlock) return
            readyAt[HypixelData.profileName] = value ?: return
        }

    private var triggersLeftOnCurrentProfile: Int?
        get() {
            if (!SkyBlockUtils.inSkyBlock) return null
            return triggersLeft[HypixelData.profileName]
        }
        set(value) {
            if (!SkyBlockUtils.inSkyBlock) return
            triggersLeft[HypixelData.profileName] = value ?: return
        }


    companion object {
        private const val NEU_TEMPLATE_PREFIX = "NEU:CUSTOMTODO/"
        private const val TEMPLATE_PREFIX = "SH:CUSTOMTODO/"
        const val MS_IN_A_DAY = (24 * 60 * 60 * 1000)

        fun fromTemplate(data: String): CustomTodo? {
            val maybeDecoded = TemplateUtil.maybeDecodeTemplate(TEMPLATE_PREFIX, data, CustomTodo::class.java)
                ?: TemplateUtil.maybeDecodeTemplate(NEU_TEMPLATE_PREFIX, data, CustomTodo::class.java)
            if (maybeDecoded == null) {
                ChatUtils.chat("§cInvalid Todo")
            }
            return maybeDecoded?.also {
                it.readyAt.clear()
                it.triggersLeft.clear()
            }
        }
    }

    fun toTemplate(): String {
        return TemplateUtil.encodeTemplate(
            TEMPLATE_PREFIX,
            this.copy(readyAt = mutableMapOf(), triggersLeft = mutableMapOf()),
        )
    }

    fun getRenderable(): Renderable? {
        if (!this.isEnabled || !this.isValid()) return null
        val readyAt = this.readyAtOnCurrentProfile ?: return null
        if (this.triggersLeftOnCurrentProfile == null) {
            this.triggersLeftOnCurrentProfile = this.totalTriggers
        }
        val triggers = this.triggersLeftOnCurrentProfile ?: return null
        if (this.showOnlyWhenReady && readyAt.isInFuture()) return null
        if (this.showWhen != 0 && readyAt.timeUntil().inWholeSeconds > this.showWhen) return null

        var timer = readyAt.timeUntil().format(maxUnits = 2)
        if (readyAt.isInPast()) {
            timer = if (this.totalTriggers == 1) "§aReady"
            else "§a$triggers Left"
        }
        val label = this.label.replace("&&", "§")
        val textRenderable = Renderable.text("§3$label: §c$timer")

        if (this.icon.isEmpty()) return textRenderable
        return Renderable.horizontal(Renderable.item(CustomTodosGui.parseItem(this.icon)), textRenderable)

    }

    private var compiledRegex: Regex? = null

    fun getRegex(): Regex? {
        if (this.triggerMatcher != TriggerMatcher.REGEX) return null
        if (compiledRegex != null) return compiledRegex
        val regex = this.trigger.toRegex()
        compiledRegex = regex
        return regex
    }
}
