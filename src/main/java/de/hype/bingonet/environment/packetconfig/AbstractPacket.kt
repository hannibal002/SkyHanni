package de.hype.bingonet.environment.packetconfig

import de.hype.bingonet.client.common.chat.Chat
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.communication.BBsentialConnection
import de.hype.bingonet.shared.packets.network.InvalidCommandFeedbackPacket

open class AbstractPacket protected constructor(val apiVersionMin: Int = 1, val apiVersionMax: Int = 1) {
    fun isValid(connection: BBsentialConnection?, allowedNullFields: Array<String?>?): Boolean {
        if (this.isApiSupported) {
            Chat.sendPrivateMessageToSelfFatal("You are using an outdated version of the mod")
        }
        return true
    }

    val isApiSupported: Boolean
        get() {
            //int version = Core.getConfig().getVersion();
            val version = BingoNet.generalConfig.getApiVersion()
            if (version >= apiVersionMin && version <= apiVersionMax) {
                return true
            }
            return false
        }

    fun hasNullFields(allowedNullFields: Array<String>?): String? {
        val fields = this.javaClass.getDeclaredFields()
        if (this.javaClass.getSimpleName() != InvalidCommandFeedbackPacket::class.java.getSimpleName()) {
            for (field in fields) {
                field.setAccessible(true)
                try {
                    if (field.get(this) == null) {
                        if (allowedNullFields == null) return field.getName()
                        if (isAllowedNull(allowedNullFields, field.getName())) {
                            return field.getName()
                        }
                    }
                } catch (e: IllegalAccessException) {
                    // Handle the exception if needed
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun isAllowedNull(allowedFields: Array<String>, fieldName: String?): Boolean {
        for (allowedField in allowedFields) {
            if (allowedField == fieldName) {
                return true
            }
        }
        return false
    }
}
