package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.SkyblockMobUtils

class onMobHitEvent(val mob: SkyblockMobUtils.SkyblockMob, val trigger: hitTrigger, val isFirstHit: Boolean) :
    LorenzEvent()

interface hitQualifier{
    fun isMagic() : Boolean
}

enum class hitTrigger : hitQualifier{
    Melee{
         override fun isMagic() = false
         },
    Cleave {
        override fun isMagic() = false
    },
    Bow {
        override fun isMagic() = false
    },
    LMage{
        override fun isMagic() = true
    },
    AuroraStaff{
        override fun isMagic() = true
    }
}