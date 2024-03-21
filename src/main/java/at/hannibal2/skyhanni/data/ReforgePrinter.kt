package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.ReforgeAPI
import at.hannibal2.skyhanni.api.ReforgeAPI.ReforgeType
import at.hannibal2.skyhanni.api.ReforgeAPI.StatType
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import com.google.gson.GsonBuilder
import java.io.FileOutputStream

class ReforgePrinter {
    val reforgesPrint = listOf(
        ReforgeAPI.Reforge(
            "Epic", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.CRIT_DAMAGE to 10.0, StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.CRIT_DAMAGE to 15.0, StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_DAMAGE to 20.0, StatType.BONUS_ATTACK_SPEED to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 32.0, StatType.CRIT_DAMAGE to 27.0, StatType.BONUS_ATTACK_SPEED to 7.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 40.0, StatType.CRIT_DAMAGE to 35.0, StatType.BONUS_ATTACK_SPEED to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 50.0, StatType.CRIT_DAMAGE to 45.0, StatType.BONUS_ATTACK_SPEED to 15.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Fair", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.CRIT_DAMAGE to 3.0,
                    StatType.INTELLIGENCE to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 4.0,
                    StatType.CRIT_DAMAGE to 4.0,
                    StatType.INTELLIGENCE to 4.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0,
                    StatType.CRIT_CHANCE to 7.0,
                    StatType.CRIT_DAMAGE to 7.0,
                    StatType.INTELLIGENCE to 7.0,
                    StatType.BONUS_ATTACK_SPEED to 7.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 10.0,
                    StatType.CRIT_DAMAGE to 10.0,
                    StatType.INTELLIGENCE to 10.0,
                    StatType.BONUS_ATTACK_SPEED to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.CRIT_DAMAGE to 12.0,
                    StatType.INTELLIGENCE to 12.0,
                    StatType.BONUS_ATTACK_SPEED to 12.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Fast", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.BONUS_ATTACK_SPEED to 10.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.BONUS_ATTACK_SPEED to 20.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.BONUS_ATTACK_SPEED to 30.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.BONUS_ATTACK_SPEED to 40.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.BONUS_ATTACK_SPEED to 50.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.BONUS_ATTACK_SPEED to 60.0)
            )
        ), ReforgeAPI.Reforge(
            "Gentle", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.BONUS_ATTACK_SPEED to 8.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.BONUS_ATTACK_SPEED to 10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.BONUS_ATTACK_SPEED to 15.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.BONUS_ATTACK_SPEED to 20.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.BONUS_ATTACK_SPEED to 25.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.BONUS_ATTACK_SPEED to 30.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Heroic", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.INTELLIGENCE to 40.0, StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.INTELLIGENCE to 50.0, StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.INTELLIGENCE to 65.0, StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 32.0, StatType.INTELLIGENCE to 80.0, StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 40.0, StatType.INTELLIGENCE to 100.0, StatType.BONUS_ATTACK_SPEED to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 50.0, StatType.INTELLIGENCE to 125.0, StatType.BONUS_ATTACK_SPEED to 7.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Legendary", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 5.0,
                    StatType.CRIT_DAMAGE to 5.0,
                    StatType.INTELLIGENCE to 5.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0,
                    StatType.CRIT_CHANCE to 7.0,
                    StatType.CRIT_DAMAGE to 10.0,
                    StatType.INTELLIGENCE to 8.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 9.0,
                    StatType.CRIT_DAMAGE to 15.0,
                    StatType.INTELLIGENCE to 12.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.CRIT_DAMAGE to 22.0,
                    StatType.INTELLIGENCE to 18.0,
                    StatType.BONUS_ATTACK_SPEED to 7.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0,
                    StatType.CRIT_CHANCE to 15.0,
                    StatType.CRIT_DAMAGE to 28.0,
                    StatType.INTELLIGENCE to 25.0,
                    StatType.BONUS_ATTACK_SPEED to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 32.0,
                    StatType.CRIT_CHANCE to 18.0,
                    StatType.CRIT_DAMAGE to 36.0,
                    StatType.INTELLIGENCE to 35.0,
                    StatType.BONUS_ATTACK_SPEED to 15.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Odd", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 5.0, StatType.INTELLIGENCE to -5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 10.0, StatType.INTELLIGENCE to -10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 15.0, StatType.INTELLIGENCE to -18.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 20.0, StatType.CRIT_DAMAGE to 22.0, StatType.INTELLIGENCE to -32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 30.0, StatType.INTELLIGENCE to -50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 30.0, StatType.CRIT_DAMAGE to 40.0, StatType.INTELLIGENCE to -75.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Sharp", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 20.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 30.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 14.0, StatType.CRIT_DAMAGE to 40.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 17.0, StatType.CRIT_DAMAGE to 55.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 20.0, StatType.CRIT_DAMAGE to 75.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 90.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Spicy", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 25.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 35.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 45.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 60.0,
                    StatType.BONUS_ATTACK_SPEED to 7.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 80.0,
                    StatType.BONUS_ATTACK_SPEED to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 100.0,
                    StatType.BONUS_ATTACK_SPEED to 15.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Awkward", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 5.0, StatType.INTELLIGENCE to -5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 10.0, StatType.INTELLIGENCE to -10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 15.0, StatType.INTELLIGENCE to -18.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 20.0, StatType.CRIT_DAMAGE to 22.0, StatType.INTELLIGENCE to -32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 30.0, StatType.INTELLIGENCE to -50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 30.0, StatType.CRIT_DAMAGE to 35.0, StatType.INTELLIGENCE to -72.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Deadly", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 13.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 16.0, StatType.CRIT_DAMAGE to 18.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 19.0, StatType.CRIT_DAMAGE to 32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 22.0, StatType.CRIT_DAMAGE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 78.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Fine", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.CRIT_CHANCE to 5.0, StatType.CRIT_DAMAGE to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_CHANCE to 7.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.CRIT_CHANCE to 9.0, StatType.CRIT_DAMAGE to 7.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0, StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 15.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 33.0, StatType.CRIT_CHANCE to 18.0, StatType.CRIT_DAMAGE to 20.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Grand", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 32.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 40.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 50.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 60.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 75.0)
            )
        ), ReforgeAPI.Reforge(
            "Hasty", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.CRIT_CHANCE to 20.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.CRIT_CHANCE to 25.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_CHANCE to 30.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.CRIT_CHANCE to 40.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.CRIT_CHANCE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.CRIT_CHANCE to 60.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Neat", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 4.0, StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 8.0, StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 14.0, StatType.CRIT_DAMAGE to 14.0, StatType.INTELLIGENCE to 10.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 17.0, StatType.CRIT_DAMAGE to 20.0, StatType.INTELLIGENCE to 15.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 20.0, StatType.CRIT_DAMAGE to 30.0, StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 40.0, StatType.INTELLIGENCE to 30.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Rapid", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.CRIT_DAMAGE to 35.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.CRIT_DAMAGE to 45.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.CRIT_DAMAGE to 55.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_DAMAGE to 65.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.CRIT_DAMAGE to 75.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.CRIT_DAMAGE to 90.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Rich", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 2.0, StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 4.0, StatType.INTELLIGENCE to 5.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 14.0, StatType.CRIT_DAMAGE to 7.0, StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 17.0, StatType.CRIT_DAMAGE to 10.0, StatType.INTELLIGENCE to 15.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 20.0, StatType.CRIT_DAMAGE to 15.0, StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 25.0, StatType.CRIT_DAMAGE to 20.0, StatType.INTELLIGENCE to 30.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Unreal", ReforgeType.BOW, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.CRIT_CHANCE to 8.0, StatType.CRIT_DAMAGE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_CHANCE to 9.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 18.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0, StatType.CRIT_CHANCE to 11.0, StatType.CRIT_DAMAGE to 32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_CHANCE to 13.0, StatType.CRIT_DAMAGE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 34.0, StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 70.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Clean", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 5.0, StatType.DEFENSE to 5.0, StatType.CRIT_CHANCE to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 7.0, StatType.DEFENSE to 7.0, StatType.CRIT_CHANCE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0, StatType.DEFENSE to 10.0, StatType.CRIT_CHANCE to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.DEFENSE to 15.0, StatType.CRIT_CHANCE to 8.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.DEFENSE to 20.0, StatType.CRIT_CHANCE to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 25.0, StatType.DEFENSE to 25.0, StatType.CRIT_CHANCE to 12.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Fierce", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.CRIT_CHANCE to 2.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.CRIT_CHANCE to 3.0, StatType.CRIT_DAMAGE to 7.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0, StatType.CRIT_CHANCE to 4.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0, StatType.CRIT_CHANCE to 5.0, StatType.CRIT_DAMAGE to 14.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.CRIT_CHANCE to 6.0, StatType.CRIT_DAMAGE to 18.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.CRIT_CHANCE to 8.0, StatType.CRIT_DAMAGE to 24.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Heavy", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 25.0, StatType.CRIT_DAMAGE to -1.0, StatType.SPEED to -1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 35.0, StatType.CRIT_DAMAGE to -2.0, StatType.SPEED to -1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 50.0, StatType.CRIT_DAMAGE to -2.0, StatType.SPEED to -1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 65.0, StatType.CRIT_DAMAGE to -3.0, StatType.SPEED to -1.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 80.0, StatType.CRIT_DAMAGE to -5.0, StatType.SPEED to -1.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 110.0, StatType.CRIT_DAMAGE to -7.0, StatType.SPEED to -1.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Light", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 5.0,
                    StatType.DEFENSE to 1.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 2.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.SPEED to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 3.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 3.0,
                    StatType.SPEED to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0,
                    StatType.DEFENSE to 4.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 4.0,
                    StatType.SPEED to 4.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0,
                    StatType.DEFENSE to 5.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.CRIT_DAMAGE to 5.0,
                    StatType.SPEED to 5.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 25.0,
                    StatType.DEFENSE to 6.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.CRIT_DAMAGE to 6.0,
                    StatType.SPEED to 6.0,
                    StatType.BONUS_ATTACK_SPEED to 6.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Mythic", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.STRENGTH to 2.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 4.0,
                    StatType.DEFENSE to 3.0,
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 25.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0,
                    StatType.STRENGTH to 6.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 30.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 8.0,
                    StatType.STRENGTH to 8.0,
                    StatType.CRIT_CHANCE to 4.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 40.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 10.0,
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 5.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0,
                    StatType.DEFENSE to 12.0,
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 6.0,
                    StatType.SPEED to 2.0,
                    StatType.INTELLIGENCE to 60.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Pure", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.STRENGTH to 2.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 3.0,
                    StatType.DEFENSE to 3.0,
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 4.0,
                    StatType.CRIT_DAMAGE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 4.0,
                    StatType.DEFENSE to 4.0,
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 6.0,
                    StatType.CRIT_DAMAGE to 4.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 4.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0,
                    StatType.STRENGTH to 6.0,
                    StatType.CRIT_CHANCE to 8.0,
                    StatType.CRIT_DAMAGE to 6.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 6.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 8.0,
                    StatType.STRENGTH to 8.0,
                    StatType.CRIT_CHANCE to 10.0,
                    StatType.CRIT_DAMAGE to 8.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 8.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 10.0,
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.CRIT_DAMAGE to 10.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 10.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Titanic", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0, StatType.DEFENSE to 10.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.DEFENSE to 15.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 20.0, StatType.DEFENSE to 20.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 25.0, StatType.DEFENSE to 25.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 35.0, StatType.DEFENSE to 35.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 50.0, StatType.DEFENSE to 50.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Smart", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 4.0, StatType.DEFENSE to 4.0, StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.DEFENSE to 6.0, StatType.INTELLIGENCE to 40.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 9.0, StatType.DEFENSE to 9.0, StatType.INTELLIGENCE to 60.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0, StatType.DEFENSE to 12.0, StatType.INTELLIGENCE to 80.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.DEFENSE to 15.0, StatType.INTELLIGENCE to 100.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.DEFENSE to 20.0, StatType.INTELLIGENCE to 120.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Wise", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.SPEED to 1.0, StatType.INTELLIGENCE to 25.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0, StatType.SPEED to 1.0, StatType.INTELLIGENCE to 50.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0, StatType.SPEED to 1.0, StatType.INTELLIGENCE to 75.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0, StatType.SPEED to 2.0, StatType.INTELLIGENCE to 100.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.SPEED to 2.0, StatType.INTELLIGENCE to 125.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.SPEED to 3.0, StatType.INTELLIGENCE to 150.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Stained", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 2.0, StatType.HEALTH to 2.0, StatType.CRIT_CHANCE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.HEALTH to 3.0, StatType.CRIT_CHANCE to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 4.0, StatType.HEALTH to 4.0, StatType.CRIT_CHANCE to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0, StatType.HEALTH to 5.0, StatType.CRIT_CHANCE to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 6.0, StatType.HEALTH to 5.0, StatType.CRIT_CHANCE to 3.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 7.0, StatType.HEALTH to 7.0, StatType.CRIT_CHANCE to 4.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Menacing", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 1.0, StatType.CRIT_DAMAGE to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 1.0, StatType.CRIT_DAMAGE to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 1.0, StatType.CRIT_DAMAGE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 1.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 2.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 2.0, StatType.CRIT_DAMAGE to 5.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Hefty", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 7.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 9.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 15.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 20.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -4.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 25.0, StatType.SPEED to -1.0, StatType.CRIT_DAMAGE to -5.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Soft", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 3.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 4.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 5.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 2.0,
                    StatType.HEALTH to 6.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 2.0,
                    StatType.HEALTH to 7.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Honored", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 0.0,
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 1.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 1.0,
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 1.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.INTELLIGENCE to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.INTELLIGENCE to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 3.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.DEFENSE to 4.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 4.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.INTELLIGENCE to 7.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Blended", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 0.0,
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 1.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 1.0,
                    StatType.INTELLIGENCE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 1.0,
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 1.0,
                    StatType.CRIT_CHANCE to 1.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 1.0,
                    StatType.DEFENSE to 1.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.HEALTH to 3.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.CRIT_DAMAGE to 2.0,
                    StatType.INTELLIGENCE to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Astute", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 1.0, StatType.HEALTH to 1.0, StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 2.0, StatType.HEALTH to 1.0, StatType.INTELLIGENCE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 2.0, StatType.HEALTH to 2.0, StatType.INTELLIGENCE to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.HEALTH to 3.0, StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 4.0, StatType.HEALTH to 4.0, StatType.INTELLIGENCE to 8.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0, StatType.HEALTH to 5.0, StatType.INTELLIGENCE to 10.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Colossal", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.HEALTH to 3.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 4.0, StatType.HEALTH to 4.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 6.0, StatType.HEALTH to 6.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 8.0, StatType.HEALTH to 8.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 10.0, StatType.HEALTH to 10.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0, StatType.HEALTH to 12.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Brilliant", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.HEALTH to 1.0, StatType.INTELLIGENCE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.HEALTH to 1.0, StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.HEALTH to 2.0, StatType.INTELLIGENCE to 7.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.HEALTH to 3.0, StatType.INTELLIGENCE to 9.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.HEALTH to 4.0, StatType.INTELLIGENCE to 12.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.HEALTH to 5.0, StatType.INTELLIGENCE to 15.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Unyielding", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.MINING_FORTUNE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.MINING_FORTUNE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.MINING_FORTUNE to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.MINING_FORTUNE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 9.0, StatType.MINING_FORTUNE to 6.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Prospector's", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.MINING_WISDOM to 0.5
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.MINING_WISDOM to 0.75
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.MINING_WISDOM to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.MINING_WISDOM to 1.25
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.MINING_WISDOM to 2.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 9.0, StatType.MINING_WISDOM to 2.5
                )
            )
        ), ReforgeAPI.Reforge(
            "Excellent", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.MINING_SPEED to 4.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.MINING_SPEED to 8.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.MINING_SPEED to 12.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 4.0, StatType.MINING_SPEED to 16.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.MINING_SPEED to 20.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.MINING_SPEED to 25.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Sturdy", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.MINING_SPEED to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 6.0, StatType.MINING_SPEED to 6.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 9.0, StatType.MINING_SPEED to 9.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0, StatType.MINING_SPEED to 12.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 15.0, StatType.MINING_SPEED to 15.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 20.0, StatType.MINING_SPEED to 20.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Fortunate", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 1.0, StatType.MINING_FORTUNE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 2.0, StatType.MINING_FORTUNE to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 3.0, StatType.MINING_FORTUNE to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 4.0, StatType.MINING_FORTUNE to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 6.0, StatType.MINING_FORTUNE to 2.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 8.0, StatType.MINING_FORTUNE to 3.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Great", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.STRENGTH to 2.0, StatType.CRIT_DAMAGE to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.STRENGTH to 4.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.STRENGTH to 6.0, StatType.CRIT_DAMAGE to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 4.0, StatType.STRENGTH to 9.0, StatType.CRIT_DAMAGE to 9.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.STRENGTH to 12.0, StatType.CRIT_DAMAGE to 12.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.STRENGTH to 16.0, StatType.CRIT_DAMAGE to 16.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Rugged", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.CRIT_DAMAGE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0, StatType.CRIT_DAMAGE to 5.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 9.0, StatType.CRIT_DAMAGE to 8.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 13.0, StatType.CRIT_DAMAGE to 12.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0, StatType.CRIT_DAMAGE to 16.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 24.0, StatType.CRIT_DAMAGE to 22.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Lush", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.FORAGING_FORTUNE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 4.0, StatType.FORAGING_FORTUNE to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FORAGING_FORTUNE to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.FORAGING_FORTUNE to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 10.0, StatType.FORAGING_FORTUNE to 3.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 15.0, StatType.FORAGING_FORTUNE to 5.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Lumberjack's", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.FORAGING_WISDOM to 0.5
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.FORAGING_WISDOM to 0.75
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.FORAGING_WISDOM to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FORAGING_WISDOM to 1.25
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.FORAGING_WISDOM to 2.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 9.0, StatType.FORAGING_WISDOM to 2.5
                )
            )
        ), ReforgeAPI.Reforge(
            "Double-Bit", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 1.0, StatType.SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 2.0, StatType.SPEED to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 3.0, StatType.SPEED to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 4.0, StatType.SPEED to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 5.0, StatType.SPEED to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_FORTUNE to 6.0, StatType.SPEED to 7.0
                )
            )
        ), ReforgeAPI.Reforge(
            "Robust", ReforgeType.HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 2.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 3.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 4.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 6.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 8.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 10.0)
            )
        ), ReforgeAPI.Reforge(
            "Zooming", ReforgeType.HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.SPEED to 8.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 12.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.SPEED to 16.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 20.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.SPEED to 25.0)
            )
        ), ReforgeAPI.Reforge(
            "Peasant's", ReforgeType.HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.FARMING_WISDOM to 0.5
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.FARMING_WISDOM to 0.75
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.FARMING_WISDOM to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FARMING_WISDOM to 1.25
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.FARMING_WISDOM to 2.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 9.0, StatType.FARMING_WISDOM to 2.5
                )
            )
        ), ReforgeAPI.Reforge(
            "Green Thumb", ReforgeType.HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.FARMING_FORTUNE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.FARMING_FORTUNE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.FARMING_FORTUNE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.FARMING_FORTUNE to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FARMING_FORTUNE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.FARMING_FORTUNE to 6.0
                )
            )
        ),
        // Special
        // TODO Greater Spook
        ReforgeAPI.Reforge("Coldfused",
            ReforgeType.SPECIAL_ITEMS,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.CRIT_DAMAGE to 20.0, StatType.MAGIC_FIND to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.CRIT_DAMAGE to 30.0, StatType.MAGIC_FIND to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_DAMAGE to 40.0, StatType.MAGIC_FIND to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 35.0, StatType.CRIT_DAMAGE to 50.0, StatType.MAGIC_FIND to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 45.0, StatType.CRIT_DAMAGE to 60.0, StatType.MAGIC_FIND to 2.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 55.0, StatType.CRIT_DAMAGE to 75.0, StatType.MAGIC_FIND to 2.0
                )
            ),
            "Entropy_Suppressor".asInternalName(),
            extraPropertyText = "$7Only if Wisp is equipped\n§c+75${
                StatType.STRENGTH.iconWithName
            }\n§9+55 ${StatType.CRIT_DAMAGE.iconWithName}\n§fDeal §62x§f to fire pillars, breaking one grants +30 ${StatType.TRUE_DEFENCE.iconWithName} and §c+1.15x damage§f for §a60s§f.",
            specialItems = (listOf(
                "Firedust_Dagger",
                "Kindlebane_Dagger",
                "Mawdredge_Dagger",
                "Pyrochaos_Dagger",
                "Twilight_Dagger",
                "Deathripper_Dagger"
            )).map { it.asInternalName() }), ReforgeAPI.Reforge(
            "Dirty", ReforgeType.SWORD_AND_ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.BONUS_ATTACK_SPEED to 2.0, StatType.FEROCITY to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.BONUS_ATTACK_SPEED to 3.0, StatType.FEROCITY to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0, StatType.BONUS_ATTACK_SPEED to 5.0, StatType.FEROCITY to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.BONUS_ATTACK_SPEED to 10.0, StatType.FEROCITY to 9.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.BONUS_ATTACK_SPEED to 15.0, StatType.FEROCITY to 12.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.BONUS_ATTACK_SPEED to 20.0, StatType.FEROCITY to 15.0
                )
            ), "Dirt_Bottle".asInternalName()
        ), ReforgeAPI.Reforge(
            "Fabled",
            ReforgeType.SWORD,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 30.0, StatType.CRIT_DAMAGE to 15.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 35.0, StatType.CRIT_DAMAGE to 20.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 40.0, StatType.CRIT_DAMAGE to 25.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 50.0, StatType.CRIT_DAMAGE to 32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 60.0, StatType.CRIT_DAMAGE to 40.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 75.0, StatType.CRIT_DAMAGE to 50.0
                )
            ),
            "Dragon_Claw".asInternalName(),
            extraPropertyText = "§fCritical hits hava a chance to deal up to §a+15%§f extra damage."
        ), ReforgeAPI.Reforge(
            "Gilded",
            ReforgeType.SPECIAL_ITEMS,
            mapOf(
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 75.0, StatType.STRENGTH to 75.0, StatType.INTELLIGENCE to 350.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 90.0, StatType.STRENGTH to 90.0, StatType.INTELLIGENCE to 400.0
                )
            ),
            "Midas_Jewel".asInternalName(),
            listOf("Midas'_Sword".asInternalName(), "Midas_Staff".asInternalName()),
            "§fUpon killing an enemy you have a low (§a1%§f) chance to grant a random amount of §6Coins§f to another player around you"
        ), ReforgeAPI.Reforge(
            "Suspicious",
            ReforgeType.SWORD,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 30.0, StatType.CRIT_CHANCE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 40.0, StatType.CRIT_CHANCE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 50.0, StatType.CRIT_CHANCE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 65.0, StatType.CRIT_CHANCE to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 85.0, StatType.CRIT_CHANCE to 7.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 110.0, StatType.CRIT_CHANCE to 10.0
                )
            ),
            "Suspicious_Vial".asInternalName(),
            extraPropertyText = "§fIncreases weapon ${StatType.DAMAGE.iconWithName}§f by §c+15§f."
        ), ReforgeAPI.Reforge(
            "Warped",
            ReforgeType.SPECIAL_ITEMS,
            mapOf(
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 165.0, StatType.STRENGTH to 165.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 165.0, StatType.STRENGTH to 165.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 165.0, StatType.STRENGTH to 165.0, StatType.INTELLIGENCE to 65.0
                )
            ),
            "Warped_Stone".asInternalName(),
            listOf("Aspect_of_the_End".asInternalName(), "Aspect_of_the_Void".asInternalName())
        ), ReforgeAPI.Reforge(
            "Withered",
            ReforgeType.SWORD,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 60.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 75.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 90.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 110.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 135.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 170.0)
            ),
            "Wither_Blood".asInternalName(),
            extraPropertyText = "§fGrants §c+1 ${StatType.STRENGTH.iconWithName}§f per §cCatacombs§f level."
        ), ReforgeAPI.Reforge(
            "Bulky", ReforgeType.SWORD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 4.0, StatType.DEFENSE to 2.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.DEFENSE to 3.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 9.0, StatType.DEFENSE to 5.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0, StatType.DEFENSE to 8.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.DEFENSE to 13.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.DEFENSE to 21.0
                )
            ), "Bulky_Stone".asInternalName()
        ), ReforgeAPI.Reforge(
            "Jerry's",
            ReforgeType.SPECIAL_ITEMS,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 50.0, StatType.CRIT_CHANCE to 10.0, StatType.STRENGTH to 25.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 50.0, StatType.CRIT_CHANCE to 10.0, StatType.STRENGTH to 25.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_DAMAGE to 50.0, StatType.CRIT_CHANCE to 10.0, StatType.STRENGTH to 25.0
                )
            ),
            "Jerry_Stone".asInternalName(),
            listOf("ASPECT_OF_THE_JERRY".asInternalName(), "ASPECT_OF_THE_JERRY_SIGNATURE".asInternalName()),
            "§6Item Ability: No Parley\n§fConsumes all your mana and adds 10% of that amount as damage on your next AotJ hit."
        ), ReforgeAPI.Reforge(
            "Fanged", ReforgeType.SPECIAL_ITEMS, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 30.0, StatType.CRIT_CHANCE to 3.0, StatType.BONUS_ATTACK_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 35.0, StatType.CRIT_CHANCE to 4.0, StatType.BONUS_ATTACK_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 40.0, StatType.CRIT_CHANCE to 5.0, StatType.BONUS_ATTACK_SPEED to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 50.0, StatType.CRIT_CHANCE to 7.0, StatType.BONUS_ATTACK_SPEED to 6.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 60.0, StatType.CRIT_CHANCE to 8.0, StatType.BONUS_ATTACK_SPEED to 9.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 65.0, StatType.CRIT_CHANCE to 10.0, StatType.BONUS_ATTACK_SPEED to 10.0
                )
            ), "Full-Jaw_Fanging_Kit".asInternalName(), (listOf(
                "Iron_Sword",
                "Hunter_Knife",
                "Flaming_Sword",
                "Squire_Sword",
                "Undead_Sword",
                "Spider_Sword",
                "Golem_Sword",
                "Zombie_Sword",
                "Yeti_Sword",
                "Blade_of_the_Volacno",
                "Recluse_Fang",
                "Shaman_Sword",
                "Voidwalker_Katana",
                "Twilight_Dagger",
                "Mawdredge_Dagger",
                "Deathripper_Dagger",
                "Dreadlord_Sword",
                "Zombie_Knight_Sword",
                "Zombie_Soldier_Cutlass",
                "Slient_Death",
                "Spirit_Sword",
                "Livid_Dagger",
                "Giant_Sword",
                "Necromancer_Sword",
                "Necron's_Blade_(Unrefined)",
                "Valkyrie",
                "Hyperion",
                "Scylla",
                "Astraea",
                "Dreadlord_Sword"
            )).map { it.asInternalName() }, "§fEvery §c7th§f melee hit on an enemy deals §c+100%§f damage."
        ), ReforgeAPI.Reforge(
            "Precise",
            ReforgeType.BOW,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.CRIT_CHANCE to 8.0, StatType.CRIT_DAMAGE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_CHANCE to 9.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 18.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0, StatType.CRIT_CHANCE to 11.0, StatType.CRIT_DAMAGE to 32.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_CHANCE to 13.0, StatType.CRIT_DAMAGE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 34.0, StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 70.0
                )
            ),
            "OPTICAL_LENSE".asInternalName(),
            extraPropertyText = "§fDeal §a+10%§f extra damage when arrows hit the head of a mob"
        ), ReforgeAPI.Reforge(
            "Spiritual",
            ReforgeType.BOW,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.CRIT_CHANCE to 7.0, StatType.CRIT_DAMAGE to 10.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0, StatType.CRIT_CHANCE to 8.0, StatType.CRIT_DAMAGE to 15.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 14.0, StatType.CRIT_CHANCE to 9.0, StatType.CRIT_DAMAGE to 23.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 37.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 28.0, StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 55.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 38.0, StatType.CRIT_CHANCE to 14.0, StatType.CRIT_DAMAGE to 75.0
                )
            ),
            "SPIRIT_STONE".asInternalName(),
            extraPropertyText = "§fGrants a §a10%§f chance to spawn a Spirit Decoy when you kill an enemy in a Dungeon"
        ), ReforgeAPI.Reforge(
            "Headstrong",
            ReforgeType.BOW,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.CRIT_CHANCE to 10.0, StatType.CRIT_DAMAGE to 4.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.CRIT_CHANCE to 11.0, StatType.CRIT_DAMAGE to 8.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.CRIT_CHANCE to 12.0, StatType.CRIT_DAMAGE to 16.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 16.0, StatType.CRIT_CHANCE to 13.0, StatType.CRIT_DAMAGE to 28.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 23.0, StatType.CRIT_CHANCE to 15.0, StatType.CRIT_DAMAGE to 42.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 33.0, StatType.CRIT_CHANCE to 17.0, StatType.CRIT_DAMAGE to 60.0
                )
            ),
            "SALMON_OPAL".asInternalName(),
            extraPropertyText = "§fDeal §a+8%§f extra damage when arrows hit the head of a mob"
        ), ReforgeAPI.Reforge(
            "Candied",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 1.0, StatType.DEFENSE to 1.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 2.0, StatType.DEFENSE to 1.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 4.0, StatType.DEFENSE to 2.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.DEFENSE to 3.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0, StatType.DEFENSE to 4.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 10.0, StatType.DEFENSE to 5.0)
            ),
            "Candy_Corn".asInternalName(),
            extraPropertyText = "§fIncreases the chance to find candy during the §6Spooky Festival§f by §a+1%§f."
        ), ReforgeAPI.Reforge(
            "Submerged", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 2.0, StatType.SEA_CREATURE_CHANCE to 0.5, StatType.FISHING_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 4.0, StatType.SEA_CREATURE_CHANCE to 0.6, StatType.FISHING_SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 6.0, StatType.SEA_CREATURE_CHANCE to 0.7, StatType.FISHING_SPEED to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 8.0, StatType.SEA_CREATURE_CHANCE to 0.8, StatType.FISHING_SPEED to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 10.0, StatType.SEA_CREATURE_CHANCE to 0.9, StatType.FISHING_SPEED to 4.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 12.0, StatType.SEA_CREATURE_CHANCE to 1.0, StatType.FISHING_SPEED to 5.0
                )
            ), "Deep_Sea_Orb".asInternalName()
        ), ReforgeAPI.Reforge(
            "Perfect",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 25.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 35.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 50.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 65.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 80.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 110.0)
            ),
            "Diamond_Atom".asInternalName(),
            extraPropertyText = "§fIncreases ${StatType.DEFENSE.iconWithName}§f by §a+2%§f."
        ), ReforgeAPI.Reforge(
            "Reinforced", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 25.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 35.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 50.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 65.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 80.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 110.0)
            ), "Rare_Diamond".asInternalName()
        ), ReforgeAPI.Reforge(
            "Renowned", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 4.0,
                    StatType.CRIT_DAMAGE to 4.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0,
                    StatType.HEALTH to 3.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0,
                    StatType.CRIT_CHANCE to 6.0,
                    StatType.CRIT_DAMAGE to 6.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0,
                    StatType.HEALTH to 4.0,
                    StatType.DEFENSE to 4.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0,
                    StatType.CRIT_CHANCE to 8.0,
                    StatType.CRIT_DAMAGE to 8.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0,
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 8.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 10.0,
                    StatType.CRIT_DAMAGE to 10.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0,
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 8.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.CRIT_DAMAGE to 12.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0,
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 10.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 12.0
                )
            ), "Dragon_Horn".asInternalName(), extraPropertyText = "§fIncreases most stats by §a1%§f."
        ), ReforgeAPI.Reforge(
            "Spiked", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.CRIT_CHANCE to 2.0,
                    StatType.CRIT_DAMAGE to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0,
                    StatType.HEALTH to 2.0,
                    StatType.DEFENSE to 2.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 4.0,
                    StatType.CRIT_DAMAGE to 4.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0,
                    StatType.HEALTH to 3.0,
                    StatType.DEFENSE to 3.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0,
                    StatType.CRIT_CHANCE to 6.0,
                    StatType.CRIT_DAMAGE to 6.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0,
                    StatType.HEALTH to 4.0,
                    StatType.DEFENSE to 4.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0,
                    StatType.CRIT_CHANCE to 8.0,
                    StatType.CRIT_DAMAGE to 8.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0,
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 8.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0,
                    StatType.CRIT_CHANCE to 10.0,
                    StatType.CRIT_DAMAGE to 10.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0,
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 8.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 10.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.CRIT_DAMAGE to 12.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0,
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 10.0,
                    StatType.SPEED to 1.0,
                    StatType.INTELLIGENCE to 12.0
                )
            ), "Dragon_Scale".asInternalName()
        ), ReforgeAPI.Reforge(
            "Hyper",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.BONUS_ATTACK_SPEED to 2.0, StatType.SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.BONUS_ATTACK_SPEED to 3.0, StatType.SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0, StatType.BONUS_ATTACK_SPEED to 4.0, StatType.SPEED to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.BONUS_ATTACK_SPEED to 5.0, StatType.SPEED to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.BONUS_ATTACK_SPEED to 6.0, StatType.SPEED to 3.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.BONUS_ATTACK_SPEED to 7.0, StatType.SPEED to 3.0
                )
            ),
            "End_Stone_Geode".asInternalName(),
            extraPropertyText = "§fGain §a+$${StatType.SPEED.iconWithName} for §a5s§f after teleporting.",
            customStat = mapOf(
                LorenzRarity.COMMON to 1.0,
                LorenzRarity.UNCOMMON to 2.0,
                LorenzRarity.RARE to 3.0,
                LorenzRarity.EPIC to 4.0,
                LorenzRarity.LEGENDARY to 5.0,
                LorenzRarity.MYTHIC to 6.0
            )
        ), ReforgeAPI.Reforge(
            "Giant", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 50.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 60.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 80.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 120.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 180.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 240.0)
            ), "Giant_Tooth".asInternalName()
        ), ReforgeAPI.Reforge(
            "Jaded", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 5.0, StatType.MINING_FORTUNE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 12.0, StatType.MINING_FORTUNE to 10.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 20.0, StatType.MINING_FORTUNE to 15.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 30.0, StatType.MINING_FORTUNE to 20.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 45.0, StatType.MINING_FORTUNE to 25.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 60.0, StatType.MINING_FORTUNE to 30.0
                )
            ), "Jaderald".asInternalName()
        ), ReforgeAPI.Reforge(
            "Cubic", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0, StatType.HEALTH to 5.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.HEALTH to 7.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.STRENGTH to 7.0, StatType.HEALTH to 10.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.HEALTH to 15.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.HEALTH to 20.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.HEALTH to 25.0
                )
            ), "Molten_Cube".asInternalName(), extraPropertyText = "§Decreases damage taken from Nether mobs by §a2%§f."
        ), ReforgeAPI.Reforge(
            "Necrotic", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 30.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.INTELLIGENCE to 60.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 90.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.INTELLIGENCE to 120.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 150.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.INTELLIGENCE to 200.0)
            ), "Necromancer's_Brooch".asInternalName()
        ), ReforgeAPI.Reforge(
            "Empowered",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 10.0, StatType.DEFENSE to 10.0),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.DEFENSE to 15.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.DEFENSE to 20.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 25.0, StatType.DEFENSE to 25.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 35.0, StatType.DEFENSE to 35.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 50.0, StatType.DEFENSE to 50.0
                )
            ),
            "Sadan's_Brooch".asInternalName(),
            extraPropertyText = "§Grants §a+10 ${StatType.MENDING.iconWithName}§f while in Dungeons, which increases your healing on others."
        ), ReforgeAPI.Reforge(
            "Ancient",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0,
                    StatType.CRIT_CHANCE to 3.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 6.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0,
                    StatType.CRIT_CHANCE to 5.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 9.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0,
                    StatType.CRIT_CHANCE to 7.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 12.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 18.0,
                    StatType.CRIT_CHANCE to 9.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 16.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0,
                    StatType.CRIT_CHANCE to 12.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 35.0,
                    StatType.CRIT_CHANCE to 15.0,
                    StatType.HEALTH to 7.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 25.0
                )
            ),
            "Precursor_Gear".asInternalName(),
            extraPropertyText = "§f Grants §9+1 ${StatType.CRIT_DAMAGE.iconWithName}§f per Catacombs level."
        ), ReforgeAPI.Reforge(
            "Undead",
            ReforgeType.ARMOR,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 1.0,
                    StatType.BONUS_ATTACK_SPEED to 1.0,
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 2.0,
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 8.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0,
                    StatType.BONUS_ATTACK_SPEED to 3.0,
                    StatType.HEALTH to 12.0,
                    StatType.DEFENSE to 12.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 3.0,
                    StatType.BONUS_ATTACK_SPEED to 4.0,
                    StatType.HEALTH to 18.0,
                    StatType.DEFENSE to 18.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0,
                    StatType.BONUS_ATTACK_SPEED to 5.0,
                    StatType.HEALTH to 25.0,
                    StatType.DEFENSE to 25.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0,
                    StatType.BONUS_ATTACK_SPEED to 6.0,
                    StatType.HEALTH to 33.0,
                    StatType.DEFENSE to 33.0
                )
            ),
            "Premium_Flesh".asInternalName(),
            extraPropertyText = "§fDecreases damage taken from Zombie Pigmen, Zombies, Withers, and Skeltons, by §a+2"
        ), ReforgeAPI.Reforge(
            "Loving", ReforgeType.CHESTPLATE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 4.0,
                    StatType.DEFENSE to 4.0,
                    StatType.INTELLIGENCE to 20.0,
                    StatType.ABILITY_DAMAGE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 5.0,
                    StatType.DEFENSE to 5.0,
                    StatType.INTELLIGENCE to 40.0,
                    StatType.ABILITY_DAMAGE to 5.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0,
                    StatType.DEFENSE to 6.0,
                    StatType.INTELLIGENCE to 60.0,
                    StatType.ABILITY_DAMAGE to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0,
                    StatType.DEFENSE to 7.0,
                    StatType.INTELLIGENCE to 80.0,
                    StatType.ABILITY_DAMAGE to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0,
                    StatType.DEFENSE to 10.0,
                    StatType.INTELLIGENCE to 100.0,
                    StatType.ABILITY_DAMAGE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 14.0,
                    StatType.DEFENSE to 14.0,
                    StatType.INTELLIGENCE to 120.0,
                    StatType.ABILITY_DAMAGE to 5.0
                )
            ), "Red_Scarf".asInternalName()
        ), ReforgeAPI.Reforge(
            "Ridiculous",
            ReforgeType.HELMET,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 1.0, StatType.HEALTH to 10.0, StatType.DEFENSE to 10.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 2.0, StatType.HEALTH to 15.0, StatType.DEFENSE to 15.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 3.0, StatType.HEALTH to 20.0, StatType.DEFENSE to 20.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 4.0, StatType.HEALTH to 25.0, StatType.DEFENSE to 25.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 5.0, StatType.HEALTH to 35.0, StatType.DEFENSE to 35.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.CRIT_CHANCE to 6.0, StatType.HEALTH to 50.0, StatType.DEFENSE to 50.0
                )
            ),
            "Red_Nose".asInternalName(),
            extraPropertyText = "§fFart when you sneak. Reduces your ${StatType.CRIT_CHANCE.iconWithName}§f by §c20%§f for §a20s§f but grants §a+30 ${StatType.DEFENSE.iconWithName}§f for §a5s§f and §b+50 ${StatType.INTELLIGENCE.icon} Mana§f. Requires at least §920% ${StatType.CRIT_CHANCE.iconWithName}§f to activate."
        ), ReforgeAPI.Reforge(
            "Bustling", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 1.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 2.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 4.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 6.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 8.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.FARMING_FORTUNE to 10.0)
            ), "SkyMart_Brochure".asInternalName()
        ), ReforgeAPI.Reforge(
            "Mossy", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 5.0, StatType.SPEED to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 10.0, StatType.SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 15.0, StatType.SPEED to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 20.0, StatType.SPEED to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 25.0, StatType.SPEED to 7.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 30.0, StatType.SPEED to 7.0
                )
            ), "Overgrown_Grass".asInternalName()
        ), ReforgeAPI.Reforge(
            "Festive", ReforgeType.ARMOR, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.05, StatType.INTELLIGENCE to 5.0, StatType.FISHING_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.05, StatType.INTELLIGENCE to 10.0, StatType.FISHING_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.1, StatType.INTELLIGENCE to 15.0, StatType.FISHING_SPEED to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.15, StatType.INTELLIGENCE to 20.0, StatType.FISHING_SPEED to 6.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.2, StatType.INTELLIGENCE to 25.0, StatType.FISHING_SPEED to 8.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.25, StatType.INTELLIGENCE to 30.0, StatType.FISHING_SPEED to 10.0
                )
            ), "Frozen_Bauble".asInternalName()
        ), ReforgeAPI.Reforge(
            "Glistening", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 2.0, StatType.MINING_FORTUNE to 5.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 3.0, StatType.MINING_FORTUNE to 6.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 4.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 5.0, StatType.MINING_FORTUNE to 10.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 6.0, StatType.MINING_FORTUNE to 12.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 7.0, StatType.MINING_FORTUNE to 15.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.INTELLIGENCE to 8.0, StatType.MINING_FORTUNE to 18.0
                )
            ), "Shiny_Prism".asInternalName()
        ), ReforgeAPI.Reforge(
            "STRENGTHened", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.STRENGTH to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 4.0, StatType.STRENGTH to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0, StatType.STRENGTH to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 6.0, StatType.STRENGTH to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 8.0, StatType.STRENGTH to 6.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 10.0, StatType.STRENGTH to 7.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0, StatType.STRENGTH to 8.0
                )
            ), "Searing_Stone".asInternalName()
        ), ReforgeAPI.Reforge(
            "Waxed", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(StatType.HEALTH to 5.0, StatType.CRIT_CHANCE to 2.0),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.CRIT_CHANCE to 3.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0, StatType.CRIT_CHANCE to 4.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0, StatType.CRIT_CHANCE to 5.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0, StatType.CRIT_CHANCE to 6.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 15.0, StatType.CRIT_CHANCE to 7.0
                ),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 20.0, StatType.CRIT_CHANCE to 8.0
                )
            ), "Blaze_Wax".asInternalName()
        ), ReforgeAPI.Reforge(
            "Fortified", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 14.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 17.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 20.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 25.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 30.0),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 36.0
                )
            ), "Meteor_Shard".asInternalName()
        ), ReforgeAPI.Reforge(
            "Rooted", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 6.0, StatType.HEALTH to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 9.0, StatType.HEALTH to 5.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 12.0, StatType.HEALTH to 8.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 15.0, StatType.HEALTH to 11.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 18.0, StatType.HEALTH to 14.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 21.0, StatType.HEALTH to 17.0
                )
            ), "Burrowing_Spores".asInternalName()
        ), ReforgeAPI.Reforge(
            "Blooming", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 1.0, StatType.SPEED to 4.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 2.0, StatType.SPEED to 4.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 3.0, StatType.SPEED to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 4.0, StatType.SPEED to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 5.0, StatType.SPEED to 6.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 6.0, StatType.SPEED to 6.0
                )
            ), "Flowering_Bouquet".asInternalName()
        ), ReforgeAPI.Reforge(
            "Snowy", ReforgeType.EQUIPMENT, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.2, StatType.FISHING_SPEED to 0.5
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.2, StatType.FISHING_SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.4, StatType.FISHING_SPEED to 1.5
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.6, StatType.FISHING_SPEED to 2.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 0.8, StatType.FISHING_SPEED to 2.5
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 1.0, StatType.FISHING_SPEED to 3.0
                )
            ), "Terry's_Snowglobe".asInternalName()
        ), ReforgeAPI.Reforge(
            "Blood-Soaked",
            ReforgeType.CLOAK,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 6.0, StatType.VITALITY to 1.0, StatType.DEFENSE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0, StatType.VITALITY to 1.0, StatType.DEFENSE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 8.0, StatType.VITALITY to 2.0, StatType.DEFENSE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 9.0, StatType.VITALITY to 2.0, StatType.DEFENSE to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 10.0, StatType.VITALITY to 3.0, StatType.DEFENSE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 12.0, StatType.VITALITY to 3.0, StatType.DEFENSE to 6.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.HEALTH to 14.0, StatType.VITALITY to 4.0, StatType.DEFENSE to 7.0
                )
            ),
            "Presumed_Gallon_Of_Red_Paint".asInternalName(),
            extraPropertyText = "§fHeal §a1.15x§f more from Vampirism and Lifesteal"
        ), ReforgeAPI.Reforge(
            "Salty", ReforgeType.ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 1.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.SEA_CREATURE_CHANCE to 2.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 2.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.SEA_CREATURE_CHANCE to 3.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 5.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.SEA_CREATURE_CHANCE to 7.0)
            ), "Salt_Cube".asInternalName()
        ), ReforgeAPI.Reforge(
            "Treacherous", ReforgeType.ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.SEA_CREATURE_CHANCE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 15.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.SEA_CREATURE_CHANCE to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.SEA_CREATURE_CHANCE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 30.0, StatType.SEA_CREATURE_CHANCE to 7.0
                )
            ), "Rusty_Anchor".asInternalName()
        ), ReforgeAPI.Reforge(
            "Lucky", ReforgeType.ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 1.0, StatType.SEA_CREATURE_CHANCE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 2.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 3.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 4.0, StatType.SEA_CREATURE_CHANCE to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 5.0, StatType.SEA_CREATURE_CHANCE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.MAGIC_FIND to 6.0, StatType.SEA_CREATURE_CHANCE to 7.0
                )
            ), "Lucky_Dice".asInternalName()
        ), ReforgeAPI.Reforge(
            "Stiff", ReforgeType.ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 2.0, StatType.SEA_CREATURE_CHANCE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 4.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 6.0, StatType.SEA_CREATURE_CHANCE to 2.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0, StatType.SEA_CREATURE_CHANCE to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.SEA_CREATURE_CHANCE to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.SEA_CREATURE_CHANCE to 7.0
                )
            ), "Hardened_Wood".asInternalName()
        ), ReforgeAPI.Reforge(
            "Chomp",
            ReforgeType.ROD,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.CRIT_CHANCE to 5.0, StatType.FISHING_SPEED to 2.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 10.0, StatType.CRIT_CHANCE to 10.0, StatType.FISHING_SPEED to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 17.0, StatType.CRIT_CHANCE to 17.0, StatType.FISHING_SPEED to 5.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.CRIT_CHANCE to 25.0, StatType.FISHING_SPEED to 7.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 35.0, StatType.CRIT_CHANCE to 35.0, StatType.FISHING_SPEED to 9.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 50.0, StatType.CRIT_CHANCE to 50.0, StatType.FISHING_SPEED to 11.0
                )
            ),
            "Kuudra_Mandible".asInternalName(),
            extraPropertyText = "§fDecreases the health of Lava Sea Creatures by §c1%§f for each unique Lava Sea Creature you have killed with this rod in your inventory"
        ), ReforgeAPI.Reforge(
            "Pitchin'", ReforgeType.ROD, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 1.0, StatType.FISHING_SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 1.0, StatType.FISHING_SPEED to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 2.0, StatType.FISHING_SPEED to 4.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 3.0, StatType.FISHING_SPEED to 6.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 4.0, StatType.FISHING_SPEED to 8.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SEA_CREATURE_CHANCE to 5.0, StatType.FISHING_SPEED to 10.0
                )
            ), "Pitchin'_Koi".asInternalName()
        ), ReforgeAPI.Reforge(
            "Ambered", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 25.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 31.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 38.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 46.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 55.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 65.0),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 75.0
                )
            ), "Amber_Material".asInternalName()
        ), ReforgeAPI.Reforge(
            "Auspicious", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 7.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 14.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 23.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 34.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 45.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 60.0, StatType.MINING_FORTUNE to 8.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 75.0, StatType.MINING_FORTUNE to 8.0
                )
            ), "Rock_Gemstone".asInternalName()
        ), ReforgeAPI.Reforge(
            "Fleet", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 9.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 15.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 25.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 40.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 55.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 75.0),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 90.0
                )
            ), "Diamonite".asInternalName()
        ), ReforgeAPI.Reforge(
            "Heated",
            ReforgeType.PICKAXE,
            mapOf(),
            "Hot_Stuff".asInternalName(),
            extraPropertyText = "§fGrants §aincreased ${StatType.MINING_SPEED.iconWithName}§f the deeper your venture."
        ), ReforgeAPI.Reforge(
            "Magnetic",
            ReforgeType.PICKAXE,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 4.0),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0
                ),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 6.0),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 8.0
                ),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 10.0),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 14.0
                ),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(StatType.DEFENSE to 18.0)
            ),
            "Lapis_Crystal".asInternalName(),
            extraPropertyText = "§Gain §a+$%§f extra experience when mining",
            customStat = mapOf(
                LorenzRarity.COMMON to 10.0,
                LorenzRarity.UNCOMMON to 12.0,
                LorenzRarity.RARE to 14.0,
                LorenzRarity.EPIC to 16.0,
                LorenzRarity.LEGENDARY to 18.0,
                LorenzRarity.MYTHIC to 20.0,
                LorenzRarity.DIVINE to 22.0
            )
        ), ReforgeAPI.Reforge(
            "Mithraic",
            ReforgeType.PICKAXE,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 6.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 12.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 20.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 30.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 40.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.MINING_SPEED to 55.0),
                LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.MINING_SPEED to 70.0
                )
            ),
            "Pure_Mithril".asInternalName(),
            extraPropertyText = "§Grants a §a10%§f chance to gain §a1§f extra Mithril when mining Mithril Ore."
        ), ReforgeAPI.Reforge(
            "Refined",
            ReforgeType.PICKAXE,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0, StatType.MINING_WISDOM to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 7.0, StatType.MINING_WISDOM to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 9.0, StatType.MINING_WISDOM to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 13.0, StatType.MINING_WISDOM to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 16.0, StatType.MINING_WISDOM to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 20.0, StatType.MINING_WISDOM to 6.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 24.0, StatType.MINING_WISDOM to 7.0
                )
            ),
            "Refined_Amber".asInternalName(),
            extraPropertyText = "§fGain a §a0.1%§f chacne to drop an enchanted item for ores that you mine."
        ), ReforgeAPI.Reforge(
            "Stellar",
            ReforgeType.PICKAXE,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.DEFENSE to 5.0, StatType.MINING_SPEED to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 8.0, StatType.DEFENSE to 8.0, StatType.MINING_SPEED to 6.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 12.0, StatType.DEFENSE to 12.0, StatType.MINING_SPEED to 9.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 16.0, StatType.DEFENSE to 16.0, StatType.MINING_SPEED to 12.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.DEFENSE to 20.0, StatType.MINING_SPEED to 15.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 25.0, StatType.DEFENSE to 25.0, StatType.MINING_SPEED to 20.0
                ), LorenzRarity.DIVINE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 30.0, StatType.DEFENSE to 30.0, StatType.MINING_SPEED to 25.0
                )
            ),
            "Petrified_Starfall".asInternalName(),
            extraPropertyText = "§fDeal §a+1§f extra damage to Star Sentries and increases the chance for Starfall to drop form them by §a20%§f."
        ), ReforgeAPI.Reforge(
            "Fruitful", ReforgeType.PICKAXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 3.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 4.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 5.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 7.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 9.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DEFENSE to 12.0, StatType.INTELLIGENCE to 1.0, StatType.MINING_FORTUNE to 3.0
                )
            ), "Onyx".asInternalName()
        ), ReforgeAPI.Reforge(
            "Moil", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_WISDOM to 1.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.FORAGING_WISDOM to 1.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_WISDOM to 2.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.FORAGING_WISDOM to 2.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FORAGING_WISDOM to 3.0, StatType.FORAGING_WISDOM to 3.0
                )
            ), "Moil_Log".asInternalName()
        ), ReforgeAPI.Reforge(
            "Toil", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 5.0, StatType.CRIT_DAMAGE to 5.0, StatType.FORAGING_WISDOM to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 7.0, StatType.CRIT_DAMAGE to 7.0, StatType.FORAGING_WISDOM to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 9.0, StatType.CRIT_DAMAGE to 9.0, StatType.FORAGING_WISDOM to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 13.0, StatType.CRIT_DAMAGE to 13.0, StatType.FORAGING_WISDOM to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 16.0, StatType.CRIT_DAMAGE to 16.0, StatType.FORAGING_WISDOM to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.STRENGTH to 20.0, StatType.CRIT_DAMAGE to 20.0, StatType.FORAGING_WISDOM to 6.0
                )
            ), "Toil_Log".asInternalName()
        ), ReforgeAPI.Reforge(
            "Blessed", ReforgeType.AXE_AND_HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FARMING_FORTUNE to 5.0, StatType.FARMING_WISDOM to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 7.0, StatType.FARMING_FORTUNE to 7.0, StatType.FARMING_WISDOM to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 9.0, StatType.FARMING_FORTUNE to 9.0, StatType.FARMING_WISDOM to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 13.0, StatType.FARMING_FORTUNE to 13.0, StatType.FARMING_WISDOM to 4.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 16.0, StatType.FARMING_FORTUNE to 16.0, StatType.FARMING_WISDOM to 5.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 20.0, StatType.FARMING_FORTUNE to 20.0, StatType.FARMING_WISDOM to 6.0
                )
            ), "Blessed_Fruit".asInternalName()
        ), ReforgeAPI.Reforge(
            "Earthy", ReforgeType.AXE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 2.0, StatType.SPEED to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 4.0, StatType.SPEED to 1.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 6.0, StatType.SPEED to 1.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 8.0, StatType.SPEED to 1.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 10.0, StatType.SPEED to 1.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.FARMING_FORTUNE to 12.0, StatType.SPEED to 1.0
                )
            ), "Large_Walnut".asInternalName()
        ), ReforgeAPI.Reforge(
            "Bountiful", ReforgeType.HOE, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 1.0, StatType.FARMING_FORTUNE to 1.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 2.0, StatType.FARMING_FORTUNE to 2.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0, StatType.FARMING_FORTUNE to 3.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0, StatType.FARMING_FORTUNE to 5.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 8.0, StatType.FARMING_FORTUNE to 7.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 13.0, StatType.FARMING_FORTUNE to 10.0
                )
            ), "Golden_Ball".asInternalName(), extraPropertyText = "§fGrants §6+0.2 Coins§f per crop dorpped."
        ), ReforgeAPI.Reforge(
            "Beady", ReforgeType.VACUUM, mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 5.0, StatType.INTELLIGENCE to 10.0
                ), LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 10.0, StatType.INTELLIGENCE to 20.0
                ), LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 15.0, StatType.INTELLIGENCE to 30.0
                ), LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 20.0, StatType.INTELLIGENCE to 40.0
                ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 25.0, StatType.INTELLIGENCE to 50.0
                ), LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(
                    StatType.DAMAGE to 30.0, StatType.INTELLIGENCE to 60.0
                )
            ), "Beady_Eyes".asInternalName(), extraPropertyText = "§6Pests§f drop §a+3§f crops."
        ), ReforgeAPI.Reforge(
            "Buzzing",
            ReforgeType.VACUUM,
            mapOf(
                LorenzRarity.COMMON to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 3.0
                ),
                LorenzRarity.UNCOMMON to ReforgeAPI.StatList.mapOf(StatType.SPEED to 4.0),
                LorenzRarity.RARE to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 5.0
                ),
                LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(StatType.SPEED to 7.0),
                LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(
                    StatType.SPEED to 10.0
                ),
                LorenzRarity.MYTHIC to ReforgeAPI.StatList.mapOf(StatType.SPEED to 15.0)
            ),
            "Clipped_Wings".asInternalName(),
            extraPropertyText = "§fDoubles the ${StatType.DAMAGE.iconWithName}§f dealt by §aVACUUM§f."
        ), ReforgeAPI.Reforge("Greater Spook", ReforgeType.SPECIAL_ITEMS, mapOf(
            LorenzRarity.EPIC to ReforgeAPI.StatList.mapOf(
                StatType.FEAR to 1.0
            ), LorenzRarity.LEGENDARY to ReforgeAPI.StatList.mapOf(StatType.FEAR to 1.0)
        ), "Boo_Stone".asInternalName(), specialItems = (listOf(
            "GREAT_SPOOK_HELMET",
            "GREAT_SPOOK_CHESTPLATE",
            "GREAT_SPOOK_LEGGINGS",
            "GREAT_SPOOK_BOOTS",
            "GREAT_SPOOK_BELT",
            "GREAT_SPOOK_CLOAK",
            "GREAT_SPOOK_NECKLACE",
            "GREAT_SPOOK_GLOVES"
        )).map { it.asInternalName() })
    )

    class reforges1(list: List<ReforgeAPI.Reforge>) {
        val reforges = list.sortedBy { it.name }.map { it.name to re(it) }.toMap()

        class re(r: ReforgeAPI.Reforge) {
            val reforgeStone: String? = r.reforgeStone?.asString()
            val type = r.type
            val specialItems = r.specialItems?.map { it.asString() }
            val reforgeAbility = r.extraProperty
            val stats = r.stats
        }
    }

    val print = run {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val string = gson.toJson(reforges1(reforgesPrint.filterNot { it.isReforgeStone }))
        val stream = FileOutputStream(".\\Reforges.json")
        stream.write(string.toByteArray())
        stream.close()
    }
}

