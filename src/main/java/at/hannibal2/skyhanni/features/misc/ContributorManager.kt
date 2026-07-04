package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.ComponentArgumentType
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.PlayerArgumentType
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.UuidArgumentType
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.features.achievements.ContributorAchievement
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.mapKeysNotNull
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import java.util.UUID

@SkyHanniModule
object ContributorManager {
    private val config get() = SkyHanniMod.feature.dev

    var contributors = emptyMap<UUID, ContributorJsonEntry>()
        private set(value) {
            field = value
            namesToUuid = value.entries
                .mapNotNull { (uuid, entry) ->
                    entry.displayName?.let { it to uuid }
                }
                .toMap()
            contributorNames = namesToUuid.keys.toList()
            isContributor = null
        }
    // Do not modify these: they are automatically updated when the contributors map is updated
    var contributorNames = emptyList<String>()
        private set
    private var namesToUuid = emptyMap<String, UUID>()
    private var isContributor: Boolean? = null

    private val seenContributors get() = SkyHanniMod.seenContributorStorage.seenContributors
    private val contributorMentions get() = SkyHanniMod.seenContributorStorage.contributorMentions
    private val contributorMentionersThisSession = mutableSetOf<String>()

    private const val CONTRIBUTOR_ACHIEVEMENT_GOT = "[SkyHanni] Achievement Get! EEEEKK!!"
    private const val FOUND_WILD_CONTRIBUTOR = "A wild SkyHanni contributor appears!"
    private val patternGroup = RepoPattern.group("contributor")

    /**
     * REGEX-TEST: skyhanni dev
     * REGEX-TEST: skyhani contributor
     * REGEX-TEST: skyhanni devs are the best
     * REGEX-TEST: sh dev in the house
     */
    private val contribMentionPattern by patternGroup.pattern(
        "mention",
        """\b(?:skyhanni|skyhani|sh)\b.*\b(?:dev\w*|contrib\w*)\b"""
    )

    private val repoReloadCoroutine = CoroutineSettings("contributor list repo reload")

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
        val map = event.getConstantAsync<ContributorsJson>("ContributorList").contributors

