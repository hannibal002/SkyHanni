package de.hype.bingonet.shared.objects

import de.hype.bingonet.server.objects.BBUser
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.constants.TradeType
import kotlinx.datetime.Clock

data class BBServiceData(
    val serviceId: Int,
    val description: String,
    val hosterUsername: String,
    val type: TradeType?,
    val price: Int,
    val helpers: List<Helper>,
    val maxUsers: Int,
    val forceModOnline: Boolean,
    val status: StatusConstants,
    val title: String?,
    val participants: List<Participant>,
    val joinLock: Boolean,
    val circulateParticipants: Boolean,
) {


    data class Participant(
        @JvmField val user: BBUser,
        @JvmField val price: Int,
        @JvmField val priority: Boolean = false,
        @JvmField val joinTime: kotlinx.datetime.Instant = Clock.System.now(),
        @JvmField val autoRequeue: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (other is Participant) return other.user == user
            if (other is BBUser) return other == this.user
            return false
        }

        override fun hashCode(): Int {
            return user.hashCode()
        }
    }

    class Helper {
        private val user: BBUser?
        private val username: String?

        constructor(user: BBUser) {
            this.user = user
            this.username = user.mcusername
        }

        constructor(user: BBUser?, username: String) {
            this.user = user
            this.username = username
        }

        override fun equals(other: Any?): Boolean {
            if (other is Helper) return other.username == username
            if (other is String) {
                if (username != null) return other.equals(username, ignoreCase = true)
                else return user!!.mcusername.equals(other, ignoreCase = true)
            }
            if (other is BBUser) {
                if (user != null) return other == user
                else return other.mcusername.equals(username, ignoreCase = true)
            }
            return false
        }

        override fun hashCode(): Int {
            if (username != null) return username.hashCode()
            else return user!!.mcusername.hashCode()
        }

        fun getUserName(): String {
            return username ?: user!!.mcusername
        }

        fun getUser(): BBUser? {
            return user
        }
    }
}


