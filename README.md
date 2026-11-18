# Sodium Core Shader Support (Unofficial 1.20.1 Backport)

An unofficial Fabric backport of [Sodium Core Shader Support](https://github.com/lni-dev/SodiumCoreShaderSupport) for Minecraft 1.20.1. It carries the feature set of the upstream 1.3.4 Minecraft 1.21.1 release to Sodium 0.5.13 on 1.20.1. The upstream project has no 1.20.1 release and says older backports are not currently planned.

## Compatibility

- Minecraft 1.20.1
- Fabric Loader and Fabric API
- Sodium 0.5.13 for Minecraft 1.20.1
- Client-side only

This port adapts the upstream shader-loading code and 1.21.1 compatibility-warning behavior to Minecraft 1.20.1 and the Sodium 0.5.13 API. It also carries the upstream 1.3.4 render-pass shader defines. It is not an official release and is not affiliated with LinusDev or the Sodium project.

## Shader resource paths

Place Sodium shader files under `assets/sodium/shaders` in your resource pack. Sodium shaders differ from vanilla core shaders. They can import shared files from `assets/minecraft/shaders`, for example:

```glsl
#import <minecraft:include/test.glsl>
```

The upstream project documents shader authoring and provides example packs: [upstream README](https://github.com/lni-dev/SodiumCoreShaderSupport).

Resource packs that replace vanilla shaders in `assets/minecraft/shaders` can declare the Sodium versions they support in `assets/sodiumcoreshadersupport/versions.json`. Example:

```json
{
  "supported-versions": {
    "1.20.1": ["0.5.13+mc1.20.1"]
  }
}
```

If a pack replaces vanilla shaders but omits this file, the port asks before enabling it. Sodium-only shader packs do not need this compatibility file.

The backport also adds `RENDER_PASS_SOLID`, `RENDER_PASS_CUTOUT`, and `RENDER_PASS_TRANSLUCENT` shader defines for resource packs that need to distinguish terrain passes.

## License and attribution

The backport code and original project icon are distributed under Apache License 2.0. The upstream `LICENSE` and `NOTICE` are included; `NOTICE` retains attribution for the MIT-licensed project that inspired the upstream implementation. The icon was designed specifically for this unofficial port and does not reuse the upstream project avatar. See the repository files for the complete license texts and notices.

## Verification status

The source has compiled and remapped against the 1.20.1 mappings and Sodium 0.5.13. No in-game client or resource-pack rendering check has been completed, so this is an Alpha candidate and not a verified stable release.

AI assistance was used to adapt the source and build configuration, prepare documentation, and create the original project icon. The upstream implementation and license notices are attributed above; client behavior has not yet been verified in game.
