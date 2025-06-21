package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.RepoItemEditorConfig
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object AttributeShardExporter {
    private val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    private val patternGroup = RepoPattern.group("dev.repoitemeditor")

    /**
     * REGEX-TEST: Shards ➜ Grove
     * REGEX-TEST: Shards ➜ Barbarian Duke X
     */
    private val shardInventoryPattern by patternGroup.pattern(
        "shardinventory",
        "Shards ➜ (?<shardName>.+)",
    )

    /**
     * REGEX-TEST: §7Grants §c+2 §c❤ Health§7.
     * REGEX-TEST: §7Grants §6§6+10 §6☘ Fig Fortune §7and §6§6+10
     * REGEX-TEST: §7Grants §3+0.5 §3α Sea Creature Chance§7.
     * REGEX-TEST: §7Grants an §a+2% §7chance to obtain an
     * REGEX-TEST: §7Gain §a+1% §7more §6Coins §7from fishing
     * REGEX-TEST: §7Grants an §a+10%§7 chance to drop
     */
    private val shardBoostPattern by patternGroup.pattern(
        "shardboost",
        "\\+(?<amount>[\\d.]+)",
    )

    /**
     * REGEX-TEST: §b§6Monster Bait I
     * REGEX-TEST: §b§6Light Elemental I
     */
    private val shardPerkPattern by patternGroup.pattern(
        "shardperk",
        "§b§6(?<perkName>.+) I",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.editModeEnabled) return
        if (!shardInventoryPattern.matches(event.inventoryName)) return

        val targetStack = event.inventoryItems[13] ?: return
        val internalName = targetStack.getInternalNameOrNull()?.asString() ?: return
        val split = internalName.split(";")
        val baseInternalName = split.first()

        for (line in targetStack.getLore()) {
            shardBoostPattern.findMatcher(line) {
                processShard(baseInternalName, targetStack, group("amount"))
                println("processed shard with amount: ${group("amount")}")
                return
            }
        }
        ChatUtils.chat("§cNo Shard Boost found in the current inventory!")
    }

    private fun processShard(baseInternalName: String, baseStack: ItemStack, matchedString: String) {
        val amount = matchedString.toDoubleOrNull() ?: run {
            ChatUtils.chat("§cInvalid Shard Boost amount: $matchedString")
            return
        }
        if (amount % 1 == 0.0) {
            for (tier in 1..10) {
                val newNumber = (amount.toInt() * tier).toString()
                baseStack.saveWithReplacement(baseInternalName, tier, matchedString, newNumber)
            }
        } else {
            for (tier in 1..10) {
                val newNumber = (amount * tier).toString()
                baseStack.saveWithReplacement(baseInternalName, tier, matchedString, newNumber)
            }
        }
    }

    private fun ItemStack.saveWithReplacement(baseInternalName: String, tier: Int, oldNumber: String, newNumber: String) {
        val newInternalName = "$baseInternalName;$tier"

        val newLore = this.getLore().map { line ->
            if (shardPerkPattern.matches(line)) {
                "${line.removeSuffix("I")}${tier.toRoman()}"
            } else {
                line.replace(oldNumber, newNumber)
            }
        }

        val newStack = this.copy()
        newStack.setLore(newLore)
        val extraAttributes = newStack.extraAttributes
        val attributes = extraAttributes.getCompoundTag("attributes")
        val attributeKey = attributes.keySet.firstOrNull() ?: return
        attributes.setInteger(attributeKey, tier)
        //#if MC < 1.21
        newStack.extraAttributes.apply { setTag("attributes", attributes) }
        //#else
        //$$ newStack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(extraAttributes))
        //#endif
        RepoItemEditor.openItemInEditor(newStack, instantSave = true, internalNameOverride = newInternalName.toInternalName())
    }

}
