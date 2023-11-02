package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils.asIntOrNull
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object ConfigUpdaterMigrator {
    val logger = LorenzLogger("ConfigMigration")
    const val CONFIG_VERSION = 6
    fun JsonElement.at(chain: List<String>, init: Boolean): JsonElement? {
        if (chain.isEmpty()) return this
        if (this !is JsonObject) return null
        var obj = this.get(chain.first())
        if (obj == null && init) {
            obj = JsonObject()
            this.add(chain.first(), obj)
        }
        return obj?.at(chain.drop(1), init)
    }

    data class ConfigFixEvent(
        val old: JsonObject,
        val new: JsonObject,
        val oldVersion: Int,
        var movesPerformed: Int,
    ) : LorenzEvent() {
        fun move(since: Int, oldPath: String, newPath: String, transform: (JsonElement) -> JsonElement = { it }) {
            if (since <= oldVersion) {
                logger.log("Skipping move from $oldPath to $newPath ($since <= $oldVersion)")
                return
            }
            if (since > CONFIG_VERSION) {
                error("Illegally new version $since > $CONFIG_VERSION")
            }
            if (since > oldVersion + 1) {
                logger.log("Skipping move from $oldPath to $newPath (will be done in another pass)")
                return
            }
            val op = oldPath.split(".")
            val np = newPath.split(".")
            val oldElem = old.at(op, false)
            if (oldElem == null) {
                logger.log("Skipping move from $oldPath to $newPath ($oldPath not present)")
                return
            }
            val x = new.at(np.dropLast(1), true)
            if (x !is JsonObject) {
                logger.log("Catastrophic: element at path $old could not be relocated to $new, since another element already inhabits that path")
                return
            }
            movesPerformed++
            x.add(np.last(), transform(oldElem))
            logger.log("Moved element from $oldPath to $newPath")
        }
    }

    fun merge(a: JsonObject, b: JsonObject): Int {
        var c = 0
        b.entrySet().forEach {
            val e = a.get(it.key)
            val n = it.value
            if (e is JsonObject && n is JsonObject) {
                c += merge(e, n)
            } else {
                if (e != null) {
                    logger.log("Encountered destructive merge. Erasing $e in favour of $n.")
                    c++
                }
                a.add(it.key, n)
            }
        }
        return c
    }

    fun fixConfig(config: JsonObject): JsonObject {
        val lV = (config.get("lastVersion") as? JsonPrimitive)?.asIntOrNull ?: -1
        if (lV > CONFIG_VERSION) {
            error("Cannot downgrade config")
        }
        if (lV == CONFIG_VERSION) return config
        return (lV until CONFIG_VERSION).fold(config) { acc, i ->
            logger.log("Starting config transformation from $i to ${i + 1}")
            val migration = ConfigFixEvent(acc, JsonObject().also {
                it.add("lastVersion", JsonPrimitive(i + 1))
            }, i, 0).also { it.postAndCatch() }
            logger.log("Transformations scheduled: ${migration.new}")
            val mergesPerformed = merge(migration.old, migration.new)
            logger.log("Migration done with $mergesPerformed merges and ${migration.movesPerformed} moves performed")
            migration.old
        }.also {
            logger.log("Final config: $it")
        }
    }
}
