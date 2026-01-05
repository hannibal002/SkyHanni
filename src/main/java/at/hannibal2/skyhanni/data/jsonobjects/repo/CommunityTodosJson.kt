package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CommunityTodosJson(
    @Expose @SerializedName("community_todos") val communityTodos: List<CommunityTodo>,
)

data class CommunityTodo(
    @Expose val name: String,
    @Expose val id: String,
    @Expose val author: String,
    @Expose val icon: String,
    @Expose @SerializedName("todo_data") val todoData: String,
)
