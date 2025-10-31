package at.hannibal2.hanni.utils.tracker

import at.hannibal2.hanni.features.skillprogress.SkillType
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.Searchable
import at.hannibal2.hanni.utils.renderables.toSearchable

object TrackerUtils {

    fun MutableList<Searchable>.addSkillXpInfo(
        skillXpGained: Map<SkillType, Long>
    ) = skillXpGained.sumAllValues().takeIf { it > 0 }?.let { sumXpGained ->
        val applicableSkills = skillXpGained.filter { it.value > 0 }
        val skillHoverTips = applicableSkills.map { (skill, xp) ->
            "§7${xp.addSeparators()} §3${skill.displayName} XP"
        }.toMutableList()
        if (applicableSkills.size > 1) {
            skillHoverTips.add("§7You gained §e${sumXpGained.addSeparators()} §7total skill XP.")
        }
        this.add(
            Renderable.hoverTips(
                "§7${sumXpGained.shortFormat()} §3Skill XP",
                skillHoverTips,
            ).toSearchable("Skill XP"),
        )
    }

}
