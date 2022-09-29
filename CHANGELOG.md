# SkyHanni - Change Log

## Version 0.10.1

### Slayer
+ Added toggle to ender slayer phase display
+ Added blaze slayer phase display
+ Added toggle for blaze slayer colored mobs
+ Mark the right dagger to use for blaze slayer in the dagger overlay.

### Stats Tuning
+ Show the tuning stats in the Thaumaturgy inventory.
+ Show the amount of selected tuning points in the stats tuning inventory.
+ Highlight the selected template in the stats tuning inventory.
+ Show the type of stats for the tuning point templates.

### Misc
+ Added Lord Jawbus to damage indicator

### Changes
+ Showing Thunder Sparks while in lava

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
+ Added option to enable that clicking on a player name in chat opens the profile viewer of NotEnoughUpdates (to fix SkyHanni breaking the default NEU feature).
+ Added support for new SBA chat icon feature (show profile type and faction in chat)
+ Highlight marked player names in chat.
+ Scan messages sent by players in all-chat for blacklisted words and greys out the message
+ Links in player chat are clickable again

### Dungeon
+ Added hide the damage, ability damage and defence orbs that spawn when the healer is killing mobs in dungeon
+ Added hide the golden fairy that follows the healer in dungeon.
+ Added hidden music for the clean end of the dungeon


### Misc
+ Added hide the name of the mobs you need to kill in order for the Slayer boss to spawn. Exclude mobs that are damaged, corrupted, runic or semi rare.
+ Added option to hide all damage splashes, from anywhere in Skyblock.
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
- Added more player chat format options (show all channel prefix, hide player rank, hide colon after player name, hide/change elite position format, channel prefix design)

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
- Added highlight Skeleton Skulls in dungeon when combining into a skeleton in orange color (not useful combined with feature Hide Skeleton Skull)


### Other Misc Features
- Added option to hide the Skyblock Level from the chat messages (alpha only atm)
- Added option to change the way the Skyblock Level gets displayed in the chat (only working when skyblock level and fancy player message format are enabled)
- Added highlight the voidling extremist in pink color
- Added highlight corrupted mobs in purple color
- Added command /shmarkplayer <player> (marking a player with yellow color)
- Added highlight slayer miniboss in blue color
- Added option to hide the death messages of other players, except for players who are close to the player, inside dungeon or during a Kuudra fight.
- Added highlight the enderman slayer Yang Glyph (Beacon) in red color (supports beacon in hand and beacon flying)

### Fixes
- Fixed message filter for small bazaar messages

## Version 0.6 - Ashfang and Summoning Mobs

### New Features
- Added /wiki command (using hypixel-skyblock.fandom.com instead of Hypixel wiki)
- Added hiding damage splashes while inside the boss room (replacing a broken feature from Skytils)
- Added Summoning Mob Display (Show the health of your spawned summoning mobs listed in an extra GUI element and hiding the corresponding spawning/despawning chat messages) 
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
- Added a display that shows the last time the hopper inside a minion has been emptied
- Added a marker to the last opened minion for a couple of seconds (Seen through walls)
- Added option to hide mob nametags close to minions
- Added showing stars on all items (Not only dungeon stars and master stars but also on crimson armors, cloaks and fishing rods)
- Added a display timer that shows the real time
- Added overlay features to the RNG meter inventory (Highlight selected drop and floors without a drop and show floor)
- Added minion hopper coins per day display (Using the held coins in the hopper and the last time the hopper was collected to calculate the coins a hopper collects in a day)

### Minor Changes
- Summoning souls display is rendering better close to corners
- Ashfang gravity orbs are now rendering better close to corners
- Showing the name of ashfang gravity orbs as a nametag above the orb
- Bazaar now knows books and essences (Thanks again, Hypixel)

### Bug Fixes
- Fixed damage Indicator damage over time display order swapping sometimes

## Version 0.4.2 - Repair what Hypixel broke

### New Features
- Added grabbing the API key from other mods. First time using SkyHanni should not require you to set the API key manually (Thanks efefury)

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
- Added training dummy (on personal island) to damage indicator

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
- Added damage indicator for some bosses who are outside dungeon (4 nether bosses: Ashfang, barbarian duke, mage outlaw and Bladesoul, slayers: Enderman 1-4, revenant 5, and untested support for vanquisher in nether, Enderdragon and Endstone protector in end)
- Added item ability cooldown background display (over the whole slot, work in progress)
- Added Ashfang freeze cooldown (when you get hit by “anti ability” and slowness effect)
- Changed “hot clickable items” to show items again, but only with dark gray overlay. Looks nicer
- Made the GitHub repository public