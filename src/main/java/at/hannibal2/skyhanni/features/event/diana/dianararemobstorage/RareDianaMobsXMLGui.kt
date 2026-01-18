package at.hannibal2.skyhanni.features.event.diana.dianararemobstorage

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind

class RareDianaMobsXMLGui(
    @field:Bind
    val rareMobs: ObservableList<DianaRareMobXMLHelper>,
) {
    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shraredianamobsettings") {
                description = "Opens a Special Config Menu for Rare Diana mob Settings."
                category = CommandCategory.USERS_ACTIVE
                simpleCallback {
                    val existingSettings = updateList()
                    val location = MyResourceLocation("skyhanni", "gui/dianararemobs/dianararemobconfig.xml")
                    XmlUtils.openXmlScreen(RareDianaMobsXMLGui(existingSettings), location)
                }
            }
        }

        fun updateList(): ObservableList<DianaRareMobXMLHelper> {
            val existingSettings = ObservableList<DianaRareMobXMLHelper>(mutableListOf())
            DianaApi.mythologicalCreatures.forEach { (name, mythologicalMobData) ->
                if (SkyHanniMod.rareDianaMobSettings.RareDianaMobSettingStorage[name] == null) existingSettings.add(
                    DianaRareMobXMLHelper(
                        RareDianaMobSettings(
                            name,
                            mythologicalMobData.rare,
                        ),
                    ),
                )
            }
            SkyHanniMod.rareDianaMobSettings.RareDianaMobSettingStorage.forEach {
                existingSettings.add(DianaRareMobXMLHelper(it.value))
            }
            return existingSettings
        }

        fun save(rareMobs: ObservableList<DianaRareMobXMLHelper>) {
            for (rareMob in rareMobs) {
                SkyHanniMod.rareDianaMobSettings.RareDianaMobSettingStorage[rareMob.name] =
                    RareDianaMobSettings(
                        rareMob.name,
                        rareMob.shouldShareOnDiscovery,
                    )
            }
            SkyHanniMod.configManager.saveConfig(ConfigFileType.RARE_DIANA_MOB_SETTING, "save file")
        }
    }

    @Bind
    fun afterClose() {
        save(rareMobs)
    }

    @Bind
    fun openShareOnfind() {
        val location = MyResourceLocation("skyhanni", "gui/dianararemobs/shareonfind.xml")
        XmlUtils.openXmlScreen(RareDianaMobsXMLGui(rareMobs), location)
    }
}
