package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.boss.BossStatus

object CopyBossbarCommand {
    fun command(args: Array<String>) {
        val noFormattingCodes = args.size == 1 && args[0] == "true"
        val (bossbarName, status) = if (noFormattingCodes) Pair(BossStatus.bossName.removeColor(), "without") else Pair(BossStatus.bossName, "with")
        if (bossbarName.isBlank() || bossbarName.isEmpty()) {
            LorenzUtils.chat("Boss bar name failed to copy to clipboard. It appears to be blank/empty.")
        } else {
            OSUtils.copyToClipboard(bossbarName)
            LorenzUtils.chat("Boss bar name copied to clipboard $status formatting codes!")
        }
    }
}
