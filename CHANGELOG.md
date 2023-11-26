# SkyHanni - Change Log

## Version 0.22 (Unreleased)

### New Features

#### Garden Features

+ Added Garden Vacuum Pests in Pest bag to item number as stack size. - hannibal2
  + Enable via /sh vacuum.
+ Added Pests to Damage Indicator. - hannibal2
  + Enable Damage Indicator and select Garden Pests.
+ Change how the pest spawn chat message should be formatted. - hannibal2
  + Unchanged, compact or hide the message entirely.
+ Show a Title when a pest spawns. - hannibal2
+ Press a key to warp to the plot where the last pest has spawned. - hannibal2
+ Show the time since the last pest spawned in your garden. - hannibal2
  + Option to only show the time while holding vacuum in the hand.
+ Show the pests that are attracted when changing the selected material of the Sprayanator. - hannibal2
+ Added Garden only commands /home, /barn and /tp, and hotkeys. - hannibal2
+ Showing a better plot name in the scoreboard. Updates faster and doesn't hide when pests are spawned. - hannibal2
+ Show a display with all known pest locations. - hannibal2
  + Click to warp to the plot.
  + Option to only show the time while holding vacuum in the hand.

#### Other Features

+ Added Barn Fishing Timer to Jerry's Workshop and Crimson Isle. - martimavocado
+ Added Fishing Tracker and changed trackers in general. - hannibal2
  + This tracker GUI behaves the same way as the Slayer Tracker: Allows for single item remove or hide
  + Counts coin drops from chat.
  + Mark the amount in green when recently gained the item.
  + Option to hide the Fishing Tracker while moving.
  + Option to hide all Trackers while Estimated Item Value is visible.
  + Option to change the default display mode for all trackers.
  + The hidden flag for items in Item Trackers is now shared between total view and session view.
  + Option to exclude hidden items in the total price calculation.
  + Option to change the display mode that gets shown on default: Total, Current or remember last.

### Changes


#### Garden Changes

+ Added option to enable/disable the vacuum bag item number being capped to 40. - hannibal2
+ Automatic unlocking /shmouselock when teleporting in the garden. - hannibal2
+ Don't hide messages from Jacob. - alexia
  + This is a workaround for wrongly hidden Jakob messages.

### Fixes

#### Garden Fixes

+ Fixed pest damage indicator not working for some pests. - hannibal2

### Technical Details

+ Code cleanup in many files. - walker & hannibal2
+ Moved the JSON object files into another package. - walker
+ Replaced SkyHanniMod.feature.garden with GardenAPI.config. - hannibal2
+ Added MessageSendToServerEvent. - hannibal2
+ Added GardenPlotAPI, support for detecting the current slot of the player. - hannibal2

## Version 0.21.1

### New Features

+ Organised the config into sub categories. - nea & walker
+ Wrong crop milestone step detection. - hannibal2
    + When opening the crop milestone menu, a chat message is sent if Hypixel's crops per milestone level data is
      different from SkyHanni's.
    + You can use this to share your hypixel data with SkyHanni via the discord.
    + This will allow us to fix the crop milestone features quicker, as we currently do not have accurate data for this.
    + If you don't want to share anything, you can disable the chat message in the config with /sh copy milestone data.

### Changes

+ /shtrackcollection now supports sack messages. - hannibal2
+ Changed formatting of coin value to be more consistent over multiple features. - hannibal2
+ Made skill level as item number no longer default enabled. - hannibal2

### Fixes

+ Fixed the wrong colouring of hidden items in Slayer Profit Tracker. - hannibal2
+ Added support for NEU Heavy Pearl TO-DO fix working without nether sacks as well. - hannibal2
+ Fixed Estimated Item Value getting shown in pet rule creation wardrobe slot pick menu. - hannibal2

### Technical Details

+ Added /shwhereami command to show the current island. - martimavocado
+ Tons of code clean-ups over the whole project. - walker & hannibal2
  + Added ItemAddEvent. - hannibal2
+ Gets called when the user collects an item into inventory or sacks.
+ Created SkyHanniItemTracker. - hannibal2
  + This is a Special variant of SkyHanniTracker, that has item specific functions (hide or remove) and different price variants.
+ Migrated slayer profit data into SkyHanniTracker format. - hannibal2

#### Garden Changes

+ Added mythic/Maeve visitor support. - walker & hannibal2
+ Added option to use custom Blocks per Second value in some Garden GUIs instead of the real one. - hannibal2
+ Added option to change the item scale of SkyMart Coins per Copper list. - hannibal2
+ Added support for Sunder 6 in /ff upgrades. - hannibal2
+ Added support for mythic in Visitor Drop Statistics. - hannibal2
+ Use the crop fortune from tab in Farming Fortune HUD. - alexia
+ Shows the last saved ff value in gray while switching tools instead of the question mark. - hannibal2
+ Removed chat message that your crop milestone data is correct. - hannibal2
+ Removed the message when crop milestones look different in the menu than stored SkyHanni data. - hannibal2
    + We already have the correct data now, and Hypixel rounds the numbers in the menu poorly.
    + Only show the Total Crop Milestone info in crop milestone inventory when below tier 20. - hannibal2
    + Hypixel now has their own line for the same information for tier 20+
+ Make the FF Display only visible while holding a farming tool in hand. - hannibal2
+ Hide in crop milestone display the line with time remaining entirely when the milestone is maxed. - hannibal2

#### Other Changes

+ Added guess seconds to the Visitor Timer when the tab list doesn't show seconds. - hannibal2
+ Add option to hide the chat message when toggling /shmouselock. - hannibal2
+ Reminds to use the GUI Position Editor hotkey. - hannibal2
    + Reminds every 30 minutes after using /sh gui or clicking the GUI edit button.
+ Added Bookworm Book to the Estimated Item Value feature. - jani

### Fixes

#### Garden Fixes

+ Fixed new visitor alerts triggering wrongly and constantly. - Cad
+ Fixed visitor timer. - hannibal2
+ Fixed wrong Fungi Cutter mode warning not working. - walker
+ Fixed Maximum FF Needed display not showing in Jacob NPC menu. - hannibal2
+ Fixed calendar contest detection failing. - hannibal2
+ Fixed plot borders flickering and consistency errors when pressing the keybind - hannibal2
+ Fixed wrong ff needed values in Time Needed for Gold Medal GUI. - hannibal2
+ Fixed Farming Contest Medal Icons in Inventory not showing. - hannibal2
+ Fixed /ff not detecting collection analyst fortune. - hannibal2
+ Fixed Mushroom Cow Perk display not working. - hannibal2
+ Fixed visitor timer error if the visitors aren't unlocked yet. - hannibal2
+ Fixed farming weight no longer updating on block breaking. - hannibal2
+ Added cooldown to prevent spam clicking on farming weight buttons to open many web pages. - hannibal2
+ Fixed clickable farming weight GUI no longer opens #1000 in lb website. - hannibal2
+ Fixed /ff upgrade suggests updating bustling reforge even when no farming armor is found. - hannibal2
+ Fixed maxed sunder fortune in the /ff stats breakdown. - alexia
+ Fixed the farming contest summary not showing when the crop is buffed by Anita Talisman/Ring/Artifact. - hannibal2
+ Fixed Farming Fortune Display not showing for non crop-specific tools. - hannibal2
+ Fixed green thumb fortune in /ff to include Maeve. - hannibal2
+ Fixed crops per second and time remaining not using the 100 base ff in their formula. - alexia

#### Other Fixes

+ Fixed showing "slayer boss spawn soon" message outside the correct slayer area. - hannibal2
+ Fixed blocking clicks on bazaar with player name "wiki". - hannibal2
+ Fixed highlighting some mobs in the dungeon wrongly as area mini bosses. - hannibal2
+ Fixed opening the Pet menu no longer updating the current pet display. - hannibal2
+ Fixed Archfiend Dice and High Class Archfiend Dice counting as slayer drops when rolled. - hannibal2
+ Fixed dice roll profit counting as Mob Kill Coins in Slayer Tracker. - hannibal2
+ Fixed Sack Display sometimes not formatting a million correctly. - Hize
+ Fixed Estimated Item Value getting shown in stats breakdown menu. - hannibal2
+ Fixed a bug with the ender chest and SkyHanni GUI editor. - hannibal2
+ Fixed crimson isle faction icon in tab list showing twice and not going away fully when enabling the "hide faction"
  option of advanced player list. - hannibal2

### Technical Details

+ Updated to a newer version of MoulConfig. - nea & walker
    + This includes support for the new sub category part in the config.
+ Added TimeUtils.getDuration and deprecated TimeUtils.getMillis. - hannibal2
+ Created PetAPI and deprecated String.matchRegex(). - hannibal2
+ Extracted sacks, friends, known features and Jacob contests in to their separate files. - CalMWolfs
+ Add log clearing. - CalMWolfs
+ Add auto-prefix to chat message methods. - walker
+ Added support for extra data in error manager. - hannibal2
+ Added /readcropmilestonefromclipboard. - hannibal2
    + This command reads the clipboard content, in the format of users sending crop milestone step data.
    + The new data gets compared to the currently saved data, differences are getting replaced and the result gets put
      into the clipboard. The clipboard context can be used to update the repo content.

### Removed Features

+ Removed 100 Farming Fortune from "Show As Drop Multiplier" from all displays (also known as "base ff"). - hannibal2
    + This can cause some numbers to show 100 FF too much. Simply update the values to fix it.
    + Those "base FF" values were never really part of your farming fortune stats. They are just a result of looking at
      the crop drop formula. SkyHanni used those values to be more comparable with other Discord Bots and spreadsheets.
      This also caused confusion, so we have removed it entirely now.

## Version 0.21

### New Features

#### Inventory

+ Added Quick Craft Confirmation. - Cad
    + Require Ctrl+Click to craft items that aren't often quick crafted (e.g. armor, weapons, accessories).
    + Sack items can be crafted normally.
+ Added Shift Click Equipment. - Thunderblade73
    + This removes the need to shift-click to swap the equipment items, without the annoying "pick up animation".
+ Added option to highlight items that are full in the sack inventory.

#### GUI

+ Added **Compact Tab List**.
    + Compacts the tablist to make it look much nicer (old SBA feature, but fewer bugs). - CalMWolfs
    + Option to hide Hypixel advertisment banners. - CalMWolfs
    + Added **Advanced Player List**. - hannibal2
        + Customize the player list (inside the tab list) in various ways.
        + Change the sort order of players: Default, SkyBlock Level, alphabetical name, Iron Man first/bingo level,
          party/friends/guild
        + Option to hide different parts of the player list: Player skins/icons, Hypixel rank color, Emblems, SkyBlock
          level
+ Added AFK time to Discord RPC. - NetheriteMiner

#### Chat

+ Adds chat symbols such as iron man/bingo/nether faction like SBA had/has. - CalMWolfs
    + Will not break with emblems.
    + Optional if left or right side of name.
    + Should not break with other mods.
+ Added Chat **Translator** - NetheriteMiner
    + After enabling, click on any chat message sent by another player to translate it to English.

#### Rendering

+ Added Highlight Party Members. - Cad
    + Marking party members with a bright outline to better find them in the world.
+ Porting SBA's **chroma** into SkyHanni with many more options and chroma everything. - VixidDev
    + Options to change speed, size, saturation and direction.
+ Added Modify Visual Words (command /shwords). - CalMWolfs
    + Allows you to replace text on your screen with different text (like the SBE one, just less costly).
    + Supports all color codes, even chroma (use &&Z)
