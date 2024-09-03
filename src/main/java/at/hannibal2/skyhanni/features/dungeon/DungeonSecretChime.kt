package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DungeonSecretChime {
    private val config get() = SkyHanniMod.feature.dungeon.secretChime

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled() || event.clickType != ClickType.RIGHT_CLICK) return

        val position = event.position
        val blockType = when (position.getBlockAt()) {
            Blocks.chest, Blocks.trapped_chest, Blocks.lever, Blocks.skull -> true
            else -> false
        }

        if (event.position.getBlockAt() == Blocks.skull) {
            val text = BlockUtils.getTextureFromSkull(position.toBlockPos())
            if (text != "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQ" +
                "ubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3M" +
                "DdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="
            ) {
                return
            }
        }

        if (blockType) playSound()
    }

    fun isEnabled() = !DungeonAPI.inBossRoom && DungeonAPI.inDungeon() && config.enabled

    @JvmStatic
    fun playSound() {
        with(config) {
            SoundUtils.createSound(name, pitch).playSound()
        }
    }
}
