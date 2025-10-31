package at.hannibal2.hanni.features.gifting

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ItemInHandChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
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

    fun isHoldingGift() = SkyBlockUtils.inSkyBlock && holdingGift

    @HandleEvent
    fun onWorldChange() {
        holdingGift = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        holdingGift = giftNamePattern.matches(InventoryUtils.itemInHandId.asString())
    }
}
