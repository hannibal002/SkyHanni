package at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra

class KuudraTier(val name: String, val tierNumber: Int, var doneToday: Boolean = false) {
    fun getDisplayName() = "Tier $tierNumber ($name)"
}