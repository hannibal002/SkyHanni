package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

@SkyHanniModule
object SkyHanniKeyBindManager {

    private val allKeybinds = mutableListOf<SkyHanniKeyBind>()
    private val keyMappingToKeybind = mutableMapOf<KeyMapping, SkyHanniKeyBind>()
    private val createdCategories = mutableMapOf<String, KeyMapping.Category>()

    private var isSyncingFromMC = false
    private var isSyncingFromProperty = false
    private var initialized = false

    fun register(keybind: SkyHanniKeyBind) {
        allKeybinds.add(keybind)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (initialized) {
            syncAllValues()
            return
        }
        setupKeyMappings()
        initialized = true
    }

    private fun setupKeyMappings() {
        val grouped = allKeybinds.groupBy { it.category }

        for ((categoryName, keybinds) in grouped.entries.sortedBy { it.key }) {
            val subGroups = if (keybinds.size > 20) keybinds.chunked(20) else listOf(keybinds)

            for ((index, subGroup) in subGroups.withIndex()) {
                val suffix = if (subGroups.size > 1) " ${index + 1}" else ""
                val mcCategory = getOrCreateCategory("SkyHanni - $categoryName$suffix")

                for (keybind in subGroup) {
                    val property = keybind.keyProvider()
                    val keyCode = property.get()
                    val mappingName = buildMappingName(keybind.category, keybind.displayName)
                    val keyMapping = KeyMapping(mappingName, keyCode.toKeyType(), keyCode, mcCategory)

                    keybind.keyMapping = keyMapping
                    keyMappingToKeybind[keyMapping] = keybind

                    property.afterChange {
                        if (isSyncingFromMC) return@afterChange
                        syncPropertyToKeyMapping(keyMapping, this)
                    }
                }
            }
        }
    }

    private fun buildMappingName(category: String, displayName: String): String =
        "skyhanni.${category.sanitizeForId()}.${displayName.sanitizeForId()}"

    private fun String.sanitizeForId(): String =
        lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun getOrCreateCategory(displayName: String): KeyMapping.Category =
        createdCategories.getOrPut(displayName) {
            val path = displayName.sanitizeForId()
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("skyhanni", path))
        }

    private fun syncAllValues() {
        for ((keyMapping, keybind) in keyMappingToKeybind) {
            syncPropertyToKeyMapping(keyMapping, keybind.keyProvider().get())
        }
    }

    private fun syncPropertyToKeyMapping(keyMapping: KeyMapping, keyCode: Int) {
        isSyncingFromProperty = true
        keyMapping.setKey(keyCode.toInputConstantsKey())
        KeyMapping.resetMapping()
        isSyncingFromProperty = false
    }

    /**
     * Called from the KeyMapping mixin when setKey is invoked from the controls screen.
     * Syncs the new key value back to the corresponding config Property.
     *
     * @param keyMapping The KeyMapping whose key was changed.
     * @param key The new InputConstants.Key that was set.
     */
    @JvmStatic
    fun onKeyMappingSet(keyMapping: KeyMapping, key: InputConstants.Key) {
        if (isSyncingFromProperty) return
        val keybind = keyMappingToKeybind[keyMapping] ?: return
        val property = keybind.keyProvider()
        val newValue = key.value
        if (property.get() != newValue) {
            isSyncingFromMC = true
            property.set(newValue)
            isSyncingFromMC = false
        }
    }

    fun getActiveKeyMappings(): Array<KeyMapping> =
        keyMappingToKeybind.entries
            .filter { (_, keybind) -> keybind.condition() }
            .map { (keyMapping, _) -> keyMapping }
            .toTypedArray()
}

private fun Int.toInputConstantsKey(): InputConstants.Key = when {
    this == -1 -> InputConstants.UNKNOWN
    this in 0..5 -> InputConstants.Type.MOUSE.getOrCreate(this)
    else -> InputConstants.Type.KEYSYM.getOrCreate(this)
}

private fun Int.toKeyType(): InputConstants.Type = when {
    this in 0..5 -> InputConstants.Type.MOUSE
    else -> InputConstants.Type.KEYSYM
}
