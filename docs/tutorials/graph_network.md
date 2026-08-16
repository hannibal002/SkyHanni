# Graph Network and Graph Editor of SkyHanni

## The Network

The **Graph Network** is a list of nodes (waypoints) and edges (lines between nodes) that represent individual islands.
A powerful **Graph Editor** exists in SkyHanni that allows editing these networks.
Two distinct use cases rely on the network data:

- **Navigating** around an island (find the shortest path from the current location to some other spot on the island)
- Detecting the **Current Area** where you are (like the area names from the scoreboard, just more precise and updating instantly instead of
  delayed)

These two functionalities power many SkyHanni features.

The graph network is stored as JSON in the [SkyHanni Repo](https://github.com/hannibal002/SkyHanni-REPO/tree/main/constants/island_graphs),
one file per island.

The Hypixel Lobby is currently the only map outside SkyBlock that is also using the graph network.

### Why a Graph?

A plain list of waypoints can only describe one route in one fixed order. As soon as the player stands somewhere else, or could take a
shortcut, there is nothing left to calculate. A graph stores how places are connected instead of one finished route, so a path can be
built from wherever the player currently is to wherever they want to go.

The second reason is that not every connection is a straight walk. Warps, jump pads and teleport pads link two spots that are nowhere near
each other. Some paths only work in one direction. Some paths are slower than their length suggests. None of that can be read out of the
shape of the world, it has to be recorded by hand, and a graph holds all of it in one place.

That handmade part is also why the Graph Editor works the way it does: you walk the island and drop nodes as you go, and the network is
the result of that walk.

### Areas

The node tags **Area** and **Small Area** define the current area for other SkyHanni features.
The feature first finds the closest node to the player. Next, the graph network is traversed starting from this node until an area tag is
found.

`no_area` is a reserved name. You place such a node like any other area node, with the **Area** tag and that exact name, but the name itself
is recognized by the code: a spot that resolves to `no_area` counts as belonging to no named area, and features hide it instead of offering
it as a destination. Use it to mark the stretches of an island that deliberately have no name of their own. When the traversal ends on such
a node, the player counts as being in `no_area`.

Some islands have no area nodes at all. There the API always returns `no_area`, so mod features that strictly require a defined area do not
work on those islands.

#### Current Area

Features that only work on a specific area on an island rely on the network to detect where the user is.

A non-exhaustive list of features:

- The **Area Navigation** feature.
    - A list that shows all areas in the current world, sorted by distance.
    - Click on it to navigate to that area.
- Show the area border in the world.
- Send a title when entering a new area.

#### Small Areas

Small areas are for things like a building or a small cave, like the bank in the Hub or Trapper's Den in The Farming Islands.
(Large) normal areas are the big areas on the map, like the graveyard in the hub or the Stronghold in the Crimson Isle.
The distinction between an area and a small area allows for visual differentiation in the **Area Navigation** feature and in
`/shnavigate`, and for the option to include or exclude small areas in the **Area Navigation** list.

The two are not nested. A small area is not "inside" a large one, they are two flavors of the same thing, and the tag only controls the
color and whether the entry can be hidden. Whichever area node is closest wins, no matter which of the two tags it carries.

There is no real hierarchy because nothing needs one. Everything that asks for the current area expects a single answer, and nesting would
mean recording in every graph file which area belongs to which parent. The trade-off is that walking into a house inside a village
replaces the village instead of adding to it.

#### What area is the node in?

Giving every node an area tag is impractical.
Instead, we follow the graph chain from one node to its neighbors until we find an area name tag.
The name of that node then defines what area the player stands at.
The next area needs to start on the other side of the "imaginary area boundary" and be connected to the first area.
The boundary lies between two nodes that have different area tags.

This solves three things at once:

- Only the area nodes themselves need maintenance. Every other node inherits its area, so renaming or moving an area is one edit instead
  of hundreds.
- Walls are handled for free. Two spots on opposite sides of a wall can be one block apart, but walking between them takes a long way
  around, so they end up in different areas without anyone describing that wall.
- The same search that finds your current area also produces the distance to every other area, which is what the **Area Navigation** list
  shows.

The price is that borders come from the network, not from a shape someone drew. Where two connected nodes fall into different areas, one of
them has to carry the area tag, otherwise the border ends up at a random spot.

The **Error Finder** will warn you when nodes connect to multiple areas at the same time without
a boundary (conflicting areas).

### Navigating

The [Dijkstra](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) algorithm calculates the shortest path to navigate the user.

Features that visit many locations in one go, like **Fast Fairy Soul** or `/shnavigateall`, also need a good order to visit them in. That
is the [Traveling Salesman Problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem), solved with a
[nearest neighbor](https://en.wikipedia.org/wiki/Nearest_neighbour_algorithm) route improved by
[2-opt](https://en.wikipedia.org/wiki/2-opt). Each leg of that route is a normal Dijkstra path again.

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
    - Some categories only exist on some islands, for example Honeyhive and Pangolin on Torrhus Canyon, or Safari Bell in the Safari.
      `GraphNodeTag` in the code holds the full list.

## Graph Editor

To enable the Graph Editor, run the command `/shgraph`.
To exit the Graph Editor, run the command again or press the **Exit Key** (by default `Enter`).
To load the Graph Editor with the existing [Repo data](https://github.com/hannibal002/SkyHanni-REPO/tree/main/constants/island_graphs) of
the current island, run `/shgraphloadthisisland`.

Press the **Vision Key** (by default `M`) to toggle visibility of nodes and edges behind blocks.

Nodes further away than the **Max Node Distance** option (50 blocks by default) are not rendered at all. Raise it to see more of the
network at once.

Once the graph is loaded, you see two GUIs:

- **List of Named Nodes**
    - Contains the full list of all nodes on the graph that have a name.
    - Is used to add or remove tags to the node.
    - Open the inventory to interact with it.
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

If you are within 3 blocks of the selected node, you delete the selected node by pressing the **Place Key**.
If you are more than 3 blocks away, two other things will happen:

- A new node at the current location gets added (like if you have no node selected).
- A new edge (connection) between the selected node and the newly created node gets added.

Press the **Select Nearest Node** key after creating a new node, then move around and keep pressing the Place and Select Keys to quickly
create a new line of nodes in the world.

Enable the **Auto Select Node** option in the config to skip the select step entirely. Every node you place then becomes the active node
right away, so laying down a path is just the Place Key over and over.

#### Adding and Deleting Edges

When pressing the **Connect Key** (by default `C`), a connection between the selected node (green) and the next closest node (yellow) gets
created.
Press **Connect Key** again to delete the edge.
The normal edge color is gold, the edge between the selected and the nearest node is red.

#### Moving a Node

Instead of deleting and adding a new node, you can move the selected node around in the world.
While holding the **Node Move Key** (by default `Tab`), move the node by one block in each direction.
Use your Minecraft keybind to move the selected node:

- Use `W`-`A`-`S`-`D` to move the node forward, backwards or sideways, relative to the direction you are looking at.
- Use the **Sneak** and **Jump** keys to move the node up or down.

#### Saving

The **Save Key** (by default `O`) allows you to export the current graph.
It will compile the graph into JSON format, and put the raw text of the JSON into your clipboard.
Then you can put that text into e.g., your fork of
the [Repo file](https://github.com/hannibal002/SkyHanni-REPO/tree/main/constants/island_graphs) to include in a PR.

Additionally, while saving, these three things happen as well:

- The current graph is applied as the island’s active graph network (optional, toggleable). This serves as the primary way to test changes
  locally right away.
- The **Error Finder** gets activated. (only if the graph gets used as active network)
- It shows stats in chat.

### Named Nodes and Tags

Assigning a name and a tag to a node allows mod features to find and connect to it programmatically.

A node with a name always needs a tag, and a node with a tag always needs a name.
The vast majority of nodes don't have either.

#### Renaming a Node

The **Text Key** (by default `Y`) is used to go into the text mode.
The text mode needs you to select a node first.

In the text mode, you can use your keyboard to write down a name for the node.
You see the text on the node in the world in real time while you write.
Press **Exit Key** (by default `Enter`) to exit the text mode.

You can use `Control` + `V` to paste your clipboard as text while in the text mode.
The new string does not append to the text, but rather replaces the whole old text.

This can become useful if you want to add a lot of nodes with the same name, or if other mods or vanilla Minecraft keybind mappings activate
while you type in the text mode.

There is one shortcut: name a node `na` and leave the text mode, and it turns into a `no_area` node with the **Area** tag already applied.

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
direction changes of edges), even big actions (loading, unloading the whole graph) can be undone via `Control` + `Z`. It also supports
Redo via `Control` + `Y`.

Both are bound to the physical key position, which follows the US layout. On a QWERTY keyboard the two are therefore swapped: Undo is
`Control` + `Y` and Redo is `Control` + `Z`.

#### Select Node by Looking

You want to select a node that is further away, and it's time-consuming to walk up to the node to select it? Walk no more!
You can set a keybind for **Select Node by Looking**. Then, all you need to do is to look at the node you want to select, and press the
keybind!

#### Split

Use the **Split Key** to cut the currently red-marked edge (between the selected node and the closest node) in half.
That will create a new node in the middle of the edge, remove the old connection between the two old nodes, and add two new connections,
from the new node to the two old nodes.

The **Split Key** is not set by default.

#### Dissolve

Use the **Dissolve Key** to delete a node that has exactly two connected nodes. The two remaining nodes then get a connection between them.

The **Dissolve Key** is not set by default.

#### One Directional

Use the **One Directional Key** (by default `H`) to cycle through the direction of the currently red-marked edge (between the selected node
and the closest node).

The three directions to cycle through:

- Both (default for all edges)
- A→B
- B→A

This is useful to mark paths the user can only move in one direction. E.g., drop-down paths, or jumping pad movements.

Direction is a property of a normal edge instead of a separate kind of connection. In the saved file, a one directional edge is simply
recorded on one side only. That keeps drops and jump pads out of the pathfinding as a special case, and the file format did not need a new
field for them.

One rule comes with it: there always has to be a way back somehow. A one directional edge leading into a dead end traps the navigation
there, so the **Error Finder** flags any edge without a return path. The way back does not have to be direct, a long detour around is
perfectly fine.

#### Weighted Nodes

Use the command `/shgraphweight` to set the weight of the selected node.
By default, every node has the weight of 0.

The weight gets added to the pathfinding route calculation. One unit of weight represents one Minecraft block.
This impacts all directions the node can pass through in the same way.

The weight sits on the node rather than on the individual connections because what slows you down is a place, not a connection. Water, a
climb, a gap: it is unpleasant no matter which side you approach from. One value on the node covers all of them, while putting it on the
connections would be one edit per connection.

Two things worth knowing before you set a value:

- The weight applies to every connection that touches the node, and walking through the node pays it twice, once coming in and once going
  out. Start small, a weight of 5 already steers the pathfinder away like a 10 block detour.
- Because it counts the same in every direction, it cannot express "slow going up, fast coming down". If a path is only bad one way, use
  the **One Directional Key** instead.

When to use? When the path slows down the user considerably, e.g., moving through water or climbing up blocks without stairs. Especially
useful when longer but faster to move through alternate paths exist.

#### Load a Network

The normal way to edit existing networks is using the `/shgraphloadthisisland` command.
But sometimes you want to edit other graphs (your own version of something or to check out an open PR).
The explicit **Load Key** (by default `I`) loads other graphs.

When pressed, the current clipboard gets read as JSON and parsed into the Graph Editor.

#### Clear the Network

When you want to start fresh, or just disregard the current network, you can press the **Clear Key** (no default keybind) to wipe the whole
network from the Graph Editor (the graph network loaded to the island is unaffected).

As a safety measure (not to lose progress), the current state of the network gets stored as JSON in your clipboard (but no additional save
actions are done like normally when pressing the **Save Key**).

Also, the **Undo Key** works like normal for this.

#### Disabled Nodes

Nodes cannot be manually disabled within the Graph Editor. Instead, this happens dynamically via the mod's codebase.
The command `/shgraphtoggledisabled` merely toggles the visibility of these programmatically **Disabled Nodes**.

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

This also impacts area detection.

Why is this not simply stored in the graph file? Because it changes while you play. A node can be disabled during an event and enabled
again minutes later, and it can differ from one player to the next. The files in the repo are identical for everyone and only change
through a pull request, so a state like this has no place in them.

For mapping that means two things. The disabled state is never part of what you save, and `/shgraphloadthisisland` refuses to load the
island while parts of it are disabled, so a reduced graph does not end up in a PR by accident. Run the command again within 5 seconds and
it re-enables every node first, then loads the complete island.

### Debug Tools

There are some tools that aren't to edit the graph but rather to test or debug the network or to deal with merge conflicts.

#### Navigate to Selected Node

Press the **Navigate to Node** keybind (by default `G`) to start the navigation pathfinding logic, targeting the selected node.
This is useful to test how the current network would behave, especially after changed node weights or use of the **One Directional Key**.
Since the navigation feature relies on the graph network data and not the Graph Editor directly, make sure to apply the Graph Editor to the
current island by using the **Save Key**.

#### Error Finder

The **Error Finder** analyzes the graph network as a whole and flags issues with nodes.
It runs by default when saving the graph.
When errors are found, this happens:

- Errors are posted broadly by count and error category as chat messages.
- The specific error text for the 10 closest faulty nodes is rendered directly below them in the world.
- A pathfinding route to the nearest faulty node is automatically started.

#### Find All

The command `/shgraphfindall` navigates you through all nodes of the network.
Useful to find errors in the graph data.

### Misc Tools

Additional features of the Graph Editor.

#### Feedback Mode

The **Feedback Key** (by default `K`) toggles the Feedback Mode.

While the feedback mode is enabled, every action you do in the Graph Editor gets commented in the console. Useful for beginners.

#### Disjointed Networks

All nodes that connect form one network.
Breaking connections (e.g. via adding a new node that is not connected to the network) creates multiple disjointed networks.
This unstable state requires fixing.
Therefore, the **Error Finder** will flag such cases when saving.

There are also specific commands that come in handy:

- `/shgraphfindnetwork` lists all separate networks in the Graph Editor.
    - Clicking on a network then navigates you to it.
- `/shgraphmerge` merges graph data from the clipboard into the current Graph Network.
    - The nodes don't connect, they just exist in one Graph Editor.
- `/shgraphcopynetwork` copies only the closest network to the clipboard.

#### Parkour

A parkour is a line of waypoints that are all connected to the previous and next waypoint.
Many different SkyHanni features use this identical parkour format:

- Dungeon Race Guide
- Rescue Mission in the Crimson Isle
- Lava Maze in The Rift.
- See `ParkourHelper` for more examples.

The Graph Editor can be used to edit such waypoints:

- The command `/shgraphloadparkour` loads the current clipboard as a parkour into the Graph Editor.
- The command `/shgraphexportasparkour` saves the Graph Editor as a parkour into the clipboard.
