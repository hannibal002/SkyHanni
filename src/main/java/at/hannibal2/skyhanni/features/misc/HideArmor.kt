package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.HideArmorConfig
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.hasPotionEffect
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.world.entity.player.Player

@SkyHanniModule
object HideArmor {

    internal val config: HideArmorConfig get() = SkyHanniMod.feature.misc.hideArmor

    fun shouldHideArmor(entity: Player): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (entity.hasPotionEffect(EffectsCompat.INVISIBILITY)) return false
        if (entity.isNpc()) return false
        val playerSelection = config.playerSelection.get().takeIfNotEmpty() ?: return false

        val name = entity.gameProfile.name
        val matches = playerSelection.any { name in it.usernames }
        return if (config.invertSelection) !matches else matches
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(91, "misc.hideArmor2", "misc.hideArmor")
        event.move(
            147,
            "misc.hideArmor.mode",
            "misc.hideArmor.playerSelection"
        ) { element ->
            val oldValue = element.asString
            val newValue: List<PlayerNameSource> = when (oldValue) {
                "ALL" -> listOf(SELF, ISLAND_PLAYERS)
                "OWN" -> listOf(SELF)
                "OTHERS" -> listOf(ISLAND_PLAYERS)
                "OFF" -> emptyList()
                else -> return@move element
            }

            JsonArray().apply {
                newValue.forEach { add(JsonPrimitive(it.name)) }
            }
        }
    }
}
