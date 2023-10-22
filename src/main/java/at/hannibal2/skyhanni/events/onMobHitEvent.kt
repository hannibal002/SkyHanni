package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.combat.killDetection.hitTrigger
import at.hannibal2.skyhanni.utils.SkyblockMobUtils

class onMobHitEvent(val mob: SkyblockMobUtils.SkyblockMob, val trigger: hitTrigger, val isFirstHit: Boolean) :
    LorenzEvent()

