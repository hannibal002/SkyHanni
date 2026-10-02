# Minecraft 26.3 source port

This adds a real Java 25/Fabric26.3 target beside the existing 26.1 and26.2 matrix, based on SkyHanni beta commit 0913ae5c96fc52d03aa41ad1a8369c8643e6dc63. Target 26.3 uses Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Fabric Language Kotlin 1.14.1+kotlin.2.4.20. All 987 generated SkyHanni modules remain enabled. Existing target 26.2 remains the Stonecutter active source representation.

## Input and persisted settings

Minecraft 26.3 KEYBOARD values are native SDL physical scancodes. A=4 and B=5 therefore overlap the old library mouse numeric range. The26.3 backend uses native keyboard values, encoded mouse keybinds -100-logicalButton, and unbound -1. Logical mouse order remains left0/right1/middle2; native SDL order is left1/right3/middle2. Mouse events delivered to custom GUI widgets use the logical order, while native Minecraft superclass events retain the native order.

SkyHanni config schema 147 performs a one-time migration from GLFW values only in fields annotated ConfigEditorKeybind, including supported Property/list/map containers and SerializedName/inherited fields. Other numeric settings are unchanged. Files already at schema 147 are never translated again. The original config document is not edited on disk by this source-port preparation; normal application migration/save performs that operation in the new instance.

The new genuine MoulConfig 26.3 target follows the native representation. Its KeyboardEvent.KeyPressed.keycode is KeyEvent.key() (physical SDL), and its inherited field named scancode now receives KeyEvent.keycode() (logical SDL). MouseEvent.Click.mouseButton remains logical left0/right1/middle2. IMinecraft.encodeMouseKeybind(logicalButton) returns -100-logicalButton, and isMouseButtonDown takes logical buttons. A consuming mod storing raw integer bindings must version-migrate its own old GLFW data; the library cannot infer the schema of arbitrary consumers. Mods serializing Minecraft translation names do not have this integer ambiguity. Earlier MoulConfig targets retain their existing representation.

## Rendering and native dialogs

GPU APIs are rebuilt against RenderPearl. Existing custom pipelines, chroma uniforms and world feature submission remain. GUI item textures use genuine FeatureRenderDispatcher.prepareFrame and a render pass with explicit color/depth attachments, matching vanilla 26.3 picture-in-picture rendering. Atlas slots receive their own scissor; real-time slots receive no atlas scissor. Frame/transient uniform buffers belong to the native render frame.

Armor translucency preserves26.3 integrated enchanted glint, its GlintSampler and texture transform; trim remains a separate vanilla submission. Removed dedicated translucency factories are recreated with native pipeline snippets and registered RenderTypes. The breaking-particle hook now targets private addBreakingParticles so the added breaking sound is not cancelled. Grayscale glyph creation replaces the removed intensity factory.

Custom shaders use native GLSL330, #include syntax and explicit vertex/fragment interface locations only in target26.3. All13 active shader pairs retain their original main-function formulas. All26 stages pass official LWJGL3.4.3 shaderc with the native RenderPearl base options and Vulkan1.2 SPIR-V validation. Unused legacy GLSL120 darken shaders remain unchanged. Native tooltip extraction uses the final true flag to preserve the earlier heading gap.

The original synchronous file-selection feature remains through official LWJGL TinyFD 3.4.3 API and all 11 upstream platform-native classifiers, with distinct native resource paths and bundled upstream license. No GLFW backend is restored.

## Build and dependencies

Use the committed Gradle wrapper with Java 25. Choose external `JDK8`, `JDK25`, `MOUL_MAVEN` and `RENDER_CHEST_MAVEN` directories. Source copies of dependencies are separate review/contribution inputs, excluded from the SkyHanni parent patch.

