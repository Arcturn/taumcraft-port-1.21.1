#!/usr/bin/env python3
"""Import only the six vanilla block previews used by the original TC6 blueprints.

All resources live under thaumcraft:research/vanilla; Minecraft's world and item
resources are never replaced. Lava is deliberately kept on the existing fluid
renderer: its 1.12.2 and 1.21.1 texture pixels are identical.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    resources = root / "src/main/resources"
    prefix = "thaumcraft:research/vanilla/"
    files = []
    model_ids = set()
    imported_models = set()
    imported_textures = set()
    states = {}

    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    with zipfile.ZipFile(args.minecraft_jar) as source:
        def record(original, target, data):
            raw = source.read(original)
            write(resources / target, data)
            files.append({"source": original, "source_sha256": sha(raw),
                          "target": target, "target_sha256": sha(data), "bytes": len(data)})

        def texture(name):
            if name.startswith("#"):
                return name
            name = name.removeprefix("minecraft:")
            if name not in imported_textures:
                imported_textures.add(name)
                old = "assets/minecraft/textures/" + name + ".png"
                target = "assets/thaumcraft/textures/research/vanilla/" + name + ".png"
                record(old, target, source.read(old))
                if old + ".mcmeta" in source.namelist():
                    record(old + ".mcmeta", target + ".mcmeta", source.read(old + ".mcmeta"))
            return prefix + name

        def model(name):
            name = name.removeprefix("minecraft:")
            if "/" not in name:
                name = "block/" + name
            result = prefix + name
            if name in imported_models:
                return result
            imported_models.add(name)
            old = "assets/minecraft/models/" + name + ".json"
            value = json.loads(source.read(old))
            if "parent" in value:
                value["parent"] = model(value["parent"])
            if "textures" in value:
                value["textures"] = {key: texture(tex) for key, tex in value["textures"].items()}
            # Standalone models cannot inherit the registered block's render layer.
            value["render_type"] = "minecraft:cutout" if name.startswith("block/iron_bars_") else "minecraft:solid"
            record(old, "assets/thaumcraft/models/research/vanilla/" + name + ".json", encoded(value))
            return result

        def apply(definition):
            result = copy.deepcopy(definition)
            result["model"] = model(result["model"])
            model_ids.add(result["model"])
            return result

        def blockstate(name):
            path = "assets/minecraft/blockstates/" + name + ".json"
            raw = source.read(path)
            states[path] = sha(raw)
            return json.loads(raw)

        blocks = {}
        for name in ("nether_brick", "obsidian"):
            blocks["minecraft:" + name] = {"variants": {"0": apply(blockstate(name)["variants"]["normal"])}}
        blocks["minecraft:cauldron"] = {"variants": {"0": apply(blockstate("cauldron")["variants"]["level=0"])}}
        anvil = blockstate("anvil")["variants"]
        blocks["minecraft:anvil"] = {"variants": {
            str(meta): apply(anvil["damage=0,facing=" + facing])
            for meta, facing in enumerate(("south", "west", "north", "east"))}}
        piston = blockstate("piston")["variants"]
        blocks["minecraft:piston"] = {"variants": {
            str(meta): apply(piston["extended=false,facing=" + facing])
            for meta, facing in enumerate(("down", "up", "north", "south", "west", "east"))}}
        bars = blockstate("iron_bars")["multipart"]
        blocks["minecraft:iron_bars"] = {"multipart": [
            {**({"when": part["when"]} if "when" in part else {}), "apply": apply(part["apply"])}
            for part in bars]}

    index = {"blocks": blocks, "models": sorted(model_ids)}
    index_path = "thaumcraft-port/book-vanilla-block-model-index.json"
    write(resources / index_path, encoded(index))
    # The blocks atlas discovers vanilla item(s)/block(s) directories, not our
    # isolated research/vanilla path. Append exact sprites without replacing any
    # existing TC or vanilla atlas sources (including directory registrations).
    atlas_path = "assets/minecraft/atlases/blocks.json"
    atlas = json.loads((resources / atlas_path).read_bytes())
    sources = atlas.setdefault("sources", [])
    registered = {entry.get("sprite", entry.get("resource")) for entry in sources
                  if entry.get("type") == "minecraft:single"}
    sprites = sorted(prefix + name for name in imported_textures)
    for sprite in sprites:
        if sprite not in registered:
            sources.append({"type": "minecraft:single", "resource": sprite, "sprite": sprite})
            registered.add(sprite)
    write(resources / atlas_path, encoded(atlas))
    audit = {
        "scope": "Book-only source construction previews for six vanilla block types; no world block, item icon, registry, or global Minecraft resource replacement.",
        "original": {"version": "Minecraft 1.12.2", "jar": args.minecraft_jar.name,
                     "jar_sha256": sha(args.minecraft_jar.read_bytes()), "blockstate_sha256": states},
        "reproduce": "python3 tools/import_book_vanilla_models.py --minecraft-jar /path/to/1.12.2.jar",
        "index": {"path": index_path, "sha256": sha(encoded(index))},
        "atlas": {"path": atlas_path, "sha256": sha(encoded(atlas)), "sprites": sprites,
                  "policy": "Append missing exact single sources; preserve every existing atlas source."},
        "counts": {"blocks": len(blocks), "standalone_models": len(model_ids),
                   "model_closure": len(imported_models), "textures": len(imported_textures)},
        "files": sorted(files, key=lambda entry: entry["target"]),
        "selection": "Original metadata rotations; original iron-bars multipart predicates evaluated against the complete blueprint's connected state. No vanilla fallback for these supported preview states.",
        "geometry": "Original JSON elements/faces/UVs are copied unchanged. Only parent/texture locations and explicit NeoForge render_type are adapted. Preserves removed cauldron/anvil faces as well as original art.",
        "lava": "Not copied or modified: original lava_still/lava_flow RGBA pixels equal modern assets. Animation timing and fluid tessellation remain runtime concerns.",
        "validation": {"import": "passed", "status": "pending_root_build", "log": "verification/development-thaumonomicon-parity/resume-final-build.log"}}
    write(root / "verification/development-thaumonomicon-parity/vanilla-blueprint-source-audit.json", encoded(audit))
    print(json.dumps(audit["counts"]))


if __name__ == "__main__":
    main()
