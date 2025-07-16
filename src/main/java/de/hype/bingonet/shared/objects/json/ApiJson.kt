package de.hype.bingonet.shared.objects.json

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class ApiJson {
    var isOffPath: Boolean = false
        private set
    private var data: JsonObject?
    private var path: MutableList<String> = ArrayList()

    constructor(obj: JsonObject?) {
        this.data = obj
        if (data == null) isOffPath = true
    }

    constructor(obj: JsonObject?, isOffPath: Boolean, path: MutableList<String>) {
        this.data = obj
        this.isOffPath = isOffPath
        this.path = ArrayList<String>(path)
    }

    val `object`: JsonObject?
        get() {
            var temp = data
            for (pathPart in path) {
                temp = temp?.getAsJsonObject(pathPart)
            }
            return temp
        }

    private fun getJSONElementSafe(path: MutableList<String>): JsonElement? {
        var temp: JsonElement? = data
        isOffPath = false
        if (temp == null) return null
        for (pathPart in path) {
            temp = if (temp?.isJsonObject == true) {
                temp.asJsonObject?.get(pathPart)
            } else {
                null
            }
            if (temp == null) {
                isOffPath = true
                return null
            }
        }
        return temp
    }

    val jSONObjectSafe: JsonObject?
        get() = getJSONObjectSafe(path)

    val jSONElementSafe: JsonElement?
        get() = getJSONElementSafe(path)

    private fun getJSONObjectSafe(path: MutableList<String>): JsonObject? {
        val element = getJSONElementSafe(path)
        if (element == null) return null
        return element.asJsonObject
    }


    /**
     * @param key supports "→" for multi path. For exmaple trapper.palts is like get(trapper).get(pelts)
     * @return
     */
    fun get(key: String): ApiJson {
        val copy = this.copy()
        copy.path.addAll(key.split("→".toRegex()).dropLastWhile { it.isEmpty() })
        return copy
    }

    private fun copy(): ApiJson {
        return ApiJson(data, isOffPath, path)
    }

    fun getJsonArray(key: String?): ApiJsonList {
        return ApiJsonList(this, key)
    }

    val finalElements: MutableSet<MutableMap.MutableEntry<String, JsonElement>>
        /**
         * Only use at the entire end since these are the default and not caught!
         *
         * @return a List of Elements.
         */
        get() {
            val obj: JsonObject? = this.jSONObjectSafe
            if (obj == null) return HashSet()
            return obj.entrySet()
        }

    fun getElement(id: String): ApiJsonElement {
        try {
            val pathSplit: Array<String> = id.split("→".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val path: MutableList<String> = ArrayList(this.path)
            path.addAll(pathSplit.toList().subList(0, pathSplit.size - 1))
            val safeObj = getJSONObjectSafe(path)
            if (safeObj == null) return ApiJsonElement(null)
            return ApiJsonElement(safeObj.get(pathSplit[pathSplit.size - 1]))
        } catch (_: NullPointerException) {
            return ApiJsonElement(null)
        }
    }

    fun getString(key: String): String? {
        return getString(key, "")
    }

    fun getLong(key: String): Long? {
        return getLong(key, 0L)
    }

    fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    fun getBoolean(key: String, def: Boolean): Boolean {
        val element = getElement(key)
        return element.getBoolean(def)
    }

    fun getString(key: String, def: String): String {
        val element = getElement(key)
        return element.getString(def)
    }

    fun getInt(key: String, def: Int): Int {
        val element = getElement(key)
        val temp = element.getInt(def)
        return temp
    }

    fun getLong(key: String, def: Long): Long {
        val element = getElement(key)
        return element.getLong(def)
    }

    @JvmName("getNullableBoolean")
    fun getBoolean(key: String, def: Boolean?): Boolean? {
        val element = getElement(key)
        return element.getBoolean(def)
    }

    @JvmName("getNullableString")
    fun getString(key: String, def: String?): String? {
        val element = getElement(key)
        return element.getString(def)
    }

    @JvmName("getNullableInt")
    fun getInt(key: String, def: Int?): Int? {
        val element = getElement(key)
        val temp = element.getInt(def)
        return temp
    }

    @JvmName("getNullableLong")
    fun getLong(key: String, def: Long?): Long? {
        val element = getElement(key)
        return element.getLong(def)
    }

    val allSubObjects: MutableMap<String, ApiJson>
        get() {
            try {
                val obj: JsonObject? = this.jSONObjectSafe
                if (obj == null) return HashMap<String, ApiJson>()
                val keys = obj.keySet().toMutableList()
                val subs: MutableMap<String, ApiJson> = HashMap()
                for (key in keys) {
                    subs.put(key, ApiJson(obj.getAsJsonObject(key)))
                }
                return subs
            } catch (e: Exception) {
                return HashMap<String, ApiJson>()
            }
        }

    fun copyApplied(): ApiJson {
        if (isOffPath) return ApiJson(null)
        var temp: JsonElement? = data
        if (temp == null) return ApiJson(null)
        for (pathPart in path) {
            temp = if (temp?.isJsonObject == true) {
                temp.asJsonObject?.get(pathPart)
            } else {
                null
            }
            if (temp == null) {
                isOffPath = true
                break
            }
        }
        if (isOffPath) return ApiJson(null)
        val jsonObject = if (temp?.isJsonObject == true) temp.asJsonObject else null
        return ApiJson(jsonObject)
    }

    companion object {
        @JvmStatic
        fun of(obj: JsonObject?): ApiJson {
            return ApiJson(obj)
        }
    }
}

