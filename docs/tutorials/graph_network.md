# The Graph Network of SkyHanni

## What is this

A list of nodes (waypoints) and edges (lines between nodes) as a graph network that represents individual skyblock islands.
A powerful **Graph Editor** exists in SkyHanni that allows editing these networks.
The network data is used for two distinct use cases:

- **Navigating** around an island (find the shortest path from where you are right now to some other spot on the island)
- Detecting the **Current Area** where you are (like the area names from the scoreboard, just more precise and updating instantly instead of
  delayed)

A lot of features in SkyHanni are powered by those two functionalities.

The graph network is stored in JSON in the [SkyHanni Repo](https://github.com/hannibal002/SkyHanni-REPO/tree/main/constants/island_graphs),
one file per island.

### Current Area

Features that only work on a specific area on an island rely on the network to detect where the user is.

A non-exhaustive list of features:

- Area Navigation

### Navigating

We use Dijkstra and A* to help the user find the shortest path to where they want to go.

A non-exhaustive list of features:

- Fast Fairy Soul
- Hoppity Eggs
- Fishing Hotspot Radar
- Diana Burrows (soon)
- `/shnavigate <target>`
    - Lets you navigate to NPCs, Areas, Points of Interest, Mob-Spawn Areas, Ore Veins, Crop Farms.
- `/shnavigateall <target type>`
    - Lets you navigate in a circle to all waypoints of a given category, e.g. fairy souls, hoppity eggs, fishing hotspots/wormholes.
    - On canyon: hideonsun, honeyhive, pangolin, sanger, tree protection order.

# Graph Editor

To open up the graph editor run the command `/shgraph`. Run the command again to exit the whole graph edit at any time (or press the
default key "Home").
To load the graph editor with the existing repo data of the current island, run `/shgraphloadthisisland`.

Press (by default) the keybind M to toggle visibility of nodes and edges behind blocks.

Once the graph is loaded, you see two GUIs:

- **List of Named Nodes**
    - Contains the full list of all nodes on the graph that have a name.
    - Is used to add or remove tags to the node.
    - Useful to navigate to any given node quickly.
- A keybind list, showing you what you can do at any given time by pressing the keys.

## Basic Editing

The three most important/basic actions you can do are:

- Adding/deleting new nodes.
- Adding/deleting an edge between two nodes.
- Moving a node to another place.

The graph editor uses colors to explain what node is selected and what node is closest to you:

- The closest node to you is yellow.
- The selected node is green.

### Select a Node

Lets you select/deselect the next closest node to you.
Only one node can be selected at the same time.

By default, the keybind for "Select Nearest Node" this is "Left Mouse Button".

### Adding/Deleting Nodes

By default, when you press F, you create a new node at the location where you stand.
If you have a node selected, this behavior is altered:

If you are close to the selected node, you delete the selected node by pressing F.
If you are more than 3 blocks away from the selected node, you do two things:

- You create a new node at the current location (like if you have no node selected).
- You create an edge (connection) between the selected node and the newly created node.

If you press the select key after creating a new node, you can move around and just keep pressing the add and select keys to quickly create
a new line of nodes in the world.

### Adding/Deleting Edges

By default, when you press C, you create a connection between the selected node (green) and the next closest node (yellow).
When you press C again, the edge gets deleted.
The normal edge color is blue, the edge between the selected and the nearest node is red.

### Moving a Node

Instead of deleting and adding a new node, you can move the selected node around in the world.
While you keep pressed the **Node Move Key** (default "Tab"), move the node by one block in each direction.
Use your Minecraft keybind to move the selected node:

- W-A-S-D to move the node forward, backwards or sideways, relative to the direction you are looking at.
- Use the sneak and jump keys to move the node up or down.

## Named Nodes and Tags

To find targets on the graph and let the mod features programmatically connect to the nodes on the graph, we can give a node a name and a
tag.

A node with a name always needs a tag, a node with a tag always needs a name.
The vast majority of nodes don't have either.

### Renaming a node

By default, the keybind `Y` is used to go into the text mode.
The text mode needs you to select a node first.

In the text mode, you can use your keyboard to write down a name for the node.
You see the text on the node in the world in real time while you write.
Press Esc to exit the text mode.

You can use `Control` + `V` to paste your clipboard as text while in the text mode.
It does not append on the end of the text, but replaces the whole text to your clipboard.
This can become useful if you want to add a lot of nodes with the same name.
Or if other mods or vanilla Minecraft keybind mappings activate while you type in the text mode.

### Tagging a Node

Once the node has a name, it shows up in the **List of Named Nodes**.
In this list, you see the red "no tag" suffix next to the newly renamed node.

To give a **Named Node** a tag:

- Open the inventory to hover over the **List of Named Nodes** (move/resize the GUI so you can see it clearly while your inventory is open).
- Control-click on the node you want to edit the tags of.
- Then a list of all available tags for the given island shows up (there are some tags that are only available on specific islands. As a
  convenience, only the ones that may apply on the current island show up here).
- Select or deselect whatever tag you want the node to have.
- The tag name and color show up below the node in the world.
- Close the tag list by clicking on the "Go Back!" text at the bottom of the list. If this does not show up, scroll down.

The tag does not get automatically removed when you remove the name.
The **Error Finder** will flag "Named Nodes" without a tag, and "Tagged Nodes" without a name.

## Areas

We use the tags **Area** and **Small Area** to tell other skyhanni features what area the user is at.
That works by first finding the next closest node to the player.
Then we check what area the node is at.

### What area is the node at?

We do not need to give every node an area tag, that would be impractical.
Instead, we follow the graph chain from one node to their neighbors until we find an area name tag.
The name of that node then defines what area the player stands at.
The next area needs to start at the other side of the "imaginary area boundary" and be connected to the first area.

The **Error Finder** comes here especially handy in warning you when there are nodes that connect to multiple areas at the same time without
a clearly-defined boundary.

### Small Areas

Small areas are for things like a building or a small cave, like the bank in hub or Trapper's Den in The Farming Islands.
(Large) normal areas are the big areas on the map, like the graveyard in the hub or Stronghold in Crimson Isle.
The difference between area and small area is to visually differentiate in the **Area Navigation** feature and in /shnavigate, and for the
option to show/hide small areas in the **Area Navigation** list.

## Advanced Editing Tools

Apart from adding, deleting and moving nodes and edges, the graph editor has an amount of advanced editing tools

### Undo and Redo

Every action you do in the graph editor (adding/removing nodes/edges, renames, adding/removing tags, node weight, direction changes of
edges), even big actions (loading, unloading the whole graph) can be undone via `Control` + `Z`. Also supports Redo via `Control` + `Y`.

### Navigate to Selected Node

Press the **Navigate to Node** keybind (by default `G`) to start the navigation pathfinding logic, targeting the selected node.
Since the navigation feature relies on the graph network data and not the graph editor directly, make sure to apply the graph editor to the
current island by saving (by default 'O').

### Select looking at Node

You want to select a node that is further away, and its time consuming to walk up to the node to select it?
Walk no more! you can set a key for the **Select looking at Node** keybind.
Then, all you need to do is to look at the node you want to select, and press the keybind!

### Split

Use the **Split Key** to cut the currently red marked edge (between the selected node and the closest node) in half.
that will create a new node in the middle of the edge, remove the old connection between the two old nodes, and add two new connections,
from the new node to the two old nodes.

The **Split Key** is not set by default.

### Dissolve

Use the **Dissolve Key** to delete a node that has exactly two connected nodes. The two remaining nodes then get a connection between them.

edge cycle/one-directional edges

/weight

press save to test /shnavigate

## Tools to Test or Debug, and deal with merging networks

Error Finder

/findall

/findnetwork

/merge

## misc tools

feedback

load key

clear key

Save
what does save all do

## Parkour

two commands to import/export
