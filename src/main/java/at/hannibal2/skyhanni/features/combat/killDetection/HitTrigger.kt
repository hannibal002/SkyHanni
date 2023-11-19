package at.hannibal2.skyhanni.features.combat.killDetection

interface hitQualifier{
    fun isMagic() : Boolean
}

enum class hitTrigger : hitQualifier {
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
