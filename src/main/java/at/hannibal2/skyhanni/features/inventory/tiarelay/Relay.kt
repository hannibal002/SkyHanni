package at.hannibal2.skyhanni.features.inventory.tiarelay

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class Relay(
    val relayName: String,
    val waypoint: LorenzVec,
    val island: IslandType,
    chatMessage: String,
) {

    RELAY_1(
        "1st Relay", LorenzVec(144.0, 108.0, 93.5), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe first relay is on a branch of the large tree on the north-east of the fairy pond."
    ),
    RELAY_2(
        "2nd Relay", LorenzVec(-246.0, 123.0, 56.0), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is in the castle ruins!"
    ),
    RELAY_3(
        "3rd Relay", LorenzVec(129.0, 232.0, 201.0), IslandType.DWARVEN_MINES,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is in the §bRoyal Palace §rwithin the Dwarven Mines."
    ),
    RELAY_4(
        "4th Relay", LorenzVec(-559.5, 164.0, -286.5), IslandType.THE_END,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on the highest spike of §dThe End§r."
    ),
    RELAY_5(
        "5th Relay", LorenzVec(-375.0, 207.0, -798.5), IslandType.CRIMSON_ISLE,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay was placed by our consultant, Odger."
    ),
    RELAY_6(
        "6th Relay", LorenzVec(-68.5, 157.5, -878.5), IslandType.CRIMSON_ISLE,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fScarleton itself has one of the most robust connection to the 9f™ Network."
    ),
    RELAY_7(
        "7th Relay", LorenzVec(93.5, 85.0, 187.5), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of the shack next to the shady inn right here close to the pond."
    ),
    RELAY_8(
        "8th Relay", LorenzVec(0.5, 146.5, -75.5), IslandType.DUNGEON_HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of a statue in the dungeon hub."
    ),
    RELAY_9(
        "9th Relay", LorenzVec(-23.0, 86.5, -12.5), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of the Auction House."
    );

    val chatPattern by RepoPattern.pattern(
        "relay.chat." + relayName.takeWhile { it != ' ' },
        chatMessage
    )

    fun checkChatMessage(string: String) = chatPattern.matches(string)
}
