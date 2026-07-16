package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.FriendAddEvent
import at.hannibal2.skyhanni.events.FriendRequestDeclinedEvent
import at.hannibal2.skyhanni.events.FriendRequestExpiredEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder

@SkyHanniModule
object ContributorAchievement {
    private const val CONTRIBUTOR_ACHIEVEMENT = "Contrib Achievement"
    private const val CONTRIBUTOR_FRIEND_ACHIEVEMENT = "Contrib Friend"
    private const val CONTRIBUTOR_NOBODY_ACHIEVEMENT = "Contrib Stranger"
    private const val CONTRIBUTOR_REJECTED_ACHIEVEMENT = "Contrib Rejected"
    private const val CONTRIBUTOR_FAMOUS_ACHIEVEMENT = "Contrib Famous"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        event.register(
            Achievement(
                name = "EEEEKK!".asComponent(),
                description = componentBuilder {
                    append("Be in the same lobby as a")
                    appendWithColor(" SkyHanni ", TextHelper.chromaStyle)
                    append("contributor")
                },
                userLuckAmount = 0f,
            ),
            CONTRIBUTOR_ACHIEVEMENT,
        )

        event.register(
            Achievement(
                name = "I Know a Guy".asComponent(),
                description = componentBuilder {
                    append("Have a ")
                    appendWithColor("SkyHanni ", TextHelper.chromaStyle)
                    append("contributor as a friend")
                },
                userLuckAmount = 0f,
            ),
            CONTRIBUTOR_FRIEND_ACHIEVEMENT,
        )

        event.register(
            Achievement(
                name = "Notice Me Senpai".asComponent(),
                description = componentBuilder {
                    append("Have your friend request ignored by a ")
                    appendWithColor("SkyHanni", TextHelper.chromaStyle)
                    append(" contributor")
                },
                userLuckAmount = 0f,
            ),
            CONTRIBUTOR_NOBODY_ACHIEVEMENT,
        )

        event.register(
            Achievement(
                name = "Rejected".asComponent(),
                description = componentBuilder {
                    append("Have your friend request declined by a ")
                    appendWithColor("SkyHanni", TextHelper.chromaStyle)
                    append(" contributor")
                },
                userLuckAmount = 0f,
            ),
            CONTRIBUTOR_REJECTED_ACHIEVEMENT,
        )

        event.register(
            Achievement(
                name = "(Contributor Only Achievement) Am I famous yet?".asComponent(),
                description = componentBuilder {
                    append("Be mentioned by other players as a ")
                    appendWithColor("SkyHanni", TextHelper.chromaStyle)
                    append(" contributor")
                },
                userLuckAmount = 0f,
                tiers = listOf(1, 10, 25, 50, 100)
            ),
            CONTRIBUTOR_FAMOUS_ACHIEVEMENT,
        )
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onProfileJoin() {
        val friends = FriendApi.getAllFriends()
        if (friends.any { it.name in ContributorManager.contributorNames }) {
            AchievementManager.completeAchievement(CONTRIBUTOR_FRIEND_ACHIEVEMENT)
        }
    }

    @HandleEvent
    fun onFriendAdd(event: FriendAddEvent) {
        if (event.playerName in ContributorManager.contributorNames) {
            AchievementManager.completeAchievement(CONTRIBUTOR_FRIEND_ACHIEVEMENT)
        }
    }

    @HandleEvent
    fun onFriendRequestExpired(event: FriendRequestExpiredEvent) {
        if (event.playerName in ContributorManager.contributorNames) {
            AchievementManager.completeAchievement(CONTRIBUTOR_NOBODY_ACHIEVEMENT)
        }
    }

    @HandleEvent
    fun onFriendRequestDeclined(event: FriendRequestDeclinedEvent) {
        if (event.playerName in ContributorManager.contributorNames) {
            AchievementManager.completeAchievement(CONTRIBUTOR_REJECTED_ACHIEVEMENT)
        }
    }

    fun onUniqueContributorSeen() {
        AchievementManager.completeAchievement(CONTRIBUTOR_ACHIEVEMENT)
    }

    fun onContributorMention(amount: Int) {
        AchievementManager.updateTieredAchievement(CONTRIBUTOR_FAMOUS_ACHIEVEMENT, amount)
    }
}
