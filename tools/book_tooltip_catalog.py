#!/usr/bin/env python3
"""Build a book-only TC6 tooltip corpus, then import actual 1.12.2 EN/RU snapshots.

The verification addon supplies ItemStack.getTooltip(player, NORMAL); this tool
does not emulate equipment attributes, Baubles capabilities, or gameplay items.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
PREFIX = "book.original.tooltip."
VARIANTS = {
    "ingot": "thaumium void brass".split(),
    "plate": "brass iron thaumium void".split(),
    "nugget": "iron copper tin silver lead quicksilver thaumium void brass quartz rareearth".split(),
    "cluster": "iron gold copper tin silver lead cinnabar quartz".split(),
    "mind": "clockwork biothaumic".split(),
}
COLORS = "white orange magenta light_blue yellow lime pink gray silver cyan purple blue brown green red black".split()
# Baubles 1.5.2 assets/baubles/lang/{en_us,ru_ru}.lang; needed by the uncaptured-NBT fallback.
BAUBLES_TRANSLATIONS = {
    "en_us": dict(zip("AMULET RING BELT TRINKET HEAD BODY CHARM".split(),
                      ["Bauble (Amulet)", "Bauble (Ring)", "Bauble (Belt)", "Bauble (Any)",
                       "Bauble (Head)", "Bauble (Body)", "Bauble (Charm)"])),
    "ru_ru": dict(zip("AMULET RING BELT TRINKET HEAD BODY CHARM".split(),
                      ["Бижутерия (Амулет)", "Бижутерия (Кольцо)", "Бижутерия (Пояс)", "Бижутерия (Любая)",
                       "Бижутерия (Голова)", "Бижутерия (Тело)", "Бижутерия (Брелок)"])),
}


def canonical(source):
    if source.startswith("minecraft:thaumcraft:"):
        source = source[10:]
    parts = source.split(";", 3)
    metadata = 0 if len(parts) < 3 or parts[2] == "32767" else int(parts[2])
    return parts[0] + ";1;" + str(metadata) + (";" + parts[3] if len(parts) == 4 else "")


def referenced_sources(ore_catalog):
    sources, ores = set(), set()

    def add(source):
        if source.startswith(("thaumcraft:", "minecraft:thaumcraft:")):
            sources.add(source)
        elif source.startswith("oredict:"):
            ores.add(source[8:].split(";")[0])

    def visit(value):
        if isinstance(value, dict):
            for key, member in value.items():
                if key in ("legacy", "item", "id") and isinstance(member, str):
                    add(member)
                elif key in ("required_item", "required_craft"):
                    for source in member:
                        if isinstance(source, str):
                            add(source)
                visit(member)
        elif isinstance(value, list):
            for member in value:
                visit(member)

    visit(json.loads((RES / "thaumcraft-port/book-recipes.json").read_text()))
    for path in sorted((RES / "assets/thaumcraft/research").glob("*.json")):
        visit(json.loads(path.read_text()))
    if ore_catalog:
        ore_values = json.loads(ore_catalog.read_text())["ores"]
        for ore in ores:
            for stack in ore_values.get(ore, []):
                if stack["item"].startswith("thaumcraft:"):
                    sources.add(stack["item"] + ";1;" + str(stack["metadata"])
                                + (";" + stack["nbt"] if "nbt" in stack else ""))
    return sorted(set(map(canonical, sources)))


def original_stack(source):
    parts = source.split(";", 3)
    item, metadata = parts[0], int(parts[2])
    nbt = parts[3] if len(parts) == 4 else ""
    factory = {}
    # Reverse the port's flattened TC item ids; original ConfigItems variant order.
    for family, variants in VARIANTS.items():
        for index, variant in enumerate(variants):
            if item == "thaumcraft:" + family + "_" + variant:
                item, metadata = "thaumcraft:" + family, index
    if item == "thaumcraft:seals":
        item, factory, nbt = "thaumcraft:seal", {"seal": nbt}, ""
    if item == "thaumcraft:bucket_death":
        item, factory = "forge:bucketfilled", {"fluid": "liquid_death"}
    item = {
        "thaumcraft:leather": "minecraft:leather",
        "thaumcraft:nitor_light_gray": "thaumcraft:nitor_silver",
        "thaumcraft:arcane_stone": "thaumcraft:stone_arcane",
        "thaumcraft:arcane_brick": "thaumcraft:stone_arcane_brick",
    }.get(item, item)
    if item == "thaumcraft:metal" and metadata in (2, 3):
        item, metadata = {2: "thaumcraft:metal_brass", 3: "thaumcraft:metal_thaumium"}[metadata], 0
    if item == "thaumcraft:nitor":
        item, metadata = "thaumcraft:nitor_" + COLORS[metadata], 0
    if item in ("thaumcraft:banner_light_blue", "thaumcraft:candle_light_blue", "thaumcraft:nitor_light_blue"):
        item = item.replace("_light_blue", "_lightblue")
    # The port accepts SNBT single quotes; 1.12 JsonToNBT only accepts double quotes.
    nbt = re.sub(r"'([^']*)'", lambda match: json.dumps(match.group(1)), nbt)
    result = {"id": source, "item": item, "count": 1, "metadata": metadata, **factory}
    if nbt:
        result["nbt"] = nbt
    return result


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def import_snapshots(corpus_path, en_path, ru_path):
    corpus = json.loads(corpus_path.read_text())
    expected = {case["id"] for case in corpus}
    snapshots = {}
    for language, path in (("en_us", en_path), ("ru_ru", ru_path)):
        raw = json.loads(path.read_text())
        answers = {answer["id"]: answer for answer in raw["answers"]}
        if raw.get("language") != language or len(answers) != len(raw["answers"]):
            raise ValueError(f"Wrong language or duplicate original answers in {path}")
        missing = expected - answers.keys()
        invalid = [key for key in expected & answers.keys()
                   if answers[key].get("error") or not answers[key].get("tooltip")
                   or not answers[key].get("tooltip_advanced")
                   or answers[key].get("resolved_item") == "minecraft:air"
                   or 'id:"minecraft:air"' in answers[key].get("serialized_stack", "")]
        if missing or invalid:
            raise ValueError(f"{language}: missing={sorted(missing)}, invalid={sorted(invalid)}")
        snapshots[language] = answers
    entries, advanced_entries = {}, {}
    translations = {language: {} for language in snapshots}
    for source in sorted(expected):
        digest = hashlib.sha256(source.encode()).hexdigest()[:20]
        modes = [("tooltip", entries, ""), ("tooltip_advanced", advanced_entries, ".advanced")]
        for field, target, suffix in modes:
            en_lines, ru_lines = (snapshots[language][source][field] for language in ("en_us", "ru_ru"))
            if len(en_lines) != len(ru_lines):
                raise ValueError(f"Different locale line counts: {source} {field}: {len(en_lines)} / {len(ru_lines)}")
            keys = [PREFIX + digest + suffix + "." + str(index) for index in range(len(en_lines))]
            target[source] = keys
            for language, lines in (("en_us", en_lines), ("ru_ru", ru_lines)):
                translations[language].update(zip(keys, lines))
    for language, new_values in translations.items():
        path = RES / "assets/thaumcraft/lang" / (language + ".json")
        current = json.loads(path.read_text())
        current = {key: value for key, value in current.items() if not key.startswith(PREFIX)}
        current.update({"name." + key: value for key, value in BAUBLES_TRANSLATIONS[language].items()})
        current.update(new_values)
        write_json(path, current)
    write_json(RES / "thaumcraft-port/book-item-tooltips.json", {
        "source": "Minecraft 1.12.2 / Forge 14.23.5.2860 / Thaumcraft 6.1.BETA26 / Baubles 1.5.2",
        "method": "Actual ItemStack.getTooltip(client player, NORMAL/ADVANCED), EN/RU, book presentation only",
        "normalization": "Count=1; wildcard damage 32767 begins at 0; recipe SNBT preserved",
        "entries": entries,
        "advanced_entries": advanced_entries,
    })
    evidence = ROOT / "verification/development-thaumonomicon-parity/item-tooltips"
    write_json(evidence / "corpus.json", corpus)
    for language, path in (("en_us", en_path), ("ru_ru", ru_path)):
        write_json(evidence / (language + "-runtime.json"), json.loads(path.read_text()))
    print(json.dumps({"descriptors": len(entries), "advanced_descriptors": len(advanced_entries),
                      "lines_per_language": len(translations["en_us"]), "unresolved": 0}))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    prepare = subparsers.add_parser("prepare")
    prepare.add_argument("--ore-catalog", type=Path)
    prepare.add_argument("--output", type=Path, required=True)
    collect = subparsers.add_parser("import")
    collect.add_argument("--corpus", type=Path, required=True)
    collect.add_argument("--en", type=Path, required=True)
    collect.add_argument("--ru", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "prepare":
        corpus = [original_stack(source) for source in referenced_sources(args.ore_catalog)]
        write_json(args.output, corpus)
        print(json.dumps({"descriptors": len(corpus), "output": str(args.output)}))
    else:
        import_snapshots(args.corpus, args.en, args.ru)


if __name__ == "__main__":
    main()
