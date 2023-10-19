/// <reference types="../CTAutocomplete" />
/// <reference lib="es2015" />

const GlStateManager = Java.type("net.minecraft.client.renderer.GlStateManager");
const GL11 = Java.type("org.lwjgl.opengl.GL11");

const mc = Client.getMinecraft();

const forwardBind = new KeyBind(mc.field_71474_y.field_74351_w);
const backwardBind = new KeyBind(mc.field_71474_y.field_74368_y);
const leftBind = new KeyBind(mc.field_71474_y.field_74370_x);
const rightBind = new KeyBind(mc.field_71474_y.field_74366_z);
const jumpBind = new KeyBind(mc.field_71474_y.field_74314_A);
const sneakBind = new KeyBind(mc.field_71474_y.field_74311_E);
const sprintBind = new KeyBind(mc.field_71474_y.field_151444_V);
const attackBind = new KeyBind(mc.field_71474_y.field_74312_F);
const useBind = new KeyBind(mc.field_71474_y.field_74313_G);

const WalkBind = new KeyBind("Walk", Keyboard.KEY_7, "Pathfinder");

class Point {
    constructor(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

let currentUserPath = [];

let autoWalk = false;
let toggleLabel = false;
let rotmode = false;
let renderColor = [1, 0, 0];

const stopAllMovement = () => {
    jumpBind.setState(false);
    forwardBind.setState(false);
    rightBind.setState(false);
    backwardBind.setState(false);
    leftBind.setState(false);
}

const getBlockAtPoint = (point) => {
    return World.getBlockAt(point.x, point.y, point.z);
}

const getPathEntity = (x, y, z) => {
    nodeProcessor = new net.minecraft.world.pathfinder.WalkNodeProcessor();
    nodeProcessor.func_176175_a(true); // setEnterDoors
    nodeProcessor.func_176176_c(true); // setAvoidsWater
    pathFinder = new net.minecraft.pathfinding.PathFinder(nodeProcessor);
    return pathFinder.func_180782_a(
    	World.getWorld(),
    	Player.getPlayer(),
    	new net.minecraft.util.BlockPos(x, y, z),
    	2000
    ); // createEntityPathTo
};

const pathTo = (x, y, z) => {
    let localToConnect = [];

    let endPoint = new Point(x, y, z);

    let path = getPathEntity(endPoint.x, endPoint.y, endPoint.z);

    if(!path) return;

    for(i = 0; i < path.func_75874_d() - 1; i++) { // getCurrentPathLength
        let currentP = path.func_75877_a(i); // getPathPointFromIndex
        let point = new Point(currentP.field_75839_a + 0.5, currentP.field_75837_b + 0.5, currentP.field_75838_c + 0.5); // xCoord yCoord zCoord
        localToConnect.push(point);
    }

    return localToConnect;
}

const possibleRotations = [-180, -135, -90, -45, 0, 45, 90, 135, 180];

const getClosest = (counts, goal) => {
    return counts.reduce((prev, curr) => Math.abs(curr - goal) < Math.abs(prev - goal) ? curr : prev);
}

const pressKeys = (arr) => {
    arr.forEach(key => {
        switch(key) {
            case "W":
                forwardBind.setState(true);
                break;
            case "S":
                backwardBind.setState(true);
                break;
            case "A":
                leftBind.setState(true);
                break;
            case "D":
                rightBind.setState(true);
                break;
            case "SPACE":
                jumpBind.setState(true);
                break;
            case "SNEAK":
                sneakBind.setState(true);
                break;
            case "SPRINT":
                sprintBind.setState(true);
                break;
            case "LEFTC":
                attackBind.setState(true);
                break;
            case "RIGHTC":
                useBind.setState(true);
                break;
        }
    });
}

const getBind = (dir, pn, yaw) => {
    yaw = getClosest(possibleRotations, yaw);
    switch(yaw) {
        case 180:
        case -180:
            pn === "P" ? dir === "X" ? pressKeys(["D"]) : pressKeys(["S"]) : dir === "X" ? pressKeys(["A"]) : pressKeys(["W"]);
            break;
        case -90:
            pn === "P" ? dir === "X" ? pressKeys(["W"]) : pressKeys(["D"]) : dir === "X" ? pressKeys(["S"]) : pressKeys(["A"]);
            break;
        case 0:
            pn === "P" ? dir === "X" ? pressKeys(["A"]) : pressKeys(["W"]) : dir === "X" ? pressKeys(["D"]) : pressKeys(["S"]);
            break;
        case 90:
            pn === "P" ? dir === "X" ? pressKeys(["S"]) : pressKeys(["A"]) : dir === "X" ? pressKeys(["W"]) : pressKeys(["D"]);
            break;
        case -135:
            pn === "P" ? dir === "X" ? pressKeys(["W", "D"]) : pressKeys(["S", "D"]) : dir === "X" ? pressKeys(["S", "A"]) : pressKeys(["W", "A"]);
            break;
        case 135:
            pn === "P" ? dir === "X" ? pressKeys(["S", "D"]) : pressKeys(["S", "A"]) : dir === "X" ? pressKeys(["W", "A"]) : pressKeys(["W", "D"]);
            break;
        case -45:
            pn === "P" ? dir === "X" ? pressKeys(["W", "A"]) : pressKeys(["W", "D"]) : dir === "X" ? pressKeys(["S", "D"]) : pressKeys(["S", "A"]);
            break;
        case 45:
            pn === "P" ? dir === "X" ? pressKeys(["S", "A"]) : pressKeys(["W", "A"]) : dir === "X" ? pressKeys(["W", "D"]) : pressKeys(["S", "D"]);
            break;
    }
}

const getEyePos = () => {
    return {
        x:Player.getX(),
        y:Player.getY() + Player.getPlayer().func_70047_e(),
        z:Player.getZ()
    };
}

/* Not my code that's why its ugly sorry */
let lookAtBlock = (blockPos, playerPos) => {
    if (!playerPos) playerPos = getEyePos();
    let d = {
        x:blockPos.x - playerPos.x,
        y:blockPos.y - playerPos.y,
        z:blockPos.z - playerPos.z
    };
    let yaw = 0;

    if (d.x != 0) {
        d.x < 0 ? yaw = 1.5 * Math.PI : yaw = 0.5 * Math.PI;
        yaw = yaw - Math.atan(d.z / d.x);
    } else if (d.z < 0) yaw = Math.PI;

    d.xz = Math.sqrt(Math.pow(d.x, 2) + Math.pow(d.z, 2));

    yaw = -yaw * 180 / Math.PI;

    setYaw(yaw);
}

const setYaw = (yaw) => {
    Player.getPlayer().field_70177_z = yaw;
}

const distFormula = (x1, y1, z1, x2, y2, z2) => {
    return Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2 + (z2 - z1) ** 2);
}

const getSpeed = () => {
    const lastX = new Entity(Player.getPlayer()).getLastX();
    const lastY = new Entity(Player.getPlayer()).getLastY();
    const lastZ = new Entity(Player.getPlayer()).getLastZ();

    const currentX = Player.getX();
    const currentY = Player.getY();
    const currentZ = Player.getZ();

    return Math.round(20 * distFormula(lastX, lastY, lastZ, currentX, currentY, currentZ) * 10) / 10;
}

const walkOn = (pointsOfPath) => {
    const currX = Math.round(Player.getX() * 10) / 10;
    const currY = Math.round(Player.getY() * 10) / 10;
    const currZ = Math.round(Player.getZ() * 10) / 10;

    let previous = 10;
    let closest = null;

    let nextPoint = null;
    let nextNextPoint = null;

    pointsOfPath.forEach(point => {
        let currentDist = calculateDistance(currX, currY, currZ, point.x, point.y, point.z);
        if (currentDist <= previous) {
            previous = currentDist;
            closest = point;
            if(pointsOfPath.indexOf(point) !== pointsOfPath.length - 1) {
                nextPoint = pointsOfPath[pointsOfPath.indexOf(point) + 1];
            }

            if(pointsOfPath.indexOf(point) !== pointsOfPath.length - 2) {
                nextNextPoint = pointsOfPath[pointsOfPath.indexOf(point) + 2];
            }
        }
    });

    if(closest !== null && nextPoint !== null) {
        stopAllMovement();
        if(closest === pointsOfPath[pointsOfPath.length - 1]) return true;
        const currYaw = Math.floor(Player.getYaw());
        
        if(!rotmode) {
            if(currX !== nextPoint.x && currX < nextPoint.x) getBind("X", "P", currYaw);
            if(currX !== nextPoint.x && currX > nextPoint.x) getBind("X", "N", currYaw);
            if(currZ !== nextPoint.z && currZ < nextPoint.z) getBind("Z", "P", currYaw);
            if(currZ !== nextPoint.z && currZ > nextPoint.z) getBind("Z", "N", currYaw);
        } else {
            forwardBind.setState(true);
            lookAtBlock({
                x: nextNextPoint.x,
                y: nextNextPoint.y,
                z: nextNextPoint.z
            });
        }

        if(closest.y + 1 === nextPoint.y || Math.round(Player.getY()) + 1.5 === nextPoint.y) jumpBind.setState(true);
        return false;
    }
}

const calculateDistance = (x1, y1, z1, x2, y2, z2) => {
    return Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2 + (z2 - z1) ** 2);
}

