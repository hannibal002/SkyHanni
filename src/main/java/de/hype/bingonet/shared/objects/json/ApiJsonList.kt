package de.hype.bingonet.shared.objects.json

class ApiJsonList(var parent: ApiJson, key: String?) {
    var values: MutableList<ApiJsonElement> = ArrayList()

    init {
        val temp = parent.jSONElementSafe
        if (temp != null) {
            val array = temp.asJsonObject.getAsJsonArray(key)
            if (array != null) {
                for (jsonElement in array) {
                    values.add(ApiJsonElement(jsonElement))
                }
            }
        }
    }

    fun toList(): MutableList<ApiJsonElement> {
        return ArrayList(values)
    }
}
