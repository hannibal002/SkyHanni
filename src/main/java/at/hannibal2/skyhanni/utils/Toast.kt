package at.hannibal2.skyhanni.utils

import net.minecraft.item.ItemStack

data class Toast(
    val toastLines: List<String>,
    val toastItem: ItemStack?,
    val startTime: SimpleTimeMark,
    var endTime: SimpleTimeMark
)