package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.data.jsonobjects.repo.CommunityTodo
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.XmlUtils
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import io.github.notenoughupdates.moulconfig.common.IItemStack
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.platform.MoulConfigPlatform
import io.github.notenoughupdates.moulconfig.xml.Bind
import net.minecraft.client.Minecraft

class CommunityTodoViewer(
    communityTodos: List<CommunityTodo>,
    private val currentTodos: ObservableList<CustomTodoEditor>,
) {

    @field:Bind
    var search: String = ""

    private var lastSearch: String? = null

    private val allCommunityTodos = communityTodos.map { communityTodo ->
        CommunityTodoInfo(communityTodo, currentTodos).also { communityTodoInfo ->
            communityTodoInfo.downloaded =
                currentTodos.any { todo -> todo.into().downloadedId == communityTodoInfo.communityInfo.id }
        }
    }
    private val searchCache = ObservableList(mutableListOf<CommunityTodoInfo>())

    @Bind
    fun poll(): StructuredText {
        if (search != lastSearch) {
            lastSearch = search
            searchCache.clear()
            searchCache.addAll(allCommunityTodos.filter { it.todo.label.contains(search, true) || it.todo.trigger.contains(search, true) })
        }
        return "".asStructuredText()
    }

    @Bind
    fun openTodoMenu() {
        XmlUtils.openXmlScreen(CustomTodos(currentTodos), MyResourceLocation("skyhanni", "gui/customtodos/overview.xml"))
    }

    @Bind
    fun searchResults(): ObservableList<CommunityTodoInfo> {
        return searchCache
    }

    @Bind
    fun afterClose() {
        CustomTodos(currentTodos).save()
    }

    @Bind
    fun getHeight(): Int {
        val scale = Minecraft.getInstance().options.guiScale().get()
        return when (scale) {
            1 -> 500
            2 -> 400
            3 -> 300
            4 -> 200
            else -> 100
        }
    }

    class CommunityTodoInfo(val communityInfo: CommunityTodo, private val currentTodos: ObservableList<CustomTodoEditor>) {

        val todo = CustomTodo.fromTemplate(communityInfo.todoData)

        var downloaded = false

        @Bind
        fun getItemStack(): IItemStack {
            val item = CustomTodosGui.parseItem(todo.icon)
            return MoulConfigPlatform.wrap(item)
        }

        @Bind
        fun getLabel(): StructuredText {
            return "§3${todo.label.replace("&&", "§")}".asStructuredText()
        }

        @Bind
        fun getAuthor(): StructuredText {
            return "By: ${communityInfo.author}".asStructuredText()
        }

        @Bind
        fun download() {
            if (downloaded) return
            currentTodos.add(
                CustomTodoEditor(
                    todo.also {
                        it.downloaded = true
                        it.downloadedId = communityInfo.id
                    },
                    currentTodos
                )
            )
            downloaded = true
        }

        @Bind
        fun getDownloadText(): StructuredText {
            return (if (downloaded) "§a✅✅✅✅✅" else "Download").asStructuredText()
        }

        @Bind
        fun viewOnDiscord() {
            OSUtils.openBrowser(communityInfo.discordThread)
            ChatUtils.chat(
                componentBuilder {
                    append("Opened the Discord thread for the ${todo.label} community todo.") {
                        withColor("#7289da")
                    }
                }
            )
        }
    }

}
