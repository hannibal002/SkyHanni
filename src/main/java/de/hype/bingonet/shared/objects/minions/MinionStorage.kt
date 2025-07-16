package de.hype.bingonet.shared.objects.minions

enum class MinionStorage(val displayName: String, slots: Int) {
    SMALL("Small Storage", 3),
    MEDIUM("Medium Storage", 9),
    LARGE("Large Storage", 15),
    XLARGE("X-Large Storage", 21),
    XXLARGE("XX-Large Storage", 27);

    fun applyToMinion(data: de.hype.bingonet.shared.objects.minions.AppliedMinionData) {
        data.minionData.storage += storage
    }

    val storage: Int = slots * 64
    val storageSlots: Int = slots
}
