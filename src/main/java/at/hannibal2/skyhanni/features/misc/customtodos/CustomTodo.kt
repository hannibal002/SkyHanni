package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TemplateUtil
import com.google.gson.annotations.Expose
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
    @Expose var enabled: Boolean = true,
    @Expose var ignoreColorCodes: Boolean = true,
) {
    enum class TriggerMatcher {
        REGEX, STARTS_WITH, CONTAINS, EQUALS
    }

    enum class TriggerTarget {
        CHAT, ACTION_BAR, TAB_LIST, SCOREBOARD
    }

    fun isValid(): Boolean {
        return timer >= 0 && trigger.isNotBlank()
    }

    fun setDoneNow() {
        if (!SkyBlockUtils.inSkyBlock) return
        val now = SimpleTimeMark.now()
        readyAt[HypixelData.profileName] =
            if (isResetOffset) {
                (now.toMillis() + SECONDS_IN_A_DAY - now.toMillis() % SECONDS_IN_A_DAY + timer * 1000L).asTimeMark()
            } else {
                now + timer.seconds
            }
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


    companion object {
        const val TEMPLATE_PREFIX = "NEU:CUSTOMTODO/"
        const val SECONDS_IN_A_DAY = (24 * 60 * 60 * 100)
        fun fromTemplate(data: String): CustomTodo? {
            return TemplateUtil.maybeDecodeTemplate(TEMPLATE_PREFIX, data, CustomTodo::class.java)?.also { it.readyAt.clear() }
        }
    }

    fun toTemplate(): String {
        return TemplateUtil.encodeTemplate(
            TEMPLATE_PREFIX,
            this.copy(readyAt = mutableMapOf())
        )
    }
}
