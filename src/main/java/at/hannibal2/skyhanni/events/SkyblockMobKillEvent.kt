package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.SkyblockMobUtils

class SkyblockMobKillEvent (val mob: SkyblockMobUtils.SkyblockMob, val finalHit : Boolean) : LorenzEvent()