package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.hanni.features.nether.kuudra.KuudraTier

class KuudraQuest(val kuudraTier: KuudraTier, state: QuestState) :
    Quest(
        kuudraTier.displayItem,
        kuudraTier.location,
        QuestCategory.KUUDRA,
        "Kill Kuudra ${kuudraTier.name} Tier",
        state,
        displayName = kuudraTier.getTieredDisplayName()
    )
