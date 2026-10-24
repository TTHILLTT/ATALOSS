# ATALOSS

![Preview](/images/image9.png)

[Myau Client](https://myau.sell.app/), for those who can’t afford it.

Based on **`Myau-250910`**, with added features and improvements not found in the original, focused on expanding and refining the original Myau client.

[![Discord](https://invidget.switchblade.xyz/yjSaTufmqU)](https://discord.gg/yjSaTufmqU)

## Wardrobe

ATALOSS ships a built-in wardrobe for custom capes and skins. Both are client-side only -
nothing is uploaded anywhere and your real account skin is untouched.

### Custom cape
1. Drop a PNG into `config/ATALOSS/capes/` (created automatically; 64x32 or any **2:1** size works).
2. Open the Click GUI (default **Right Shift**) and enable **CustomCape** in the **Legit** menu, or
   set the path by hand: `.customcape cape config/ATALOSS/capes/mycape.png`.
3. Remote images work too - `cape` accepts an `http(s)://` URL, it is downloaded once and cached
   under `config/ATALOSS/capes/`.

The cape renders at its **native resolution**: 128x64 and larger images stay sharp (no downscale to
64x32), because the vanilla cape UVs are normalized.

### Custom skin
1. Drop a **64x64** (or 128x128 / 256x256 HD) skin into `config/ATALOSS/skins/`.
   Legacy 64x32 skins are rejected with a clear message - convert them first.
2. Enable **CustomSkin** in the Legit menu.
3. `model` auto-detects **Steve** (4px arms) vs **Alex** (3px arms) from the texture, and can be
   forced with `AUTO` / `DEFAULT` / `SLIM`.

### Quick switching
The top bar of the Click GUI has a **Wardrobe** button next to the `Cheat | Legit` switch. It opens a
tile grid (Liquid Glass style) with a preview of every file in `capes/` and `skins/`:
click a tile to apply it, `None` to disable the module, `ESC` to go back.

Preview rendering is flat-textured: skins are composed from the front faces of head / body / arms / legs,
capes show only the patterned outer side.

### Useful commands
```
.customcape cape <path-or-url>     # set the cape texture
.customskin skin <path-or-url>     # set the skin texture
.toggle CustomCape                 # enable / disable the cape
.toggle CustomSkin                 # enable / disable the skin
```

## Changes

* Added Liquid Glass Click GUI 
* Fixed bugs
* Added Legit Telly Module
* Added Cheat / Legit menu switch with a full Legit module set (HUD info, QoL, 1.7 animations)
* Added Liquid Glass and lightweight Click GUI styles
* Added HUD drag editor
* Added custom cape (HD), custom skin (Steve / Alex auto-detect) and the Wardrobe browser
* Added mouse 4 / 5 key bindings

## Building

```bash
./gradlew build
```

## Contributing

TTHILLTT ver.

You can open an issue or submit a pull request to help improve Open Myau.

If you’re interested in co-developing or have questions, feel free to reach out:

* Discord: `60124808866_88040`
