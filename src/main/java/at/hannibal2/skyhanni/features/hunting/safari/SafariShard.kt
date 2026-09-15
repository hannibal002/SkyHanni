package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.ResettableValue
import at.hannibal2.skyhanni.utils.SafeItemStack

enum class SafariShard(val displayName: String, val biome: SafariBiome) {

    // Cavern
    CAVERNFISH("Cavernfish", SafariBiome.CAVERN),
    FLITTER("Flitter", SafariBiome.CAVERN),
    SHYWORM("Shyworm", SafariBiome.CAVERN),
    DRIFTLING("Driftling", SafariBiome.CAVERN),
    CHUCKWALLA("Chuckwalla", SafariBiome.CAVERN),
    ROCKMITE("Rockmite", SafariBiome.CAVERN),
    SCRAPPY("Scrappy", SafariBiome.CAVERN),
    SNOOZLE("Snoozle", SafariBiome.CAVERN),
    GEMZIE("Gemzie", SafariBiome.CAVERN),

    // Forest
    FOXTROT("Foxtrot", SafariBiome.FOREST),
    HONEYBUG("Honeybug", SafariBiome.FOREST),
    HIDEONFLOOR("Hideonfloor", SafariBiome.FOREST),
    TREEFROG("Treefrog", SafariBiome.FOREST),
    WOODCHUCKER("Woodchucker", SafariBiome.FOREST),
    FLUFFLING("Fluffling", SafariBiome.FOREST),
    BLUEBIRD("Bluebird", SafariBiome.FOREST),
    PARAKEET("Parakeet", SafariBiome.FOREST),
    MACAW("Macaw", SafariBiome.FOREST),

    // Haunted
    SOLSNATCHER("Solsnatcher", SafariBiome.HAUNTED),
    BLOODBAT("Bloodbat", SafariBiome.HAUNTED),
    LITTERBUG("Litterbug", SafariBiome.HAUNTED),
    AREITA("Areita", SafariBiome.HAUNTED),
    DUPLICO("Duplico", SafariBiome.HAUNTED),
    GAZER("Gazer", SafariBiome.HAUNTED),
    HIDEONWALL("Hideonwall", SafariBiome.HAUNTED),
    GIMMIEGOLD("Gimmiegold", SafariBiome.HAUNTED),
    HIDEYHO("Hideyho", SafariBiome.HAUNTED),
    DOOMSPIRAL("Doomspiral", SafariBiome.HAUNTED),

    // Icy
    STRONGARM("Strongarm", SafariBiome.ICY),
    POLARIS("Polaris", SafariBiome.ICY),
    BILLYGOAT("Billygoat", SafariBiome.ICY),
    TEPID("Tepid", SafariBiome.ICY),
    NOZZLENOSE("Nozzlenose", SafariBiome.ICY),
    MANTIS_SHRIMP("Mantis Shrimp", SafariBiome.ICY),
    SHUDDERSQUID("Shuddersquid", SafariBiome.ICY),
    TROODON("Troodon", SafariBiome.ICY),
    WUMPA("Wumpa", SafariBiome.ICY),
    ;

    private val internalNameCache = ResettableValue { ItemResolutionQuery.attributeNameToInternalName(displayName)?.toInternalName() }
    private val itemStackCache = ResettableValue { internalName?.getItemStackOrNull() }
    private val rarityCache = ResettableValue { itemStack?.getItemRarityOrNull() }

    val internalName: NeuInternalName? by internalNameCache

    val itemStack: SafeItemStack? by itemStackCache

    val rarity: LorenzRarity? by rarityCache

    val formattedName: String get() = "${rarity?.chatColorCode ?: "§f"}$displayName"

    internal fun resetCache() {
        internalNameCache.reset()
        itemStackCache.reset()
        rarityCache.reset()
    }

    companion object {
        private val byName = entries.associateBy(SafariShard::displayName)

        fun getByName(name: String): SafariShard? = byName[name]
    }
}