+ Added In-Game Date display. - Erymanthus
    + Show the in-game date of SkyBlock (like in Apec, but with mild delays).
    + Includes the SkyBlock year.
+ Added **Arrow Trail Cosmetic** - Thunderblade73
    + Draw a colored line behind the arrows in the air.
    + Options to change the color of the line, to only show own arrows or every arrow, to have own arrows in a different
      color, to change the time alive, and the line width.

#### Crimson Isle

+ Added Quest Item Helper. - NetheriteMiner
    + When you open the fetch item quest in the town board, it shows a clickable chat message that will grab the items
      needed from the sacks.
+ Added Crimson Isle **Pablo NPC Helper**. - NetheriteMiner
    + Similar to Quest Item Helper, shows a clickable message that grabs the flower needed from sacks.

##### Fishing

+ Added alerts when the player catches a Legendary Sea Creature. - Cad
+ Added **Fishing Bait Warnings.** - cimbraien
    + Option to warn when no bait is used.
    + Option to warn when used bait is changed.

#### Dungeon

+ Added Soulweaver Skull Hider to the Dungeon Object Hider. - nea
    + Hide the annoying soulweaver skulls that float around you if you have the soulweaver gloves equipped.
+ Added **Dungeon party finder** QOL improvements - Cad
    + Floor stack size.
    + Mark Paid Carries red.
    + Mark Low-Class levels orange.
    + Mark groups you can't join dark red.
    + Mark groups without your current classes green.
+ Added working **Livid Finder** (should work 100% of the time). - hannibal2
    + Option to hide other/wrong/fake Livids (try this out and see if you really want this, it can be counter-productive
      in some cases).

#### Garden

+ Added /shmouselock command to lock mouse rotation for farming. - Cad
+ Added Highlight Visitors in SkyBlock. - nea
    + Highlights Visitors outside the Garden.
+ Block Interacting with Visitors. - nea
    + Blocks you from interacting with / unlocking Visitors to allow for Dedication Cycling.
+ Added Auto-Detection of Expired Pumpkin farming fortune. - CalMWolfs

#### Events

+ Highlight Jerries during the Jerrypoclaypse. - Erymanthus
+ Show waypoints for Baskets of the Halloween Event in the main Hypixel lobby. - Erymanthus
    + Thanks Tobbbb for the coordinates!
    + Support for hiding basket waypoints once you have clicked on them. - hannibal2
    + Option to show only the closest basket. - hannibal2
+ Help with the 2023 Halloween visitor challenge (ephemeral dingsibumsi or something) - nea
    + New Visitor Ping: Pings you when you are less than 10 seconds away from getting a new visitor.
    + Accept Hotkey: Accept a visitor when you press this keybind while in the visitor GUI.
+ Added support for showing the Primal Fear data from tab list as GUI elements. - Erymanthus
+ Play warning sound when the next Primal Fear can spawn. - thunderblade73

#### Commands

+ Added shortcuts for **Party commands** and smart **tab complete**. - CalMWolfs
    + /pw -> party warp
    + /pk -> party kick
    + /pt -> party transfer
    + /pp -> party promote
    + /pko -> party kickoffline
+ Added the option to change Hypixel Wiki to the fandom Wiki in more areas than just the /wiki command. - Erymanthus
    + E.g. inside the SkyBlock leveling guide.
+ Added command **/shpumpkin** to toggle include/exclude Expired Pumpkin farming fortune in the /ff GUI and in the true
  ff
  display. - CalMWolfs

#### Misc

+ Added Kick Duration. - hannibal2
    + Show in the Hypixel lobby since when you were last kicked from SkyBlock.
    + Useful if you get blocked because of 'You were kicked while joining that server!'.
    + Send a warning and sound after a custom amount of seconds.
+ Added Time In Limbo. - hannibal2
    + Show the time since you entered limbo.
    + Show a chat message for how long you were in limbo once you leave it.

### Changes

+ Allowing clicks on the farming weight overlay to open the Farming Profile of you or the next person in the
  leaderboard, or manual reloading the farming weight. - hannibal2
+ Added "Burning Desert" as a tarantula slayer area. - hannibal2
+ Slayer features now work only when the slayer type matches the current SkyBlock area. - hannibal2
+ Made Fatal Tempo same as Chimera in Estimated Item Value. - jani
+ Added debug options for fishing hook display. - hannibal2
    + This should help find values that the fishing hook display works 100% with.
+ Changed the color for the tab list Special Persons Mark. - hannibal2
+ Mark SkyHanni Devs in the tab list special. - hannibal2
+ Added buttons to change the format of the price and the number in the sack display. - HiZe
+ Made Smoldering same as Fatal Tempo and Chimera in Estimated Item Value. - jani
+ Added an option to change where to get the items from in the composter overlay: from the bazaar or from sacks. - HiZe
+ Added mouse button support for key binds. - CalMWolfs
+ Added 'spooked into the lobby' chat message to the Outside Hypixel filter. - CalMWolfs
+ Changed the Yaw/Pitch Display to not show scientific notations at very small numbers. - Obsidian
+ Added an option to the Advanced Player List to hide Crimson Isle faction icons. - hannibal2
+ Added Enrichment to Estimated Item Value. - jani
+ Added Plhlegblast to the Rare Sea Creature Warning and Highlight. - hannibal2
+ Disabling all Diana features if no Diana mayor is active, allowing to overwrite the Diana mayor check if the election
  API check failed. - CalMWolfs
+ Hiding a bunch of garden GUIs in a bunch of garden inventories. - hannibal2
+ More shwords support. - CalMWolfs
    + Added the option to change between case-sensitive and case-insensitive.
    + Allow the use of Ctrl + C to copy the current textbox.
    + The effect of visual words is now visible while editing text.
+ Added Diamond/Gold essence to chat filter powder mining. - HiZe
+ Added fillet count and coins to Bronze/Silver Trophy Sack. - HiZe
+ Chest value is now default disabled in the dungeon, and added an option to enable it. - HiZe
+ Added support for lower case item IDs to the Hypixel command /viewrecipe. - walker
+ Added support for tab complete item IDs in the Hypixel command /viewrecipe. - hannibal2
    + Only items with recipes are tab completed.
+ Added option to set the size of highlighted motes orbs in rift and make them smaller by default. - cimbraien
+ Disabled clicks on SkyHanni GUIs while inside NEU's or Skytils profile viewer. - hannibal2
+ Removed armor stand checks for Trevor Solver. This fixes or nerfs the feature to not highlight mobs behind blocks
  sometimes. - hannibal2
+ Added diamond and gold essence support to PowderTracker. - walker
+ Change the fandom wiki search engine (under the /wiki command) from Google to the fandom wiki's built-in search
  engine - Erymanthus
+ Added option to hide Chest Value while the Estimated Item Value display is showing. - hannibal2
+ No longer merging same items with different prices in Chest Value together. - hannibal2
+ Adding Great Spook support for Non God Pot Effect display. - hannibal2
+ Added a title warning to the Worm Cap Alert ping sound. - Vahvl
+ Added support for detecting refreshed farming fortune century cake effect. - alexia
+ Show key to press below burrow warp. - hannibal2
+ Makes the Compact Potion message open the Potion effects menu on click. - jani
+ Added option to show King Talisman Helper outside Dwarven Mines. - hannibal2
+ In-Game Date: Adds support for reading the in-game scoreboard, and also allow sun/moon symbol customization. -
  Erymanthus
+ Added Estimated Item Value support to NEU Profile Viewer - hannibal2
+ Added support to import SBE Visual Words into SkyHanni. - HiZe
+ Add custom keybinds for Harp Helper. - Thunderblade73
+ Show the custom hotkey name in the Harp inventory. - hannibal2
+ Added a GUI element to remind you while /shmouselock is enabled. - CalMWolfs
+ Make Crimson Isle Quest Item Helper only get amount needed. - NetheriteMiner
+ Change config order to alphabetical. - walker
+ Added commands /shresetpowdertracker and /shresetdicertracker to reset the Powder Tracker and Dicer Drop Tracker -
  hannibal2
+ Added current session/total session switch for Dicer Drop Tracker. - hannibal2
+ Added a button to reset the local session for Dicer Drop Tracker and for Powder Tracker. - hannibal2
+ Added more features for Ender Node Tracker and Armor Drop Tracker. - hannibal2
    + Added session/display mode support, added a button to reset the current session, and added the commands
      /shresetendernodetracker and /shresetarmordroptracker to reset the full data.
+ Added support for different current sessions per profile for all new trackers: Ender Node, Armor Drop, Dicer Drop,
  Powder and Slayer Profit Tracker
+ Added the option to change the Blaze Slayer Dagger GUI positions. - hannibal2
+ Added more features to the Frozen Treasure Tracker. - hannibal2
    + Added session/display mode support, added a button to reset the current session, and added the commands
      /shresetfrozentreasuretracker to reset the full data.
+ Added Slayer Profit Tracker support for loot from area mini-bosses. - hannibal2
+ No longer opening the empty /shdefaultoptions GUI. - walker
+ Added the SkyHanni icon and a link to the GitHub page for MC launchers like Prism to display. - hannibal2

### Bug Fixes

+ The yaw/pitch display does no longer show scientific notations at small values. - hannibal2
+ Fixed slayer RNG Meter problem to detect the selected item. - hannibal2
+ Fixed capitalization errors. - J10a1n15
+ Fixed a bug that hides the contest inventory menu. - CalMWolfs
+ Fixed an error with the tab list. - hannibal2
+ Fixed fishing hook display data not properly resetting on a world change. - hannibal2
+ Fixed an error in Quick Craft Confirmation. - hannibal2
+ Fixed a crash with future NEU versions because of renamed code. - hannibal2
+ Fixed double rendering of item tooltips with chat triggers. - nea
+ Fixed Sacks Display integer limit error. - HiZe & hannibal2
+ Fixed the vitality attribute is wrongly labeled as "mending" in Estimated Item Value. - walker
+ Fixed lever clicks getting highlighted in the water room. - hannibal2
    + This solution might not work for iron man in full parties.
+ Fixed slayer profit tracker showing Bazaar/AH price as NPC price. - hannibal2
+ Fixed Hyper reforge/End Stone Geode detection. - hannibal2
+ Better limbo leave detection. - hannibal2
+ Fixed rare crash when trying to read neu config. - hannibal2
+ Fixed rare case where the visitor description is empty. - hannibal2
+ Fixed a typo in the config. - hannibal2
+ Fixed the chat filter not applying immediately after joining the Hypixel lobby. - hannibal2
+ Fixed the selected drop in the catacombs RNG meter inventory not getting highlighted. - hannibal2
+ Fixed Fishing Hook Display showing wrong damage numbers. - hannibal2
+ Fixed Abiphone ring message not getting filtered correctly. - hannibal2
+ Fixed the bug that faction icons in Crimson Isle are always hidden in the tab list. - hannibal2
+ Fixed SkyHanni GUI Edit Button not working inside storage even if neu storage is not disabled. - hannibal2
+ Hide "click" texts with the Fishing Hook Display. - hannibal2
+ Fixed Highlight Showcase Items not working in some areas. - Obsidian
+ Fixed the Blaze Slayer Damage Indicator not working and no longer causing FPS drops. - hannibal2
+ Fixed clean end working in f5/m5. - hannibal2
+ Fixed visitor drop statistics preview. - Obsidian
+ Fixed a bracket at the wrong spot in Discord RPC. - hannibal2
+ Fixed fishing hook display triggering on wrong texts. - hannibal2
+ Fixed crop milestone inventory showing wrong level as stack size when maxed out. - hannibal2
+ Fixed various bugs with the /shwords GUI. - CalMWolfs
+ Bring back the command /shwords - CalMWolfs
+ It got deleted accidentally during code optimisations.
+ Highlight Commission Mobs: The "Golden Goblin Slayer" Commission should not trigger the "Goblin Slayer" Commission
  anymore. - Thunderblade73
