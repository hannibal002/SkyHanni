package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson.PlayerFriends.Friend
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID

object FriendAPI {
    // TODO USE SH-REPO
    private val removedFriendPattern =
        ".*\n§r§eYou removed §r(?<name>.*)§e from your friends list!§r§9§m\n.*".toPattern()
    private val addedFriendPattern = "§aYou are now friends with (?<name>.*)".toPattern()
    private val noBestFriendPattern = ".*\n§r(?<name>.*)§e is no longer a best friend!§r§9§m\n.*".toPattern()
    private val bestFriendPattern = ".*\n(?<name>.*)§a is now a best friend!§r§9§m\n.*".toPattern()
    private val readFriendListPattern = "/viewprofile (?<uuid>.*)".toPattern()

    private val tempFriends = mutableListOf<Friend>()

    private fun getFriends() = SkyHanniMod.friendsData.players.getOrPut(LorenzUtils.getRawPlayerUuid()) {
        FriendsJson.PlayerFriends().also { it.friends = mutableMapOf() }
    }.friends

    @SubscribeEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        if (SkyHanniMod.friendsData.players == null) {
            SkyHanniMod.friendsData.players = mutableMapOf()
            saveConfig()
        }
    }

    fun getAllFriends(): List<Friend> {
        val list = mutableListOf<Friend>()
        list.addAll(getFriends().values)
        list.addAll(tempFriends)
        return list
    }

    fun saveConfig() {
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FRIENDS, "Save file")
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        readFriendsList(event)

        removedFriendPattern.matchMatcher(event.message) {
            val name = group("name").cleanPlayerName()
            removedFriend(name)
        }
        addedFriendPattern.matchMatcher(event.message) {
            val name = group("name").cleanPlayerName()
            addFriend(name)
        }

        noBestFriendPattern.matchMatcher(event.message) {
            val name = group("name").cleanPlayerName()
            setBestFriend(name, false)
        }
        bestFriendPattern.matchMatcher(event.message) {
            val name = group("name").cleanPlayerName()
            setBestFriend(name, true)
        }
    }

    private fun setBestFriend(name: String, bestFriend: Boolean) {
        getFriends().entries.firstOrNull { it.value.name == name }?.let {
            it.value.bestFriend = bestFriend
            saveConfig()
        }
    }

    private fun addFriend(name: String) {
        tempFriends.add(Friend().also { it.name = name })
    }

    private fun removedFriend(name: String) {
        tempFriends.removeIf { it.name == name }
        getFriends().entries.removeIf { it.value.name == name }
        saveConfig()
    }

    private fun readFriendsList(event: LorenzChatEvent) {
        if (!event.message.contains("Friends")) return

        for (sibling in event.chatComponent.siblings) {
            val chatStyle = sibling.chatStyle ?: continue
            val value = chatStyle.chatClickEvent?.value ?: continue
            if (!value.startsWith("/viewprofile")) continue

            val uuid = readFriendListPattern.matchMatcher(value) {
                group("uuid")?.let {
                    try {
                        UUID.fromString(it)
                    } catch (e: IllegalArgumentException) {
                        ErrorManager.logErrorWithData(
                            e, "Error reading friend list.",
                            "raw uuid" to it,
                            "value" to value,
                            "chatStyle" to chatStyle,
                            "event.chatComponent" to event.chatComponent,
                            "event.message" to event.message,
                        )
                        return
                    }
                }
            }
            val bestFriend = sibling.unformattedText.contains("§l")
            val name = readName(chatStyle)
            if (uuid != null && name != null) {
                getFriends()[uuid] = Friend().also {
                    it.name = name
                    it.bestFriend = bestFriend
                }
            }
        }

        saveConfig()
    }

    private fun readName(chatStyle: ChatStyle): String? {
        for (component in chatStyle.chatHoverEvent.value.siblings) {
            val rawName = component.unformattedText
            val rawNamePattern = "\\n§eClick to view §.(?<name>.*)§e's profile".toPattern()
            rawNamePattern.matchMatcher(rawName) {
                return group("name")
            }
        }

        return null
    }
}
