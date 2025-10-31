package at.hannibal2.hanni.features.bingo.card.nextstephelper.steps

class ChatMessageStep(displayName: String) : NextStep(displayName)
class ObtainCrystalStep(val crystalName: String) : NextStep("Obtain a $crystalName Crystal")
