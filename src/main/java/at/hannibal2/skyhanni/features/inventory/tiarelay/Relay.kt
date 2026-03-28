package at.hannibal2.skyhanni.features.inventory.tiarelay

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.phys.Vec3

// TODO use repo and remove color codes
enum class Relay(
    val relayName: String,
    val waypoint: Vec3,
    val island: IslandType,
    chatMessage: String,
) {

    RELAY_1(
        "1st Relay", Vec3(143.5, 108.0, 93.0), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe first relay is on a branch of the large tree on the north-east of the fairy pond."
    ),
    RELAY_2(
        "2nd Relay", Vec3(-246.5, 123.0, 55.5), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is in the castle ruins!"
    ),
    RELAY_3(
        "3rd Relay", Vec3(128.5, 232.0, 200.5), IslandType.DWARVEN_MINES,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is in the §bRoyal Palace §rwithin the Dwarven Mines."
    ),
    RELAY_4(
        "4th Relay", Vec3(-560.0, 164.0, -287.0), IslandType.THE_END,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on the highest spike of §dThe End§r."
    ),
    RELAY_5(
        "5th Relay", Vec3(-375.0, 207.0, -799.0), IslandType.CRIMSON_ISLE,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay was placed by our consultant, Odger."
    ),
    RELAY_6(
        "6th Relay", Vec3(-69.0, 157.0, -879.0), IslandType.CRIMSON_ISLE,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fScarleton itself has one of the most robust connection to the 9f™ Network."
    ),
    RELAY_7(
        "7th Relay", Vec3(93.0, 86.0, 187.0), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of the shack next to the shady inn right here close to the pond."
    ),
    RELAY_8(
        "8th Relay", Vec3(0.0, 146.0, -75.0), IslandType.DUNGEON_HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of a statue in the dungeon hub."
    ),
    RELAY_9(
        "9th Relay", Vec3(-19.0, 88.5, -91.0), IslandType.HUB,
        "§e[NPC] §dTia the Fairy§f: §b✆ §f§r§fThe next relay is on top of the Auction House."
    );

    val chatPattern by RepoPattern.pattern(
        "relay.chat." + relayName.takeWhile { it != ' ' },
        chatMessage
    )

    fun checkChatMessage(string: String) = chatPattern.matches(string)
}
