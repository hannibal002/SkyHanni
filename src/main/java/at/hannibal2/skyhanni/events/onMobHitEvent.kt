package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.features.combat.killDetection.hitTrigger

class onMobHitEvent(val mob: SkyblockMob, val trigger: hitTrigger, val isFirstHit: Boolean) : LorenzEvent()