        contributors = map.mapKeysNotNull {
            try {
                UUID.fromString(it.key)
            } catch (e: IllegalArgumentException) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to parse contributor UUID",
                    "key" to it.key, "value" to it.value
                )
                null
            }
        }
    }

    // <editor-fold desc="Commands">
    @HandleEvent
    fun onCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("shlistseencontributors") {
            description = "List all contributors you've seen in chat or in nametags"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { listSeenContributors() }
        }

        event.registerBrigadier("shresetseencontributors") {
            description = "Reset the list of seen contributors"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetSeenContributors() }
        }

        event.registerBrigadier("shtestcontributor") {
            description = "Manage contributors: add or remove"
            category = CommandCategory.DEVELOPER_TEST

            literal("add") {
                literal("player") {
                    arg(
                        "username",
                        PlayerArgumentType.player(),
                        PlayerSuggestions.builder {
                            include(PlayerNameSource.ISLAND_PLAYERS)
                            filterNot { it in contributorNames }
                            include(PlayerNameSource.SELF)
                        },
                    ) { playerRef ->
                        callback {
                            val gameProfile = getArg(playerRef).gameProfile
                            addTestContributor(gameProfile.id, gameProfile.name, null)
                        }
                        argCallback("suffix", ComponentArgumentType.component(allowPlainText = true)) { suffix ->
                            val gameProfile = getArg(playerRef).gameProfile
                            addTestContributor(gameProfile.id, gameProfile.name, suffix)
                        }
                    }
                }
                literal("uuid") {
                    arg("uuid", UuidArgumentType.uuid()) { uuidRef ->
                        arg("displayName", BrigadierArguments.string()) { displayNameRef ->
                            callback {
                                addTestContributor(
                                    getArg(uuidRef),
                                    getArg(displayNameRef),
                                    null,
                                )
                            }
                            argCallback("suffix", ComponentArgumentType.component(allowPlainText = true)) { suffix ->
                                addTestContributor(
                                    getArg(uuidRef),
                                    getArg(displayNameRef),
                                    suffix,
                                )
                            }
                        }
                    }
                }
            }

            literal("remove") {
                argCallback("name", BrigadierArguments.string(), contributorNames) { name ->
                    removeTestContributor(name)
                }
            }
        }

        event.registerBrigadier("shaddcontributormention") {
            description = "Add/Remove a contributor mention."
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { addContributorMention() }
            argCallback("amount", BrigadierArguments.integer()) { amount ->
                addContributorMention(amount)
            }
        }
    }

    private fun addTestContributor(uuid: UUID, displayName: String, suffix: Component?) {
        val alreadyExists = contributors.containsKey(uuid)

        val testEntry = ContributorJsonEntry(
            displayName = displayName,
            componentSuffix = suffix
        )
        contributors = contributors + (uuid to testEntry)

        ChatUtils.chat {
            append(if (alreadyExists) "Test contributor updated: " else "Test contributor added: ")
            appendWithColor(displayName, ChatFormatting.AQUA)
            appendWithColor(" (UUID: $uuid)", ChatFormatting.GRAY)
        }
    }

    private fun removeTestContributor(displayName: String) {
        val uuidToRemove = contributors.entries.find { it.value.displayName == displayName }?.key
        if (uuidToRemove == null) {
            ChatUtils.userError("Contributor not found: $displayName")
            return
        }
        contributors = contributors.filterKeys { it != uuidToRemove }

        ChatUtils.chat {
            append("Test contributor removed: ")
            appendWithColor(displayName, ChatFormatting.AQUA)
        }
    }

    private fun listSeenContributors() {
        if (seenContributors.isEmpty()) {
            ChatUtils.chat("You haven't seen any contributors yet.")
            return
        }
        ChatUtils.clickableLinkChat(
            "If you need support, please do not contact contributors directly.\n" +
                "You can report issues or get help on the SkyHanni Discord.\n ",
            "https://discord.gg/skyhanni-997079228510117908",
            prefixColor = "§c"
        )
        ChatUtils.clickableChat(
            "[View seen contributors]",
            oneTimeClick = true,
            prefix = false,
            onClick = {
                ChatUtils.chat {
                    append("Seen contributors (${seenContributors.size}):\n")
                    appendWithColor(
                        seenContributors.keys.joinToString("\n")
                            { uuid -> getDisplayNameFromUUID(uuid) ?: uuid.toString() },
                        ChatFormatting.AQUA,
                    )
                }
            },
            hover = "§eClick to view contributors you've encountered."
        )
    }

    private fun resetSeenContributors() {
        ChatUtils.clickableChat(
            "Are you sure you want to clear the list of seen contributors? This cannot be undone.",
            oneTimeClick = true,
            onClick = {
                seenContributors.clear()
                saveConfig("cleared seen contributors list")
                ChatUtils.chat {
                    appendWithColor("Seen contributors list cleared.", ChatFormatting.GREEN)
                }
            },
            hover = "§eClick to confirm clearing the seen contributors list."
        )
    }
    // </editor-fold>

    @HandleEvent
    fun onPlayerAllChat(event: PlayerAllChatEvent.Allow) {
        if (!isSelfContributor()) return
        if (!config.contributorMentionTracker) return
        val msg = event.messageComponent.getText()
        if (!isContributorMentionMessage(msg)) return
        val author = event.author
        // contributorNames includes the current player
        if (author in contributorNames) return
        if (!contributorMentionersThisSession.add(author)) return

        ChatUtils.clickableChat(
            "Hey people seem to be talking about you, want to increment the popularity counter?",
            oneTimeClick = true,
            onClick = { addContributorMention() },
            hover = "§eClick to add this contributor mention",
        )
    }

    private fun addContributorMention(amount: Int = 1) {
        if (!isSelfContributor()) {
            ChatUtils.userError("Only contributors can add contributor mentions to the counter.")
            return
        }

        when {
            amount > 0 -> {
                val ts = SimpleTimeMark.now()
                repeat(amount) {
                    contributorMentions.addLast(ts)
                }
            }

            amount < 0 -> {
                val amountToRemove = minOf(-amount, contributorMentions.size)
                repeat(amountToRemove) {
                    contributorMentions.removeLastOrNull()
                }
            }

            else -> {
                ChatUtils.userError("Amount cannot be 0.")
                return
            }
        }

        ChatUtils.chat("Total contributor mentions: ${contributorMentions.size}")
        saveConfig("added contributor mention record")
    }

    private fun isContributorMentionMessage(message: String): Boolean {
        if (message.startsWith(CONTRIBUTOR_ACHIEVEMENT_GOT)) return true
        if (message.startsWith(FOUND_WILD_CONTRIBUTOR)) return true

        val msg = message.lowercase()
        return contribMentionPattern.find(msg)
    }

    @HandleEvent
    fun onRenderNametag(event: EntityDisplayNameEvent<Player>) {
        val gameProfile = event.entity.gameProfile
        getSuffix(gameProfile.id)?.let {
            recordSeenContributor(gameProfile.id, gameProfile.name)
            if (!config.contributorNametags) return
            event.chatComponent.append(it)
        }
    }

    private fun recordSeenContributor(uuid: UUID, username: String) {
        if (uuid == PlayerUtils.getRawUuid()) return
        if (uuid in seenContributors) return
        seenContributors[uuid] = SimpleTimeMark.now()

        if (config.discoverContributorMessage) {
            ChatUtils.chat {
                appendWithColor(FOUND_WILD_CONTRIBUTOR, ChatFormatting.GOLD)
                appendWithColor(" (hover)", ChatFormatting.GRAY)
                hover = componentBuilder {
                    appendWithColor("You have encountered ", ChatFormatting.GRAY)
                    appendWithColor(username, ChatFormatting.AQUA)
                    appendWithColor(" for the first time!", ChatFormatting.GRAY)
                }
            }
        }

        saveConfig("added new seen contributor")
        ContributorAchievement.onUniqueContributorSeen()
    }

    fun getDisplayNameFromUUID(uuid: UUID): String? = contributors[uuid]?.displayName
    fun getUUIDFromDisplayName(displayName: String): UUID? = namesToUuid[displayName]

    fun getSuffix(uuid: UUID): Component? {
        return contributors[uuid]?.componentSuffix ?: Component.literal(contributors[uuid]?.suffix ?: return null)
    }

    fun shouldSpin(uuid: UUID): Boolean = contributors[uuid]?.spinny ?: false
    fun shouldBeUpsideDown(uuid: UUID): Boolean = contributors[uuid]?.upsideDown ?: false

    // Due to using PlayerUtils.getRawUuid(), this will only work if logged in
    // which is why it HAS to be a lazy-loaded value instead of being calculated on repo load
    fun isSelfContributor(): Boolean {
        isContributor?.let { return it }

        val result = PlayerUtils.getRawUuid() in contributors
        isContributor = result
        return result
    }

    private fun saveConfig(reason: String) {
        SkyHanniMod.configManager.saveConfig(
            ConfigFileType.SEEN_CONTRIBUTORS,
            reason
        )
    }
}
