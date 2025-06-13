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
        "mob.bat.idle" to "entity.bat.ambient",
        "mob.cat.meow" to "entity.cat.ambient",
        "mob.chicken.say" to "entity.chicken.ambient",
        "mob.cow.say" to "entity.cow.ambient",
        "mob.enderdragon.growl" to "entity.ender_dragon.growl",
        "mob.enderman.portal" to "entity.enderman.teleport",
        "mob.ghast.affectionate_scream" to "entity.ghast.ambient",
        "mob.ghast.fireball" to "entity.ghast.shoot",
        "mob.ghast.scream" to "entity.ghast.scream",
        "mob.guardian.curse" to "entity.elder_guardian.curse",
        "mob.horse.donkey.death" to "entity.donkey.death",
        "mob.horse.donkey.hit" to "entity.donkey.hurt",
        "mob.pig.say" to "entity.pig.ambient",
        "mob.sheep.say" to "entity.sheep.ambient",
        "mob.wolf.bark" to "entity.wolf.ambient",
        "mob.wolf.panting" to "entity.wolf.pant",
        "mob.wolf.whine" to "entity.wolf.whine",
        "mob.zombie.remedy" to "entity.zombie_villager.cure",
        "mob.zombie.unfect" to "entity.zombie_villager.converted",
        "mob.zombiepig.zpigangry" to "entity.piglin.angry",
        "note.bassattack" to "block.note_block.bass",
        "note.harp" to "block.note_block.harp",
        "random.anvil_break" to "block.anvil.break",
        "random.anvil_land" to "block.anvil.land",
        "random.burp" to "entity.player.burp",
        "random.chestopen" to "block.chest.open",
        "random.drink" to "entity.generic.drink",
        "random.eat" to "entity.generic.eat",
        "random.explode" to "entity.generic.explode",
        "random.levelup" to "entity.player.levelup",
        "random.orb" to "entity.experience_orb.pickup",

        // todo modern
        "random.successful_hit" to "",
        "mob.guardian.elder.idle" to "",
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