+ Fixed potential crash with future neu version. - CalMWolfs
+ Added missing beep sound to rejoin SkyBlock after kick warning. - hannibal2
+ Fixed Fire Veil particle hider not working. - cimbraien
+ Fixed a bug with the Trophy Sack Display reading number. - HiZe
+ Fixed Ghost Counter item stack detection. - HiZe
+ Fixed Farming Weight Display shows 0 weight wrongly sometimes. - Kaeso
+ Fixed damage indicator errors during Enderman Slayer. - hannibal2
+ Fixed weird error messages in the Damage Indicator. - hannibal2
+ Fixed Enderman Slayer errors again. - hannibal2
+ Fixed the Sack Display error again. - HiZe
+ Fixed the attribute Vitality getting wrongly labeled as Mending on attribute shards. - hannibal2
+ Made Livid Solver great again. - hannibal2
+ Fixed Damage Indicator in Dungeons for some floors. - hannibal2
+ Fixed Damage Indicator "hide nametag" feature works even for disabled mobs. - hannibal2
+ Fixed item rarity detection for pets. - hannibal2
+ Fixed rare error message while disconnecting. - Thunderblade73
+ Disabled the Daily Quest part of the Reputation Helper during the great spook. - hannibal2
+ Fixed Diana warp key not working. - hannibal2
+ SkyHanni Keybinds no longer work inside SkyHanni config. - hannibal2
+ Fixed Great Spook potion not working in Non God Pot Effect feature. - jani
+ Fixed wrong Rhys (Deep Caverns NPC) items needed for Dwarven Mines unlock in Bingo Step Helper. - ReyMaratov
+ Fixed King Talisman Helper once again. - hannibal2
+ Made the ESC -> Mod Options -> SkyHanni -> Config button not crash you. - hannibal2
+ Disabled Diana Warp key and Inquis Share key while inside any GUI. - hannibal2
+ Removed Diana warp data on world switch. - hannibal2
+ Reset mouse sensitivity back to 100% if you log off with lock mouse look enabled. - hannibal2
+ Fixed mouse sensitivity stuck after restarting by storing old sensitivity. - CalMWolfs
+ Fixed tool fortune. - CalMWolfs
+ Fixed Item Ability Cooldown display not activating for Sword of Bad Health. - hannibal2
+ Fixed the crop name gets replaced to internal name in /shwords. - hannibal2
+ Show obfuscated fish as bait instead of caught item. - cimbraien
+ Fixed Estimated Item Value that renders twice inside NEU PV, by not rendering anything when the cursor is exactly in
  between two items. - hannibal2
+ fixed more error messages with The Great Spook data getting stored in the Reputation Helper quest config
  accidentally. - hannibal2
+ Hopefully fixed resets of Visitor Drops stats. - hannibal2
+ Fixed typo in The Art Of Peace. - walker
+ Fixed compatibility problems with ChatTriggers that caused flickering in the Estimated Item Value while inside the NEU
  Profile Viewer. - hannibal2
+ Fixed Quest Item Helper showing negative numbers. - hannibal2
+ Fixed YouTuber and Admin ranks getting lost in the tab list. - walker
+ Added a cooldown to the current session tracker reset button to fix the chat spam. - hannibal2
+ Changed the color of the "Slayer boss soon!" warning from red to yellow. - hannibal2
+ Fixed a bug where some items were counted twice in the Slayer Profit Tracker. - hannibal2
+ Fixed item rarity errors in the museum. - hannibal2
+ Fixed mob highlighting problems with Blaze Slayer and Skytils. - hannibal2
+ Pablo Helper: Fixed some messages not showing the "get from sack" clickable message. - hannibal2
+ Fixed scoreboard date number suffixes are missing sometimes. - Erymanthus
+ Fixed the leftStronghold area not getting detected. - hannibal2
+ Fixed error message with Ashfang Blazes. - hannibal2
+ Fixed crash with pet exp tooltip. - hannibal2
+ Fixed dungeoneering showing as 0 in the skill menu. - hannibal2
+ Fixed showing minion level as 101 in some menus. - hannibal2

#### Config

+ Fixed two typos in the config description. - Absterge
+ Fixed small typos in config. - Absterge

#### Removed Features

+ Removed **Duplicate Hider**.
    + Hypixel now fixed the bug themselves and hides duplicate farming contests in the Jacob inventory.

#### Technical Details

+ Add Repo TODOs to regex patterns. - walker
+ Moved many patterns from function scope to members. - hannibal2
+ Avoid hardcoded dispatcher. - walker
+ Created and used Entity.canBeSeen and LorenzVec.canBeSeen. - hannibal2
+ Reducing cognitive complexity in StatsTuning.kt - walker
+ Reducing indentations and line counts in StatsTuning.kt - hannibal2
+ Mark functions around item utils and neu items as deprecated. - walker
+ Added debug command /shconfigmanagerreset. - hannibal2
+ Reloads the config manager and rendering processors of MoulConfig. This WILL RESET your config, but also update the
  java config files (names, description, orderings and stuff).
+ Adding 100 lines to MobFinder.kt and making it better readable in the process. - walker
+ Making ChatFiler.kt way better, storing regex objects for reuse and preparing future repo support. - walker
+ Added command /shkingfix to reset the internal King Talisman Helper offset. - hannibal2
+ Updated dependency version of junixsocket in DiscordIPC so that antivirus websites no longer show false positives. -
  NetheriteMiner
+ Changed wrong/missing neu version message to show NEU version 2.1.1-Pre-4 instead of beta versions. - CalMWolfs
+ Deleting the old "hidden" part of the config. - hannibal2
+ This will reset parts of the config for users with 7-month-old SkyHanni versions that want to migrate into the
  present.
+ Added a workaround for the NEU Profile Viewer bug where the ItemTooltipEvent gets called for two items when hovering
  over the border between two items. - hannibal2
+ Using visitorDrops.visitorRarities directly from the config instead of accessing the local field. Hopefully this will
  prevent partial config resets in the future. - hannibal2
+ Added a tracker API that can be used for all features in SkyHanni that currently track stuff that the user collects. -
  hannibal2
+ Added the slayer profit tracker logic (command to reset, toggle between total view and session view, and button to
  delete session) to powder tracker and Dicer Drop Tracker. - hannibal2
+ Added support for migrating parts of the player or session storage. - nea
+ Changed the config format for dicerRngDrops/dicerDropsTracker. - hannibal2
+ Created SkyHanniTracker, the core API for working with tracker stuff. This should be used everywhere someday in the
  future. - hannibal2
+ Used SkyHanniTracker in FrozenTreasureTracker. - hannibal2
+ Added /shdebugwaypoint as a test/debug command. - hannibal2
+ Added debug messages to detect hot swaps. - hannibal2
+ Added /shdebugtablist
+ Set your clipboard as a fake tab list. - hannibal2
+ /shversion now copies the SkyHanni version into the clipboard as well. - hannibal2
+ Moved location fixes to the repo. - hannibal2
+ Added debug information for PetExpTooltip crash. - hannibal2

## Version 0.20

### New Features

+ Replacing command `/warp is` with `/is`.
+ Added command `/shbingotoggle` to toggle the bingo card.
+ Added option to disable quick bingo card toggle with sneaking.
+ **King Talisman Helper**
    + Show kings you have not talked to yet, and when the next missing king will appear.
+ **Harp Keybinds** - NetheriteMiner
    + In Melodys Harp, press buttons with your number row on the keyboard instead of clicking.
+ **Ender Node Tracker** - pretz
    + Tracks items and profit obtained from mining ender nodes and killing normal endermen.
+ **Fishing timer** now works in **Crystal Hollows** as well. (Worm fishing)
+ Added keybind to manually reset the barn/worm fishing timer. - CarsCupcake
+ Added barn fishing timer support for stranded. - hannibal2
+ Option to shorten the **bestiary level-up message**.
+ **Bestiary overlay** - HiZe
    + Options for change number format, display time, number type and hide maxed.
    + Highlight maxed bestiaries.
+ Show the names of the **4 areas** while in the center of **crystal Hollows**.
+ Chest Value - HiZe
    + Shows a list of all items and their price when inside a chest on your private island.
+ In Melody's Harp, show buttons as stack size. - NetheriteMiner
    + Intended to be used with Harp Keybinds
+ Added timer till shared inquisitors will despawn. - hannibal2
+ Account upgrade complete reminder. - appable0
+ Chat message how many places you dropped in the farming weight lb when joining garden.
+ Added command /shfarmingprofile [player name]
    + Opens the elitebot.dev website in your web browser to show your Farming Weight profile.
+ Pet Experience Tooltip
    + Show the full pet exp and the progress to level 100 (ignoring rarity) when hovering over a pet while pressing
      shift key.
    + Highlight the level 100 text in gold for pets below legendary. - hannibal2
      (This is to better indicate that the pet exp bar in the item tooltip is calculating with legendary.)
    + Option to only show level 100 for golden dragon in the pet experience tooltip. - hannibal2
+ Anita Extra Farming Fortune:
    + Show current tier and cost to max out in the item tooltip.
+ Sync Jacob Contests - Kaeso + CalMWolfs
    + No need to open the calendar every SkyBlock year again.
    + Grab Jacob Contest data from the elitebot.dev website.
    + Option to send local contest data to elitebot.dev at the start of the new SkyBlock year.
+ Added SkyHanni **Installer** - NetheriteMiner
    + Double-clicking the mod jar file will open a window that asks you where to move the mod into.
+ Show the progress bar until maxed crop milestone in the crop milestone inventory. - hannibal2
+ Show a line to the enderman slayer beacon. - hannibal2
+ Added **Default Option Settings:** - nea
    + Enables or disables all features at once, or per category.
    + Sends a chat message on first SkyHanni startup (starting with this feature, so this version everyone will see this
      message).
    + Shows new features after an update (starting with the next beta, not this one).
    + Allows to change those settings anytime again with /shdefaultoptions.
+ While on the Winter Island, show a timer until Jerry's Workshop closes. - hannibal2
+ Added Reindrake support to the Damage Indicator. - hannibal2
+ Added visual garden plot borders. - VixidDev
    + Press F3 + G to enable/disable the view.
+ Added **Following Line** Cosmetic - hannibal2
    + Draws a colored line behind the player.
    + Change the color, width, and duration of the line.
+ Show alert when reaching max super-pairs clicks. - pretz
    + Plays a beep sound and sends a message in chat when the player reaches the maximum number of clicks gained for
      super-pairs minigames.
+ Added gemstone slot unlock costs to the Estimated Item Value. - Fix3dll
+ **Powder Grinding Tracker** - HiZe
    + Shows the Mithril/Gemstone Powder gained, the number of chests opened, if Double Powder is active, and the items
      collected.
    + Change between current session and total (open the inventory and click on Display Mode).
    + Fully customizable: change what items or stats to show.
    + Has support for the maxed Great Explorer perk.
    + Option to hide while not grinding powder.
+ Added Anniversary Event Active Player Ticket Timer. - nea
    + Option to play a sound as well.
+ Added highlight and outline feature for rare sea creatures. - Cad
+ Add feature to outline dropped items. - Cad
+ Add Outline Dungeon Teammates. - Cad
+ Added Price Website button. - hannibal2
    + Adds a button to the bazaar product inventory that will open the item page in skyblock.bz.
