#!/bin/bash

# Step 1: Check if build/libs/SkyHanni* exists
if ls build/libs/SkyHanni* 1> /dev/null 2>&1; then
    # Step 2: Check if ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/SkyHanni* exists
    if ls ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/SkyHanni* 1> /dev/null 2>&1; then
        # Step 3: Delete the file checked in 2, and move the one from 1 to the location in 2
        rm ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/SkyHanni*
        mv build/libs/SkyHanni* ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/
    else
        # Step 4: If only check 1 passes, move it to the location checked in 2
        mv build/libs/SkyHanni* ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/
    fi

    # Step 6: Execute the command flatpak run org.prismlauncher.PrismLauncher -l dev
    flatpak run org.prismlauncher.PrismLauncher -l dev
else
    # Step 5: If only check 2 passes, do not change anything
    if ls ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/dev/.minecraft/mods/SkyHanni* 1> /dev/null 2>&1; then
        echo "SkyHanni found, no changes needed."
    else
        # Step 7: If none of the checks pass, print "SkyHanni not found" and exit
        echo "SkyHanni not found"
        exit 1
    fi
fi
