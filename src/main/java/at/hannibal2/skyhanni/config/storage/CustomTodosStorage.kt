package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.misc.customtodos.CustomTodo
import com.google.gson.annotations.Expose

class CustomTodosStorage {
    @Expose
    val customTodos: MutableList<CustomTodo> = ArrayList()
}