+ Added icons for the medals received in a contest. - CalMWolfs
    + Different symbol for when it was a Finnegan contest.
+ Added Tab Complete support to sacks command /gfs and /getfromsacks. - J10a1n15
+ Added GUI Scale. - nea
    + Scroll within the position editor to independently adjust the GUI scale for each SkyHanni element.
    + Change the global scale of all SkyHanni elements at once (in the config under /sh scale).
+ Added Fishing Hook Display. - hannibal2
    + Display the Hypixel timer until the fishing hook can be pulled out of the water/lava, only bigger and on your
      screen.
+ Added **Trevor the Trapper Tracker**. - CalMWolfs
    + Quests done
    + A breakdown of their rarity
    + Animals killed vs. animals that kill themselves
    + Pelts per hour
+ Press the hotkey to accept the next Trevor the Trapper quest. - CalMWolfs
+ Added a countdown for Arachne spawn. - Cad
    + Supports quick spawns.
+ Added **Sack Change** chat message hider. - hannibal2
    + Enable this option instead of Hypixel's own setting to hide the chat message while enabling mods to utilize sack
      data for future features.
+ Added dungeon information to the Discord RPC. - NetheriteMiner
    + Show the current floor name.
    + Time since the dungeon started.
    + Number of boss collections of the current boss.
+ Added a Dynamic Priority Box to Discord RPC. - NetheriteMiner
    + Change the order or disable dynamically rendered features (e.g. Slayer, Dungeon, Crop Milestone, Stacking
      Enchantment)
+ Dungeon Colored Class Level. - hannibal2
    + Color class levels in the tab list. (Also hide rank colors and emblems because who needs that in dungeons anyway?)

### Changes

+ Added option to disable quick bingo card toggle with sneaking.
+ Made damage indicator more performant. - nea
+ Ghost bestiary update. - HiZe
+ Api error messages are now formatted more helpful.
+ Added option to only show the reputation helper while pressing a hotkey.
+ Garden **Money per Hour** now uses the **dicer drops** from melon and pumpkins as well. - CalMWolfs
+ Adds **Double Hook** to the **sea creature chat message** instead of in a previous line. - appable0
+ Rune display now shows always in sack display.
+ Shark fish counter now counts twice for Double hook. - appable0
+ Ghost counter check for Mist now ignores y coordiantes - HiZe
+ Telling the user about the bypass hotkey when the visitor drop warning blocks a visitor refusal. - CalMWolfs
+ Added warning sound when the worm fishing cap of 60 is hit. - CarsCupcake
+ Shared inquisitor waypoints will now get removed after 75 seconds. - hannibal2
+ Chest Value now works with Backpack and Ender Chest. - HiZe
    + Only works if NEU storage is not active.
+ Removed distance checks when detecting sea creatures for fishing timer. - hannibal2
+ Added Enchantments Cap to EstimatedItemValue. - hannibal2
    + Only show the top # most expensive enchantments on an item.
+ Count sea emperor and rider of the deep twice against sea creature cap.
+ Clicking on the chat message from farming weight will run the /shfarmingprofile command.
+ Changed Bestiary Display number format - HiZe
+ Changed ff buffs for Elephant and Anita.
+ Changed chicken head timer from 20s to 5s.
+ Added option to show reputation locations only when pressing the hotkey.
+ Delay the custom garden level up message by a few milliseconds to not cut into the garden milestone message.
+ Added runic support for Zealots/Bruiser.
+ Added cultivating and replenish to the visitor rewards. - ReyMaratov
+ Added Bee pet support to /ff - derholzmann12321
+ Added exportable carrot to /ff. - CalMWolfs
    + If SkyHanni doesn't know you have it, run /shcarrot
+ The damage indicator now includes a toggle option for the Ender Slayer Laser Phase timer. - hannibal2
+ Added ender slayer beacon explode timer. - hannibal2
+ Show the name over Nukekubi Skulls for in ender slayer (deadly eyes). - hannibal2
+ Changed the enderman slayer laser timer to be more exact. - hannibal2
+ Added support for the new fixed inquisitor spawn message from Hypixel for the detection. - hannibal2
+ Added option to hide the off-screen drop chat message. - hannibal2
+ Now hides the farming weight display by default when below 200 weight (can be disabled). - hannibal2
+ Added option to change the Enderman Slayer beacon color. - hannibal2
+ Added option to show a line to every slayer mini boss around you. - hannibal2
+ Added options to ignore the wizard and the crypt warp for Diana.
+ Loading farming weight values directly from elitebot.dev. - CalMWolfs
    + This has no effect right now but allows Kaeso to change the values easier in the future.
+ Slightly changed the FF guide tab colors to make more sense. - Obsidian
+ Changed the bingo tips description to show the actual data source.
+ Added Fire Fury Staff support to item ability cooldown feature. - Cad
+ Estimated item value now shows the reforge stone apply cost as well. - hannibal2
+ Added Bits, Mithril Powder, and Gemstone Powder to visitor drop statistics. - Obsidian
+ Added support for slayer profit trackers on stranded. - hannibal2
+ Added Trapper Cooldown GUI. - NetheriteMiner
    + Show the cooldown on screen in an overlay (intended for abiphone users).
+ Made the fake garden level up message (after level 15) clickable. - J10a1n15
+ Moving many GUI elements in the config around. - hannibal2
    + This will not reset anything.
    + This includes the creation of new categories and regrouping.
+ Better Trevor the Trapper detection. - CalMWolfs
+ Show Trevor the Trapper's mob name next to the waypoint. - CalMWolfs
+ Small performance improvements when working with color codes. - brainage04
+ Added dungeon mage cooldown reduction support for item ability cooldown. - Cad
+ Improved the wording and fixed typos of the config category descriptions. - zapteryx
+ Changed the option in the pet experience tooltip to show progress to level 100 for golden dragon eggs (can be
  disabled). - hannibal2
+ Replaces the word Tps to TPS. - Erymanthus
+ Improved performance of item ability cooldown and farming fortune display data. - Cad
    + The item flickering for ability cooldown background should be less/gone completely.
+ Added the ability to bypass not clickable items when holding the control key. - CalMWolfs
+ Croesus Chest Tracker can now determine chest that only can be open with a dungeon key - Thunderblade73
+ Added armor drops to the Money per Hour display. - CalMWolfs
+ Croesus chest highlight now uses different colors for Dungeon Chest Key-only chests. - Thunderblade73
+ Added support to change the scale of GUI elements with plus and minus keys. - CalMWolfs
+ Ignoring non-slayer drops in the slayer profit tracker. - hannibal2
+ Added support for slayer drops that go directly into the sack. - hannibal2
    + This still does not work for items that land directly in your inventory. (e.g., Netherrack-Looking Sunshade,
      Summoning Eye, etc.)
+ Added toggle for 12hr/24hr in real-time HUD. - Thunderblade73
+ Removed Flowering Bouquet from and added Cultivating to the default enabled Visitor Reward Warnings list. - hannibal2
+ Improved performance in the Garden Visitor inventory. - hannibal2

### Fixes

+ Removed `Simple Carrot Candy` from composter overlay.
+ Fixed croesus highlight unopened chest not working anymore. (ty hypixel)
+ Should not crash anymore if a file in repo is missing.
+ Fixed Killer Spring tower still highlighting even with the feature turned off. - HiZe
+ Fixed weird tuba ability cooldown not working in rift.
+ Fixed holy ice cooldown sometimes not working.
+ Fixed a rare startup crash.
+ Fixed Ghost stats after bestiary update. - HiZe
+ Watchdog hider now correctly hides empty lines as well, even when empty line hider is disabled. - appable0
+ Fixed `saw mob` title for trevor trapper solver still showing even when feature is disabled.
+ Fixed chicken from raider of the sea get detected as trevor trapper mob.
+ Fixed master star detection in estimate item value broken for some times.
+ Fixed description in golden trophy fish info tooltip. - appable0
+ Fixed End Node Tracker not updating when changing the text format.
+ Fixed neu repo error messages with runes.
+ Fixed rare crashes with scoreboard.
+ Fixed feature that replaces the sack stitched lore message. - hannibal2
+ Fixed some typos in config descriptions and correctly rounding down in composter overlay. - CalMWolfs
+ Fixed a typo in an error message. - Obsidian
+ Fixed Chest Value fails to detect chests when using a resource pack. - HiZe
+ Fixed Sea Creature Timer not working on barn. - hannibal2
+ Arachne boss highlighter no longer requires damage indicator to be enabled. - hannibal2
+ /shtrackcollection Cactus now detects cactus green. hannibal2
+ Fixed arachne minis falsely show in damage indicator.
+ Fixed rare cases where special laptop keys trigger behavior unintended. - hannibal2
+ Fixed rendering problems with stack background and custom text lore. - nea
+ Hopefully fixed Derpy problems with Trevor Trapper, Arachne, Arachne Keeper and Zealots.
+ Fixed Anita upgrade detection from the Anita Shop inventory.
+ Fixed error message when clicking a finished upgrade in the community shop.
+ Fixed everything **crop milestone** related. - CalMWolfs
+ Fixed estimated item value detects master stars on non-dungeon items. - hannibal2
+ Fixed wrong progress bar for pet exp display. - hannibal2
+ Fixed compatibility problem with NEU for pet exp number format. - hannibal2
+ Various **/ff fixes** - CalMWolfs
+ Numbers, rounding and pets being reset.
+ Fixed inventory item number offset being wrong. - CalMWolfs
+ Fixed slayer quest detection after death. - hannibal2
+ Fixed rounding errors with yaw and pitch display. - hannibal2
+ Fixed ender slayer beacon don't disappear sometimes. - hannibal2
+ Fixed multiple bugs with garden visitor: - hannibal2
    + Visitors with multiple different crops now calculate the copper price and the visitor drop stats correctly.
    + Reward items no longer impact the visitor drop coins spent data negatively.
    + Copper per coin price now respects the reward item profit as well.
    + Now showing the NPC price for items without ah/bazaar data (looking at pet candy).
