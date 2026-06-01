package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandType.Companion.isInAnyIsland
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.misc.UserLuckBreakdown
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.chatMessage
import at.hannibal2.skyhanni.utils.ChatUtils.passedSinceSent
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.isVowel
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object RareDropMessages {

    private val repoGroup = RepoPattern.group("raredrop")
    private val petGroup = repoGroup.group("pet")

    /**
     * REGEX-TEST: §6§lPET DROP! §r§5Baby Yeti §r§b(+§r§b168% §r§b✯ Magic Find§r§b)
     * REGEX-TEST: §6§lPET DROP! §r§5Baby Yeti §r§b(+§r§b168 §r§b✯ Magic Find§r§b)
     * REGEX-TEST: §6§lPET DROP! §r§5Slug §6(§6+1300☘)
     * REGEX-TEST: §6§lPET DROP! §r§6Rat
     */
    private val petDroppedPattern by petGroup.pattern(
        "droppedmessage",
        "(?<start>(?:§.)*PET DROP! )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>(?: .*)?)",
    )

    /**
     * REGEX-TEST: §6⛃ §r§6§lGREAT CATCH! §r§fYou caught a §r§7[Lvl 1] §r§aSquid§r§f!
     */
    private val petFishedPattern by petGroup.pattern(
        "fishedmessage",
        "(?<start>(?:§.)*⛃ (?:§.)*(?:GOOD|GREAT|OUTSTANDING) CATCH! (?:§.)*You caught a (?:§.)*\\[Lvl 1] )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*)",
    )

    /**
     * REGEX-TEST: §aYou claimed a §5Tarantula Pet§a! §r§aYou can manage your Pets in the §r§fPets Menu§r§a in your §r§fSkyBlock Menu§r§a.
     */
    private val petClaimedPattern by petGroup.pattern(
        "claimedmessage",
        "(?<start>(?:§.)*You claimed a )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*You can manage your Pets.*)",
    )

    /**
     * REGEX-TEST: §b[MVP§r§c+§r§b] Empa_§r§f §r§ehas obtained §r§a§r§7[Lvl 1] §r§6Bal§r§e!
     */
    private val petObtainedPattern by petGroup.pattern(
        "obtainedmessage",
        "(?<start>.*has obtained (?:§.)*\\[Lvl 1] )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*)",
    )

    /**
     * REGEX-TEST: SMITE;6
     * REGEX-TEST: ENDER_SLAYER;7
     * REGEX-TEST: ULTIMATE_REITERATE;1
     */
    private val slayerBookIDPattern by repoGroup.pattern(
        "slayerbook",
        "SMITE;[67]|ENDER_SLAYER;[67]|MANA_STEAL;1|SMARTY_PANTS;1|BANE_OF_ARTHROPODS;6|CRITICAL;6|FIRE_ASPECT;3|ULTIMATE_REITERATE;1",
    )

    /**
     * REGEX-TEST: §e[NPC] Oringo§f: §b✆ §f§r§8• §fBlue Whale Pet
     * REGEX-TEST: §e[NPC] Oringo§f: §b✆ §f§r§8• §5Giraffe Pet
     */
    private val oringoPattern by petGroup.pattern(
        "oringomessage",
        "(?<start>§e\\[NPC] Oringo§f: §b✆ §f§r§8• )§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end> Pet)",
    )

    /**
     * REGEX-TEST: RARE DROP! Enchanted Book
     * REGEX-TEST: RARE DROP! Enchanted Book (+208% ✯ Magic Find)
     * REGEX-TEST: RARE DROP! Enchanted Book (+208 ✯ Magic Find)
     */
    private val enchantedBookPattern by repoGroup.pattern(
        "enchantedbook.colorless",
        "RARE DROP! Enchanted Book(?: \\(\\+\\d+%? ✯ Magic Find\\))?.*",
    )

    private val petPatterns = listOf(
        petDroppedPattern, petFishedPattern, petClaimedPattern, petObtainedPattern, oringoPattern,
    )

    private val ignoredBookIslands = setOf(
        IslandType.DARK_AUCTION,
        IslandType.DUNGEON_HUB,
        IslandType.CATACOMBS,
        IslandType.KUUDRA_ARENA,
    )

    private val userLuck get() = UserLuckBreakdown.getTotalUserLuck()

    private val config get() = SkyHanniMod.feature.chat.rareDropMessages

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (!config.petRarity) return

        petPatterns.matchMatchers(event.message) {
            var start = group("start")
            val rarityColor = group("rarityColor")
            val rarity = LorenzRarity.getByColorCode(rarityColor.first()) ?: return@matchMatchers
            val rarityName = rarity.formattedName.uppercase()
            val petName = group("petName")
            val end = group("end")
            if (start.endsWith("a ") && rarityName.first().isVowel())
                start = start.substring(0..start.length - 2) + "n "


            val newComponent = "$start§$rarityColor§l$rarityName §$rarityColor$petName$end".asComponent()
            event.replaceComponent(newComponent, "pet_rarity")

        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemAdd(event: ItemAddEvent) {
        if (event.amount != 1 || event.source != ItemAddManager.Source.ITEM_ADD) return
        if (!config.enchantedBook || !config.enchantedBookMissingMessage) return
        val internalName = event.internalName
        val category = internalName.getItemStackOrNull()?.getItemCategoryOrNull() ?: return
        if (category != ItemCategory.ENCHANTED_BOOK) return
        if (ignoredBookIslands.isInAnyIsland()) return

        val itemName = internalName.repoItemName
        var anyRecentMessage = false
        for (line in ChatUtils.chatMessages) {
            if (line.passedSinceSent() > 1.seconds) break
            val message = line.content.string.removeColor()
            if (itemName in message) return // the message already has the enchant name
            if (enchantedBookPattern.matches(message)) {
                anyRecentMessage = true
                break
            }
        }

        if (anyRecentMessage && config.enchantedBook) {
            ChatManager.editMessage(
                replacement = {
                    it.formattedTextCompat().replace("Enchanted Book", internalName.repoItemName).asComponent()
                },
                reason = "enchanted book",
            ) { it.passedSinceSent() < 1.seconds && enchantedBookPattern.matches(it.chatMessage) }
        }

        // Hypixel sends slayer book messages late, so we do a manual internalName regex match
        if (!anyRecentMessage && config.enchantedBookMissingMessage && slayerBookIDPattern.matches(internalName.asString())) {
            var message = "§r§6§lRARE DROP! ${internalName.repoItemName}"
            if (SkyHanniMod.feature.misc.userLuck) {
                userLuck.takeIf { it != 0f }?.let { luck ->
                    var luckString = luck.roundTo(2).addSeparators()
                    if (luck > 0) luckString = "+$luckString"
                    message += " §a($luckString ✴ SkyHanni User Luck)"
                }
            }
            ChatUtils.chat(message, prefix = false)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(71, "chat.petRarityDropMessage", "chat.rareDropMessages.petRarity")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestenchantedbookname") {
            description = "Test Enchanted Book Name feature"
            category = CommandCategory.DEVELOPER_TEST

            simpleCallback {
                ChatUtils.chat("§6§lRARE DROP! §r§fEnchanted Book §r§b(+§r§b208% §r§b✯ Magic Find§r§b)", prefix = false)
                ChatUtils.chat("Testing Enchanted Book Name")
                onItemAdd(ItemAddEvent("ANGLER;6".toInternalName(), 1, ItemAddManager.Source.ITEM_ADD))
            }
        }
    }
}
