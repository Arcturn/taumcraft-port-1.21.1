# Original item tooltip oracle

These files capture the original Minecraft 1.12.2 client with Forge 14.23.5.2860,
Thaumcraft 6.1.BETA26 and Baubles 1.5.2. The verification addon calls
`ItemStack.getTooltip(actual client player, NORMAL/ADVANCED)` in an entered world.
The raw arrays preserve line order, blank lines, formatting codes, attribute
values, metadata-specific names, NBT details and global event-added lines.
No GUI rarity color is prepended: `GuiResearchPage.drawStackAt` also uses these
raw arrays. The current GUI applies its own first/subsequent-line base colors.

`corpus.json` contains 451 canonical book descriptors. Coverage includes every
TC stack in the recipe catalog, stage item/craft requirements, and the TC
variants of the OreIngredients referenced by recipes. Counts are normalized to
one; wildcard damage 32767 starts at zero, as in the original recipe display.
Recipe-specific SNBT remains part of the key, including all six infusion
modifications. Original item metadata is retained. Port aliases are explicitly
converted back to original registry names; seals and Liquid Death buckets use
the original factory methods. No missing registry id is accepted as AIR.

Important reverse conversions are recorded in `tools/book_tooltip_catalog.py`:
flattened TC material ids, `*_light_blue` to the actual original `*_lightblue`,
and single-quoted port SNBT strings to double-quoted 1.12 SNBT. These conversions
change the sampled original stack, not the canonical descriptor used by the port.

The importer creates `thaumcraft-port/book-item-tooltips.json` plus stable
`book.original.tooltip.*` language keys for EN/RU. Resource packs can override
those keys normally. Other languages use Minecraft's English fallback.
The catalog is used only by the book; it does not register items or assign
equipment/gameplay statistics. Unknown future descriptors retain the existing
metadata/NBT helper fallback and require a new original capture for full parity.

To reproduce the corpus:

```sh
python tools/book_tooltip_catalog.py prepare --ore-catalog /path/to/original-oreingredient-expanded-runtime.json --output /path/to/item-tooltip-full-corpus.json
```

After the verification addon has captured both languages:

```sh
python tools/book_tooltip_catalog.py import --corpus /path/to/item-tooltip-full-corpus.json --en /path/to/original-item-tooltips-full-en_us-runtime.json --ru /path/to/original-item-tooltips-full-ru_ru-runtime.json
```

`BookItemTooltipCatalogTest` compares every imported line to the runtime oracle,
checks complete recipe/stage coverage and preserves independently specified
armor, weapon and Baubles examples. `BookItemPresentationTest` checks fallback
metadata names, styles and the six modified-result/unmodified-input pairs.

## Runtime context and limits of this evidence

- These are the original book stacks' display values. They do not certify or
  change the physical port items' armor, damage, durability or accessory behavior.
- ADVANCED text intentionally retains the reference 1.12.2 profile's numeric
  registry IDs, registry strings and NBT counts. Those numbers are not NeoForge
  1.21 registry IDs and can differ in another original modpack/load order.
- Wildcard damage is captured at its initial value zero. The snapshots do not
  enumerate every durability value. Arbitrary player-renamed, enchanted,
  charged, linked-mirror or configured-focus stacks outside the recorded book
  descriptors are not covered by the full original oracle.
- Weapon attribute strings were captured with the original test player's
  baseline attributes. Command-modified base attack damage/speed were not
  independently sampled. No additional Shift-dependent tooltip branch was
  found in the audited TC item tooltip methods; this does not assert parity for
  other mods' tooltip events.
- The one confirmed TC player-dependent case is handled dynamically:
  `ItemVoidseerCharm` lines 64-76 computes Vis discount from permanent warp and
  Warping as one fifth of that discount. The book reads synchronized
  `ClientJournal.state.permanentWarp()` on hover and inserts the conditional
  original lines before the Baubles slot. `TC.WARP` NBT is summed once, as in
  `PlayerEvents.getFinalWarp`. Tests cover threshold changes and both tooltip
  modes. The captured zero-discount baseline is never mutated.
- EN/RU strings are compared against runtime. Other languages receive English
  fallback until translated by the mod or a resource pack. The string oracle
  alone is not a screenshot/pixel comparison of fonts and tooltip layout.