+ Fixed highlight in the main bestiary menu. - HiZe
+ Fixed maxed farming fortune possible in /ff - CalMWolfs
+ Fixed negative coins per copper price in visitor inventory. - hannibal2
+ Fixed rare error message while farming. - hannibal2
+ Fixed a rare error message while showing the lore of a farming tool. - hannibal2
+ Fixed estimated item value doesn't detect master stars in Auction House. - hannibal2
+ Fixed enderman slayer beacon warning only visible for a very short amount of time. - hannibal2
+ Fixed enderman slayer line to beacon only shows when beacon highlight is enabled. - hannibal2
+ Fixed major composter profit calculation with multi-drop. - pretz
+ Garden Money Per Hour: Fixed the wrong NPC price when Merge Seeds is enabled. - hannibal2
+ Hopefully fixed error messages in bingo around collection values. - hannibal2
+ Hopefully fixed a rare chat error. - hannibal2
+ Fixed duration format in non-god-potion display. - appable0
+ Fixed various typos. - pretz
+ Fixed minion nametag not getting removed after picking it up. - Cad
+ Fixed 3k hp spiders being falsely highlighted as Arachne's Keepers. - oofy
+ Fixed a bug in GhostCounter for the 48th time. - HiZe
+ Fixed typo of effigy in the rift blood effigies display. - Vahvl
+ Fixed potential bug in salvage inventory with "hide not clickable items" on the alpha. - hannibal2
+ Fixed small typos in config. - hannibal2
+ Fixed maxed pet exp progress bar is wrong for bingo pet. - hannibal2
+ Hopefully fixed bug that opening a visitor too quickly causes detection problems. - hannibal2
+ Added nametags for minions as soon as they are placed. - Cad
+ Fixed wrong display offset in Powder Tracker. - Hize
+ Fixed some features not working correctly because of broken location detection for wrong Hypixel area names. - Cad
+ This includes:
+ Wilted Berberis Helper (in the Rift, Dreadfarm)
+ Slayer Profit Tracker (in the Park, Howling Cave)
+ End Node profit Tracker (in the End)
+ Fixed crash when item rarity cannot be detected in Auction House. - hannibal2
+ Fixed Replenish and Cultivating being bugged in visitor drop statistics. - Obsidian
+ Summoning souls should no longer be counted as sea creatures for the fishing timer. - hannibal2
+ Fixed Jacob Contests times needed display showing impossible FF values. - ReyMaratov
+ Fixed item cooldown issues with fire fury staff and jinxed voodoo doll. - Cad
+ Fixed the percentage going above 100% for bestiary progress. - HiZe
+ Fixed max kills from Ghost Counter using old bestiary data when resetting the config. - HiZe
+ Fixed part of Zealot Hideout area showing as The End. - Cad
+ Fixed many typos in the config. - schlaumeyer, oofy, CalMWolfs & hannibal2
+ Fixed a missing bracket in the reforge apply cost display from Estimated Item Value. - jaNI
+ Fixed a rare crash while doing enderman slayer. - hannibal2
+ Fixed pet exp tooltip doesn't show in pet inventory or Hypixel profile viewer (right-click a player). - hannibal2
+ Fixed turbo books price not getting detected in visitor rewards. - hannibal2
+ Fixed that the Paste Into Sign feature only pastes into the first line. - hannibal2 + nea
+ Hopefully fixed rare config reset cases. - nea
+ This should also fix problems with false positive detections in the crimson isle.
+ Fixed item rarity problems. - hannibal2
+ Fixed a rare error when opening minion inventory. - hannibal2
+ Fixed stuff in the **Trozen Treasure Tracker**. - CalMWolfs
    + The ice/hour calculation
    + Rate Timer
    + Typos
+ Fixed a small typo in the config. - hannibal2
+ Fixed inconsistencies with Arachne brood highlighting. - Cad
+ Fixed Crimson Reputation Helper doesn't count trophy fish when sacks exist. - Fix3dll
+ Fixed a rare error when switching to a dicer farming tool too quickly. - Cad
+ Added workaround for new fire sale cosmetics (pet skin, helmet skin, rune) in estimated item value. - hannibal2
+ Fixed garden visitors not highlighting on status "new". - hannibal2
+ Fixed wrongly highlighting enchanted sacks for reputation helper fetch quests. - hannibal2
+ Fixed Fragged Spirit Mask not showing a cooldown after being triggered. - Cad
+ Fixed item rarity problems with very special. - hannibal2
+ Fixed party member detection issues for the tab complete feature. - CalMWolfs
+ Hide item rarity error message in /ff. - hannibal2
+ Fixed an issue with the Wheat Profit Display not showing the correct value when combined with seeds. - Thunderblade73
+ Tab complte party members now also detects if the party leader leaves. - CalMWolfs
+ Fixed typos in many config settings. - Absterge
+ Fixed NEU Heavy Pearl detection. - hannibal2
+ Fixed showing Wrong Slayer Warning sometimes at the wrong time. - Cad
+ Fixed farming contest summary time being off sometimes. - hannibal2
+ Fixed wrong order at Visitor Drop Statistics. - hannibal2
+ Fixed bits and powder not saving in Visitor Drop Statistics. - hannibal2
+ Fixed not being able to disable show Dungeon Head Floor Number as stack size. - hannibal2
+ Fixed green bandana not getting detected for visitor reward warning. - hannibal2
+ Fixed zealots and zealot bruisers not getting detected when being runic. - hannibal2
+ Fixed mixins not getting detected correctly from effects inventory for Non God Pot Effect display. - hannibal2
+ Fixed an error when detecting Jacob Contest times. - hannibal2
+ Fixed Mushroom Goal breaking the Bingo Card detection. - hannibal2
+ Fixed Diana's Griffin Pet Warning not working during the Jerry Mayor. - hannibal2
+ Fixed /ff not detecting changes in the /pets inventory. - CalMWolfs

### Removed Features

- Removed **Broken Wither Impact** detection. - hannibal2
    - (Hypixel fixed their bug, finally)
- Removed remaining **Hypixel API** support for still existing legacy api keys. - hannibal2
    - This should not affect much
- Removed wishing compass in item number.
    - Every Wishing Compass now only has one use instead of three.

## Version 0.19

### Rift Features

+ Added **Rift Timer**
    + Show the remaining rift time, max time, percentage, and extra time changes.
+ **Highlight Guide**
    + Highlight things to do in the Rift Guide.
+ Added **Shy Warning** (Contributed by CalMWolfs)
    + Shows a warning when a shy crux will steal your time.
    + Useful if you play without volume.
+ Added **Larvas Highlighter**
    + Highlight larvas on trees in Wyld Woods while holding a Larva Hook in the hand
    + Customize the color
+ Added **Odonatas Highlighter**
    + Highlight the small Odonatas flying around the trees while holding an Empty Odonata Bottle in the hand.
    + Customize the color.
+ Added **Agaricus Cap** countdown
    + Counts down the time until Agaricus Cap (Mushroom) changes color from brown to red and is breakable.
+ Added **Leech Supreme** to Damage Indicator
+ Added **Volt Crux Warning** (Contributed by nea)
    + Shows a warning while a volt is discharging lightning
    + Shows the area in which a Volt might strike lightning
    + Change the color of the area
    + Change the color of the volt enemy depending on their mood (default disabled)
+ Added **Enigma Soul Waypoints** (Contributed by CalMWolfs)
    + Click on the soul name inside Rift Guide to show/hide
+ Added **Kloon Hacking** (Contributed by CalMWolfs)
    + Highlights the correct button to click in the hacking inventory
    + Tells you which color to pick
    + While wearing the helmet, waypoints will appear at each terminal location
+ Added **Crux Talisman Progress** Display - HiZe
    + Show bonuses you get from the talisman
+ Added existing slayer feature support for **Vampire Slayer** - HiZe
    + This contains RNG Meter, Slayer quest warning and Items on ground
+ Added item ability cooldown support for **Weirder Tuba** and **Holy Ice** - HiZe
+ Added **Lazer Parkour** Solver - CalMWolfs
    + Highlights the location of the invisible blocks in the Mirrorverse
+ Added Mirrorverse **Dance Room Helper** - HiZe
    + Helps to solve the dance room in the Mirrorverse by showing multiple tasks at once.
    + Change how many tasks you should see
    + Hide other players inside the dance room
    + Added timer before next instruction
    + Option to hide default title (instructions, "Keep it up!" and "It's happening!")
    + Fully customize the description for now, next and later (with color)
+ Added **Upside Down** Parkour & **Lava Maze** - hannibal2
    + Helps to solve the Upside Down Parkour and Lava Maze in the Mirrorverse by showing the correct way
    + Change how many platforms should be shown in front of you
    + Rainbow color (optional) - nea
    + Hide other players while doing the parkour
    + Outlines the top edge of the platforms (for Upside Down Parkour only) - CalMWolfs
+ Added Jinxed voodoo doll ability cooldown support - HiZe
+ Added Polarvoid Books to estimated item value - jani
+ Added **Motes NPC** price in the item lore
    + With Burgers multiplier - HiZe
+ Added Motes Grubber to Not Clickable Items feature
+ **Living Metal Suit** Progress - HiZe
    + Display progress Living Metal Suit (Default disabled)
    + Option to show a compacted version of the overlay when the set is maxed
+ Added Highlight for Blobbercysts in Bacte fight in colloseum in rift - HiZe
+ Show a line between **Defense blocks** and the mob and highlight the blocks - hannibal2
    + Hide particles around Defense Blocks
+ Show a moving animation between **Living Metal** and the next block - hannibal2
    + Hide Living Metal particles
+ Highlight **flying Motes Orbs** - hannibal2
    + Hide normal motes orbs particles
+ Added bloodfiend (vampire slayer) to Damage Indicator - HiZe
+ Add Bacte to Damage Indicator
+ Hide Not Rift-transferable items in Rift Transfer Chest as part of the hide not clickable items feature
+ Add npc motes sell value for current opened chest - HiZe
+ Vampire Slayer Feature - HiZe
    + Sound for twinclaws
    + Option to delay twinclaws notification and sound (in millis, configurable)
    + Draw line starting from the boss head to the Killer Spring/Blood Ichor (if the boss is highlighted)
    + Draw line starting from your crosshair to the boss head
    + Configurable to work only on your boss, on bosses hit, or on coop boss
+ Show locations of inactive Blood Effigy
    + Show effigies that are about to respawn
    + Show effigies without known time
+ Added **Wilted Berberis Helper**
    + Option to only show the helper while standing on Farmland blocks
    + Option to hide the wilted berberis particles
+ Added **Vampire Slayer** features in Damage Indicator
    + Show the amount of HP missing until the steak can be used on the vampire slayer on top of the boss.
    + Show a timer until the boss leaves the invincible Mania Circles state.
    + Show the percentage of HP next to the HP.
+ Added **Horsezooka Hider**
    + Hide horses while holding the Horsezooka in the hand.

### Other New Features

+ Added **Frozen Treasure Tracker** (Contributed by CalMWolfs)
    + Show different items collected while breaking treasures in the Glacial Cave in Jerry's Workshop
    + Show Ice per hour
    + Customizable GUI
    + Option to hide the chat messages
+ Added **Custom Text Box** - CalMWolfs
    + Write fancy text into a gui element to show on your screen at all time
    + Supports color codes
    + Supports line breaks `\n` - hannibal2
+ Added **Highlight Commission Mobs** - hannibal2
    + Highlight Mobs that are part of active commissions
+ Added command **/shcommands**
    + Show all commands in SkyHanni
+ Added Attribute price support to Estimated Item Value - nea
+ Added warning when enderman slayer beacon spawns - dragon99z
+ Added Highlight enderman slayer Nukekubi (Skulls) - dragon99z
+ Added option to hide the vanilla particles around enderman
+ Hide particles around enderman slayer bosses and mini bosses
+ Added support for Shadow Fury abilty cooldown - HiZe
+ Added /sendcoords sending, detecting and rendering - dragon99z
+ Added **Boss Spawn Warning** - HiZe + hannibal2
    + Send a title when your slayer boss is about to spawn
    + Configurable percentage at which the title and sound should be sent
+ Added **Refusal Bypass Key** - HiZe
    + Hold a custom key to bypass the Prevent Refusing feature for visitors
+ Added **Farming Weight ETA Goal** - Kaeso
    + Override the Overtake ETA to show when you will reach the specified rank.
    + If not there yet
    + Default: #10k
+ Added **Dungeon Potion level as item stack size - HiZe
+ Added **Griffin Pet Warning**
    + Warn when holding an Ancestral Spade while no Griffin pet is selected.
+ More **Trophy Fish** features - appable0
    + **Trophy Fish Info** - Hover over trophy fish caught chat message to see information and stats about the trophy
      fish.
    + **Fillet Tooltip** - Adding fillet amount and price to the tooltip of a trophy fish. Left shift to show stack
      value.
+ Added Chumcap support for Chum Bucket Hider - jani

### Changes

