package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind

// Taken and modified from Not Enough Updates https://github.com/NotEnoughUpdates/NotEnoughUpdates
class CustomTodos(
    @field:Bind
    val todos: ObservableList<CustomTodoEditor>,
) {

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shtodos") {
                description = "Edit custom TODOs"
                category = CommandCategory.USERS_ACTIVE
                aliases = listOf("shcustomtodos")
                simpleCallback {
                    val todosList = ObservableList<CustomTodoEditor>(mutableListOf())
                    SkyHanniMod.customTodos.customTodos.forEach { todosList.add(CustomTodoEditor(it, todosList)) }
                    val location = MyResourceLocation("skyhanni", "gui/customtodos/overview.xml")
                    XmlUtils.openXmlScreen(CustomTodos(todosList), location)
                }
            }
        }

        fun save() {
            val todosList = ObservableList<CustomTodoEditor>(mutableListOf())
            SkyHanniMod.customTodos.customTodos.forEach { todosList.add(CustomTodoEditor(it, todosList)) }
            CustomTodos(todosList).save()
        }

    }

    @Bind
    fun pasteTodo() {
        SkyHanniMod.launchIOCoroutine("import custom todos") {
            val customTodo = CustomTodo.fromTemplateOrNull(
                ClipboardUtils.readFromClipboard() ?: return@launchIOCoroutine,
                printErrors = true,
            )
            DelayedRun.runNextTick {
                todos.add(CustomTodoEditor(customTodo ?: return@runNextTick, todos))
                save()
            }
        }
    }

    @Bind
    fun viewCommunityTodos() {
        XmlUtils.openXmlScreen(
            CommunityTodoViewer(CustomTodoDownload.todos, todos),
            MyResourceLocation("skyhanni", "gui/customtodos/communitytodos.xml"),
        )
    }

    @Bind
    fun afterClose() {
        save()
    }

    fun save() {
        SkyHanniMod.customTodos.customTodos = todos.map { it.into() }.toMutableList()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.CUSTOM_TODOS, "Save file")
    }

    @Bind
    fun addTodo() {
        todos.add(
            CustomTodoEditor(
                CustomTodo(
                    "Custom Todo # ${todos.size + 1}",
                    0,
                    "",
                    "",
                    false,
                ),
                todos,
            ),
        )
        save()
    }

}
