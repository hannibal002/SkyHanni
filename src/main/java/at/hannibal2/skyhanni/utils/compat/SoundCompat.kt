package at.hannibal2.skyhanni.utils.compat

object SoundCompat {

    // map of 1.8 sound names to modern sound names
    private val soundMap = mapOf(
        "dig.cloth" to "block.wool.break",
        "dig.glass" to "block.glass.break",
        "dig.gravel" to "block.gravel.break",
        "dig.stone" to "block.stone.break",
        "liquid.lavapop" to "block.lava.pop",
        "mob.bat.hurt" to "entity.bat.hurt",
        "mob.enderdragon.growl" to "entity.ender_dragon.growl",
        "mob.enderman.portal" to "entity.enderman.teleport",
        "mob.ghast.scream" to "entity.ghast.scream",
        "mob.guardian.curse" to "entity.elder_guardian.curse",
        "mob.horse.donkey.death" to "entity.donkey.death",
        "mob.horse.donkey.hit" to "entity.donkey.hurt",
        "mob.wolf.panting" to "entity.wolf.pant",
        "mob.wolf.whine" to "entity.wolf.whine",
        "mob.zombie.remedy" to "entity.zombie_villager.cure",
        "note.bassattack" to "block.note_block.bass",
        "note.harp" to "block.note_block.harp",
        "random.anvil_break" to "block.anvil.break",
        "random.anvil_land" to "block.anvil.land",
        "random.burp" to "entity.player.burp",
        "random.chestopen" to "block.chest.open",
        "random.explode" to "entity.generic.explode",
        "random.levelup" to "entity.player.levelup",
        "random.orb" to "entity.experience_orb.pickup",
        "mob.zombie.unfect" to "entity.zombie_villager.converted",
        "mob.zombiepig.zpigangry" to "entity.piglin.angry",
        "mob.ghast.fireball" to "entity.ghast.shoot",

        // todo modern
        "random.successful_hit" to "",
        "mob.ghast.affectionate_scream" to "",
        "mob.guardian.elder.idle" to "",
        "random.eat" to "",
        "random.drink" to "",
        "mob.bat.idle" to "",
        "fire.ignite" to "",
        "random.wood_click" to "",
        "fireworks.launch" to "",
    )

    fun getModernSoundName(soundName: String): String {
        return soundMap[soundName] ?: soundName
    }

    fun getLegacySoundName(soundName: String): String {
        return soundMap.entries.firstOrNull { it.value == soundName }?.key ?: soundName
    }

}
