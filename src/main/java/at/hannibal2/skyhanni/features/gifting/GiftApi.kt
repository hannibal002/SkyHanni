package at.hannibal2.skyhanni.features.gifting

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object GiftApi {

    private val patternGroup = RepoPattern.group("event.winter.giftapi")

    /**
     * REGEX-TEST: WHITE_GIFT
     * REGEX-TEST: RED_GIFT
     * REGEX-TEST: GREEN_GIFT
     */
    private val giftNamePattern by patternGroup.pattern(
        "giftname",
        "(?:WHITE|RED|GREEN)_GIFT\$",
    )

    private var holdingGift = false

    fun isHoldingGift() = LorenzUtils.inSkyBlock && holdingGift

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        holdingGift = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        holdingGift = giftNamePattern.matches(InventoryUtils.itemInHandId.asString())
    }
}