+ SkyHanni no longer requires Patcher to start! (Big thanks nea)
+ Option to show the yaw and pitch display outside garden or while not holding a farming tool (Contributed by CalMWolfs)
+ Added wizard warp as diana waypoint
+ Added option to show exact prices in estimated item value
+ Better error handling with missing neu repo items - CalMWolfs
+ Changes to **Discord RCP** - NetheriteMiner
    + More images for different locations
    + Fixed names unnecessarily getting cut off when visiting an island in Location
    + Fewer crashes because of NEU Item Repo
+ Changed time for Trapper from 30 to 15 seconds while Finnegan is mayor – CalMWolfs
+ Added warning to not disable repo auto-update
+ Wither impact ability cooldown now only shows when all 3 scrolls are attached
+ Show total amount of all trophy fish rarities at the end of the chat message
+ Changed **Elite farming weight** display - Kaeso
    + Instantly showing the next player in the lb when passing someone on the leaderboard
+ Show in chat the total amount of sharks fished after the fishing contest
+ Custom text over Visitor name moves more smoothly now
+ Discord Rich Presence now supports Motes in the rift and the garden plot number - NetheriteMiner
+ Crop money display: Using npc price for mushroom cow pet calculation when on ironman, stranded or bingo
+ Sacks no longer get blocked from moving in storage (not clickable items feature)
+ Using 19.9 bps if bps is too low for jacob contest ff needed display
+ Better default positions for some guis

### Fixes

+ Fixed Pocket Sack-In-A-Sack Replace in lore
+ Fixed possible crash with broken neu repo when opening the desk inventory in the garden (Contributed by CalMWolfs)
+ Fixed frozen treasures per hour display being inaccurate (Contributed by CalMWolfs)
+ Fixed bug with ghost counter sometimes not detecting new kills (Contributed by CalMWolfs)
+ Fixed **Ghost Counter** - HiZe & ksipli
    + Should no longer have compatibility issues with other mods
    + It work even if the action bar show percent +xxx (xx%) instead of +xxx (xxx/xxx)
    + Added command /shresetghostcounter to reset everything (not the bestiary data)
    + Added time format in ETA formatting (can show days, hours, minutes and seconds remaining)
+ Fixed **Dungeon Master Stars** calculation not working correctly sometimes – hannibal2 & Fix3dll
+ Fixed Discord Rich Presence detecting absorption hearts not correctly – NetheriteMiner
+ Fixed Reputation Helper **Kuudra Completions** not getting detected and adding T4 and T5 support - Cinshay
    + Fixed Item Ability cooldown not working for Astraea - hannibal2
    + Fixed a typo in the config - hannibal2
    + Added a workaround for a crash when refusing a visitor - hannibal2
+ Added support for new counter drops to dicer rng counter - ReyMaratov
+ Fixed composter inventory numbers after Hypixel changes - hannibal2
+ RNG dicer chat hider now works without enabling drop counter
+ Server restart timer no longer shows all the time if over 2 minutes
+ Fixed crazy rare drops not counting properly - ReyMaratov
+ Fixed individual attribute prices in estimated item value - nea
+ Fixed sack display detection - hize
+ Fixed rare Ghost Counter bugs - hize
+ Fixed a bug that farming weight display does not show up if you only have one profile
+ Fixed broken thorn damage indicator detection in dungeon F4/M4
+ Fixed togglesneak mod breaking escape menu open detection for quick mod menu switch
+ Fixed error with detecting hit phase during eman slayer in damage indicator
+ No longer double counting mob kill coins in slayer item profit tracker
+ Fixed jacob contest time chat message chat shows one second too long
+ Fixed farming contest calendar reading going above 100% - Contrabass26
+ Changed multiple descriptions in the misc category, fixed grammar and punctuation problems - Absterge
+ Fixed rare bug with Damage Indicator
+ Fixed showing skill level as item stack being broken for level 0. - Erymanthus
+ Fixed ability cooldown not working for some items. - Cad
+ Fixed Bazaar item category "Ink Sack" gets wrongly detected as an item sack. - Erymanthus
+ Fixed reforge stone "Pitchin' Koi" not detected in the Estimated Item Value. - Fix3dll
+ Minion Craft Helper now ignores Inferno and Vampire minions as well. - walker
+ Better wording in the auto updater. - zapteryx

## Version 0.18 (2023-06-19)

### New Features

+ Added **Visitor Drop Counter** (Contributed by CalMWolfs)
    + Counts up all the drops that you get from visitors
    + Count each rarity of visitor Accepted
    + Count copper, farming exp and coins spent
    + Setting to show number or drop first
    + Setting to show as the icon instead of the name
    + Setting to show only on the barn plot
+ Added **Contest Time Needed**
    + Show the time and missing FF for every crop inside Jacob's Farming Contest inventory.
+ Added **Garden Start Location**
    + Show the start waypoint for your farm with the currently holding tool.
    + Auto-detects the start of the farm when farming for the first time
    + Option to manually set the waypoint with /shcropstartlocation
+ Added **price display next to sack items** (Contributed by HiZe)
    + Can be disabled
    + Sortable by price or items stored (both desc/asc)
    + Option to show prices from Bazaar or NPC
+ Added Farming Fortune Breakdown for Armor and Equipment (Contributed by CalMWolfs)
    + Run /ff to open the menu
    + Works with: Base Stats, Reforge Bonus, Ability Fortune and Green Thumb
    + Breakdown for the true farming fortune from each crop
    + Ability to select a single piece of armor or equipment
+ Added Server Restart Title
+ Added Jacob Contest Stats Summary
    + Showing Blocks per Second and total Blocks clicked after a farming contest in chat
+ Added City Project Features
    + Show missing items to contribute inside the inventory
        + Click on the item name to open the bazaar
    + Highlight a component in the inventory that can be contributed
+ Added `/pt <player>` alias for `/party transfer <player>`
    + SkyBlock Command `/tp` to check the play time still works
+ Added **Command Autocomplete**
    + Supports tab completing for warp points when typing /warp
    + Supports party members, friends (need to visit all friend list pages), player on the same server
    + Supports these commands: /p, /party, /pt (party transfer), /f, /friend /msg, /w, /tell, /boop, /visit, /invite,
      /ah, /pv (NEU's Profile Viewer), /shmarkplayer (SkyHanni's Mark Player feature)
    + Supports VIP /visit suggestions (e.g. PortalHub or Hubportal)
+ Added Item Profit Tracker (Slayer only)
    + Count items collected and how much you pay while doing slayer, calculates final profit
    + Shows the price of the item collected in chat (default disabled)
+ Added Items on Ground (Slayer only)
    + Show item name and price over items laying on ground (only in slayer areas)
+ Added Broken Hyperion Warning (Slayer only)
    + Warns when right-clicking with a Wither Impact weapon (e.g. Hyperion) no longer gains combat exp
    + Kill a mob with melee-hits to fix this hypixel bug
    + Only while doing slayer
+ Added Piece of Wizard Portal show earned by player name (Contributed by HiZe)
+ City Project Daily Reminder
    + Remind every 24 hours to participate
+ Added Quick Mod Menu Switching (default disabled)
    + Allows for fast navigation between one Mod Config and another
    + Default disabled
    + Detects your SkyBlock Mod automatically
    + Does detect Chat Triggers and OneConfig itself, but no single mods that require these libraries
+ Added **Arachne Chat Hider**
    + Hide chat messages about the Arachne Fight while outside of Arachne's Sanctuary
+ Added **Contest Time Needed**
    + Show the time and missing FF for every crop inside Jacob's Farming Contest inventory
+ Added **Sack Item Display** (Contributed by HiZe)
    + Added price display next to sack items
    + Can be disabled
    + Sortable by price or items stored (both desc/asc)
    + Option to show prices from Bazaar or NPC
+ Added profile id chat hider
+ Added Garden Crop Start Location
    + Show the start waypoint for your farm with the currently holding tool.
    + Auto-detects the start of the farm when farming for the first time
    + Option to manually set the waypoint with `/shcropstartlocation`
+ Added Pet Candies Used number
    + Works even after Hypixel removed the `10 pet candies applied` line
+ Added Estimated Armor Value display
    + Shows the price of all 4 armor pieces combined inside the wardrobe
+ Added Garden Plot Icon (Contributed by HiZe)
    + Select an item from the inventory to replace the icon in the Configure Plots inventory
    + Change the Edit mode in the bottom right corner in the Configure Plots inventory
+ Showing fished item names
+ Show numbers of Pocket Sack-In-A-Sack applied on a sack (Default disabled, contributed by HiZe)
+ Added a warning when finding a visitor with a rare reward
    + Show message in chat, over the visitor and prevents refusing
+ Added composter empty timer for outside garden
+ Added title warning when picking up an expensive slayer item
+ Added **RNG Meter Display**
    + Display number of bosses needed until the next RNG Meter item drops
    + Warn when no item is set in the RNG Meter
    + Hide the RNG Meter message from chat if the current item is selected
+ Added **Ghost Counter** (Contributed by HiZe)
    + Shows number of ghosts killed in the Mist in Dwarven Mines
    + Shows kill combo, coins per scavenger, all item drops, bestiarity, magic find and more
    + Each display line is highly customizable

### Changes

+ Added Options for displays Crop Milestone and Best Crop Time.
    + Change the time format/the highest time unit to show. (1h30m vs. 90 min)
    + Maxed Milestone:  Calculate the progress and ETA until maxed milestone (46) instead of the next milestone.
+ Changed 'Warp to Trapper Hotkey' to only work while in the Farming Islands
+ Changed Trevor Trapper find mods logic (Contributed by CalMWolfs)
    + Fixed Detection of Oasis Mobs and Horse
    + Improved detection speed
    + Derpy double health support
    + More fair detection logic
+ Added extra setting to allow/block clicks for the 'hide not clickable' feature
+ Disabled hide far particles feature in M7 boss fight. This will fix M7 boss fight features from other mods
+ Added support for multiple players/profiles:
    + SkyHanni saves all profile specific data now separately (e.g., garden data, crimson isle reputation progress,
      minion display text on the island)
    + Config toggles and GUI elements stay synced between all profiles
+ Reworked Hide Armor
    + More options
        + Fixed Movement bugs with Depth Strider enchantment
        + Fixed compatibility issues with other mods
        + Note: The hide armor config got reset, if you want to use it, you have to enable it again)
+ Added support for maxed out crop milestones
+ Showing total price for visitor items needed
+ Warning when BPS in Jacob contest ff needed display is below 1
+ More Discord Rich Presence features (Contributed by NetheriteMiner)
    + Option "Profile" not only shows the profile name but also the profile type and SkyBlock level
    + Option "Dynamic" now also supports stacking enchantments
+ Highlight the item in bazaar search result inventory when clicking on an item list (Contributed by CalMWolfs)
+ Resetting Ragnarock ability cooldown when being hit during channeling
+ Hide Non God Pot Effect Display while inside the Rift

### Fixes

+ Fixed typos in Trevor Trapper texts (Contributed by CalMWolfs)
+ Fixed Hypixel bug that the equipment lore talks about "kills" instead of "visitors" (Contributed by CalMWolfs)
+ Fixed reforge stone 'warped' not detected in item price display
+ Hotkey to open SkyHanni Position Editor no longer works inside signs
+ Fixed non god pot effects display being wrong sometimes
+ Fixed duplex not working in Bazaar features
+ Fixed crashes with repo errors
+ If you touch water, the King Scent form Non God Pot Timer will now be correctly removed

### Removals

- Removed Garden Recent Teleport Pads display

## Version 0.17 (2023-05-11)

### Features

+ Added **Time to Kill**
    + Show the time it takes to kill the Slayer boss.
