package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator.at
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.json.SkippingTypeAdapterFactory
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class SkyblockStatStorageTest {

    class Profile {
        @Expose
        var stats: MutableMap<SkyblockStat, Double?> = mutableMapOf()
    }

    class Player {
        @Expose
        var profiles: MutableMap<String, Profile> = mutableMapOf()
    }

    class Storage {
        @Expose
        var players: MutableMap<UUID, Player> = mutableMapOf()
    }

    private val uuid = "0a2af95e-21cf-4ee2-8a96-e30352680646"
    private val profilePath = "storage.players.$uuid.profiles.pear"

    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(69, "#profile.stats.TRUE_DEFENCE", "#profile.stats.TRUE_DEFENSE")
        event.move(112, "#profile.stats.NETHER_WART_FORTUNE", "#profile.stats.NETHER_STALK_FORTUNE")
        event.remove(113, "#profile.stats.null")
        event.move(141, "#profile.stats.HUNTER_FORTUNE", "#profile.stats.HUNTING_FORTUNE")
        // Stats are stored under their lowercase name, so none of the renames above ever matched anything
        event.move(142, "#profile.stats.true_defence", "#profile.stats.true_defense")
        event.move(142, "#profile.stats.nether_wart_fortune", "#profile.stats.nether_stalk_fortune")
        event.move(142, "#profile.stats.hunter_fortune", "#profile.stats.hunting_fortune")
        // Left behind by stats that were read back while their rename was still missing
        event.remove(142, "#profile.stats.unknown")
    }

    @Test
    fun `removed stats do not break reading the surrounding storage`() {
        // Mirrors ConfigManager, where a failed read is swallowed instead of resetting the config.
        val gson = BaseGsonBuilder.gson().registerTypeAdapterFactory(SkippingTypeAdapterFactory).create()
        val json = """
            {"players":{"$uuid":{"profiles":{"pear":{
              "stats":{"strength":5.0,"hunter_fortune":121.0,"unknown":60.0,"speed":100.0}
            }}}}}
        """.trimIndent()

        val storage = gson.fromJson(json, Storage::class.java)

        val profile = storage.players.getValue(UUID.fromString(uuid)).profiles.getValue("pear")
        assertEquals(mapOf(SkyblockStat.STRENGTH to 5.0, SkyblockStat.SPEED to 100.0), profile.stats)
        assertEquals("""{"stats":{"strength":5.0,"speed":100.0}}""", gson.toJson(profile).filterNot { it.isWhitespace() })
    }

    @Test
    fun `renamed stats are migrated`() {
        val stats = JsonObject().apply {
            addProperty("hunter_fortune", 121.0)
            addProperty("nether_wart_fortune", 42.0)
            addProperty("unknown", 60.0)
            addProperty("strength", 5.0)
        }
        val old = JsonObject()
        val profile = old.at(profilePath.split("."), true) as JsonObject
        profile.add("stats", stats)

        val event = ConfigUpdaterMigrator.ConfigFixEvent(
            old = old,
            new = JsonObject(),
            oldVersion = 141,
            movesPerformed = 0,
            dynamicPrefix = mapOf("#profile" to listOf(profilePath)),
        )
        onConfigFix(event)

        val migrated = checkNotNull(event.new.at("$profilePath.stats".split("."), false) as? JsonObject) {
            "migration did not move any stats"
        }
        assertEquals(121.0, migrated["hunting_fortune"].asDouble)
        assertEquals(42.0, migrated["nether_stalk_fortune"].asDouble)
        assertNull(migrated["unknown"])

        assertFalse(stats.has("hunter_fortune"))
        assertFalse(stats.has("nether_wart_fortune"))
        assertFalse(stats.has("unknown"))
        assertEquals(5.0, stats["strength"].asDouble)
    }
}
