package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object StereoHarmonyDiscReplacer {

    private val inventoryPattern by PestApi.patternGroup.pattern(
        "stereo.inventory",
        "Stereo Harmony"
    )

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!inventoryPattern.matches(event.inventory.name)) return
        val indexValid = event.slot in 11..15 || event.slot in 20..24
        if (!indexValid) return

        val internalName = event.originalItem.getInternalNameOrNull() ?: return
        val vinylType = VinylType.getByInternalNameOrNull(internalName) ?: return
        val pestType = PestType.getByVinylOrNull(vinylType) ?: return
        val cropType = pestType.crop ?: return
        val isActiveVinyl = StereoHarmonyDisplay.activeVinyl == vinylType
    }

}