+ Added skill and collection level as item stack.
+ Added **Auction Highlighter**
    + Highlight own items that are sold in green and that are expired in red.
+ Added support for tier 1 minions and title send for the minion craft helper.
+ Added Chicken head Timer.
+ Added **rancher boots** speed display.
+ Added **Unclaimed Rewards**
    + Highlight contests with unclaimed rewards in the jacob inventory.
+ Added **Duplicate Hider**
    + Hides duplicate farming contests in the inventory.
+ Added **Contest Time**
    + Adds the real time format to the farming contest description.
+ Added **Hide Repeated Catches** (contributed by appable)
    + Delete past catches of the same trophy fish from chat
+ Added **Trophy Counter Design** (contributed by appable)
    + Change the way trophy fish messages gets displayed in the chat
+ Added **CH Join**
    + Helps buy a Pass for accessing the Crystal Hollows if needed
+ Added **Estimated Item Value**
    + Displays an estimated item value for the item you hover over
+ Added Arachne to damage indicator.
+ Added **Arachne Minis Hider**
    + Hides the nametag above arachne minis
+ Added **Arachne Boss Highlighter**
    + Highlight the arachne boss in red and mini bosses and orange.
+ Added **Discord RPC** (contributed by NetheriteMiner)
    + Showing stats like Location, Purse, Bits, Purse or Held Item at Discord Rich Presence

### Garden Features

+ Added **Copper Price**
    + Show copper to coin prices inside the Sky Mart inventory.
+ Added **Visitor Display**
    + Show all items needed for the visitors.
+ Added **Visitor Highlight**
    + Highlight visitor when the required items are in the inventory or the visitor is new and needs to checked what
      items it needs
+ Added **Show Price**
    + Show the Bazaar price of the items required for the visitors.
+ Added **Crop Milestone** Number
    + Show the number of the crop milestone in the inventory.
+ Added **Crop Upgrades** Number
    + Show the number of upgrades in the crop upgrades inventory.
+ Added **Visitor Timer**
    + Timer when the next visitor will appear, and a number how many visitors are already waiting.
+ Added **Visitor Notification**
    + Show as title and in chat when a new visitor is visiting your island.
+ Added **Plot Price**
    + Show the price of the plot in coins when inside the Configure Plots inventory.
+ Added **Garden Crop Milestone Display**
    + Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. (Requires
      a tool with either a counter or cultivating enchantment)
+ Added **Best Crop Display**
    + Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden level or SkyBlock level.
+ Added **Copper Price**
    + Show the price for copper inside the visitor gui.
+ Added **Amount and Time**
    + Show the exact item amount and the remaining time when farmed manually. Especially useful for ironman.
+ Added **Custom Keybinds**
    + Use custom keybinds while having a farming tool or Daedalus Axe in the hand in the garden.
+ Added Desk shortcut in SkyBlock Menu.
+ Added **Garden Level Display**
    + Show the current garden level and progress to the next level.
+ Added **Farming Weight and Leaderboard**, provided by the Elite SkyBlock farmers.
+ Added farming weight next leaderboard position eta.
+ Added **Dicer Counter**
    + Count RNG drops for Melon Dicer and Pumpkin Dicer.
+ Added **Optimal Speed**
    + Show the optimal speed for your current tool in the hand. (Ty MelonKingDE for the values)
    + Also available to select directly in the rancher boots overlay (contributed by nea)
+ Added **Warn When Close**
    + Warn with title and sound when the next crop milestone upgrade happens in 5 seconds
    + Useful for switching to a different pet for leveling
+ Added **Money per Hour**
    + Displays the money per hour YOU get with YOUR crop/minute value when selling the items to bazaar.
+ Added farming contest timer.
+ Added wrong fungi cutter mode warning.
+ Added show the price per garden experience inside the visitor gui.
+ Added support for mushroom cow pet perk. (Counting and updating mushroom collection when breaking crops with mushroom
  blocks, added extra gui for time till crop milestones)
+ Added blocks/second display to crop milestone gui and made all crop milestone gui elements customizable/toggleable.
+ Added farming armor drops counter.
+ Added **Colored Name**
    + Show the visitor name in the color of the rarity.
+ Added **Visitor Item Preview**
    + Show the base type for the required items next to new visitors (Note that some visitors may require any crop)
+ Added **Teleport Pad Compact Name**
    + Hide the 'Warp to' and 'No Destination' texts over teleport pads.
+ Added **Money per Hour Advanced stats**
    + Show not only Sell Offer price but also Instant Sell price and NPC Sell price
    + Suggestion: Enable Compact Price as well for this
+ Added **Anita Medal Profit**
    + Helps to identify profitable items to buy at the Anita item shop and potential profit from selling the item at the
      auction house.
+ Added **Composter Compact Display**
    + Displays the compost data from the tab list in a compact form as gui element.
+ Added **Composter Upgrade Price**
    + Show the price for the composter upgrade in the lore
+ Added **Highlight Upgrade**
    + Highlight Upgrades that can be bought right now.
+ Added **Number Composter Upgrades**
    + Show the number of upgrades in the composter upgrades inventory.
+ Added **Composter Inventory Numbers**
    + Show the amount of Organic Matter, Fuel and Composts Available while inside the composter inventory.
+ Added **True Farming Fortune
    + Displays** current farming fortune, including crop-specific bonuses. (contributed by appable)
+ Added **Tooltip Tweaks Compact Descriptions**
    + Hides redundant parts of reforge descriptions, generic counter description, and Farmhand perk explanation. (
      contributed by appable)
+ Added **Tooltip Tweaks Breakdown Hotkey**
    + When the keybind is pressed, show a breakdown of all fortune sources on a tool. (contributed by appable)
+ Added **Tooltip Tweaks Tooltip Format**
    + Show crop-specific farming fortune in tooltip. (contributed by appable)
+ Added command **/shcropspeedmeter**
    + Helps calculate the real farming fortune with the formula crops broken per block.
+ Added **Compost Low Notification**
    + Shows a notification as title when organic matter/fuel is low.
+ Added **Jacob's Contest Warning**
    + Show a warning shortly before a new jacob contest starts.
+ Added **Inventory Numbers**
    + Show the number of the teleport pads inside the 'Change Destination' inventory as stack size.
+ Added **Composter Overlay**
    + Show the cheapest items for organic matter and fuel, show profit per compost/hour/day and time per compost
+ Added **Composter Upgrades Overlay**
    + Show an overview of all composter stats, including time till organic matter and fuel is empty when fully filled
      and show a preview how these stats change when hovering over an upgrade
+ Hide crop money display, crop milestone display and garden visitor list while inside anita show, SkyMart or the
  composter inventory
+ Hide chat messages from the visitors in the garden. (Except Beth and Spaceman)
+ Introduced a new command '/shcroptime <amount> <item>' that displays the estimated time it will take to gather the
  requested quantity of a particular item based on the current crop speed.
+ Show the average crop milestone in the crop milestone inventory.
+ Added **FF for Contest**
    + Show the minimum needed Farming Fortune for reaching a medal in the Jacob's Farming Contest inventory.
+ Added **yaw and pitch display**
    + Shows yaw and pitch with customizable precision while holding a farm tool.
    + Automatically fades out if no movement for a customizable duration (Contributed by Sefer)
+ Added warning when 6th visitors is ready (Contributed by CalMWolfs)

### Features from other Mods

> *The following features are only there because I want them when testing SkyHanni features without other mods present.*

+ Added Hide explosions.
+ Added **Enderman Teleportation Hider**
    + Stops the enderman teleportation animation (Like in SBA)
+ Added **Fire Overlay Hider**
    + Hide the fire overlay (Like in Skytils)

### Changes

+ Reworked reputation helper design in the crimson isle.
+ Moved the key setting for diana `warp to nearest burrow waypoint` from vanilla mc (esc -> config -> controls -> scroll
  all the way down to skyhanni category) to just `/sh diana`

### Fixed

