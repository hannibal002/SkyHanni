package at.hannibal2.skyhanni.features.misc.discordrpc

class DiscordLocationKey {

    private val normalRPC = setOf(
        "auction-house",
        "bank",
        "canvas-room",
        "coal-mine",
        "colosseum",
        "farm",
        "fashion-shop",
        "flower-house",
        "forest",
        "graveyard",
        "library",
        "mountain",
        "ruins",
        "tavern",
        "village",
        "wilderness",
        "wizard-tower",
        "birch-park",
        "spruce-woods",
        "savanna-woodland",
        "dark-thicket",
        "jungle-island",
        "gold-mine",
        "slimehill",
        "diamond-reserve",
        "obsidian-sanctuary",
        "the-barn",
        "mushroom-desert",
        "the-end"
    )
    // list of tokens where the name can just be lowercased and spaces can be replaced with dashes

    private val specialRPC = mapOf(
        "Fisherman's Hut" to "fishermans-hut", "Unincorporated" to "high-level",
        "Dragon's Nest" to "dragons-nest", "Void Sepulture" to "the-end", "Void Slate" to "the-end",
        "Zealot Bruiser Hideout" to "the-end", "Desert Settlement" to "mushroom-desert",
        "Oasis" to "mushroom-desert", "Desert Mountain" to "mushroom-desert", "Jake's House" to "mushroom-desert",
        "Trapper's Den" to "mushroom-desert", "Mushroom Gorge" to "mushroom-desert",
        "Glowing Mushroom Cave" to "mushroom-desert", "Overgrown Mushroom Cave" to "mushroom-desert",
        "Shepherd's Keep" to "mushroom-desert", "Treasure Hunter Camp" to "mushroom-desert",
        "Windmill" to "the-barn", "Spider's Den" to "spiders-den", "Arachne's Burrow" to "spiders-den",
        "Arachne's Sanctuary" to "spiders-den", "Archaeologist's Camp" to "spiders-den",
        "Grandma's House" to "spiders-den", "Gravel Mines" to "spiders-den", "Spider Mound" to "spiders-den",
        "Melody's Plateau" to "forest", "Viking Longhouse" to "forest", "Lonely Island" to "forest",
        "Howling Cave" to "forest"
    ) // maps locations that do have a token, but have parentheses or a legacy key

    private val specialNetherRPC = arrayOf(
        "Aura's Lab",
        "Barbarian Outpost",
        "Belly of the Beast",
        "Blazing Volcano",
        "Burning Desert",
        "Cathedral",
        "Chief's Hut",
        "Courtyard",
        "Crimson Fields",
        "Crimson Isle",
        "Dojo",
        "Dragontail Auction House",
        "Dragontail Bank",
        "Dragontail Bazaar",
        "Dragontail Blacksmith",
        "Dragontail Townsquare",
        "Dragontail",
        "Forgotten Skull",
        "Igrupan's Chicken Coop",
        "Igrupan's House",
        "Mage Council",
        "Mage Outpost",
        "Magma Chamber",
        "Matriarch's Lair",
        "Minion Shop",
        "Mystic Marsh",
        "Odger's Hut",
        "Plhlegblast Pool",
        "Ruins of Ashfang",
        "Scarleton Auction House",
        "Scarleton Bank",
        "Scarleton Bazaar",
        "Scarleton Blacksmith",
        "Scarleton Minion Shop",
        "Scarleton Plaza",
        "Scarleton",
        "Smoldering Tomb",
        "Stronghold",
        "The Bastion",
        "The Dukedom",
        "The Wasteland",
        "Throne Room"
    )
    // list of nether locations because there are sooo many (truncated some according to scoreboard)

    fun getDiscordIconKey(location: String): String {
        val keyIfNormal = location.lowercase().replace(' ', '-')

        return if (normalRPC.contains(keyIfNormal)) {
            keyIfNormal
        } else if (specialRPC.containsKey(location)) {
            specialRPC[location]!!
        } else if (specialNetherRPC.contains(location)) {
            "blazing-fortress"
        } else {
            "skyblock" // future proofing since we can't update the images anymore :(
        }
    }
}