register('command', (cmd, x, y, z) => {
    switch(cmd) {
        case "pathto":
            if(x && y && z) {
                try {
                    currentUserPath = pathTo(
                        parseFloat(x),
                        parseFloat(y),
                        parseFloat(z)
                    );
                    ChatLib.simulateChat(`§5[§dDebugtone§5] §7ok. pathing to ${x} ${y} ${z}`);
                } catch(e) { } // idc abt number error bs. just dont enter fucking numbers
            }
            break;
        case "clear":
            currentUserPath = [];
            ChatLib.simulateChat(`§5[§dDebugtone§5] §7ok. cleared path`);
            break;
        case "label":
            toggleLabel = !toggleLabel;
            ChatLib.simulateChat(`§5[§dDebugtone§5] §7set rendering point labels to ${toggleLabel.toString().toUpperCase()}`);
            break;
        case "go":
            if(currentUserPath.length === 0) {
                ChatLib.simulateChat(`§5[§dDebugtone§5] §7specify a path first by doing /debugtone pathto x y z`);
                break;
            }
            if(!autoWalk) {
                autoWalk = true;
                ChatLib.simulateChat(`§5[§dDebugtone§5] §7going`);
            } else if(autoWalk) {
                ChatLib.simulateChat(`§5[§dDebugtone§5] §7already going idiot`);
            }
            break;
        case "stop":
            if(autoWalk) {
                autoWalk = false;
                stopAllMovement();
                ChatLib.simulateChat(`§5[§dDebugtone§5] §7stopped`);
            } else {
                ChatLib.simulateChat(`§5[§dDebugtone§5] §7nothing to stop idiot`);
            }
            break;
        case "rotmode":
            rotmode = !rotmode;
            ChatLib.simulateChat(`§5[§dDebugtone§5] §7set rotation mode to §8${rotmode.toString().toUpperCase()}`);
            break;
    }
    
}).setName("debugtone");

