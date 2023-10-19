# debugtone
Crappy pathfinder bot thing made in ChatTriggers

To use this you must have the latest 1.8.9 Forge installed (11.15.1.2318) and have the latest ChatTriggers version (1.3.0+).
ChatTriggers: http://chattriggers.com/

Might turn this into a library in the future so you can integrate a shitty pathfinder API in your own module.

**Please do not use this for botting on Hypixel. You WILL get banned.**

Commands:

- `/debugtone pathto <x> <y> <z>` - Finds path to a point (must be in 2000 block range, you can edit this in the file as there is no config)
- `/debugtone clear` - Clears path
- `/debugtone label` - Mainly a command used for debugging but all it does is show "Point *n*" above every point in the path.
- `/debugtone go` - Starts walking towards the goal following the path, there is a freelook like in baritone so you can look in any direction while it does its thing.
- `/debugtone stop` - Stops walking
- `/debugtone rotmode` - Instead of navigating with WASD, it only holds down W and changes your yaw to walk forwards. (Doesn't touch pitch)

If you hold down the "7" key it will walk until you release the button. (No need to do `/debugtone go` in order to use this). I didn't even remember this exists so it might not even work.


WARNING: this is a proof of concept, It's horrible... Like seriously. It uses built in Minecraft pathfinding so it does things like this:

![image](https://user-images.githubusercontent.com/55459283/119272637-37175100-bc07-11eb-9095-7543d4d44e2d.png)
![image](https://user-images.githubusercontent.com/55459283/119272642-3d0d3200-bc07-11eb-85c4-043e27a68c7d.png)


**Please do not use this for botting on Hypixel. You WILL get banned.**
