package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob

class SkyblockMobKillEvent(val mob: SkyblockMob, val finalHit: Boolean) : LorenzEvent()
