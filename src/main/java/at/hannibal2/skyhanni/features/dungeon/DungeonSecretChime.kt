package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ClickedBlockType
import at.hannibal2.hanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.dungeon.DungeonBlockClickEvent
import at.hannibal2.hanni.events.entity.EntityRemovedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.SoundUtils.playSound
import net.minecraft.entity.item.EntityItem

@HanniModule
object DungeonSecretChime {
    private val config get() = HanniMod.feature.dungeon.secretChime
    private var dungeonSecretItems = setOf<NeuInternalName>()

    @HandleEvent
    fun onDungeonClickedBlock(event: DungeonBlockClickEvent) {
        if (!isEnabled()) return
        if (DungeonApi.inWaterRoom && event.blockType == ClickedBlockType.LEVER) return

        when (event.blockType) {
            ClickedBlockType.CHEST,
            ClickedBlockType.TRAPPED_CHEST,
            ClickedBlockType.LEVER,
            ClickedBlockType.WITHER_ESSENCE,
            -> playSound()
        }
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (isEnabled() && event.mob.name == "Dungeon Secret Bat") {
            playSound()
        }
    }

    @HandleEvent
    fun onItemPickup(event: EntityRemovedEvent<EntityItem>) {
        if (!isEnabled()) return
        val itemName = event.entity.entityItem.displayName
        if (NeuInternalName.fromItemName(itemName) in dungeonSecretItems) {
            playSound()
        }
    }

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        with(config.muteSecretSound) {
            if (!muteChestSound && !muteLeverSound) return
            if (muteChestSound && event.isChestSound()) event.cancel()
            if (muteLeverSound && event.isLeverSound()) event.cancel()
        }
    }

    private fun PlaySoundEvent.isChestSound(): Boolean {
        return when (soundName) {
            "random.chestopen" -> volume == 0.5f
            "note.harp" ->
                volume == 1f && pitch in setOf(0.7936508f, 0.8888889f, 1f, 1.0952381f, 1.1904762f)

            else -> false
        }
    }

    private fun PlaySoundEvent.isLeverSound(): Boolean {
        return when (soundName) {
            "random.anvil_break" -> volume == 1f && pitch == 1.6984127f
            "random.wood_click" -> volume in setOf(1f, 2f) && pitch == 0.4920635f
            else -> false
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        dungeonSecretItems = data.dungeonSecretItems
    }

    private fun isEnabled() = DungeonApi.inDungeon() && !DungeonApi.inBossRoom && config.enabled

    @JvmStatic
    fun playSound() {
        with(config) {
            SoundUtils.createSound(soundName, soundPitch, 100f).playSound()
        }
    }
}
