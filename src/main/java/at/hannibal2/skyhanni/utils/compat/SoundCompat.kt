package at.hannibal2.skyhanni.utils.compat

object SoundCompat {

    // map of 1.8 sound names to modern sound names
    private val soundMap = mapOf(
        "random.orb" to "entity.experience_orb.pickup",
        "random.explode" to "entity.generic.explode",
        "dig.glass" to "block.glass.break",
        "dig.stone" to "block.stone.break",
        "dig.gravel" to "block.gravel.break",
        "dig.cloth" to "block.wool.break",
    )

    fun getModernSoundName(soundName: String): String {
        return soundMap[soundName] ?: soundName
    }

    fun getLegacySoundName(soundName: String): String {
        return soundMap.entries.firstOrNull { it.value == soundName }?.key ?: soundName
    }

}
