package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object InstanceChestAPI {
    private val patternGroup = RepoPattern.group("combat.instance-chest-api")

    /**
     * REGEX-TEST: §6Paid Chest
     * REGEX-TEST: §6Paid
     * REGEX-TEST: §fFree Chest
     * REGEX-TEST: §fFree
     */
    /* Dungeon chests are just the chest Type for example just 'Emerald', Kuudra CURRENTLY has them as Free Chest/Paid Chest in the same UI
    if the Croesus main UI shows just Paid/Free this regex pattern should be removable mainly
    if the Croesus UI starts showing like "Emerald Chest" as the Chest Name the Regex should include all the cata chest names too then. */
    private val chestFutureProofing by patternGroup.pattern(
        "kuudrachest",
        "(?<chestname>§.(?:Free|Paid))(?: Chest)?",
    )

    /**
     * REGEX-TEST: Master Catacombs - Floor II
     * REGEX-TEST: Catacombs - Floor V
     * REGEX-TEST: Kuudra - Infernal
     */
    private val runNameCroesus by patternGroup.pattern(
        "runname",
        ".*Catacombs - Flo.*|Kuudra - .*",
    )

    enum class CroesusChestType(val stackChestName: String) {
        WOOD("§fWood"),
        GOLD("§6Gold"),
        DIAMOND("§bDiamond"),
        EMERALD("§2Emerald"),
        OBSIDIAN("§5Obsidian"),
        BEDROCK("§8Bedrock"),
        FREE("§fFree"),
        PAID("§6Paid"),
        ;

        companion object {
            fun getByStackName(stackName: String): CroesusChestType? {
                val newStackName = fixInstanceChestName(stackName)
                return entries.firstOrNull { it.stackChestName == newStackName }
            }

            fun getByInventoryName(): CroesusChestType? {
                var inventoryName = InventoryUtils.openInventoryName()
                inventoryName = inventoryName.replace(" Chest Chest", "")
                inventoryName = inventoryName.replace(" Chest", "")
                return entries.firstOrNull { it.stackChestName.removeColor() == inventoryName }
            }
        }
    }

    fun isInCroesusMenu() = runNameCroesus.matches(InventoryUtils.openInventoryName())

    fun isInstanceChestGUI() = CroesusChestType.getByInventoryName() != null

    fun fixInstanceChestName(chest: String): String {
        chestFutureProofing.matchMatcher(chest) {
            return group("chestname")
        }
        return chest
    }
}
