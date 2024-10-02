package at.hannibal2.skyhanni.config.commands

enum class CommandCategory(val color: String, val categoryName: String, val description: String) {
    MAIN(
        "§6",
        "Main Command",
        "Most useful commands of SkyHanni",
    ),
    USERS_NORMAL(
        "§e",
        "Normal Command",
        "Normal Command for everyone to use",
    ),
    USERS_BUG_FIX(
        "§f",
        "User Bug Fix",
        "A Command to fix small bugs",
    ),
    DEVELOPER_CODING_HELP(
        "§5",
        "Developer Coding Help",
        "A Command that can help with developing new features. §cIntended for developers only!",
    ),
    DEVELOPER_DEBUG_FEATURES(
        "§9",
        "Developer Debug Features",
        "A Command that is useful for monitoring/debugging existing features. §cIntended for developers only!",
    ),
    INTERNAL(
        "§8",
        "Internal Command",
        "A Command that should §cnever §7be called manually!",
    ),
    SHORTENED_COMMANDS(
        "§b",
        "Shortened Commands",
        "Commands that shorten or improve existing Hypixel commands!",
    )
}
