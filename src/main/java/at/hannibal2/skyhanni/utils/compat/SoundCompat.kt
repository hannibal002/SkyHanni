package at.hannibal2.skyhanni.utils.compat

object SoundCompat {

    // map of 1.8 sound names to modern sound names
    private val soundMap = mapOf(
        "dig.cloth" to "block.wool.break",
        "dig.glass" to "block.glass.break",
        "dig.gravel" to "block.gravel.break",
        "dig.stone" to "block.stone.break",
        "fire.ignite" to "item.flintandsteel.use",
        "fireworks.launch" to "entity.firework_rocket.launch",
        "gui.button.press" to "ui.button.click",
        "liquid.lavapop" to "block.lava.pop",
        "mob.bat.hurt" to "entity.bat.hurt",
        "mob.bat.idle" to "entity.bat.ambient",
        "mob.cat.hiss" to "entity.cat.hiss",
        "mob.cat.meow" to "entity.cat.ambient",
        "mob.cat.purr" to "entity.cat.purr",
        "mob.cat.purreow" to "entity.cat.purreow",
        "mob.chicken.say" to "entity.chicken.ambient",
        "mob.cow.say" to "entity.cow.ambient",
        "mob.enderdragon.growl" to "entity.ender_dragon.growl",
        "mob.endermen.portal" to "entity.enderman.teleport",
        "mob.ghast.affectionate_scream" to "entity.ghast.ambient",
        "mob.ghast.fireball" to "entity.ghast.shoot",
        "mob.ghast.scream" to "entity.ghast.scream",
        "mob.guardian.curse" to "entity.elder_guardian.curse",
        "mob.guardian.elder.idle" to "entity.elder_guardian.ambient",
        "mob.horse.donkey.death" to "entity.donkey.death",
        "mob.horse.donkey.hit" to "entity.donkey.hurt",
        "mob.pig.say" to "entity.pig.ambient",
        "mob.sheep.say" to "entity.sheep.ambient",
        "mob.wither.shoot" to "entity.wither.shoot",
        "mob.wither.spawn" to "entity.wither.spawn",
        "mob.wolf.bark" to "entity.wolf.ambient",
        "mob.wolf.death" to "entity.wolf.death",
        "mob.wolf.growl" to "entity.wolf.growl",
        "mob.wolf.howl" to "entity.wolf.death",
        "mob.wolf.panting" to "entity.wolf.pant",
        "mob.wolf.whine" to "entity.wolf.whine",
        "mob.zombie.remedy" to "entity.zombie_villager.cure",
        "mob.zombie.unfect" to "entity.zombie_villager.converted",
        "mob.zombiepig.zpigangry" to "entity.piglin.angry",
        "note.bassattack" to "block.note_block.bass",
        "note.harp" to "block.note_block.harp",
        "note.pling" to "block.note_block.pling",
        "random.anvil_break" to "block.anvil.break",
        "random.anvil_land" to "block.anvil.land",
        "random.burp" to "entity.player.burp",
        "random.chestopen" to "block.chest.open",
        "random.drink" to "entity.generic.drink",
        "random.eat" to "entity.generic.eat",
        "random.explode" to "entity.generic.explode",
        "random.levelup" to "entity.player.levelup",
        "random.orb" to "entity.experience_orb.pickup",
        "random.successful_hit" to "entity.arrow.hit_player",
        "random.wood_click" to "block.lever.click",
    )

    fun getModernSoundName(soundName: String): String {
        return soundMap[soundName] ?: soundName
    }

    fun getLegacySoundName(soundName: String): String {
        return soundMap.entries.firstOrNull { it.value == soundName }?.key ?: soundName
    }

}
