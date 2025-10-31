package at.hannibal2.hanni.features.bingo.card.nextstephelper.steps

import at.hannibal2.hanni.data.SkillExperience

class SkillLevelStep(
    val skillName: String,
    private val skillLevelNeeded: Int,
    skillExpNeeded: Long = SkillExperience.getExpForLevel(skillLevelNeeded),
) :
    ProgressionStep("$skillName $skillLevelNeeded", skillExpNeeded)
