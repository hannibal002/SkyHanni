# Graph Network and Graph Editor of SkyHanni

## The Network

The **Graph Network** is a list of nodes (waypoints) and edges (lines between nodes) that represent individual islands.
A powerful **Graph Editor** exists in SkyHanni that allows editing these networks.
The network data is used for two distinct use cases:

- **Navigating** around an island (find the shortest path from where you are right now to some other spot on the island)
- Detecting the **Current Area** where you are (like the area names from the scoreboard, just more precise and updating instantly instead of
  delayed)

A lot of features in SkyHanni are powered by those two functionalities.

The graph network is stored in JSON in the [SkyHanni Repo](https://github.com/hannibal002/SkyHanni-REPO/tree/main/constants/island_graphs),
one file per island.

The Hypixel Lobby is currently the only map outside SkyBlock that is also using the graph network.

### Areas

We use the node tags **Area** and **Small Area** to tell other SkyHanni features what area the user is at.
That works by first finding the next closest node to the player.
Then we check what area the node is at.
We use the name `no_area` on nodes that are outside any specific area (the main area that spans over the whole island).
Some islands do not have any area at all. On those islands, the API will return `no_area`.

#### Current Area

Features that only work on a specific area on an island rely on the network to detect where the user is.

A non-exhaustive list of features:

- The **Area Navigation** feature.
    - A list that shows all areas on the current world, sorted by distance.
    - Click on it to navigate to that area.
- Show area border in world.
- Send a title when entering a new area.

#### Small Areas

Small areas are for things like a building or a small cave, like the bank in the Hub or Trapper's Den in The Farming Islands.
(Large) normal areas are the big areas on the map, like the graveyard in the hub or Stronghold in Crimson Isle.
The distinction between an area and a small area allows for visual differentiation in the **Area Navigation** feature and in
`/shnavigate`, and for the option to include or exclude small areas in the **Area Navigation** list.

#### What area is the node at?

We do not need to give every node an area tag, that would be impractical.
Instead, we follow the graph chain from one node to its neighbors until we find an area name tag.
The name of that node then defines what area the player stands at.
The next area needs to start at the other side of the "imaginary area boundary" and be connected to the first area.

The **Error Finder** will warn you when nodes connect to multiple areas at the same time without
a boundary (conflicting areas).

### Navigating

We use Dijkstra and A* to help the user find the shortest path to where they want to go.

A non-exhaustive list of features:

- **Fast Fairy Soul**
- **Hoppity Eggs**
- **Fishing Hotspot Radar**
- **Diana Burrows** (soon)
- `/shnavigate <target>`
    - This allows you to navigate to NPCs, Areas, Points of Interest, Mob-Spawn Areas, Ore Veins, Crop Farms.
- `/shnavigateall <target type>`
    - This allows you to navigate in a circle to all waypoints of a given category, e.g., fairy souls, hoppity eggs, fishing
      hotspots/wormholes.
    - On Torrhus Canyon: hideonsun, honeyhive, pangolin, sanger, tree protection order.

## Graph Editor

To open up the Graph Editor run the command `/shgraph`. Run the command again to exit the Graph Editor at any time (or press the
default key `Home`).
To load the Graph Editor with the existing repo data of the current island, run `/shgraphloadthisisland`.

Press the **Vision Key** (by default `M`) to toggle visibility of nodes and edges behind blocks.

Once the graph is loaded, you see two GUIs:

- **List of Named Nodes**
    - Contains the full list of all nodes on the graph that have a name.
    - Is used to add or remove tags to the node.
    - Useful to navigate to any given node quickly.
- A keybind list, showing you what you can do at any given time by pressing the keys.

### Basic Editing

The three basic actions you can do are:

- Adding and deleting new nodes.
- Adding and deleting an edge between two nodes.
- Moving a node to another place.

The Graph Editor uses colors to explain what node is selected and what node is closest to you:

- The closest node to you is yellow.
- The selected node is green.

#### Select Nearest Node

This keybind selects or deselects the next closest node to you.
Only one node can be selected at the same time.

By default, the keybind for **Select Nearest Node** is `Left Mouse Button`.

#### Adding and Deleting Nodes

When you press the **Place Key** (by default `F`), you create a new node at the location where you stand.
But if you have a node selected, this behavior is altered:

If you are close to the selected node, you delete the selected node by pressing the **Place Key**.
If you are more than 3 blocks away from the selected node, two other things will happen:

- A new node at the current location gets added (like if you have no node selected).
- A new edge (connection) between the selected node and the newly created node gets added.

If you press the **Select Key** after creating a new node, you can move around and just keep pressing the add and select keys to quickly
create a new line of nodes in the world.

#### Adding and Deleting Edges

By default, when you press `C`, you create a connection between the selected node (green) and the next closest node (yellow).
When you press `C` again, the edge gets deleted.
The normal edge color is blue, the edge between the selected and the nearest node is red.

#### Moving a Node

Instead of deleting and adding a new node, you can move the selected node around in the world.
While holding the **Node Move Key** (by default `Tab`), move the node by one block in each direction.
Use your Minecraft keybind to move the selected node:

- Use `W`-`A`-`S`-`D` to move the node forward, backwards or sideways, relative to the direction you are looking at.
- Use the **Sneak** and **Jump** keys to move the node up or down.

#### Saving

The **Save Key** (by default `O`) allows you to export the current graph.
It will compile the graph into JSON format, and put the raw text of the JSON into your clipboard.
Then you can put that text into e.g., your fork of the REPO file to include in a PR.

Additionally, while saving, these three things happen as well:

- The **Error Finder** gets activated.
- The Graph Network gets used on the current island (optionally, toggleable)
    - Sets the current Graph Editor data as the Graph Network used on the current island.
- It shows stats in chat.

### Named Nodes and Tags

To find targets on the graph and let the mod features programmatically connect to the nodes on the graph, we can give a node a name and a
tag.

A node with a name always needs a tag, a node with a tag always needs a name.
The vast majority of nodes don't have either.

#### Renaming a Node

By default, the keybind `Y` is used to go into the text mode.
The text mode needs you to select a node first.

In the text mode, you can use your keyboard to write down a name for the node.
You see the text on the node in the world in real time while you write.
Press `Esc` to exit the text mode.

You can use `Control` + `V` to paste your clipboard as text while in the text mode.
The new string does not append to the text, but rather replaces the whole old text.
This can become useful if you want to add a lot of nodes with the same name.
Or if other mods or vanilla Minecraft keybind mappings activate while you type in the text mode.

#### Tagging a Node

Once the node has a name, it shows up in the **List of Named Nodes**.
In this list, you see the red "no tag" suffix next to the newly renamed node.

To give a **Named Node** a tag:

- Open the inventory to hover over the **List of Named Nodes** (move or resize the GUI so you can see it clearly while your inventory is
  open).
- Control-click on the node you want to edit the tags of.
- Then a list of all available tags for the given island shows up.
    - There are some tags that are only available on specific islands.
    - As a convenience, only the ones that may apply on the current island show up here.
- Select or deselect whatever tag you want the node to have.
- The tag name and color show up below the node in the world.
- Close the tag list by clicking on the "Go Back!" text at the bottom of the list. If this does not show up, scroll down.

The tag does not get automatically removed when you remove the name.
The **Error Finder** will flag "Named Nodes" without a tag, and "Tagged Nodes" without a name.

### Advanced Editing Tools

Apart from adding, deleting and moving nodes and edges, the Graph Editor has a number of advanced editing tools.

#### Undo and Redo

Every action you do in the Graph Editor (adding and removing nodes or edges, renaming nodes, adding and removing tags, node weight changes,
direction changes of edges), even big actions (loading, unloading the whole graph) can be undone via `Control` + `Z`. Also supports
Redo via `Control` + `Y`.

#### Select Node by Looking

You want to select a node that is further away, and it's time-consuming to walk up to the node to select it?
Walk no more! You can set a key for the **Select looking at Node** keybind.
Then, all you need to do is to look at the node you want to select, and press the keybind!

#### Split

Use the **Split Key** to cut the currently red marked edge (between the selected node and the closest node) in half.
That will create a new node in the middle of the edge, remove the old connection between the two old nodes, and add two new connections,
from the new node to the two old nodes.

The **Split Key** is not set by default.

#### Dissolve

Use the **Dissolve Key** to delete a node that has exactly two connected nodes. The two remaining nodes then get a connection between them.

The **Dissolve Key** is not set by default.

#### One Directional

Use the **One Directional Key** (by default `H`) to cycle through the direction of the currently red marked edge (between the selected node
and the closest node).

The three directions to cycle through:

- Both (default for all edges)
- A→B
- B→A

This is useful to mark paths the user can only move in one direction. E.g., drop-down paths, or jumping pad movements.

#### Weighted Nodes

Use the command `/shgraphweight` to set the weight of the selected node.
By default, every node has the weight of 0.

The weight gets added on top of the length of the path when Dijkstra or A* calculates a path through the node.
This impacts all directions the node can pass through in the same way.

When to use? When the path slows down the user considerably, e.g., moving through water or climbing up blocks without stairs. Especially
useful when longer but faster to move through alternate paths exist.

#### Load a Network

The normal way to edit existing networks is using the `/shgraphloadthisisland` command.
But sometimes you want to edit other graphs (your own version of something or to check out an open PR).
For that, we have the explicit **Load Key** (by default `I`).

When you press it, the current clipboard gets read as JSON and parsed into the Graph Editor.

#### Clear the Network

When you want to start fresh, or just disregard the current network, you can press the **Clear Key** (no default keybind) to wipe the whole
network from the Graph Editor (the graph network loaded to the island is unaffected).

As a safety measure (not to lose progress), the current state of the network gets stored as JSON in your clipboard (but no additional save
actions are done like normally when pressing the **Save Key**).

Also, the **Undo Key** works like normal for this.

#### Disabled Nodes

The command `/shgraphtoggledisabled` toggles the visibility of **Disabled Nodes**.

Edges that touch a disabled node are also hidden if the disabled node is hidden.

A disabled node will not count in features, e.g.:

- `/shnavigate` will not use disabled nodes.
- Pathfinding will not navigate through a disabled node.

Why does this exist?

- For NPCs or structures that are conditionally existing (when the Hub Island changes for seasons, or Hub NPCs that show up only for special
  events)
- For Rescue Missions in the Crimson Isle.
- For dodging the Temporal Pillar in The Rift.
- For more examples, see `IslandGraphs.disableNodes` in the code base.

Area detection is also impacted by this.

### Debug Tools

There are some tools that aren't to edit the graph but rather to test or debug the network or to deal with merging conflicts.

#### Navigate to Selected Node

Press the **Navigate to Node** keybind (by default `G`) to start the navigation pathfinding logic, targeting the selected node.
This is useful to test how the current network would behave, especially after changed node weights or use of the **One Directional Key**.
Since the navigation feature relies on the graph network data and not the Graph Editor directly, make sure to apply the Graph Editor to the
current island by using the **Save Key**.

#### Error Finder

The **Error Finder** analyzes the graph network as a whole and flags issues with nodes.
It runs by default when saving the graph.

#### Find All

The command `/shgraphfindall` navigates you through all nodes of the network.
Useful to find errors in the graph data.

### Misc Tools

Additional features of the Graph Editor.

#### Feedback Mode

The **Feedback Key** (by default `K`) toggles the Feedback Mode.

While the feedback mode is enabled, every action you do in the Graph Editor gets commented in the console. Useful for beginners.

#### Disjoined Networks

All nodes that connect are one network. When the connection breaks (or a new node gets added that is not connected to the network)
we have multiple networks.
This is not a stable state, we want to fix this.
Therefore, the **Error Finder** will flag such cases when saving.

There are also specific commands that come in handy:

- `/shgraphfindnetwork` lists all separate networks in the Graph Editor.
    - Clicking on a network then navigates you to it.
- `/shgraphmerge` merges graph data from the clipboard into the current Graph Network.
    - The nodes don't connect, they just exist in one Graph Editor.

#### Parkour

A parkour is a line of waypoints that are all connected to the previous and next waypoint.
Many different SkyHanni features use this identical parkour format:

- Dungeon Race Guide
- Rescue Mission in Crimson Isle
- Lava Maze in The Rift.
- See `ParkourHelper` for more examples.

The Graph Editor can be used to edit such waypoints:

- The command `/shgraphloadparkour` loads the current clipboard as a parkour into the Graph Editor.
- The command `/shgraphexportasparkour` saves the Graph Editor as a parkour into the clipboard.
