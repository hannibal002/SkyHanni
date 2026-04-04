package at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.ObservableList
import io.github.notenoughupdates.moulconfig.xml.Bind

class SpecificSeaCreatures(
    @field:Bind
    val seaCreatures: ObservableList<SpecificSeaCreatureStorageXMLHelper>,
) {

    @field:Bind
    var search: String = ""

    private var lastSearch: String? = null

    private val searchCache = ObservableList(mutableListOf<SpecificSeaCreatureStorageXMLHelper>())

    @Bind
    fun searchResults(): ObservableList<SpecificSeaCreatureStorageXMLHelper> {
        return searchCache
    }

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shseacreatures") {
                description = "Opens a Special Config Menu for Specific Sea Creature Settings."
                category = CommandCategory.USERS_ACTIVE
                aliases = listOf("shsc")
                simpleCallback {
                    val existingSettings = updateList()
                    val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/seacreaturetoggles.xml")
                    XmlUtils.openXmlScreen(SpecificSeaCreatures(existingSettings), location)
                }
            }
            event.registerBrigadier("shresetSeaCreatureSpecificSettings") {
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
                SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage.clear()
                existingSettings.add(
                    SpecificSeaCreatureStorageXMLHelper(
                        SpecificSeaCreatureSettings(
                            name,
                            shouldRenderLootshare = seaCreature.rare,
                            shouldShowHealthOverlay = seaCreature.rare,
                            shouldShareInChat = seaCreature.rare,
                            shouldShowKillTime = seaCreature.rare,
                            shouldSelfNotifyOnCatch = seaCreature.rare,
                            shouldNotifyForNonOwn = seaCreature.rare,
                            shouldHighlight = seaCreature.rare,
                            shouldShareCocoonInChat = seaCreature.rare,
                            shouldWarnWhenCocooned = seaCreature.rare,
                        ),
                    ),
                )
            }
            SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage.forEach {
                existingSettings.add(SpecificSeaCreatureStorageXMLHelper(it.value))
            }
        }

        fun updateList(): ObservableList<SpecificSeaCreatureStorageXMLHelper> {
            val existingSettings = ObservableList<SpecificSeaCreatureStorageXMLHelper>(mutableListOf())
            SeaCreatureManager.allFishingMobs.forEach { (name, seaCreature) ->
                if (SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage[name] == null) existingSettings.add(
                    SpecificSeaCreatureStorageXMLHelper(
                        SpecificSeaCreatureSettings(
                            name,
                            shouldRenderLootshare = seaCreature.rare,
                            shouldShowHealthOverlay = seaCreature.rare,
                            shouldShareInChat = seaCreature.rare,
                            shouldShowKillTime = seaCreature.rare,
                            shouldSelfNotifyOnCatch = seaCreature.rare,
                            shouldNotifyForNonOwn = seaCreature.rare,
                            shouldHighlight = seaCreature.rare,
                            shouldShareCocoonInChat = seaCreature.rare,
                            shouldWarnWhenCocooned = seaCreature.rare,
                        ),
                    ),
                )
            }
            SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage.forEach {
                existingSettings.add(SpecificSeaCreatureStorageXMLHelper(it.value))
            }
            return existingSettings
        }

        fun saveSeaCreatures(seaCreatures: ObservableList<SpecificSeaCreatureStorageXMLHelper>) {
            for (seaCreature in seaCreatures) {
                SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage[seaCreature.name] =
                    SpecificSeaCreatureSettings(
                        seaCreature.name,
                        seaCreature.shouldRenderLootshare,
                        seaCreature.shouldShowHealthOverlay,
                        seaCreature.shouldShareInChat,
                        seaCreature.shouldShowKillTime,
                        seaCreature.shouldSelfNotifyOnCatch,
                        seaCreature.shouldNotifyForNonOwn,
                        seaCreature.shouldHighlight,
                        seaCreature.shouldShareCocoonInChat,
                        seaCreature.shouldWarnWhenCocooned,
                    )
            }
            SkyHanniMod.configManager.saveConfig(ConfigFileType.SEA_CREATURES, "save file")
        }

    }

    @Bind
    fun afterClose() {
        saveSeaCreatures(seaCreatures)
    }

    @Bind
    fun poll(): StructuredText {
        if (search != lastSearch) {
            lastSearch = search
            searchCache.clear()
            searchCache.addAll(seaCreatures.filter { it.name.contains(search, true) })
        }
        return "".asStructuredText()
    }

    @Bind
    fun showLootshareSphere() {
        openXML("lootsharesphere")
    }

    @Bind
    fun showHealthDisp() {
        openXML("healthdisplay")
    }

    @Bind
    fun showShareParty() {
        openXML("sharetoparty")
    }

    @Bind
    fun showKillTime() {
        openXML("killtime")
    }

    @Bind
    fun showSelfNotify() {
        openXML("selfnotify")
    }

    @Bind
    fun showOtherNotify() {
        openXML("notifynonown")
    }

    @Bind
    fun showHighlight() {
        openXML("shouldhighlight")
    }

    @Bind
    fun showCocoonChatSettings() {
        openXML("sharecocoontoparty")
    }

    @Bind
    fun showCocoonWarnSettings() {
        openXML("warnwhencocooned")
    }

    private fun openXML(string: String) {
        val location = MyResourceLocation("skyhanni", "gui/seacreaturetoggles/$string.xml")
        XmlUtils.openXmlScreen(SpecificSeaCreatures(seaCreatures), location)
    }

}