Build the reviewed MoulConfig v4 port first, based on [NotEnoughUpdates/MoulConfig bd7b9aa3dbf7cef428280daf0156cb5e06338530](https://github.com/NotEnoughUpdates/MoulConfig/tree/bd7b9aa3dbf7cef428280daf0156cb5e06338530). From its own source root:

```sh
export JAVA_HOME="$JDK25"
./gradlew --no-daemon --max-workers=2 --configure-on-demand \
  :modern:modern-26.3:remapJar :modern:modern-26.3:check \
  :modern:modern-26.3:publishMavenPublicationToPortDependenciesRepository \
  -Dmoulconfig.portVersion=4.7.2-codex26.3.1 \
  -Porg.gradle.java.installations.paths="$JDK8,$JDK25" \
  -PportDependencyRepository="$MOUL_MAVEN"
```

This publishes the genuine `org.notenoughupdates.moulconfig:modern-26.3:4.7.2-codex26.3.1` platform, which SkyHanni shades into its existing private namespace. The tested unclassified production library SHA256 is `eebfbf6ff1cfacc117f47b75877b1a5df4c239204633dbd23bc68b8c76722ce0`. Its original Unimined SNAPSHOT is retained; strict binary reproduction requires the exact resolved artifact, rather than an arbitrary future snapshot.

RenderChest `net.azureaaron:render-chest:1.0.3+26.3` is nested in the production JAR. Its official Maven coordinate was not yet published when this port was prepared. Build original [AzureAaron/RenderChest 0512b13ae5ee780b82a0be80535a8856ac8431f8](https://github.com/AzureAaron/RenderChest/tree/0512b13ae5ee780b82a0be80535a8856ac8431f8) and publish locally with its wrapper:

```sh
export JAVA_HOME="$JDK25"
./gradlew --no-daemon --max-workers=2 build publishMavenJavaPublicationToMavenLocal \
  -Dmaven.repo.local="$RENDER_CHEST_MAVEN"
```

That Maven-local override requires an absolute external directory. A source rebuild may differ in ZIP hash; verify genuine 26.3 metadata and retain its own provenance. The tested unchanged official CI JAR came from [run 35052254428](https://github.com/AzureAaron/RenderChest/actions/runs/35052254428), artifact 10429656413, source commit above. Archive SHA256: `52dd5f9ea0f792aa2c5b5a038431cffbe0312011fc0fcb5491304150a05a6521`; contained `render-chest-1.0.3+26.3.jar` SHA256: `6df5281fd4d89f236281468b9612c84a7e53df72d7648df42fb5189925ee434e`. Preserve its Apache 2.0 license; CI artifacts can expire. No additional RenderChest source PR or modified 26.2 metadata is needed.

From the SkyHanni root, build the production shaded artifact:

```sh
export JAVA_HOME="$JDK25"
./gradlew --no-daemon --max-workers=2 :26.3:shadowJar \
  -PmoulconfigPortRepository="$MOUL_MAVEN" \
  -PrenderChestPortRepository="$RENDER_CHEST_MAVEN"
```

Both repository properties are optional. Caller-supplied repositories precede the original local/official defaults; no workspace layout is assumed. Absolute external directories or file URIs make the build location independent. The production output is `build/libs/SkyHanni-9.1.0-mc26.3.jar`, not the development/no-dependency JAR. These tasks build or publish only to selected local directories; they do not invoke remote publication. No player config, account data, DevAuth or graphical client is needed to build.

MoulConfig common code keeps its original Java 8 ABI through a verified original Adoptium8u504 compiler; modern 26.3 and build-src use Java 25. The resulting artifact is production remapped, not a development JAR. Dependency verification remains enabled.

## Validation status

Production compile and shadow packaging pass. Static class-file checks validate 172 injection/shadow/accessor/invoker member selectors across 60 mixin classes, 57 explicit Minecraft invocation/field targets, and 89 root class-tweaker entries. The relocated MoulConfig service provider is present, and all shaded MoulConfig access-widener entries are covered by SkyHanni's root class tweaker. Core mod classes have Java 25 class major 69. An additional 152 class-file checks validate callback argument prefixes and method-local INVOKE/FIELD sites and declared ordinals. Two local-variable/STORE dataflow sites, transformed-mixin interactions, local capture and GPU behavior still require runtime validation. The preserved 26.2 target compileKotlin and compileJava also pass with 987 modules.

Six focused source regression fixtures pass: A/B versus right/middle collisions; GLFW F13/modifier/unbound migration; annotation/container selection; capture/save/reload without repeated migration; atlas scissor coordinates/bounds; and atlas versus real-time attachment/scissor isolation. They do not launch Minecraft or exercise the actual configuration editor.

Separate isolated native validation passed all470 runtime mixins, the configuration GUI and a fresh world. The production build also passed with both explicit external-repository flags. Combined-pack release review remains separate. These passes do not verify live Hypixel features, every GUI path, native file dialogs on all supported platforms, rotating/animated item atlas and real-time textures, all chroma/world rendering, translucent armor/glint/trim/cape, sign editing/clipboard, custom farming keybinds, window-opacity behavior or Vulkan runtime.