let infractions = 0;

register('tick', () => {
    if(WalkBind.isKeyDown() || autoWalk) {
        let walkTask = walkOn(currentUserPath);
        if(autoWalk && !walkTask && getSpeed() === 0) {
            infractions++;
            if(infractions >= 100) {
                jumpBind.setState(true);
                if(infractions >= 140) {
                    infractions = 0;
                }
            }
        }
        if(autoWalk && walkTask) {
            autoWalk = false;
            currentUserPath = [];
            ChatLib.simulateChat(`§5[§dDebugtone§5] §7arrived.`);
        }
    }
});

register('renderWorld', () => {
    if(currentUserPath === undefined) return;
    if(currentUserPath.length <= 1) return;
    
    currentUserPath.forEach((point, index) => {
        if(toggleLabel) {
            disableGlShit();
            Tessellator.drawString(`§cPoint ${index + 1}`, point.x, point.y, point.z, Renderer.WHITE, true, 0.02, false);
            enableGlShit();
        }

        if(point !== currentUserPath[currentUserPath.length - 1]) {
            disableGlShit();
            
            Tessellator.begin(1)
            .colorize(renderColor[0], renderColor[1], renderColor[2], 1)
            .pos(
                point.x,
                point.y,
                point.z
            )
            .pos(
                currentUserPath[index + 1].x,
                currentUserPath[index + 1].y,
                currentUserPath[index + 1].z
            )
            .draw();

            enableGlShit();
        }
    });
});

const disableGlShit = () => {
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glBlendFunc(770, 771);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glLineWidth(3.0);
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);

    GlStateManager.func_179094_E();
};

const enableGlShit = () => {
    GL11.glEnable(GL11.GL_CULL_FACE);
    GlStateManager.func_179121_F();
    GL11.glEnable(GL11.GL_DEPTH_TEST);
	GL11.glEnable(GL11.GL_TEXTURE_2D);
	GL11.glDepthMask(true);
	GL11.glDisable(GL11.GL_BLEND);
};

const getCurrentPath = () => {
    return currentUserPath;
}

const setCurrentPath = (toSet) => {
    currentUserPath = toSet;
}

const getAutoWalkStatus = () => {
    return autoWalk;
}

const setAutoWalkStatus = (boolean) => {
    autoWalk = boolean;
}

/* Exports for API Usage */
export {
    Point,
    pathTo,
    walkOn,
    getCurrentPath,
    setCurrentPath,
    getAutoWalkStatus,
    setAutoWalkStatus,
    getEyePos,
    lookAtBlock,
    setYaw,
    getBlockAtPoint
};
