package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.IItemStack
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.platform.MoulConfigPlatform
import io.github.notenoughupdates.moulconfig.xml.Bind
import kotlin.time.Duration.Companion.seconds

// Taken and modified from Not Enough Updates https://github.com/NotEnoughUpdates/NotEnoughUpdates
@Suppress("TooManyFunctions")
class CustomTodoEditor(
    private val from: CustomTodo,
    private val todos: ObservableList<CustomTodoEditor>,
) {

    @field:Bind
    var label: String = from.label

    @field:Bind
    var enabled: Boolean = from.isEnabled

    @field:Bind
    var timer: String = from.timer.toString()

    @field:Bind
    var showWhen: String = from.showWhen.toString()

    @field:Bind
    var totalTriggers: String = from.totalTriggers.toString()

    @field:Bind
    var trigger: String = from.trigger

    @field:Bind
    var icon: String = from.icon

    @field:Bind
    var isResetOffset: Boolean = from.isResetOffset

    @field:Bind
    var showOnlyWhenReady: Boolean = from.showOnlyWhenReady

    @field:Bind
    var ignoreColorCodes: Boolean = from.ignoreColorCodes

    @field:Bind
    var cronEnabled: Boolean = from.cronEnabled

    @field:Bind
    var cronExpression: String = from.cronExpression

    var target = from.triggerTarget
    var matchMode = from.triggerMatcher

    fun into(): CustomTodo {
        if (from.readyAtOnCurrentProfile == null) markAsReady()
        if (from.totalTriggers != totalTriggers.toIntOrNull()) from.triggersLeft = mutableMapOf()
        return CustomTodo(
            label,
            timer.toIntOrNull() ?: 0,
            trigger,
            icon,
            isResetOffset,
            showWhen.toIntOrNull() ?: 0,
            showOnlyWhenReady,
            target, matchMode,
            from.readyAt,
            enabled,
            ignoreColorCodes,
            from.position,
            totalTriggers.toIntOrNull() ?: 1,
            from.triggersLeft,
            cronEnabled,
            cronExpression,
            from.downloaded,
            from.downloadedId,
        )
    }

    @Bind
    fun setChat() {
        target = CustomTodo.TriggerTarget.CHAT
    }

    @Bind
    fun setActionbar() {
        target = CustomTodo.TriggerTarget.ACTION_BAR
    }

    @Bind
    fun setScoreboard() {
        target = CustomTodo.TriggerTarget.SIDEBAR
    }

    @Bind
    fun setTablist() {
        target = CustomTodo.TriggerTarget.TAB_LIST
    }

    private fun colorFromBool(b: Boolean): String {
        return if (b) "§a" else "§c"
    }

    @Bind
    fun getChat(): StructuredText {
        return StructuredText.of(colorFromBool(target == CustomTodo.TriggerTarget.CHAT) + "Chat")
    }

    @Bind
    fun getActionbar(): StructuredText {
        return StructuredText.of(colorFromBool(target == CustomTodo.TriggerTarget.ACTION_BAR) + "Actionbar")
    }

    @Bind
    fun getScoreboard(): StructuredText {
        return StructuredText.of(colorFromBool(target == CustomTodo.TriggerTarget.SIDEBAR) + "Scoreboard")
    }

    @Bind
    fun getTablist(): StructuredText {
        return StructuredText.of(colorFromBool(target == CustomTodo.TriggerTarget.TAB_LIST) + "Tablist")
    }

    @Bind
    fun setRegex() {
        matchMode = CustomTodo.TriggerMatcher.REGEX
    }

    @Bind
    fun setStartsWith() {
        matchMode = CustomTodo.TriggerMatcher.STARTS_WITH
    }

    @Bind
    fun setContains() {
        matchMode = CustomTodo.TriggerMatcher.CONTAINS
    }

    @Bind
    fun setEquals() {
        matchMode = CustomTodo.TriggerMatcher.EQUALS
    }

    @Bind
    fun getRegex(): StructuredText {
        return StructuredText.of(colorFromBool(matchMode == CustomTodo.TriggerMatcher.REGEX) + "Regex")
    }

    @Bind
    fun getStartsWith(): StructuredText {
        return StructuredText.of(colorFromBool(matchMode == CustomTodo.TriggerMatcher.STARTS_WITH) + "Starts With")
    }

    @Bind
    fun getContains(): StructuredText {
        return StructuredText.of(colorFromBool(matchMode == CustomTodo.TriggerMatcher.CONTAINS) + "Contains")
    }

    @Bind
    fun getEquals(): StructuredText {
        return StructuredText.of(colorFromBool(matchMode == CustomTodo.TriggerMatcher.EQUALS) + "Equals")
    }

    @Bind
    fun getItemStack(): IItemStack {
        val item = CustomTodosGui.parseItem(icon)
        return MoulConfigPlatform.wrap(item)
    }

    @Bind
    fun copyTemplate() {
        ClipboardUtils.copyToClipboard(into().toTemplate())
    }

    @Bind
    fun markAsReady() {
        from.triggersLeftOnCurrentProfile = totalTriggers.toIntOrNull() ?: 1
        from.readyAtOnCurrentProfile = SimpleTimeMark.now()
    }

    @Bind
    fun markAsCompleted() {
        from.triggersLeftOnCurrentProfile = 0
        from.setDoneNow()
    }

    @Bind
    fun getFancyTimeTimer(): StructuredText {
        val wholeSeconds = timer.toIntOrNull() ?: return "§3Invalid Time".asStructuredText()
        val formattedTime = wholeSeconds.seconds.format()
        if (isResetOffset) {
            return "Resets $formattedTime after 00:00 GMT".asStructuredText()
        }
        return "Resets $formattedTime after completion".asStructuredText()
    }

    @Bind
    fun getFancyTimeShowWhen(): StructuredText {
        if (showOnlyWhenReady) {
            return "Shown only when task is ready".asStructuredText()
        }
        val wholeSeconds = showWhen.toIntOrNull() ?: return "§3Invalid Time".asStructuredText()
        if (wholeSeconds == 0) {
            return "Always shown".asStructuredText()
        }
        val formattedTime = wholeSeconds.seconds.format()
        return "Show if less than $formattedTime until ready".asStructuredText()
    }

    fun changeTimer(value: Int) {
        timer = ((timer.toIntOrNull() ?: 0) + value).coerceAtLeast(0).toString()
    }

    fun changeShowWhen(value: Int) {
        showWhen = ((showWhen.toIntOrNull() ?: 0) + value).coerceAtLeast(0).toString()
    }

    @Bind
    fun plusDayTimer() {
        changeTimer(60 * 60 * 24)
    }

    @Bind
    fun minusDayTimer() {
        changeTimer(-60 * 60 * 24)
    }

    @Bind
    fun minusHourTimer() {
        changeTimer(-60 * 60)
    }

    @Bind
    fun plusHourTimer() {
        changeTimer(60 * 60)
    }

    @Bind
    fun plusMinuteTimer() {
        changeTimer(60)
    }

    @Bind
    fun minusMinuteTimer() {
        changeTimer(-60)
    }

    @Bind
    fun plusDayShowWhen() {
        changeShowWhen(60 * 60 * 24)
    }

    @Bind
    fun minusDayShowWhen() {
        changeShowWhen(-60 * 60 * 24)
    }

    @Bind
    fun minusHourShowWhen() {
        changeShowWhen(-60 * 60)
    }

    @Bind
    fun plusHourShowWhen() {
        changeShowWhen(60 * 60)
    }

    @Bind
    fun plusMinuteShowWhen() {
        changeShowWhen(60)
    }

    @Bind
    fun minusMinuteShowWhen() {
        changeShowWhen(-60)
    }

    @Bind
    fun delete() {
        todos.remove(this)
        CustomTodos(todos).save()
    }

    @Bind
    fun getLabel(): StructuredText {
        return (label.replace("&&", "§") + if (from.downloaded) " §a(Downloaded)" else "").asStructuredText()
    }

    @Bind
    fun getTitle(): StructuredText {
        return "Editing ${label.replace("&&", "§")}".asStructuredText()
    }

    @Bind
    fun afterClose() {
        CustomTodos(todos).save()
    }

    @Bind
    fun close() {
        XmlUtils.openXmlScreen(CustomTodos(todos), MyResourceLocation("skyhanni", "gui/customtodos/overview.xml"))
    }

    @Bind
    fun edit() {
        from.downloaded = false
        from.downloadedId = ""
        XmlUtils.openXmlScreen(this, MyResourceLocation("skyhanni", "gui/customtodos/edit.xml"))
    }
}
