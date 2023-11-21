package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.MobData

abstract class MobEvent(val mob: MobData.Mob) : LorenzEvent() {
    open class Spawn(mob: MobData.Mob) : MobEvent(mob) {
        class SkyblockMob(mob: MobData.Mob) : Spawn(mob)
        class Summon(mob: MobData.Mob) : Spawn(mob)
        class Player(mob: MobData.Mob) : Spawn(mob)
        class DisplayNPC(mob: MobData.Mob) : Spawn(mob)
        class Special(mob: MobData.Mob) : Spawn(mob)
        class Projectile(mob: MobData.Mob) : Spawn(mob)
    }

    open class DeSpawn(mob: MobData.Mob) : MobEvent(mob) {
        class SkyblockMob(mob: MobData.Mob) : DeSpawn(mob)
        class Summon(mob: MobData.Mob) : DeSpawn(mob)
        class Player(mob: MobData.Mob) : DeSpawn(mob)
        class DisplayNPC(mob: MobData.Mob) : DeSpawn(mob)
        class Special(mob: MobData.Mob) : DeSpawn(mob)
        class Projectile(mob: MobData.Mob) : DeSpawn(mob)
    }
}
