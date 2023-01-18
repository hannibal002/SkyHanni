package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.KuudraTier

class KuudraQuest(val kuudraTier: KuudraTier, state: QuestState) :
    Quest(QuestCategory.KUUDRA, "Kill Kuudra ${kuudraTier.name} Tier", state, displayName = kuudraTier.getDisplayName())