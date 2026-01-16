package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind

class SpecificSeaCreatures(
    @field:Bind
    val seaCreatures: ObservableList<SpecificSeaCreatureStorageXMLHelper>,
) {

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shSpecificSeaCreatureSettings") {
                description = "Opens a Special Config Menu for Specific Sea Creature Settings."
                category = CommandCategory.USERS_ACTIVE
                aliases = listOf("SeaCreatureSpecificSettings")
                simpleCallback {
                    val existingSettings = updateList()
                    val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/seacreaturetoggles.xml")
                    XmlUtils.openXmlScreen(SpecificSeaCreatures(existingSettings), location)
                }
            }
            event.registerBrigadier("resetSeaCreatureSpecificSettings") {
                description = "Resets entirety of Specific Sea Creature Settings to Default."
                category = CommandCategory.USERS_RESET
                simpleCallback {
                    resetConfig()
                }
            }
        }

        private fun resetConfig() {
            val existingSettings = ObservableList<SpecificSeaCreatureStorageXMLHelper>(mutableListOf())
            SeaCreatureManager.allFishingMobs.forEach { (name, seaCreature) ->
                if (SkyHanniMod.seaCreatureStorage.specificSeaCreatureStorage[name] == null) existingSettings.add(
                    SpecificSeaCreatureStorageXMLHelper(
                        SpecificSeaCreatureSettings(
                            name,
                            shouldRenderLootshare = seaCreature.rare,
                            shouldShowHealthOverlay =  seaCreature.rare,
                            shouldShareInChat =  seaCreature.rare,
                            shouldShowKillTime = seaCreature.rare,),
                        existingSettings),
                )
            }
            SkyHanniMod.seaCreatureStorage.specificSeaCreatureStorage.forEach {
                existingSettings.add(SpecificSeaCreatureStorageXMLHelper(it.value, existingSettings))
            }
        }

        fun updateList():ObservableList<SpecificSeaCreatureStorageXMLHelper> {
            val existingSettings = ObservableList<SpecificSeaCreatureStorageXMLHelper>(mutableListOf())
            SeaCreatureManager.allFishingMobs.forEach { (name, seaCreature) ->
                if (SkyHanniMod.seaCreatureStorage.specificSeaCreatureStorage[name] == null) existingSettings.add(
                    SpecificSeaCreatureStorageXMLHelper(
                        SpecificSeaCreatureSettings(
                            name,
                            shouldRenderLootshare = seaCreature.rare,
                            shouldShowHealthOverlay =  seaCreature.rare,
                            shouldShareInChat =  seaCreature.rare,
                            shouldShowKillTime = seaCreature.rare,),
                        existingSettings),
                )
            }
            SkyHanniMod.seaCreatureStorage.specificSeaCreatureStorage.forEach {
                existingSettings.add(SpecificSeaCreatureStorageXMLHelper(it.value, existingSettings))
            }
            return existingSettings
        }

        fun save(seaCreatures: ObservableList<SpecificSeaCreatureStorageXMLHelper>) {
            for (seaCreature in seaCreatures) {
                SkyHanniMod.seaCreatureStorage.specificSeaCreatureStorage[seaCreature.name] = SpecificSeaCreatureSettings(
                    seaCreature.name,
                    seaCreature.shouldRenderLootshare,
                    seaCreature.shouldShowHealthOverlay,
                    seaCreature.shouldShareInChat,
                    seaCreature.shouldShowKillTime,
                )
            }
            SkyHanniMod.configManager.saveConfig(ConfigFileType.SEA_CREATURES, "save file")
        }

    }
    @Bind
    fun afterClose() {
        save(seaCreatures)
    }

    @Bind
    fun showLootshare() {
        val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/lootsharesphere.xml")
        XmlUtils.openXmlScreen(SpecificSeaCreatures(seaCreatures), location)
    }

    @Bind
    fun showHealthDisp() {
        val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/healthdisplay.xml")
        XmlUtils.openXmlScreen(SpecificSeaCreatures(seaCreatures), location)
    }

    @Bind
    fun showShareParty() {
        val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/sharetoparty.xml")
        XmlUtils.openXmlScreen(SpecificSeaCreatures(seaCreatures), location)
    }

    @Bind
    fun showKillTime() {
        val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/killtime.xml")
        XmlUtils.openXmlScreen(SpecificSeaCreatures(seaCreatures), location)
    }

}