+ Barbarian Duke Damage Indicator now only starts displaying after the player is getting close to him. (30 blocks)
+ Fixed a bug that caused fire veil particle `hider/redline drawer` to not always detect the right click correctly.
+ Removed `Fixing Skytils custom Damage Splash` (Skytils has already fixed this bug. Additionally, this option enabled
  and skytils' damage splash disabled caused the hypixel damage splash to not show the tailing commas at all)
+ Fixed bug with particles that blocks NotEnoughUpdates' Fishing features.
+ Hopefully fixed incompatibility with skytils `hide cheap coins` feature.
+ Fixed dungeon milestone messages getting wrongfully formatted.
+ Fixed bazaar features not working for items with an `-` (turbo farming books and triple strike)
+ Fixed Crab Hat of Celebration not being detected as an accessory correctly.
+ Added support for soopy's \[hand] feature

## Version 0.16 (2023-02-11)

## Features

+ Added highlight for stuff that is missing in the SkyBlock level guide inventory.
+ Added Bingo Card display.
+ **Minion Craft Helper** - Show how many more items you need to upgrade the minion in your inventory. Especially useful
  for bingo.
+ Hide dead entities - Similar to Skytil's feature for inside dungeon, but for everywhere.
+ Hide Fireball particles and hide Fire Block particles.
+ Made **blaze slayer clear view** work with more particles.
+ Added colors for the missing slayer area bosses (Golden Ghoul, Old Wolf and Spider Keeper)
+ Added Arachne keeper highlighter.
+ Added area boss highlighter.
+ Added area boss spawn timer.
+ Added Corleone highlighter.
+ Added **Tps Display** - Show the Tps of the current server.
+ Added Kuudra Key Number overlay.
+ Added colored highlight support for zealots, bruisers and special zealots.
+ Added more particle hiders. (Far particles, near redstone particles and smoke particles)

## Removals

- Removed Blaze slayer Pillar warning + timer (The Feature caused lags and Soopy's approach is better)

## Version 0.15.1 (2023-01-25)

## Features

+ Adding green line around items that are clickable. (Inside the **Not Clickable Items Feature**)
+ Added **Odger waypoint** - Show the Odger waypoint when trophy fishes are in the inventory and no lava rod in hand.
+ Added the option to hide the red numbers in the scoreboard sidebar on the right side of the screen.
+ Added **Tia Relay Waypoint** - Show the next Relay waypoint for Tia The Fairy, where maintenance for the abiphone
  network needs to be done.
+ Added **Tia Relay Helper** - Helps with solving the sound puzzle.

## Changes

+ Hide reputation boss waypoint when damage indicator is present.

## Version 0.15 (2023-01-22)

### Features

+ Added Bazaar Update Timer - Forrick.
+ Added Crimson Isle Reputation Helper.
+ Added **Barn Timer** - Show the time and amount of sea creatures while fishing on the barn via hub.
+ Added **Shark Fish Counter** - Counts how many sharks have been caught.
+ Added **Hide Silver Duplicates** - Hiding chat message when catching a duplicate silver trophy fish.

### Changes

+ **Damage Indicator** text size scale now changes dynamically.
+ Enderman slayer beacon now renders behind the damage indicator text, for better readability.

### Fixes

+ Fixed **Steaming Hot Flounder** in custom trophy fish counter.
+ Fixed hide powder gain message from chests in crystal hollows. (thanks hypixel)
+ Fixed damage indicator for M3 Professor bug. (Thanks hypixel)

## Version 0.14

### Features

+ Added /shtrackcollection <item> - This tracks the number of items you collect, but it does not work with sacks.
+ Added Compact Bingo Chat Messages.
+ Added Compact Potion Effect Chat Messages.
+ Added Brewing Stand Overlay.
+ Added minion name display with minion tier.

### Changes

+ Don't render overlays when tab list key is pressed.
+ Do no longer prevent the selling of bazaar items to NPC when on ironman, stranded or bingo mode.

### Fixes

+ Fixed wrong dungeon level colors. (in Party Finder)
+ Fixed error in Sea Creature Guide inventory.

## Version 0.13

### Features

+ Player Rank Hider works now with all messages.
+ Add two more chat filter categories: Powder Mining and Winter Gifts.
+ Add catacombs class level color to party finder.
+ Add wishing compass uses amount display.
+ Saves missing items from canceled buy orders to clipboard for faster re-entry.
+ Adds a visual highlight to the Croesus inventory that show what chests have not yet been opened.

### Removals

- Removed additional settings for the chat design, like channel prefix and SkyBlock level

### Fixes

+ Fixed void slayer mini bosses not being detected and colored.

## Version 0.12.2

### Changes

+ Add Bonzo's Mask highlight timer
+ Made the config button in the forge mod list work.
+ Blaze slayer pillar warning text is now much bigger.
+ Hides the new 'you earn x event exp' message.
+ Added Peek Chat feature.

### Fixes

+ Fixed a bug that caused the right blaze slayer dagger highlight to show wrong values.
+ Fixed kill combo message filter format

## Version 0.12.1

### Fixes

+ Fixed a crash when checking if the hotkey is pressed.

## Version 0.12

### Diana

+ Show burrows near you.
+ Uses Soopy's Guess Logic to find the next burrow. Does not require SoopyV2 or chat triggers to be installed.
+ Let Soopy guess the next burrow location for you.
+ Warps to the nearest warp point on the hub, if closer to the next burrow.

### Crimson Isle

+ Option to remove the wrong blaze slayer dagger messages from chat.
+ Hide particles and fireballs near blaze slayer bosses and demons.
+ Added Option to hide blaze particles.
+ Highlight millenia aged blaze color in red

### Changes

+ Removed more blaze slayer item drop message spam.
+ Showing number next to custom hellion shield from damage indicator.
+ All particles next to ashfang are now hidden when clear ashfang view is enabled.
+ All particles around blaze slayer are hidden when blaze particles is enabled.
+ Hide blocks circling around ashfang.
+ Hide the vanilla nametag of damage indicator bosses.

## Version 0.11

### Slayer

+ Added toggle to ender slayer phase display
+ Added blaze slayer phase display
+ Added toggle for blaze slayer colored mobs
+ Mark the right dagger to use for blaze slayer in the dagger overlay.
+ Added option to select the first, left sided dagger for the display.

### Stats Tuning

+ Show the tuning stats in the Thaumaturgy inventory.
+ Show the amount of selected tuning points in the stats tuning inventory.
+ Highlight the selected template in the stats tuning inventory.
+ Show the type of stats for the tuning point templates.

### Misc

+ Added Lord Jawbus to damage indicator
+ Display the active non-god potion effects.

### Changes

+ Showing Thunder Sparks while in lava
+ Damage indicator switches into cleaner design when close to boss and not in f5 view

### Fixes

+ Reduced lags after world switch

## Version 0.10 - Slayer

### Blaze Slayer

+ Added a cooldown when the Fire Pillars from the Blaze Slayer will kill you.
+ Added a faster and permanent display for the Blaze Slayer daggers.
+ Added custom countdown sound for the Fire Pillar timer for the Blaze Slayer.
+ Added hide sound and entities when building the Fire Pillar for the Blaze Slayer.
+ Added warning when the fire pit phase starts for the Blaze Slayer tier 3.

### Slayer

+ Added warning when wrong slayer quest is selected, or killing mobs for the wrong slayer.
+ Added hide more poor slayer drop chat messages.

### Misc

+ Added option to hide armor or just helmet of other player or yourself.

### Fixes

+ Fixed overload damage gets not detected as damage splash.

## Version 0.9 - Chat + Dungeon

### Chat

+ Added option to enable that clicking on a player name in chat opens the profile viewer of NotEnoughUpdates (to fix
  SkyHanni breaking the default NEU feature).
+ Added support for new SBA chat icon feature (show profile type and faction in chat)
+ Highlight marked player names in chat.
+ Scan messages sent by players in all-chat for blacklisted words and greys out the message
+ Links in player chat are clickable again

### Dungeon

+ Added hide the damage, ability damage and defense orbs that spawn when the healer is killing mobs in dungeon
+ Added hide the golden fairy that follows the healer in dungeon.
+ Added hidden music for the clean end of the dungeon

### Misc

+ Added hide the name of the mobs you need to kill in order for the Slayer boss to spawn. Exclude mobs that are damaged,
  corrupted, runic or semi rare.
+ Added option to hide all damage splashes, from anywhere in SkyBlock.
+ Added highlight Thunder Sparks after killing a Thunder
+ Added Thunder to damage indicator
+ Mark the own player name (for Chat messages)

### Fixed

- Fixed vanilla items not being sellable to npc

## Version 0.8

### Misc

- Added hiding the flame particles when using the Fire Veil Wand ability
- Added circle around the player when having the Fire Veil Wand ability is active
- The config GUI how has a search function (top right corner)
- Added more player chat format options (show all channel prefix, hide player rank, hide colon after player name,
  hide/change elite position format, channel prefix design)

### Small Changes

- Changed the ashfang gravity orb highlight from square to cylinder
- Added msg support to fancy chat format
- Added damage indicator for diana mobs

### Fixes

- Fixed enchanted books in bazaar
- Fixed LorenzLogger printing output into console
- Fixed best sell method not working for some bazaar items
- Fixed summoning mobs display don't track mobs when moving too fast while spawning summoning mobs

## Version 0.7 - Dungeon and Colored Highlight

### New Dungeon Features

- Added highlight deathmites in dungeon in red color
- Added hide Superboom TNT lying around in dungeon
- Added hide Blessings lying around in dungeon
- Added hide Revive Stones lying around in dungeon
- Added hide Premium Flesh lying around in dungeon
- Added Dungeon Copilot (Suggests to you what to do next in dungeon)
- Added separate option to hide dungeon key pickup and door open messages
- Added hide Journal Entry pages lying around in dungeon.
- Added hide Skeleton Skulls lying around in dungeon.
- Added highlight Skeleton Skulls in dungeon when combining into a skeleton in orange color (not useful combined with
  feature Hide Skeleton Skull)

### Other Misc Features

- Added option to hide the SkyBlock Level from the chat messages (alpha only atm)
- Added option to change the way the SkyBlock Level gets displayed in the chat (only working when SkyBlock level and
  fancy player message format are enabled)
- Added highlight the voidling extremist in pink color
- Added highlight corrupted mobs in purple color
- Added command /shmarkplayer <player> (marking a player with yellow color)
- Added highlight slayer miniboss in blue color
- Added option to hide the death messages of other players, except for players who are close to the player, inside
  dungeon or during a Kuudra fight.
- Added highlight the enderman slayer Yang Glyph (Beacon) in red color (supports beacon in hand and beacon flying)

### Fixes

- Fixed message filter for small bazaar messages

## Version 0.6 - Ashfang and Summoning Mobs

### New Features

- Added /wiki command (using hypixel-skyblock.fandom.com instead of Hypixel wiki)
- Added hiding damage splashes while inside the boss room (replacing a broken feature from Skytils)
- Added Summoning Mob Display (Show the health of your spawned summoning mobs listed in an extra GUI element and hiding
  the corresponding spawning/despawning chat messages)
- Added option to hide the nametag of your spawned summoning mobs
- Added option to mark the own summoning mobs in green
- Added Ashfang Blazing Souls display
- Added highlight for the different ashfang blazes in their respective color
- Added option to hide all the particles around the ashfang boss
- Added option to hide the name of full health blazes around ashfang (only useful when highlight blazes is enabled)
- Added option to hide damage splashes around ashfang

### Minor Changes

- Optimizing the highlight block size for minions, blazing souls and gravity orbs
- Added option to change the gray-out opacity for 'Not Clickable Items'
- Added option to show the health of Voidgloom Seraph 4 during the laser phase (useful when trying to phase skip)
- Fixed that items with stars don't gray out properly when hidden by the 'hide not clickable' feature
- Fixed 'hiding the nametag of mobs close to minions' not working when minion nearby was never collected

## Version 0.5 - Minions and RNG Meter

### New Features

- Added a display that show the last time the hopper inside a minion has been emptied
- Added a marker to the last opened minion for a couple of seconds (Seen through walls)
- Added option to hide mob nametags close to minions
- Added showing stars on all items (Not only dungeon stars and master stars but also on crimson armors, cloaks and
  fishing rods)
- Added **Real Time** - Display the current computer time, a handy feature when playing in full-screen mode.
- Added overlay features to the RNG meter inventory (Highlight selected drop and floors without a drop and show floor)
- Added minion hopper coins per day display (Using the held coins in the hopper and the last time the hopper was
  collected to calculate the coins a hopper collects in a day)

### Minor Changes

- Summoning souls display is rendering better close to corners
- Ashfang gravity orbs are now rendering better close to corners
- Showing the name of ashfang gravity orbs as a nametag above the orb
- Bazaar now knows books and essences (Thanks again, Hypixel)

### Bug Fixes

- Fixed damage Indicator damage over time display order swapping sometimes

## Version 0.4.2 - Repair what Hypixel broke

### New Features

- Added grabbing the API key from other mods. First time using SkyHanni should not require you to set the API key
  manually (Thanks efefury)

### Fixes

- Fixing ender slayer health for damage indicator (Hypixel broke it)
- Fixing format to hide all blessing messages in dungeon again (Hypixel broke it)
- Fixing 'damage splash hiding' when near damage indicator (Hypixel broke it)
- Fixed Skytils custom damage splash (Hypixel broke it)

## Hotfix 0.4.1 - Removing red bazaar errors

- This is no support for the new enchanted books in the bazaar. It’s just removing the red error messages from the chat.

## Version 0.4

### Damage Indicator stuff

- Added damage indicator healing chat messages
- Added damage indicator showing boss name (not working in dungeon yet)
- Added damage indicator option to hide or only show short name
- Added option to enable/disable damage indicator for specific bosses
- Added enderman slayer hits display to damage indicator
- Added that damage indicator is showing when the boss is dead
- Added enderman slayer laser phase cooldown to damage indicator
- Added all slayers to damage indicator (except blaze above tier 1)
- Added revenant slayer 5 boom display to damage indicator
- Fixed damage indicator f4 and m4 thorn support for Derpy
- Added option to hide vanilla (or Skytils) damage splashes next to damage indicator
- Added damage/healing over time display to damage indicator
- Added Training Dummy (on personal island) to damage indicator

### Other stuff

+ Added ashfang reset cooldown
+ Added fire veil wand ability cooldown
+ Added custom player chat for dead players in dungeon and for visiting players on own islands
+ Added ashfang gravity orbs display

## Version 0.3

- Added damage indicator for magma boss and headless horseman
- Added summoning souls display
- Added Derpy support for damage indicator

## Version 0.1

- Added damage indicator for some bosses who are outside dungeon (4 nether bosses: Ashfang, barbarian duke, mage outlaw
  and Bladesoul, slayers: Enderman 1-4, revenant 5, and untested support for vanquisher in nether, Enderdragon and
  Endstone protector in end)
- Added item ability cooldown background display (over the slot, work in progress)
- Added Ashfang freeze cooldown (when you get hit by “anti ability” and slowness effect)
- Changed “hot clickable items” to show items again, but only with dark gray overlay. Looks nicer
- Made the GitHub repository public
