package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.client.resources.Language

data class LanguageChangeEvent(val newLanguage: Language) : SkyHanniEvent()
