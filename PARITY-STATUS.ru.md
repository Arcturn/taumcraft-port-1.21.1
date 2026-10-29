# Реестр переноса TC6: alpha.9.8

Источник — Thaumcraft 6.1.BETA26. Это учёт объектов и обнаруженных пропусков, не процент готовности.

**Ни одна строка не объявлена совпадающей 1 в 1 в игре.** Зарегистрированный предмет может не иметь рецепта, полной механики или источника в выживании.

Столбец «Сопоставлено» означает наличие ID в реальном datagen-снимке либо определения в каталоге. Для теорий это не доступность карты/помощника.

| Категория | Исходных записей | Сопоставлено |
|---|---:|---:|
| aspect | 37 | 37 |
| block | 200 | 83 |
| block_entity_type | 48 | 18 |
| entity_type | 43 | 10 |
| fluid | 3 | 0 |
| focus | 21 | 0 |
| golem_addon | 4 | 0 |
| golem_arm | 5 | 0 |
| golem_head | 5 | 0 |
| golem_leg | 4 | 0 |
| golem_material | 6 | 0 |
| golem_trait | 19 | 0 |
| item | 108 | 29 |
| mob_effect | 9 | 3 |
| recipe_call | 302 | 0 |
| research | 148 | 148 |
| seal | 16 | 0 |
| setting | 43 | 0 |
| sound_event | 65 | 65 |
| theory_aid | 14 | 14 |
| theory_card | 33 | 33 |
| worldgen/biome | 3 | 1 |

## Границы подсчёта

- 200 блоков включают 48 цветных блоков, служебные блоки и отдельные двойные плиты 1.12. Их предметы не повторяются среди 108 самостоятельных предметов.
- 302 recipe_call — места регистрации с сохранёнными условиями и циклами, а не число рецептов. Отдельно требуется сверить развёрнутые и динамические рецепты.
- В порте 292 ID предметов: 157 служебных значков и 135 остальных. Это не 292 перенесённых игровых предмета.
- Учитываются все три зарегистрированных биома TC6. Регистрация Eldritch не доказывает существование доступного генератора отдельного измерения.
- Медиа: 918 исходных файлов; 890 найдены побайтно. Преобразованные JPG/MTL требуют своих проверок; отсутствие одинаковых байтов не равно отсутствию визуала.
- Строки вариантов с component_behavior_audit_required требуют проверки компонентов/NBT. Простой совпавший ID не доказывает правильность варианта.
- Все публичные API, события, частицы и мировые алгоритмы остаются предметом проверки по сценариям; очереди исходников сохранены в JSON.

## Предметные требования, которые сейчас блокируются

| Исследование | Этап | Условие | Исходный предмет |
|---|---:|---|---|
| UNLOCKAUROMANCY | 2 | required_craft | `thaumcraft:caster_basic` |
| BASEAUROMANCY | 1 | required_craft | `thaumcraft:focus_1` |
| BASEAUROMANCY | 2 | required_craft | `thaumcraft:wand_workbench` |
| FOCUSADVANCED | 1 | required_craft | `thaumcraft:focus_2` |
| FOCUSGREATER | 1 | required_craft | `thaumcraft:focus_3` |
| MIRROR | 1 | required_item | `thaumcraft:mirrored_glass` |
| GRAPPLEGUN | 1 | required_item | `thaumcraft:nugget;1;10` |
| INFUSION | 2 | required_craft | `thaumcraft:infusion_matrix` |
| INFUSIONANCIENT | 1 | required_item | `thaumcraft:stone_ancient` |
| INFUSIONELDRITCH | 1 | required_item | `thaumcraft:stone_eldritch_tile` |
| ELEMENTALTOOLS | 1 | required_craft | `thaumcraft:thaumium_axe` |
| ELEMENTALTOOLS | 1 | required_craft | `thaumcraft:thaumium_sword` |
| ELEMENTALTOOLS | 1 | required_craft | `thaumcraft:thaumium_pick` |
| ELEMENTALTOOLS | 1 | required_craft | `thaumcraft:thaumium_shovel` |
| ELEMENTALTOOLS | 1 | required_craft | `thaumcraft:thaumium_hoe` |

Поддержка остальных предметных требований не проверяет все специальные условия, знания, награды или способы получения.

## Пообъектное сопоставление

| Тип | Оригинал | Найдено в порте | Источник |
|---|---|---|---|
| aspect | `aer` | `aer` | `thaumcraft/api/aspects/Aspect.java:32` |
| aspect | `alienis` | `alienis` | `thaumcraft/api/aspects/Aspect.java:53` |
| aspect | `alkimia` | `alkimia` | `thaumcraft/api/aspects/Aspect.java:50` |
| aspect | `aqua` | `aqua` | `thaumcraft/api/aspects/Aspect.java:35` |
| aspect | `auram` | `auram` | `thaumcraft/api/aspects/Aspect.java:49` |
| aspect | `aversio` | `aversio` | `thaumcraft/api/aspects/Aspect.java:63` |
| aspect | `bestia` | `bestia` | `thaumcraft/api/aspects/Aspect.java:67` |
| aspect | `cognitio` | `cognitio` | `thaumcraft/api/aspects/Aspect.java:61` |
| aspect | `desiderium` | `desiderium` | `thaumcraft/api/aspects/Aspect.java:65` |
| aspect | `exanimis` | `exanimis` | `thaumcraft/api/aspects/Aspect.java:66` |
| aspect | `fabrico` | `fabrico` | `thaumcraft/api/aspects/Aspect.java:57` |
| aspect | `gelum` | `gelum` | `thaumcraft/api/aspects/Aspect.java:41` |
| aspect | `herba` | `herba` | `thaumcraft/api/aspects/Aspect.java:55` |
| aspect | `humanus` | `humanus` | `thaumcraft/api/aspects/Aspect.java:68` |
| aspect | `ignis` | `ignis` | `thaumcraft/api/aspects/Aspect.java:34` |
| aspect | `instrumentum` | `instrumentum` | `thaumcraft/api/aspects/Aspect.java:56` |
| aspect | `lux` | `lux` | `thaumcraft/api/aspects/Aspect.java:39` |
| aspect | `machina` | `machina` | `thaumcraft/api/aspects/Aspect.java:58` |
| aspect | `metallum` | `metallum` | `thaumcraft/api/aspects/Aspect.java:43` |
| aspect | `mortuus` | `mortuus` | `thaumcraft/api/aspects/Aspect.java:45` |
| aspect | `motus` | `motus` | `thaumcraft/api/aspects/Aspect.java:40` |
| aspect | `ordo` | `ordo` | `thaumcraft/api/aspects/Aspect.java:36` |
| aspect | `perditio` | `perditio` | `thaumcraft/api/aspects/Aspect.java:37` |
| aspect | `permutatio` | `permutatio` | `thaumcraft/api/aspects/Aspect.java:47` |
| aspect | `potentia` | `potentia` | `thaumcraft/api/aspects/Aspect.java:46` |
| aspect | `praecantatio` | `praecantatio` | `thaumcraft/api/aspects/Aspect.java:48` |
| aspect | `praemunio` | `praemunio` | `thaumcraft/api/aspects/Aspect.java:64` |
| aspect | `sensus` | `sensus` | `thaumcraft/api/aspects/Aspect.java:62` |
| aspect | `spiritus` | `spiritus` | `thaumcraft/api/aspects/Aspect.java:60` |
| aspect | `tenebrae` | `tenebrae` | `thaumcraft/api/aspects/Aspect.java:52` |
| aspect | `terra` | `terra` | `thaumcraft/api/aspects/Aspect.java:33` |
| aspect | `vacuos` | `vacuos` | `thaumcraft/api/aspects/Aspect.java:38` |
| aspect | `victus` | `victus` | `thaumcraft/api/aspects/Aspect.java:44` |
| aspect | `vinculum` | `vinculum` | `thaumcraft/api/aspects/Aspect.java:59` |
| aspect | `vitium` | `vitium` | `thaumcraft/api/aspects/Aspect.java:51` |
| aspect | `vitreus` | `vitreus` | `thaumcraft/api/aspects/Aspect.java:42` |
| aspect | `volatus` | `volatus` | `thaumcraft/api/aspects/Aspect.java:54` |
| block | `thaumcraft:activator_rail` |  | `thaumcraft/common/config/ConfigBlocks.java:364` |
| block | `thaumcraft:alembic` | `thaumcraft:alembic` | `thaumcraft/common/config/ConfigBlocks.java:336` |
| block | `thaumcraft:amber_block` | `thaumcraft:amber_block` | `thaumcraft/common/config/ConfigBlocks.java:267` |
| block | `thaumcraft:amber_brick` | `thaumcraft:amber_brick` | `thaumcraft/common/config/ConfigBlocks.java:268` |
| block | `thaumcraft:arcane_ear` |  | `thaumcraft/common/config/ConfigBlocks.java:323` |
| block | `thaumcraft:arcane_ear_toggle` |  | `thaumcraft/common/config/ConfigBlocks.java:324` |
| block | `thaumcraft:arcane_workbench` | `thaumcraft:arcane_workbench` | `thaumcraft/common/config/ConfigBlocks.java:318` |
| block | `thaumcraft:arcane_workbench_charger` |  | `thaumcraft/common/config/ConfigBlocks.java:319` |
| block | `thaumcraft:banner_black` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_blue` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_brown` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_crimson_cult` |  | `thaumcraft/common/config/ConfigBlocks.java:310` |
| block | `thaumcraft:banner_cyan` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_gray` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_green` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_lightblue` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_lime` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_magenta` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_orange` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_pink` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_purple` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_red` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_silver` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_white` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:banner_yellow` |  | `thaumcraft/common/config/ConfigBlocks.java:305` |
| block | `thaumcraft:barrier` |  | `thaumcraft/common/config/ConfigBlocks.java:393` |
| block | `thaumcraft:bellows` | `thaumcraft:bellows` | `thaumcraft/common/config/ConfigBlocks.java:330` |
| block | `thaumcraft:brain_box` | `thaumcraft:brain_box` | `thaumcraft/common/config/ConfigBlocks.java:354` |
| block | `thaumcraft:candle_black` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_blue` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_brown` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_cyan` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_gray` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_green` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_lightblue` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_lime` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_magenta` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_orange` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_pink` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_purple` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_red` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_silver` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_white` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:candle_yellow` |  | `thaumcraft/common/config/ConfigBlocks.java:302` |
| block | `thaumcraft:centrifuge` | `thaumcraft:centrifuge` | `thaumcraft/common/config/ConfigBlocks.java:329` |
| block | `thaumcraft:cinderpearl` | `thaumcraft:cinderpearl` | `thaumcraft/common/config/ConfigBlocks.java:251` |
| block | `thaumcraft:condenser` |  | `thaumcraft/common/config/ConfigBlocks.java:367` |
| block | `thaumcraft:condenser_lattice` |  | `thaumcraft/common/config/ConfigBlocks.java:368` |
| block | `thaumcraft:condenser_lattice_dirty` |  | `thaumcraft/common/config/ConfigBlocks.java:369` |
| block | `thaumcraft:crucible` | `thaumcraft:crucible` | `thaumcraft/common/config/ConfigBlocks.java:322` |
| block | `thaumcraft:crystal_aer` | `thaumcraft:crystal_aer` | `thaumcraft/common/config/ConfigBlocks.java:198` |
| block | `thaumcraft:crystal_aqua` | `thaumcraft:crystal_aqua` | `thaumcraft/common/config/ConfigBlocks.java:200` |
| block | `thaumcraft:crystal_ignis` | `thaumcraft:crystal_ignis` | `thaumcraft/common/config/ConfigBlocks.java:199` |
| block | `thaumcraft:crystal_ordo` | `thaumcraft:crystal_ordo` | `thaumcraft/common/config/ConfigBlocks.java:202` |
| block | `thaumcraft:crystal_perditio` | `thaumcraft:crystal_perditio` | `thaumcraft/common/config/ConfigBlocks.java:203` |
| block | `thaumcraft:crystal_terra` | `thaumcraft:crystal_terra` | `thaumcraft/common/config/ConfigBlocks.java:201` |
| block | `thaumcraft:crystal_vitium` | `thaumcraft:crystal_vitium` | `thaumcraft/common/config/ConfigBlocks.java:204` |
| block | `thaumcraft:dioptra` |  | `thaumcraft/common/config/ConfigBlocks.java:320` |
| block | `thaumcraft:effect_glimmer` |  | `thaumcraft/common/config/ConfigBlocks.java:385` |
| block | `thaumcraft:effect_sap` |  | `thaumcraft/common/config/ConfigBlocks.java:384` |
| block | `thaumcraft:effect_shock` |  | `thaumcraft/common/config/ConfigBlocks.java:383` |
| block | `thaumcraft:empty` |  | `thaumcraft/common/config/ConfigBlocks.java:392` |
| block | `thaumcraft:essentia_input` |  | `thaumcraft/common/config/ConfigBlocks.java:359` |
| block | `thaumcraft:essentia_output` |  | `thaumcraft/common/config/ConfigBlocks.java:360` |
| block | `thaumcraft:everfull_urn` |  | `thaumcraft/common/config/ConfigBlocks.java:351` |
| block | `thaumcraft:flesh_block` |  | `thaumcraft/common/config/ConfigBlocks.java:269` |
| block | `thaumcraft:flux_goo` | `thaumcraft:flux_goo` | `thaumcraft/common/config/ConfigBlocks.java:372` |
| block | `thaumcraft:golem_builder` |  | `thaumcraft/common/config/ConfigBlocks.java:356` |
| block | `thaumcraft:grass_ambient` | `thaumcraft:grass_ambient` | `thaumcraft/common/config/ConfigBlocks.java:283` |
| block | `thaumcraft:hole` |  | `thaumcraft/common/config/ConfigBlocks.java:382` |
| block | `thaumcraft:hungry_chest` |  | `thaumcraft/common/config/ConfigBlocks.java:339` |
| block | `thaumcraft:infernal_furnace` |  | `thaumcraft/common/config/ConfigBlocks.java:350` |
| block | `thaumcraft:infusion_matrix` |  | `thaumcraft/common/config/ConfigBlocks.java:349` |
| block | `thaumcraft:inlay` |  | `thaumcraft/common/config/ConfigBlocks.java:317` |
| block | `thaumcraft:jar_brain` |  | `thaumcraft/common/config/ConfigBlocks.java:348` |
| block | `thaumcraft:jar_normal` | `thaumcraft:jar_normal` | `thaumcraft/common/config/ConfigBlocks.java:346` |
| block | `thaumcraft:jar_void` | `thaumcraft:jar_void` | `thaumcraft/common/config/ConfigBlocks.java:347` |
| block | `thaumcraft:lamp_arcane` |  | `thaumcraft/common/config/ConfigBlocks.java:325` |
| block | `thaumcraft:lamp_fertility` |  | `thaumcraft/common/config/ConfigBlocks.java:326` |
| block | `thaumcraft:lamp_growth` |  | `thaumcraft/common/config/ConfigBlocks.java:327` |
| block | `thaumcraft:leaves_greatwood` | `thaumcraft:leaves_greatwood` | `thaumcraft/common/config/ConfigBlocks.java:248` |
| block | `thaumcraft:leaves_silverwood` | `thaumcraft:leaves_silverwood` | `thaumcraft/common/config/ConfigBlocks.java:249` |
| block | `thaumcraft:levitator` |  | `thaumcraft/common/config/ConfigBlocks.java:328` |
| block | `thaumcraft:liquid_death` |  | `thaumcraft/common/config/ConfigBlocks.java:376` |
| block | `thaumcraft:log_greatwood` | `thaumcraft:log_greatwood` | `thaumcraft/common/config/ConfigBlocks.java:246` |
| block | `thaumcraft:log_silverwood` | `thaumcraft:log_silverwood` | `thaumcraft/common/config/ConfigBlocks.java:247` |
| block | `thaumcraft:loot_crate_common` | `thaumcraft:loot_crate_common` | `thaumcraft/common/config/ConfigBlocks.java:270` |
| block | `thaumcraft:loot_crate_rare` | `thaumcraft:loot_crate_rare` | `thaumcraft/common/config/ConfigBlocks.java:272` |
| block | `thaumcraft:loot_crate_uncommon` | `thaumcraft:loot_crate_uncommon` | `thaumcraft/common/config/ConfigBlocks.java:271` |
| block | `thaumcraft:loot_urn_common` | `thaumcraft:loot_urn_common` | `thaumcraft/common/config/ConfigBlocks.java:273` |
| block | `thaumcraft:loot_urn_rare` | `thaumcraft:loot_urn_rare` | `thaumcraft/common/config/ConfigBlocks.java:275` |
| block | `thaumcraft:loot_urn_uncommon` | `thaumcraft:loot_urn_uncommon` | `thaumcraft/common/config/ConfigBlocks.java:274` |
| block | `thaumcraft:matrix_cost` |  | `thaumcraft/common/config/ConfigBlocks.java:300` |
| block | `thaumcraft:matrix_speed` |  | `thaumcraft/common/config/ConfigBlocks.java:299` |
| block | `thaumcraft:metal_alchemical` | `thaumcraft:metal_alchemical` | `thaumcraft/common/config/ConfigBlocks.java:292` |
| block | `thaumcraft:metal_alchemical_advanced` | `thaumcraft:metal_alchemical_advanced` | `thaumcraft/common/config/ConfigBlocks.java:293` |
| block | `thaumcraft:metal_brass` | `thaumcraft:metal_brass` | `thaumcraft/common/config/ConfigBlocks.java:289` |
| block | `thaumcraft:metal_thaumium` | `thaumcraft:metal_thaumium` | `thaumcraft/common/config/ConfigBlocks.java:290` |
| block | `thaumcraft:metal_void` | `thaumcraft:metal_void` | `thaumcraft/common/config/ConfigBlocks.java:291` |
| block | `thaumcraft:mirror` |  | `thaumcraft/common/config/ConfigBlocks.java:357` |
| block | `thaumcraft:mirror_essentia` |  | `thaumcraft/common/config/ConfigBlocks.java:358` |
| block | `thaumcraft:nitor_black` | `thaumcraft:nitor_black` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_blue` | `thaumcraft:nitor_blue` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_brown` | `thaumcraft:nitor_brown` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_cyan` | `thaumcraft:nitor_cyan` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_gray` | `thaumcraft:nitor_gray` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_green` | `thaumcraft:nitor_green` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_lightblue` | `thaumcraft:nitor_light_blue` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_lime` | `thaumcraft:nitor_lime` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_magenta` | `thaumcraft:nitor_magenta` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_orange` | `thaumcraft:nitor_orange` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_pink` | `thaumcraft:nitor_pink` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_purple` | `thaumcraft:nitor_purple` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_red` | `thaumcraft:nitor_red` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_silver` | `thaumcraft:nitor_light_gray` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_white` | `thaumcraft:nitor_white` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:nitor_yellow` | `thaumcraft:nitor_yellow` | `thaumcraft/common/config/ConfigBlocks.java:314` |
| block | `thaumcraft:ore_amber` | `thaumcraft:ore_amber` | `thaumcraft/common/config/ConfigBlocks.java:195` |
| block | `thaumcraft:ore_cinnabar` | `thaumcraft:ore_cinnabar` | `thaumcraft/common/config/ConfigBlocks.java:196` |
| block | `thaumcraft:ore_quartz` | `thaumcraft:ore_quartz` | `thaumcraft/common/config/ConfigBlocks.java:197` |
| block | `thaumcraft:pattern_crafter` |  | `thaumcraft/common/config/ConfigBlocks.java:362` |
| block | `thaumcraft:paving_stone_barrier` |  | `thaumcraft/common/config/ConfigBlocks.java:295` |
| block | `thaumcraft:paving_stone_travel` |  | `thaumcraft/common/config/ConfigBlocks.java:294` |
| block | `thaumcraft:pedestal_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:287` |
| block | `thaumcraft:pedestal_arcane` |  | `thaumcraft/common/config/ConfigBlocks.java:286` |
| block | `thaumcraft:pedestal_eldritch` |  | `thaumcraft/common/config/ConfigBlocks.java:288` |
| block | `thaumcraft:pillar_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:297` |
| block | `thaumcraft:pillar_arcane` |  | `thaumcraft/common/config/ConfigBlocks.java:296` |
| block | `thaumcraft:pillar_eldritch` |  | `thaumcraft/common/config/ConfigBlocks.java:298` |
| block | `thaumcraft:placeholder_anvil` |  | `thaumcraft/common/config/ConfigBlocks.java:389` |
| block | `thaumcraft:placeholder_bars` |  | `thaumcraft/common/config/ConfigBlocks.java:388` |
| block | `thaumcraft:placeholder_brick` |  | `thaumcraft/common/config/ConfigBlocks.java:386` |
| block | `thaumcraft:placeholder_cauldron` |  | `thaumcraft/common/config/ConfigBlocks.java:390` |
| block | `thaumcraft:placeholder_obsidian` |  | `thaumcraft/common/config/ConfigBlocks.java:387` |
| block | `thaumcraft:placeholder_table` |  | `thaumcraft/common/config/ConfigBlocks.java:391` |
| block | `thaumcraft:plank_greatwood` | `thaumcraft:plank_greatwood` | `thaumcraft/common/config/ConfigBlocks.java:253` |
| block | `thaumcraft:plank_silverwood` | `thaumcraft:plank_silverwood` | `thaumcraft/common/config/ConfigBlocks.java:254` |
| block | `thaumcraft:potion_sprayer` |  | `thaumcraft/common/config/ConfigBlocks.java:363` |
| block | `thaumcraft:purifying_fluid` |  | `thaumcraft/common/config/ConfigBlocks.java:380` |
| block | `thaumcraft:recharge_pedestal` |  | `thaumcraft/common/config/ConfigBlocks.java:337` |
| block | `thaumcraft:redstone_relay` |  | `thaumcraft/common/config/ConfigBlocks.java:361` |
| block | `thaumcraft:research_table` | `thaumcraft:research_table` | `thaumcraft/common/config/ConfigBlocks.java:321` |
| block | `thaumcraft:sapling_greatwood` | `thaumcraft:sapling_greatwood` | `thaumcraft/common/config/ConfigBlocks.java:244` |
| block | `thaumcraft:sapling_silverwood` | `thaumcraft:sapling_silverwood` | `thaumcraft/common/config/ConfigBlocks.java:245` |
| block | `thaumcraft:shimmerleaf` | `thaumcraft:shimmerleaf` | `thaumcraft/common/config/ConfigBlocks.java:250` |
| block | `thaumcraft:slab_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:228` |
| block | `thaumcraft:slab_arcane_brick` |  | `thaumcraft/common/config/ConfigBlocks.java:226` |
| block | `thaumcraft:slab_arcane_stone` |  | `thaumcraft/common/config/ConfigBlocks.java:224` |
| block | `thaumcraft:slab_double_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:229` |
| block | `thaumcraft:slab_double_arcane_brick` |  | `thaumcraft/common/config/ConfigBlocks.java:227` |
| block | `thaumcraft:slab_double_arcane_stone` |  | `thaumcraft/common/config/ConfigBlocks.java:225` |
| block | `thaumcraft:slab_double_eldritch` |  | `thaumcraft/common/config/ConfigBlocks.java:231` |
| block | `thaumcraft:slab_double_greatwood` |  | `thaumcraft/common/config/ConfigBlocks.java:258` |
| block | `thaumcraft:slab_double_silverwood` |  | `thaumcraft/common/config/ConfigBlocks.java:260` |
| block | `thaumcraft:slab_eldritch` |  | `thaumcraft/common/config/ConfigBlocks.java:230` |
| block | `thaumcraft:slab_greatwood` |  | `thaumcraft/common/config/ConfigBlocks.java:257` |
| block | `thaumcraft:slab_silverwood` |  | `thaumcraft/common/config/ConfigBlocks.java:259` |
| block | `thaumcraft:smelter_aux` | `thaumcraft:smelter_aux` | `thaumcraft/common/config/ConfigBlocks.java:334` |
| block | `thaumcraft:smelter_basic` | `thaumcraft:smelter_basic` | `thaumcraft/common/config/ConfigBlocks.java:331` |
| block | `thaumcraft:smelter_thaumium` | `thaumcraft:smelter_thaumium` | `thaumcraft/common/config/ConfigBlocks.java:332` |
| block | `thaumcraft:smelter_vent` | `thaumcraft:smelter_vent` | `thaumcraft/common/config/ConfigBlocks.java:335` |
| block | `thaumcraft:smelter_void` | `thaumcraft:smelter_void` | `thaumcraft/common/config/ConfigBlocks.java:333` |
| block | `thaumcraft:spa` |  | `thaumcraft/common/config/ConfigBlocks.java:355` |
| block | `thaumcraft:stabilizer` |  | `thaumcraft/common/config/ConfigBlocks.java:365` |
| block | `thaumcraft:stairs_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:223` |
| block | `thaumcraft:stairs_arcane` |  | `thaumcraft/common/config/ConfigBlocks.java:221` |
| block | `thaumcraft:stairs_arcane_brick` |  | `thaumcraft/common/config/ConfigBlocks.java:222` |
| block | `thaumcraft:stairs_greatwood` |  | `thaumcraft/common/config/ConfigBlocks.java:255` |
| block | `thaumcraft:stairs_silverwood` |  | `thaumcraft/common/config/ConfigBlocks.java:256` |
| block | `thaumcraft:stone_ancient` |  | `thaumcraft/common/config/ConfigBlocks.java:214` |
| block | `thaumcraft:stone_ancient_doorway` |  | `thaumcraft/common/config/ConfigBlocks.java:218` |
| block | `thaumcraft:stone_ancient_glyphed` |  | `thaumcraft/common/config/ConfigBlocks.java:217` |
| block | `thaumcraft:stone_ancient_rock` |  | `thaumcraft/common/config/ConfigBlocks.java:216` |
| block | `thaumcraft:stone_ancient_tile` |  | `thaumcraft/common/config/ConfigBlocks.java:215` |
| block | `thaumcraft:stone_arcane` | `thaumcraft:arcane_stone` | `thaumcraft/common/config/ConfigBlocks.java:212` |
| block | `thaumcraft:stone_arcane_brick` | `thaumcraft:arcane_brick` | `thaumcraft/common/config/ConfigBlocks.java:213` |
| block | `thaumcraft:stone_eldritch_tile` |  | `thaumcraft/common/config/ConfigBlocks.java:219` |
| block | `thaumcraft:stone_porous` | `thaumcraft:stone_porous` | `thaumcraft/common/config/ConfigBlocks.java:220` |
| block | `thaumcraft:table_stone` |  | `thaumcraft/common/config/ConfigBlocks.java:285` |
| block | `thaumcraft:table_wood` | `thaumcraft:table_wood` | `thaumcraft/common/config/ConfigBlocks.java:284` |
| block | `thaumcraft:taint_crust` | `thaumcraft:taint_crust` | `thaumcraft/common/config/ConfigBlocks.java:277` |
| block | `thaumcraft:taint_feature` |  | `thaumcraft/common/config/ConfigBlocks.java:281` |
| block | `thaumcraft:taint_fibre` | `thaumcraft:taint_fibre` | `thaumcraft/common/config/ConfigBlocks.java:276` |
| block | `thaumcraft:taint_geyser` |  | `thaumcraft/common/config/ConfigBlocks.java:280` |
| block | `thaumcraft:taint_log` | `thaumcraft:taint_log` | `thaumcraft/common/config/ConfigBlocks.java:282` |
| block | `thaumcraft:taint_rock` | `thaumcraft:taint_rock` | `thaumcraft/common/config/ConfigBlocks.java:279` |
| block | `thaumcraft:taint_soil` | `thaumcraft:taint_soil` | `thaumcraft/common/config/ConfigBlocks.java:278` |
| block | `thaumcraft:thaumatorium` | `thaumcraft:thaumatorium` | `thaumcraft/common/config/ConfigBlocks.java:352` |
| block | `thaumcraft:thaumatorium_top` | `thaumcraft:thaumatorium_top` | `thaumcraft/common/config/ConfigBlocks.java:353` |
| block | `thaumcraft:tube` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:340` |
| block | `thaumcraft:tube_buffer` | `thaumcraft:tube_buffer` | `thaumcraft/common/config/ConfigBlocks.java:345` |
| block | `thaumcraft:tube_filter` | `thaumcraft:tube_filter` | `thaumcraft/common/config/ConfigBlocks.java:344` |
| block | `thaumcraft:tube_oneway` | `thaumcraft:tube_oneway` | `thaumcraft/common/config/ConfigBlocks.java:343` |
| block | `thaumcraft:tube_restrict` | `thaumcraft:tube_restrict` | `thaumcraft/common/config/ConfigBlocks.java:342` |
| block | `thaumcraft:tube_valve` | `thaumcraft:tube_valve` | `thaumcraft/common/config/ConfigBlocks.java:341` |
| block | `thaumcraft:vis_battery` |  | `thaumcraft/common/config/ConfigBlocks.java:316` |
| block | `thaumcraft:vis_generator` |  | `thaumcraft/common/config/ConfigBlocks.java:366` |
| block | `thaumcraft:vishroom` | `thaumcraft:vishroom` | `thaumcraft/common/config/ConfigBlocks.java:252` |
| block | `thaumcraft:void_siphon` |  | `thaumcraft/common/config/ConfigBlocks.java:370` |
| block | `thaumcraft:wand_workbench` |  | `thaumcraft/common/config/ConfigBlocks.java:338` |
| block_entity_type | `thaumcraft:TileAlembic` | `thaumcraft:jar` | `thaumcraft/common/config/ConfigBlocks.java:420` |
| block_entity_type | `thaumcraft:TileArcaneEar` |  | `thaumcraft/common/config/ConfigBlocks.java:399` |
| block_entity_type | `thaumcraft:TileArcaneWorkbench` | `thaumcraft:arcane_workbench` | `thaumcraft/common/config/ConfigBlocks.java:397` |
| block_entity_type | `thaumcraft:TileBanner` |  | `thaumcraft/common/config/ConfigBlocks.java:442` |
| block_entity_type | `thaumcraft:TileBarrierStone` |  | `thaumcraft/common/config/ConfigBlocks.java:444` |
| block_entity_type | `thaumcraft:TileBellows` | `thaumcraft:bellows` | `thaumcraft/common/config/ConfigBlocks.java:418` |
| block_entity_type | `thaumcraft:TileCentrifuge` | `thaumcraft:centrifuge` | `thaumcraft/common/config/ConfigBlocks.java:414` |
| block_entity_type | `thaumcraft:TileChestHungry` |  | `thaumcraft/common/config/ConfigBlocks.java:413` |
| block_entity_type | `thaumcraft:TileCondenser` |  | `thaumcraft/common/config/ConfigBlocks.java:440` |
| block_entity_type | `thaumcraft:TileCrucible` | `thaumcraft:crucible` | `thaumcraft/common/config/ConfigBlocks.java:401` |
| block_entity_type | `thaumcraft:TileDioptra` |  | `thaumcraft/common/config/ConfigBlocks.java:398` |
| block_entity_type | `thaumcraft:TileEssentiaInput` |  | `thaumcraft/common/config/ConfigBlocks.java:434` |
| block_entity_type | `thaumcraft:TileEssentiaOutput` |  | `thaumcraft/common/config/ConfigBlocks.java:435` |
| block_entity_type | `thaumcraft:TileFocalManipulator` |  | `thaumcraft/common/config/ConfigBlocks.java:403` |
| block_entity_type | `thaumcraft:TileGolemBuilder` |  | `thaumcraft/common/config/ConfigBlocks.java:433` |
| block_entity_type | `thaumcraft:TileHole` |  | `thaumcraft/common/config/ConfigBlocks.java:443` |
| block_entity_type | `thaumcraft:TileInfernalFurnace` |  | `thaumcraft/common/config/ConfigBlocks.java:423` |
| block_entity_type | `thaumcraft:TileInfusionMatrix` |  | `thaumcraft/common/config/ConfigBlocks.java:421` |
| block_entity_type | `thaumcraft:TileJar` | `thaumcraft:jar` | `thaumcraft/common/config/ConfigBlocks.java:415` |
| block_entity_type | `thaumcraft:TileJarBrain` |  | `thaumcraft/common/config/ConfigBlocks.java:417` |
| block_entity_type | `thaumcraft:TileJarVoid` | `thaumcraft:jar` | `thaumcraft/common/config/ConfigBlocks.java:416` |
| block_entity_type | `thaumcraft:TileLampArcane` |  | `thaumcraft/common/config/ConfigBlocks.java:428` |
| block_entity_type | `thaumcraft:TileLampFertility` |  | `thaumcraft/common/config/ConfigBlocks.java:429` |
| block_entity_type | `thaumcraft:TileLampGrowth` |  | `thaumcraft/common/config/ConfigBlocks.java:427` |
| block_entity_type | `thaumcraft:TileLevitator` |  | `thaumcraft/common/config/ConfigBlocks.java:400` |
| block_entity_type | `thaumcraft:TileMirror` |  | `thaumcraft/common/config/ConfigBlocks.java:430` |
| block_entity_type | `thaumcraft:TileMirrorEssentia` |  | `thaumcraft/common/config/ConfigBlocks.java:431` |
| block_entity_type | `thaumcraft:TileNitor` | `thaumcraft:nitor` | `thaumcraft/common/config/ConfigBlocks.java:402` |
| block_entity_type | `thaumcraft:TilePatternCrafter` |  | `thaumcraft/common/config/ConfigBlocks.java:436` |
| block_entity_type | `thaumcraft:TilePedestal` |  | `thaumcraft/common/config/ConfigBlocks.java:404` |
| block_entity_type | `thaumcraft:TilePotionSprayer` |  | `thaumcraft/common/config/ConfigBlocks.java:437` |
| block_entity_type | `thaumcraft:TileRechargePedestal` |  | `thaumcraft/common/config/ConfigBlocks.java:405` |
| block_entity_type | `thaumcraft:TileRedstoneRelay` |  | `thaumcraft/common/config/ConfigBlocks.java:432` |
| block_entity_type | `thaumcraft:TileResearchTable` | `thaumcraft:research_table` | `thaumcraft/common/config/ConfigBlocks.java:406` |
| block_entity_type | `thaumcraft:TileSmelter` | `thaumcraft:smelter` | `thaumcraft/common/config/ConfigBlocks.java:419` |
| block_entity_type | `thaumcraft:TileSpa` |  | `thaumcraft/common/config/ConfigBlocks.java:426` |
| block_entity_type | `thaumcraft:TileStabilizer` |  | `thaumcraft/common/config/ConfigBlocks.java:439` |
| block_entity_type | `thaumcraft:TileThaumatorium` | `thaumcraft:thaumatorium` | `thaumcraft/common/config/ConfigBlocks.java:424` |
| block_entity_type | `thaumcraft:TileThaumatoriumTop` | `thaumcraft:thaumatorium` | `thaumcraft/common/config/ConfigBlocks.java:425` |
| block_entity_type | `thaumcraft:TileTube` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:407` |
| block_entity_type | `thaumcraft:TileTubeBuffer` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:412` |
| block_entity_type | `thaumcraft:TileTubeFilter` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:409` |
| block_entity_type | `thaumcraft:TileTubeOneway` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:411` |
| block_entity_type | `thaumcraft:TileTubeRestrict` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:410` |
| block_entity_type | `thaumcraft:TileTubeValve` | `thaumcraft:tube` | `thaumcraft/common/config/ConfigBlocks.java:408` |
| block_entity_type | `thaumcraft:TileVisGenerator` |  | `thaumcraft/common/config/ConfigBlocks.java:438` |
| block_entity_type | `thaumcraft:TileVoidSiphon` |  | `thaumcraft/common/config/ConfigBlocks.java:441` |
| block_entity_type | `thaumcraft:TileWaterJug` |  | `thaumcraft/common/config/ConfigBlocks.java:422` |
| entity_type | `thaumcraft:alumentum` | `thaumcraft:alumentum` | `thaumcraft/common/config/ConfigEntities.java:109` |
| entity_type | `thaumcraft:arcanebore` |  | `thaumcraft/common/config/ConfigEntities.java:121` |
| entity_type | `thaumcraft:bottletaint` |  | `thaumcraft/common/config/ConfigEntities.java:112` |
| entity_type | `thaumcraft:brainyzombie` |  | `thaumcraft/common/config/ConfigEntities.java:127` |
| entity_type | `thaumcraft:causalitycollapser` |  | `thaumcraft/common/config/ConfigEntities.java:115` |
| entity_type | `thaumcraft:cultistcleric` | `thaumcraft:cultist_cleric` | `thaumcraft/common/config/ConfigEntities.java:136` |
| entity_type | `thaumcraft:cultistknight` | `thaumcraft:cultist_knight` | `thaumcraft/common/config/ConfigEntities.java:135` |
| entity_type | `thaumcraft:cultistleader` |  | `thaumcraft/common/config/ConfigEntities.java:125` |
| entity_type | `thaumcraft:cultistportalgreater` |  | `thaumcraft/common/config/ConfigEntities.java:103` |
| entity_type | `thaumcraft:cultistportallesser` | `thaumcraft:cultist_portal_lesser` | `thaumcraft/common/config/ConfigEntities.java:104` |
| entity_type | `thaumcraft:eldritchcrab` |  | `thaumcraft/common/config/ConfigEntities.java:137` |
| entity_type | `thaumcraft:eldritchgolem` |  | `thaumcraft/common/config/ConfigEntities.java:124` |
| entity_type | `thaumcraft:eldritchguardian` |  | `thaumcraft/common/config/ConfigEntities.java:134` |
| entity_type | `thaumcraft:eldritchorb` | `thaumcraft:cultist_orb` | `thaumcraft/common/config/ConfigEntities.java:111` |
| entity_type | `thaumcraft:eldritchwarden` |  | `thaumcraft/common/config/ConfigEntities.java:123` |
| entity_type | `thaumcraft:fallingtaint` |  | `thaumcraft/common/config/ConfigEntities.java:108` |
| entity_type | `thaumcraft:firebat` |  | `thaumcraft/common/config/ConfigEntities.java:130` |
| entity_type | `thaumcraft:fluxrift` | `thaumcraft:flux_rift` | `thaumcraft/common/config/ConfigEntities.java:105` |
| entity_type | `thaumcraft:focuscloud` |  | `thaumcraft/common/config/ConfigEntities.java:117` |
| entity_type | `thaumcraft:focusmine` |  | `thaumcraft/common/config/ConfigEntities.java:118` |
| entity_type | `thaumcraft:focusprojectile` |  | `thaumcraft/common/config/ConfigEntities.java:116` |
| entity_type | `thaumcraft:followitem` |  | `thaumcraft/common/config/ConfigEntities.java:107` |
| entity_type | `thaumcraft:giantbrainyzombie` |  | `thaumcraft/common/config/ConfigEntities.java:128` |
| entity_type | `thaumcraft:golem` |  | `thaumcraft/common/config/ConfigEntities.java:122` |
| entity_type | `thaumcraft:golemdart` |  | `thaumcraft/common/config/ConfigEntities.java:110` |
| entity_type | `thaumcraft:golemorb` |  | `thaumcraft/common/config/ConfigEntities.java:113` |
| entity_type | `thaumcraft:grapple` |  | `thaumcraft/common/config/ConfigEntities.java:114` |
| entity_type | `thaumcraft:inhabitedzombie` |  | `thaumcraft/common/config/ConfigEntities.java:138` |
| entity_type | `thaumcraft:mindspider` |  | `thaumcraft/common/config/ConfigEntities.java:133` |
| entity_type | `thaumcraft:pech` |  | `thaumcraft/common/config/ConfigEntities.java:132` |
| entity_type | `thaumcraft:specialitem` |  | `thaumcraft/common/config/ConfigEntities.java:106` |
| entity_type | `thaumcraft:spellbat` |  | `thaumcraft/common/config/ConfigEntities.java:131` |
| entity_type | `thaumcraft:taintacle` |  | `thaumcraft/common/config/ConfigEntities.java:141` |
| entity_type | `thaumcraft:taintaclegiant` |  | `thaumcraft/common/config/ConfigEntities.java:126` |
| entity_type | `thaumcraft:taintacletiny` |  | `thaumcraft/common/config/ConfigEntities.java:142` |
| entity_type | `thaumcraft:taintcrawler` |  | `thaumcraft/common/config/ConfigEntities.java:140` |
| entity_type | `thaumcraft:taintseed` | `thaumcraft:taint_seed` | `thaumcraft/common/config/ConfigEntities.java:144` |
| entity_type | `thaumcraft:taintseedprime` | `thaumcraft:taint_seed_prime` | `thaumcraft/common/config/ConfigEntities.java:145` |
| entity_type | `thaumcraft:taintswarm` |  | `thaumcraft/common/config/ConfigEntities.java:143` |
| entity_type | `thaumcraft:thaumslime` | `thaumcraft:thaumic_slime` | `thaumcraft/common/config/ConfigEntities.java:139` |
| entity_type | `thaumcraft:turretadvanced` |  | `thaumcraft/common/config/ConfigEntities.java:120` |
| entity_type | `thaumcraft:turretbasic` |  | `thaumcraft/common/config/ConfigEntities.java:119` |
| entity_type | `thaumcraft:wisp` | `thaumcraft:wisp` | `thaumcraft/common/config/ConfigEntities.java:129` |
| fluid | `thaumcraft:flux_goo` |  | `thaumcraft/common/config/ConfigBlocks.java:371` |
| fluid | `thaumcraft:liquid_death` |  | `thaumcraft/common/config/ConfigBlocks.java:374` |
| fluid | `thaumcraft:purifying_fluid` |  | `thaumcraft/common/config/ConfigBlocks.java:378` |
| focus | `FocusEffectAir` |  | `thaumcraft/common/config/ConfigItems.java:426` |
| focus | `FocusEffectBreak` |  | `thaumcraft/common/config/ConfigItems.java:429` |
| focus | `FocusEffectCurse` |  | `thaumcraft/common/config/ConfigItems.java:432` |
| focus | `FocusEffectEarth` |  | `thaumcraft/common/config/ConfigItems.java:427` |
| focus | `FocusEffectExchange` |  | `thaumcraft/common/config/ConfigItems.java:431` |
| focus | `FocusEffectFire` |  | `thaumcraft/common/config/ConfigItems.java:424` |
| focus | `FocusEffectFlux` |  | `thaumcraft/common/config/ConfigItems.java:428` |
| focus | `FocusEffectFrost` |  | `thaumcraft/common/config/ConfigItems.java:425` |
| focus | `FocusEffectHeal` |  | `thaumcraft/common/config/ConfigItems.java:433` |
| focus | `FocusEffectRift` |  | `thaumcraft/common/config/ConfigItems.java:430` |
| focus | `FocusMediumBolt` |  | `thaumcraft/common/config/ConfigItems.java:418` |
| focus | `FocusMediumCloud` |  | `thaumcraft/common/config/ConfigItems.java:420` |
| focus | `FocusMediumMine` |  | `thaumcraft/common/config/ConfigItems.java:421` |
| focus | `FocusMediumPlan` |  | `thaumcraft/common/config/ConfigItems.java:422` |
| focus | `FocusMediumProjectile` |  | `thaumcraft/common/config/ConfigItems.java:419` |
| focus | `FocusMediumRoot` |  | `thaumcraft/common/config/ConfigItems.java:416` |
| focus | `FocusMediumSpellBat` |  | `thaumcraft/common/config/ConfigItems.java:423` |
| focus | `FocusMediumTouch` |  | `thaumcraft/common/config/ConfigItems.java:417` |
| focus | `FocusModScatter` |  | `thaumcraft/common/config/ConfigItems.java:434` |
| focus | `FocusModSplitTarget` |  | `thaumcraft/common/config/ConfigItems.java:435` |
| focus | `FocusModSplitTrajectory` |  | `thaumcraft/common/config/ConfigItems.java:436` |
| golem_addon | `ARMORED` |  | `thaumcraft/common/golems/GolemProperties.java:224` |
| golem_addon | `FIGHTER` |  | `thaumcraft/common/golems/GolemProperties.java:225` |
| golem_addon | `HAULER` |  | `thaumcraft/common/golems/GolemProperties.java:226` |
| golem_addon | `NONE` |  | `thaumcraft/common/golems/GolemProperties.java:223` |
| golem_arm | `BASIC` |  | `thaumcraft/common/golems/GolemProperties.java:214` |
| golem_arm | `BREAKERS` |  | `thaumcraft/common/golems/GolemProperties.java:217` |
| golem_arm | `CLAWS` |  | `thaumcraft/common/golems/GolemProperties.java:216` |
| golem_arm | `DARTS` |  | `thaumcraft/common/golems/GolemProperties.java:218` |
| golem_arm | `FINE` |  | `thaumcraft/common/golems/GolemProperties.java:215` |
| golem_head | `BASIC` |  | `thaumcraft/common/golems/GolemProperties.java:209` |
| golem_head | `SCOUT` |  | `thaumcraft/common/golems/GolemProperties.java:212` |
| golem_head | `SMART` |  | `thaumcraft/common/golems/GolemProperties.java:210` |
| golem_head | `SMART_ARMORED` |  | `thaumcraft/common/golems/GolemProperties.java:211` |
| golem_head | `SMART_SCOUT` |  | `thaumcraft/common/golems/GolemProperties.java:213` |
| golem_leg | `CLIMBER` |  | `thaumcraft/common/golems/GolemProperties.java:221` |
| golem_leg | `FLYER` |  | `thaumcraft/common/golems/GolemProperties.java:222` |
| golem_leg | `ROLLER` |  | `thaumcraft/common/golems/GolemProperties.java:220` |
| golem_leg | `WALKER` |  | `thaumcraft/common/golems/GolemProperties.java:219` |
| golem_material | `BRASS` |  | `thaumcraft/common/golems/GolemProperties.java:206` |
| golem_material | `CLAY` |  | `thaumcraft/common/golems/GolemProperties.java:205` |
| golem_material | `IRON` |  | `thaumcraft/common/golems/GolemProperties.java:204` |
| golem_material | `THAUMIUM` |  | `thaumcraft/common/golems/GolemProperties.java:207` |
| golem_material | `VOID` |  | `thaumcraft/common/golems/GolemProperties.java:208` |
| golem_material | `WOOD` |  | `thaumcraft/common/golems/GolemProperties.java:203` |
| golem_trait | `ARMORED` |  | `thaumcraft/api/golems/EnumGolemTrait.java:26` |
| golem_trait | `BLASTPROOF` |  | `thaumcraft/api/golems/EnumGolemTrait.java:32` |
| golem_trait | `BREAKER` |  | `thaumcraft/api/golems/EnumGolemTrait.java:29` |
| golem_trait | `BRUTAL` |  | `thaumcraft/api/golems/EnumGolemTrait.java:27` |
| golem_trait | `CLIMBER` |  | `thaumcraft/api/golems/EnumGolemTrait.java:20` |
| golem_trait | `CLUMSY` |  | `thaumcraft/api/golems/EnumGolemTrait.java:16` |
| golem_trait | `DEFT` |  | `thaumcraft/api/golems/EnumGolemTrait.java:15` |
| golem_trait | `FIGHTER` |  | `thaumcraft/api/golems/EnumGolemTrait.java:17` |
| golem_trait | `FIREPROOF` |  | `thaumcraft/api/golems/EnumGolemTrait.java:28` |
| golem_trait | `FLYER` |  | `thaumcraft/api/golems/EnumGolemTrait.java:19` |
| golem_trait | `FRAGILE` |  | `thaumcraft/api/golems/EnumGolemTrait.java:23` |
| golem_trait | `HAULER` |  | `thaumcraft/api/golems/EnumGolemTrait.java:30` |
| golem_trait | `HEAVY` |  | `thaumcraft/api/golems/EnumGolemTrait.java:21` |
| golem_trait | `LIGHT` |  | `thaumcraft/api/golems/EnumGolemTrait.java:22` |
| golem_trait | `RANGED` |  | `thaumcraft/api/golems/EnumGolemTrait.java:31` |
| golem_trait | `REPAIR` |  | `thaumcraft/api/golems/EnumGolemTrait.java:24` |
| golem_trait | `SCOUT` |  | `thaumcraft/api/golems/EnumGolemTrait.java:25` |
| golem_trait | `SMART` |  | `thaumcraft/api/golems/EnumGolemTrait.java:14` |
| golem_trait | `WHEELED` |  | `thaumcraft/api/golems/EnumGolemTrait.java:18` |
| item | `thaumcraft:alumentum` | `thaumcraft:alumentum` | `thaumcraft/common/config/ConfigItems.java:257` |
| item | `thaumcraft:amber` | `thaumcraft:amber` | `thaumcraft/common/config/ConfigItems.java:209` |
| item | `thaumcraft:amulet_vis` |  | `thaumcraft/common/config/ConfigItems.java:381` |
| item | `thaumcraft:bath_salts` |  | `thaumcraft/common/config/ConfigItems.java:265` |
| item | `thaumcraft:baubles` | `thaumcraft:amulet_mundane`, `thaumcraft:ring_mundane`, `thaumcraft:girdle_mundane`, `thaumcraft:amulet_fancy`, `thaumcraft:ring_fancy`, `thaumcraft:girdle_fancy` | `thaumcraft/common/config/ConfigItems.java:379` |
| item | `thaumcraft:bottle_taint` |  | `thaumcraft/common/config/ConfigItems.java:261` |
| item | `thaumcraft:brain` | `thaumcraft:brain` | `thaumcraft/common/config/ConfigItems.java:251` |
| item | `thaumcraft:caster_basic` |  | `thaumcraft/common/config/ConfigItems.java:397` |
| item | `thaumcraft:causality_collapser` |  | `thaumcraft/common/config/ConfigItems.java:269` |
| item | `thaumcraft:celestial_notes` | `thaumcraft:celestial_notes` | `thaumcraft/common/config/ConfigItems.java:207` |
| item | `thaumcraft:charm_undying` | `thaumcraft:charm_undying` | `thaumcraft/common/config/ConfigItems.java:391` |
| item | `thaumcraft:chunk` |  | `thaumcraft/common/config/ConfigItems.java:247` |
| item | `thaumcraft:cloth_boots` |  | `thaumcraft/common/config/ConfigItems.java:335` |
| item | `thaumcraft:cloth_chest` |  | `thaumcraft/common/config/ConfigItems.java:331` |
| item | `thaumcraft:cloth_legs` |  | `thaumcraft/common/config/ConfigItems.java:333` |
| item | `thaumcraft:cloud_ring` |  | `thaumcraft/common/config/ConfigItems.java:389` |
| item | `thaumcraft:cluster` | `thaumcraft:cluster_iron`, `thaumcraft:cluster_gold`, `thaumcraft:cluster_copper`, `thaumcraft:cluster_cinnabar` | `thaumcraft/common/config/ConfigItems.java:217` |
| item | `thaumcraft:creative_flux_sponge` |  | `thaumcraft/common/config/ConfigItems.java:393` |
| item | `thaumcraft:crimson_blade` |  | `thaumcraft/common/config/ConfigItems.java:313` |
| item | `thaumcraft:crimson_boots` |  | `thaumcraft/common/config/ConfigItems.java:365` |
| item | `thaumcraft:crimson_plate_chest` |  | `thaumcraft/common/config/ConfigItems.java:361` |
| item | `thaumcraft:crimson_plate_helm` |  | `thaumcraft/common/config/ConfigItems.java:359` |
| item | `thaumcraft:crimson_plate_legs` |  | `thaumcraft/common/config/ConfigItems.java:363` |
| item | `thaumcraft:crimson_praetor_chest` |  | `thaumcraft/common/config/ConfigItems.java:375` |
| item | `thaumcraft:crimson_praetor_helm` |  | `thaumcraft/common/config/ConfigItems.java:373` |
| item | `thaumcraft:crimson_praetor_legs` |  | `thaumcraft/common/config/ConfigItems.java:377` |
| item | `thaumcraft:crimson_robe_chest` |  | `thaumcraft/common/config/ConfigItems.java:369` |
| item | `thaumcraft:crimson_robe_helm` |  | `thaumcraft/common/config/ConfigItems.java:367` |
| item | `thaumcraft:crimson_robe_legs` |  | `thaumcraft/common/config/ConfigItems.java:371` |
| item | `thaumcraft:crystal_essence` | `thaumcraft:crystal_essence` | `thaumcraft/common/config/ConfigItems.java:245` |
| item | `thaumcraft:curio` | `thaumcraft:curio_arcane`, `thaumcraft:curio_preserved`, `thaumcraft:curio_ancient`, `thaumcraft:curio_eldritch`, `thaumcraft:curio_knowledge`, `thaumcraft:curio_twisted`, `thaumcraft:curio_rites` | `thaumcraft/common/config/ConfigItems.java:199` |
| item | `thaumcraft:curiosity_band` |  | `thaumcraft/common/config/ConfigItems.java:385` |
| item | `thaumcraft:elemental_axe` |  | `thaumcraft/common/config/ConfigItems.java:301` |
| item | `thaumcraft:elemental_hoe` |  | `thaumcraft/common/config/ConfigItems.java:309` |
| item | `thaumcraft:elemental_pick` |  | `thaumcraft/common/config/ConfigItems.java:307` |
| item | `thaumcraft:elemental_shovel` |  | `thaumcraft/common/config/ConfigItems.java:305` |
| item | `thaumcraft:elemental_sword` |  | `thaumcraft/common/config/ConfigItems.java:303` |
| item | `thaumcraft:enchanted_placeholder` |  | `thaumcraft/common/config/ConfigItems.java:395` |
| item | `thaumcraft:fabric` | `thaumcraft:fabric` | `thaumcraft/common/config/ConfigItems.java:219` |
| item | `thaumcraft:filter` | `thaumcraft:filter` | `thaumcraft/common/config/ConfigItems.java:231` |
| item | `thaumcraft:focus_1` |  | `thaumcraft/common/config/ConfigItems.java:399` |
| item | `thaumcraft:focus_2` |  | `thaumcraft/common/config/ConfigItems.java:401` |
| item | `thaumcraft:focus_3` |  | `thaumcraft/common/config/ConfigItems.java:403` |
| item | `thaumcraft:focus_pouch` |  | `thaumcraft/common/config/ConfigItems.java:405` |
| item | `thaumcraft:fortress_chest` |  | `thaumcraft/common/config/ConfigItems.java:341` |
| item | `thaumcraft:fortress_helm` |  | `thaumcraft/common/config/ConfigItems.java:339` |
| item | `thaumcraft:fortress_legs` |  | `thaumcraft/common/config/ConfigItems.java:343` |
| item | `thaumcraft:goggles` |  | `thaumcraft/common/config/ConfigItems.java:321` |
| item | `thaumcraft:golem` |  | `thaumcraft/common/config/ConfigItems.java:409` |
| item | `thaumcraft:golem_bell` |  | `thaumcraft/common/config/ConfigItems.java:407` |
| item | `thaumcraft:grapple_gun` |  | `thaumcraft/common/config/ConfigItems.java:315` |
| item | `thaumcraft:grapple_gun_spool` |  | `thaumcraft/common/config/ConfigItems.java:319` |
| item | `thaumcraft:grapple_gun_tip` |  | `thaumcraft/common/config/ConfigItems.java:317` |
| item | `thaumcraft:hand_mirror` |  | `thaumcraft/common/config/ConfigItems.java:279` |
| item | `thaumcraft:ingot` | `thaumcraft:ingot_thaumium`, `thaumcraft:ingot_void`, `thaumcraft:ingot_brass` | `thaumcraft/common/config/ConfigItems.java:213` |
| item | `thaumcraft:jar_brace` | `thaumcraft:jar_brace` | `thaumcraft/common/config/ConfigItems.java:259` |
| item | `thaumcraft:label` | `thaumcraft:label` | `thaumcraft/common/config/ConfigItems.java:253` |
| item | `thaumcraft:loot_bag` |  | `thaumcraft/common/config/ConfigItems.java:201` |
| item | `thaumcraft:mechanism_complex` |  | `thaumcraft/common/config/ConfigItems.java:227` |
| item | `thaumcraft:mechanism_simple` | `thaumcraft:mechanism_simple` | `thaumcraft/common/config/ConfigItems.java:225` |
| item | `thaumcraft:mind` | `thaumcraft:mind_clockwork` | `thaumcraft/common/config/ConfigItems.java:241` |
| item | `thaumcraft:mirrored_glass` |  | `thaumcraft/common/config/ConfigItems.java:237` |
| item | `thaumcraft:module` |  | `thaumcraft/common/config/ConfigItems.java:243` |
| item | `thaumcraft:morphic_resonator` | `thaumcraft:morphic_resonator` | `thaumcraft/common/config/ConfigItems.java:233` |
| item | `thaumcraft:nugget` | `thaumcraft:nugget_brass`, `thaumcraft:nugget_thaumium`, `thaumcraft:nugget_void`, `thaumcraft:nugget_quicksilver`, `thaumcraft:nugget_quartz` | `thaumcraft/common/config/ConfigItems.java:215` |
| item | `thaumcraft:pech_wand` |  | `thaumcraft/common/config/ConfigItems.java:205` |
| item | `thaumcraft:phial` | `thaumcraft:phial` | `thaumcraft/common/config/ConfigItems.java:255` |
| item | `thaumcraft:plate` | `thaumcraft:plate_brass`, `thaumcraft:plate_iron`, `thaumcraft:plate_thaumium`, `thaumcraft:plate_void` | `thaumcraft/common/config/ConfigItems.java:229` |
| item | `thaumcraft:primal_crusher` |  | `thaumcraft/common/config/ConfigItems.java:311` |
| item | `thaumcraft:primordial_pearl` | `thaumcraft:primordial_pearl` | `thaumcraft/common/config/ConfigItems.java:203` |
| item | `thaumcraft:quicksilver` | `thaumcraft:quicksilver` | `thaumcraft/common/config/ConfigItems.java:211` |
| item | `thaumcraft:resonator` |  | `thaumcraft/common/config/ConfigItems.java:275` |
| item | `thaumcraft:salis_mundus` | `thaumcraft:salis_mundus` | `thaumcraft/common/config/ConfigItems.java:235` |
| item | `thaumcraft:sanity_checker` |  | `thaumcraft/common/config/ConfigItems.java:277` |
| item | `thaumcraft:sanity_soap` |  | `thaumcraft/common/config/ConfigItems.java:263` |
| item | `thaumcraft:scribing_tools` | `thaumcraft:scribing_tools` | `thaumcraft/common/config/ConfigItems.java:271` |
| item | `thaumcraft:seal` |  | `thaumcraft/common/config/ConfigItems.java:411` |
| item | `thaumcraft:tallow` | `thaumcraft:tallow` | `thaumcraft/common/config/ConfigItems.java:223` |
| item | `thaumcraft:thaumium_axe` |  | `thaumcraft/common/config/ConfigItems.java:281` |
| item | `thaumcraft:thaumium_boots` |  | `thaumcraft/common/config/ConfigItems.java:329` |
| item | `thaumcraft:thaumium_chest` |  | `thaumcraft/common/config/ConfigItems.java:325` |
| item | `thaumcraft:thaumium_helm` |  | `thaumcraft/common/config/ConfigItems.java:323` |
| item | `thaumcraft:thaumium_hoe` |  | `thaumcraft/common/config/ConfigItems.java:289` |
| item | `thaumcraft:thaumium_legs` |  | `thaumcraft/common/config/ConfigItems.java:327` |
| item | `thaumcraft:thaumium_pick` |  | `thaumcraft/common/config/ConfigItems.java:287` |
| item | `thaumcraft:thaumium_shovel` |  | `thaumcraft/common/config/ConfigItems.java:285` |
| item | `thaumcraft:thaumium_sword` |  | `thaumcraft/common/config/ConfigItems.java:283` |
| item | `thaumcraft:thaumometer` | `thaumcraft:thaumometer` | `thaumcraft/common/config/ConfigItems.java:273` |
| item | `thaumcraft:thaumonomicon` | `thaumcraft:thaumonomicon` | `thaumcraft/common/config/ConfigItems.java:197` |
| item | `thaumcraft:traveller_boots` |  | `thaumcraft/common/config/ConfigItems.java:337` |
| item | `thaumcraft:triple_meat_treat` |  | `thaumcraft/common/config/ConfigItems.java:249` |
| item | `thaumcraft:turret` |  | `thaumcraft/common/config/ConfigItems.java:267` |
| item | `thaumcraft:verdant_charm` |  | `thaumcraft/common/config/ConfigItems.java:383` |
| item | `thaumcraft:vis_resonator` | `thaumcraft:vis_resonator` | `thaumcraft/common/config/ConfigItems.java:221` |
| item | `thaumcraft:void_axe` |  | `thaumcraft/common/config/ConfigItems.java:291` |
| item | `thaumcraft:void_boots` |  | `thaumcraft/common/config/ConfigItems.java:351` |
| item | `thaumcraft:void_chest` |  | `thaumcraft/common/config/ConfigItems.java:347` |
| item | `thaumcraft:void_helm` |  | `thaumcraft/common/config/ConfigItems.java:345` |
| item | `thaumcraft:void_hoe` |  | `thaumcraft/common/config/ConfigItems.java:299` |
| item | `thaumcraft:void_legs` |  | `thaumcraft/common/config/ConfigItems.java:349` |
| item | `thaumcraft:void_pick` |  | `thaumcraft/common/config/ConfigItems.java:297` |
| item | `thaumcraft:void_robe_chest` |  | `thaumcraft/common/config/ConfigItems.java:355` |
| item | `thaumcraft:void_robe_helm` |  | `thaumcraft/common/config/ConfigItems.java:353` |
| item | `thaumcraft:void_robe_legs` |  | `thaumcraft/common/config/ConfigItems.java:357` |
| item | `thaumcraft:void_seed` | `thaumcraft:void_seed` | `thaumcraft/common/config/ConfigItems.java:239` |
| item | `thaumcraft:void_shovel` |  | `thaumcraft/common/config/ConfigItems.java:295` |
| item | `thaumcraft:void_sword` |  | `thaumcraft/common/config/ConfigItems.java:293` |
| item | `thaumcraft:voidseer_charm` |  | `thaumcraft/common/config/ConfigItems.java:387` |
| mob_effect | `thaumcraft:blurredvision` |  | `thaumcraft/Registrar.java:115` |
| mob_effect | `thaumcraft:deathgaze` |  | `thaumcraft/Registrar.java:114` |
| mob_effect | `thaumcraft:fluxtaint` | `thaumcraft:flux_taint` | `thaumcraft/Registrar.java:109` |
| mob_effect | `thaumcraft:infectiousvisexhaust` | `thaumcraft:infectious_vis_exhaust` | `thaumcraft/Registrar.java:111` |
| mob_effect | `thaumcraft:sunscorned` |  | `thaumcraft/Registrar.java:116` |
| mob_effect | `thaumcraft:thaumarhia` |  | `thaumcraft/Registrar.java:117` |
| mob_effect | `thaumcraft:unnaturalhunger` |  | `thaumcraft/Registrar.java:112` |
| mob_effect | `thaumcraft:visexhaust` | `thaumcraft:vis_exhaust` | `thaumcraft/Registrar.java:110` |
| mob_effect | `thaumcraft:warpward` |  | `thaumcraft/Registrar.java:113` |
| research | `!BrainyZombie` | `!BrainyZombie` | `assets/thaumcraft/research/scans.json` |
| research | `!CrimsonCultist` | `!CrimsonCultist` | `assets/thaumcraft/research/scans.json` |
| research | `!EldritchCrab` | `!EldritchCrab` | `assets/thaumcraft/research/scans.json` |
| research | `!EldritchGuardian` | `!EldritchGuardian` | `assets/thaumcraft/research/scans.json` |
| research | `!Firebat` | `!Firebat` | `assets/thaumcraft/research/scans.json` |
| research | `!Pech` | `!Pech` | `assets/thaumcraft/research/scans.json` |
| research | `!TaintCrawler` | `!TaintCrawler` | `assets/thaumcraft/research/scans.json` |
| research | `!TaintSeed` | `!TaintSeed` | `assets/thaumcraft/research/scans.json` |
| research | `!TaintSwarm` | `!TaintSwarm` | `assets/thaumcraft/research/scans.json` |
| research | `!Taintacle` | `!Taintacle` | `assets/thaumcraft/research/scans.json` |
| research | `!ThaumSlime` | `!ThaumSlime` | `assets/thaumcraft/research/scans.json` |
| research | `!Wisp` | `!Wisp` | `assets/thaumcraft/research/scans.json` |
| research | `ADVANCEDTURRET` | `ADVANCEDTURRET` | `assets/thaumcraft/research/artifice.json` |
| research | `ALUMENTUM` | `ALUMENTUM` | `assets/thaumcraft/research/alchemy.json` |
| research | `ARCANEBORE` | `ARCANEBORE` | `assets/thaumcraft/research/artifice.json` |
| research | `ARCANEEAR` | `ARCANEEAR` | `assets/thaumcraft/research/artifice.json` |
| research | `ARCANELAMP` | `ARCANELAMP` | `assets/thaumcraft/research/artifice.json` |
| research | `ARCANEPATTERNCRAFTER` | `ARCANEPATTERNCRAFTER` | `assets/thaumcraft/research/artifice.json` |
| research | `ARCANESPA` | `ARCANESPA` | `assets/thaumcraft/research/alchemy.json` |
| research | `ARMORFORTRESS` | `ARMORFORTRESS` | `assets/thaumcraft/research/infusion.json` |
| research | `BASEALCHEMY` | `BASEALCHEMY` | `assets/thaumcraft/research/alchemy.json` |
| research | `BASEARTIFICE` | `BASEARTIFICE` | `assets/thaumcraft/research/artifice.json` |
| research | `BASEAUROMANCY` | `BASEAUROMANCY` | `assets/thaumcraft/research/auromancy.json` |
| research | `BASEELDRITCH` | `BASEELDRITCH` | `assets/thaumcraft/research/eldritch.json` |
| research | `BASEGOLEMANCY` | `BASEGOLEMANCY` | `assets/thaumcraft/research/golemancy.json` |
| research | `BASEINFUSION` | `BASEINFUSION` | `assets/thaumcraft/research/infusion.json` |
| research | `BASICTURRET` | `BASICTURRET` | `assets/thaumcraft/research/artifice.json` |
| research | `BATHSALTS` | `BATHSALTS` | `assets/thaumcraft/research/alchemy.json` |
| research | `BELLOWS` | `BELLOWS` | `assets/thaumcraft/research/artifice.json` |
| research | `BOOTSTRAVELLER` | `BOOTSTRAVELLER` | `assets/thaumcraft/research/infusion.json` |
| research | `BOTTLETAINT` | `BOTTLETAINT` | `assets/thaumcraft/research/alchemy.json` |
| research | `CELESTIALSCANNING` | `CELESTIALSCANNING` | `assets/thaumcraft/research/basics.json` |
| research | `CENTRIFUGE` | `CENTRIFUGE` | `assets/thaumcraft/research/alchemy.json` |
| research | `CHARMUNDYING` | `CHARMUNDYING` | `assets/thaumcraft/research/infusion.json` |
| research | `CLOUDRING` | `CLOUDRING` | `assets/thaumcraft/research/infusion.json` |
| research | `CONTROLSEALS` | `CONTROLSEALS` | `assets/thaumcraft/research/golemancy.json` |
| research | `CRYSTALFARMER` | `CRYSTALFARMER` | `assets/thaumcraft/research/basics.json` |
| research | `CURIOSITYBAND` | `CURIOSITYBAND` | `assets/thaumcraft/research/infusion.json` |
| research | `CrimsonRites` | `CrimsonRites` | `assets/thaumcraft/research/basics.json` |
| research | `DIOPTRA` | `DIOPTRA` | `assets/thaumcraft/research/artifice.json` |
| research | `ELEMENTALTOOLS` | `ELEMENTALTOOLS` | `assets/thaumcraft/research/infusion.json` |
| research | `ESSENTIASMELTER` | `ESSENTIASMELTER` | `assets/thaumcraft/research/alchemy.json` |
| research | `ESSENTIASMELTERTHAUMIUM` | `ESSENTIASMELTERTHAUMIUM` | `assets/thaumcraft/research/alchemy.json` |
| research | `ESSENTIASMELTERVOID` | `ESSENTIASMELTERVOID` | `assets/thaumcraft/research/alchemy.json` |
| research | `ESSENTIATRANSPORT` | `ESSENTIATRANSPORT` | `assets/thaumcraft/research/alchemy.json` |
| research | `EVERFULLURN` | `EVERFULLURN` | `assets/thaumcraft/research/alchemy.json` |
| research | `FIRSTSTEPS` | `FIRSTSTEPS` | `assets/thaumcraft/research/basics.json` |
| research | `FLUX` | `FLUX` | `assets/thaumcraft/research/basics.json` |
| research | `FLUXCLEANUP` | `FLUXCLEANUP` | `assets/thaumcraft/research/basics.json` |
| research | `FLUXRIFT` | `FLUXRIFT` | `assets/thaumcraft/research/basics.json` |
| research | `FOCUSADVANCED` | `FOCUSADVANCED` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSBOLT` | `FOCUSBOLT` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSBREAK` | `FOCUSBREAK` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSCLOUD` | `FOCUSCLOUD` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSCURSE` | `FOCUSCURSE` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSELEMENTAL` | `FOCUSELEMENTAL` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSEXCHANGE` | `FOCUSEXCHANGE` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSFLUX` | `FOCUSFLUX` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSGREATER` | `FOCUSGREATER` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSHEAL` | `FOCUSHEAL` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSMINE` | `FOCUSMINE` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSPLAN` | `FOCUSPLAN` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSPOUCH` | `FOCUSPOUCH` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSPROJECTILE` | `FOCUSPROJECTILE` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSRIFT` | `FOCUSRIFT` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSSCATTER` | `FOCUSSCATTER` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSSPELLBAT` | `FOCUSSPELLBAT` | `assets/thaumcraft/research/auromancy.json` |
| research | `FOCUSSPLIT` | `FOCUSSPLIT` | `assets/thaumcraft/research/auromancy.json` |
| research | `FORTRESSMASK` | `FORTRESSMASK` | `assets/thaumcraft/research/infusion.json` |
| research | `GOLEMBREAKER` | `GOLEMBREAKER` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMCLIMBER` | `GOLEMCLIMBER` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMCOMBATADV` | `GOLEMCOMBATADV` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMDIRECT` | `GOLEMDIRECT` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMFLYER` | `GOLEMFLYER` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMLOGISTICS` | `GOLEMLOGISTICS` | `assets/thaumcraft/research/golemancy.json` |
| research | `GOLEMVISION` | `GOLEMVISION` | `assets/thaumcraft/research/golemancy.json` |
| research | `GRAPPLEGUN` | `GRAPPLEGUN` | `assets/thaumcraft/research/artifice.json` |
| research | `HEDGEALCHEMY` | `HEDGEALCHEMY` | `assets/thaumcraft/research/alchemy.json` |
| research | `HUNGRYCHEST` | `HUNGRYCHEST` | `assets/thaumcraft/research/artifice.json` |
| research | `IMPROVEDSMELTING` | `IMPROVEDSMELTING` | `assets/thaumcraft/research/alchemy.json` |
| research | `IMPROVEDSMELTING2` | `IMPROVEDSMELTING2` | `assets/thaumcraft/research/alchemy.json` |
| research | `INFERNALFURNACE` | `INFERNALFURNACE` | `assets/thaumcraft/research/artifice.json` |
| research | `INFUSION` | `INFUSION` | `assets/thaumcraft/research/infusion.json` |
| research | `INFUSIONANCIENT` | `INFUSIONANCIENT` | `assets/thaumcraft/research/infusion.json` |
| research | `INFUSIONBOOST` | `INFUSIONBOOST` | `assets/thaumcraft/research/infusion.json` |
| research | `INFUSIONELDRITCH` | `INFUSIONELDRITCH` | `assets/thaumcraft/research/infusion.json` |
| research | `INFUSIONENCHANTMENT` | `INFUSIONENCHANTMENT` | `assets/thaumcraft/research/infusion.json` |
| research | `INFUSIONSTABLE` | `INFUSIONSTABLE` | `assets/thaumcraft/research/infusion.json` |
| research | `JARBRAIN` | `JARBRAIN` | `assets/thaumcraft/research/golemancy.json` |
| research | `KNOWLEDGETYPES` | `KNOWLEDGETYPES` | `assets/thaumcraft/research/basics.json` |
| research | `LAMPFERTILITY` | `LAMPFERTILITY` | `assets/thaumcraft/research/artifice.json` |
| research | `LAMPGROWTH` | `LAMPGROWTH` | `assets/thaumcraft/research/artifice.json` |
| research | `LEVITATOR` | `LEVITATOR` | `assets/thaumcraft/research/artifice.json` |
| research | `LIQUIDDEATH` | `LIQUIDDEATH` | `assets/thaumcraft/research/alchemy.json` |
| research | `MATSTUDBRASS` | `MATSTUDBRASS` | `assets/thaumcraft/research/golemancy.json` |
| research | `MATSTUDCLAY` | `MATSTUDCLAY` | `assets/thaumcraft/research/golemancy.json` |
| research | `MATSTUDIRON` | `MATSTUDIRON` | `assets/thaumcraft/research/golemancy.json` |
| research | `MATSTUDTHAUMIUM` | `MATSTUDTHAUMIUM` | `assets/thaumcraft/research/golemancy.json` |
| research | `MATSTUDVOID` | `MATSTUDVOID` | `assets/thaumcraft/research/golemancy.json` |
| research | `MATSTUDWOOD` | `MATSTUDWOOD` | `assets/thaumcraft/research/golemancy.json` |
| research | `METALLURGY` | `METALLURGY` | `assets/thaumcraft/research/alchemy.json` |
| research | `METALPURIFICATION` | `METALPURIFICATION` | `assets/thaumcraft/research/alchemy.json` |
| research | `MINDBIOTHAUMIC` | `MINDBIOTHAUMIC` | `assets/thaumcraft/research/golemancy.json` |
| research | `MINDCLOCKWORK` | `MINDCLOCKWORK` | `assets/thaumcraft/research/golemancy.json` |
| research | `MIRROR` | `MIRROR` | `assets/thaumcraft/research/artifice.json` |
| research | `MIRRORESSENTIA` | `MIRRORESSENTIA` | `assets/thaumcraft/research/artifice.json` |
| research | `MIRRORHAND` | `MIRRORHAND` | `assets/thaumcraft/research/artifice.json` |
| research | `ORE` | `ORE` | `assets/thaumcraft/research/basics.json` |
| research | `PAVINGSTONES` | `PAVINGSTONES` | `assets/thaumcraft/research/infusion.json` |
| research | `PLANTS` | `PLANTS` | `assets/thaumcraft/research/basics.json` |
| research | `POTIONSPRAYER` | `POTIONSPRAYER` | `assets/thaumcraft/research/alchemy.json` |
| research | `PRIMALCRUSHER` | `PRIMALCRUSHER` | `assets/thaumcraft/research/eldritch.json` |
| research | `PRIMPEARL` | `PRIMPEARL` | `assets/thaumcraft/research/basics.json` |
| research | `RECHARGEPEDESTAL` | `RECHARGEPEDESTAL` | `assets/thaumcraft/research/auromancy.json` |
| research | `REDSTONERELAY` | `REDSTONERELAY` | `assets/thaumcraft/research/artifice.json` |
| research | `RIFTCLOSER` | `RIFTCLOSER` | `assets/thaumcraft/research/basics.json` |
| research | `RUNICSHIELDING` | `RUNICSHIELDING` | `assets/thaumcraft/research/infusion.json` |
| research | `SANESOAP` | `SANESOAP` | `assets/thaumcraft/research/alchemy.json` |
| research | `SEALBREAK` | `SEALBREAK` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALBUTCHER` | `SEALBUTCHER` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALCOLLECT` | `SEALCOLLECT` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALEMPTY` | `SEALEMPTY` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALGUARD` | `SEALGUARD` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALHARVEST` | `SEALHARVEST` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALLUMBER` | `SEALLUMBER` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALPROVIDE` | `SEALPROVIDE` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALSTOCK` | `SEALSTOCK` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALSTORE` | `SEALSTORE` | `assets/thaumcraft/research/golemancy.json` |
| research | `SEALUSE` | `SEALUSE` | `assets/thaumcraft/research/golemancy.json` |
| research | `THAUMATORIUM` | `THAUMATORIUM` | `assets/thaumcraft/research/alchemy.json` |
| research | `THEORYRESEARCH` | `THEORYRESEARCH` | `assets/thaumcraft/research/basics.json` |
| research | `TUBES` | `TUBES` | `assets/thaumcraft/research/alchemy.json` |
| research | `UNLOCKALCHEMY` | `UNLOCKALCHEMY` | `assets/thaumcraft/research/basics.json` |
| research | `UNLOCKARTIFICE` | `UNLOCKARTIFICE` | `assets/thaumcraft/research/basics.json` |
| research | `UNLOCKAUROMANCY` | `UNLOCKAUROMANCY` | `assets/thaumcraft/research/basics.json` |
| research | `UNLOCKELDRITCH` | `UNLOCKELDRITCH` | `assets/thaumcraft/research/basics.json` |
| research | `UNLOCKGOLEMANCY` | `UNLOCKGOLEMANCY` | `assets/thaumcraft/research/basics.json` |
| research | `UNLOCKINFUSION` | `UNLOCKINFUSION` | `assets/thaumcraft/research/basics.json` |
| research | `VERDANTCHARMS` | `VERDANTCHARMS` | `assets/thaumcraft/research/infusion.json` |
| research | `VISAMULET` | `VISAMULET` | `assets/thaumcraft/research/auromancy.json` |
| research | `VISBATTERY` | `VISBATTERY` | `assets/thaumcraft/research/auromancy.json` |
| research | `VISGENERATOR` | `VISGENERATOR` | `assets/thaumcraft/research/artifice.json` |
| research | `VOIDROBEARMOR` | `VOIDROBEARMOR` | `assets/thaumcraft/research/eldritch.json` |
| research | `VOIDSEERPEARL` | `VOIDSEERPEARL` | `assets/thaumcraft/research/eldritch.json` |
| research | `VOIDSIPHON` | `VOIDSIPHON` | `assets/thaumcraft/research/eldritch.json` |
| research | `WARDEDJARS` | `WARDEDJARS` | `assets/thaumcraft/research/alchemy.json` |
| research | `WARP` | `WARP` | `assets/thaumcraft/research/basics.json` |
| research | `WORKBENCHCHARGER` | `WORKBENCHCHARGER` | `assets/thaumcraft/research/auromancy.json` |
| seal | `SealBreaker` |  | `thaumcraft/common/config/ConfigItems.java:451` |
| seal | `SealBreakerAdvanced` |  | `thaumcraft/common/config/ConfigItems.java:455` |
| seal | `SealButcher` |  | `thaumcraft/common/config/ConfigItems.java:447` |
| seal | `SealEmpty` |  | `thaumcraft/common/config/ConfigItems.java:444` |
| seal | `SealEmptyAdvanced` |  | `thaumcraft/common/config/ConfigItems.java:445` |
| seal | `SealFill` |  | `thaumcraft/common/config/ConfigItems.java:442` |
| seal | `SealFillAdvanced` |  | `thaumcraft/common/config/ConfigItems.java:443` |
| seal | `SealGuard` |  | `thaumcraft/common/config/ConfigItems.java:448` |
| seal | `SealGuardAdvanced` |  | `thaumcraft/common/config/ConfigItems.java:449` |
| seal | `SealHarvest` |  | `thaumcraft/common/config/ConfigItems.java:446` |
| seal | `SealLumber` |  | `thaumcraft/common/config/ConfigItems.java:450` |
| seal | `SealPickup` |  | `thaumcraft/common/config/ConfigItems.java:440` |
| seal | `SealPickupAdvanced` |  | `thaumcraft/common/config/ConfigItems.java:441` |
| seal | `SealProvide` |  | `thaumcraft/common/config/ConfigItems.java:453` |
| seal | `SealStock` |  | `thaumcraft/common/config/ConfigItems.java:454` |
| seal | `SealUse` |  | `thaumcraft/common/config/ConfigItems.java:452` |
| setting | `CONFIG_GRAPHICS.blueBiome` |  | `thaumcraft/common/config/ModConfig.java:383` |
| setting | `CONFIG_GRAPHICS.crooked` |  | `thaumcraft/common/config/ModConfig.java:377` |
| setting | `CONFIG_GRAPHICS.dialBottom` |  | `thaumcraft/common/config/ModConfig.java:379` |
| setting | `CONFIG_GRAPHICS.disableShaders` |  | `thaumcraft/common/config/ModConfig.java:373` |
| setting | `CONFIG_GRAPHICS.largeTagText` |  | `thaumcraft/common/config/ModConfig.java:371` |
| setting | `CONFIG_GRAPHICS.nostress` |  | `thaumcraft/common/config/ModConfig.java:375` |
| setting | `CONFIG_GRAPHICS.showGolemEmotes` |  | `thaumcraft/common/config/ModConfig.java:385` |
| setting | `CONFIG_GRAPHICS.showTags` |  | `thaumcraft/common/config/ModConfig.java:381` |
| setting | `CONFIG_MISC.allowCheatSheet` |  | `thaumcraft/common/config/ModConfig.java:305` |
| setting | `CONFIG_MISC.noSleep` |  | `thaumcraft/common/config/ModConfig.java:301` |
| setting | `CONFIG_MISC.shieldCost` |  | `thaumcraft/common/config/ModConfig.java:314` |
| setting | `CONFIG_MISC.shieldRecharge` |  | `thaumcraft/common/config/ModConfig.java:308` |
| setting | `CONFIG_MISC.shieldWait` |  | `thaumcraft/common/config/ModConfig.java:311` |
| setting | `CONFIG_MISC.wussMode` |  | `thaumcraft/common/config/ModConfig.java:303` |
| setting | `CONFIG_WORLD.allowChampionMobs` |  | `thaumcraft/common/config/ModConfig.java:364` |
| setting | `CONFIG_WORLD.allowSpawnAngryZombie` |  | `thaumcraft/common/config/ModConfig.java:357` |
| setting | `CONFIG_WORLD.allowSpawnElder` |  | `thaumcraft/common/config/ModConfig.java:362` |
| setting | `CONFIG_WORLD.allowSpawnFireBat` |  | `thaumcraft/common/config/ModConfig.java:358` |
| setting | `CONFIG_WORLD.allowSpawnPech` |  | `thaumcraft/common/config/ModConfig.java:361` |
| setting | `CONFIG_WORLD.allowSpawnTaintacle` |  | `thaumcraft/common/config/ModConfig.java:360` |
| setting | `CONFIG_WORLD.allowSpawnWisp` |  | `thaumcraft/common/config/ModConfig.java:359` |
| setting | `CONFIG_WORLD.biomeMagicalForestWeight` |  | `thaumcraft/common/config/ModConfig.java:334` |
| setting | `CONFIG_WORLD.dimensionOuterId` |  | `thaumcraft/common/config/ModConfig.java:325` |
| setting | `CONFIG_WORLD.generateAmber` |  | `thaumcraft/common/config/ModConfig.java:344` |
| setting | `CONFIG_WORLD.generateAura` |  | `thaumcraft/common/config/ModConfig.java:341` |
| setting | `CONFIG_WORLD.generateCinnabar` |  | `thaumcraft/common/config/ModConfig.java:343` |
| setting | `CONFIG_WORLD.generateCrystals` |  | `thaumcraft/common/config/ModConfig.java:346` |
| setting | `CONFIG_WORLD.generateMagicForest` |  | `thaumcraft/common/config/ModConfig.java:330` |
| setting | `CONFIG_WORLD.generateQuartz` |  | `thaumcraft/common/config/ModConfig.java:345` |
| setting | `CONFIG_WORLD.generateStructure` |  | `thaumcraft/common/config/ModConfig.java:342` |
| setting | `CONFIG_WORLD.generateTrees` |  | `thaumcraft/common/config/ModConfig.java:347` |
| setting | `CONFIG_WORLD.oreDensity` |  | `thaumcraft/common/config/ModConfig.java:329` |
| setting | `CONFIG_WORLD.overworldDim` |  | `thaumcraft/common/config/ModConfig.java:322` |
| setting | `CONFIG_WORLD.regenAmber` |  | `thaumcraft/common/config/ModConfig.java:353` |
| setting | `CONFIG_WORLD.regenAura` |  | `thaumcraft/common/config/ModConfig.java:350` |
| setting | `CONFIG_WORLD.regenCinnabar` |  | `thaumcraft/common/config/ModConfig.java:352` |
| setting | `CONFIG_WORLD.regenCrystals` |  | `thaumcraft/common/config/ModConfig.java:355` |
| setting | `CONFIG_WORLD.regenKey` |  | `thaumcraft/common/config/ModConfig.java:349` |
| setting | `CONFIG_WORLD.regenQuartz` |  | `thaumcraft/common/config/ModConfig.java:354` |
| setting | `CONFIG_WORLD.regenStructure` |  | `thaumcraft/common/config/ModConfig.java:351` |
| setting | `CONFIG_WORLD.regenTrees` |  | `thaumcraft/common/config/ModConfig.java:356` |
| setting | `CONFIG_WORLD.taintSpreadArea` |  | `thaumcraft/common/config/ModConfig.java:340` |
| setting | `CONFIG_WORLD.taintSpreadRate` |  | `thaumcraft/common/config/ModConfig.java:337` |
| sound_event | `thaumcraft:brain` | `thaumcraft:brain` | `thaumcraft/common/lib/SoundsTC.java:118` |
| sound_event | `thaumcraft:bubble` | `thaumcraft:bubble` | `thaumcraft/common/lib/SoundsTC.java:101` |
| sound_event | `thaumcraft:chant` | `thaumcraft:chant` | `thaumcraft/common/lib/SoundsTC.java:157` |
| sound_event | `thaumcraft:clack` | `thaumcraft:clack` | `thaumcraft/common/lib/SoundsTC.java:110` |
| sound_event | `thaumcraft:coins` | `thaumcraft:coins` | `thaumcraft/common/lib/SoundsTC.java:158` |
| sound_event | `thaumcraft:crabclaw` | `thaumcraft:crabclaw` | `thaumcraft/common/lib/SoundsTC.java:154` |
| sound_event | `thaumcraft:crabdeath` | `thaumcraft:crabdeath` | `thaumcraft/common/lib/SoundsTC.java:155` |
| sound_event | `thaumcraft:crabtalk` | `thaumcraft:crabtalk` | `thaumcraft/common/lib/SoundsTC.java:156` |
| sound_event | `thaumcraft:craftfail` | `thaumcraft:craftfail` | `thaumcraft/common/lib/SoundsTC.java:136` |
| sound_event | `thaumcraft:craftstart` | `thaumcraft:craftstart` | `thaumcraft/common/lib/SoundsTC.java:138` |
| sound_event | `thaumcraft:creak` | `thaumcraft:creak` | `thaumcraft/common/lib/SoundsTC.java:102` |
| sound_event | `thaumcraft:crystal` | `thaumcraft:crystal` | `thaumcraft/common/lib/SoundsTC.java:119` |
| sound_event | `thaumcraft:dust` | `thaumcraft:dust` | `thaumcraft/common/lib/SoundsTC.java:162` |
| sound_event | `thaumcraft:egattack` | `thaumcraft:egattack` | `thaumcraft/common/lib/SoundsTC.java:151` |
| sound_event | `thaumcraft:egdeath` | `thaumcraft:egdeath` | `thaumcraft/common/lib/SoundsTC.java:152` |
| sound_event | `thaumcraft:egidle` | `thaumcraft:egidle` | `thaumcraft/common/lib/SoundsTC.java:150` |
| sound_event | `thaumcraft:egscreech` | `thaumcraft:egscreech` | `thaumcraft/common/lib/SoundsTC.java:153` |
| sound_event | `thaumcraft:erase` | `thaumcraft:erase` | `thaumcraft/common/lib/SoundsTC.java:117` |
| sound_event | `thaumcraft:evilportal` | `thaumcraft:evilportal` | `thaumcraft/common/lib/SoundsTC.java:160` |
| sound_event | `thaumcraft:fly` | `thaumcraft:fly` | `thaumcraft/common/lib/SoundsTC.java:107` |
| sound_event | `thaumcraft:gore` | `thaumcraft:gore` | `thaumcraft/common/lib/SoundsTC.java:143` |
| sound_event | `thaumcraft:grind` | `thaumcraft:grind` | `thaumcraft/common/lib/SoundsTC.java:161` |
| sound_event | `thaumcraft:heartbeat` | `thaumcraft:heartbeat` | `thaumcraft/common/lib/SoundsTC.java:99` |
| sound_event | `thaumcraft:hhoff` | `thaumcraft:hhoff` | `thaumcraft/common/lib/SoundsTC.java:127` |
| sound_event | `thaumcraft:hhon` | `thaumcraft:hhon` | `thaumcraft/common/lib/SoundsTC.java:128` |
| sound_event | `thaumcraft:ice` | `thaumcraft:ice` | `thaumcraft/common/lib/SoundsTC.java:125` |
| sound_event | `thaumcraft:infuser` | `thaumcraft:infuser` | `thaumcraft/common/lib/SoundsTC.java:148` |
| sound_event | `thaumcraft:infuserstart` | `thaumcraft:infuserstart` | `thaumcraft/common/lib/SoundsTC.java:149` |
| sound_event | `thaumcraft:jacobs` | `thaumcraft:jacobs` | `thaumcraft/common/lib/SoundsTC.java:126` |
| sound_event | `thaumcraft:jar` | `thaumcraft:jar` | `thaumcraft/common/lib/SoundsTC.java:104` |
| sound_event | `thaumcraft:key` | `thaumcraft:key` | `thaumcraft/common/lib/SoundsTC.java:108` |
| sound_event | `thaumcraft:learn` | `thaumcraft:learn` | `thaumcraft/common/lib/SoundsTC.java:115` |
| sound_event | `thaumcraft:monolith` | `thaumcraft:monolith` | `thaumcraft/common/lib/SoundsTC.java:147` |
| sound_event | `thaumcraft:page` | `thaumcraft:page` | `thaumcraft/common/lib/SoundsTC.java:113` |
| sound_event | `thaumcraft:pageturn` | `thaumcraft:pageturn` | `thaumcraft/common/lib/SoundsTC.java:114` |
| sound_event | `thaumcraft:pech_charge` | `thaumcraft:pech_charge` | `thaumcraft/common/lib/SoundsTC.java:134` |
| sound_event | `thaumcraft:pech_death` | `thaumcraft:pech_death` | `thaumcraft/common/lib/SoundsTC.java:133` |
| sound_event | `thaumcraft:pech_dice` | `thaumcraft:pech_dice` | `thaumcraft/common/lib/SoundsTC.java:131` |
| sound_event | `thaumcraft:pech_hit` | `thaumcraft:pech_hit` | `thaumcraft/common/lib/SoundsTC.java:132` |
| sound_event | `thaumcraft:pech_idle` | `thaumcraft:pech_idle` | `thaumcraft/common/lib/SoundsTC.java:129` |
| sound_event | `thaumcraft:pech_trade` | `thaumcraft:pech_trade` | `thaumcraft/common/lib/SoundsTC.java:130` |
| sound_event | `thaumcraft:poof` | `thaumcraft:poof` | `thaumcraft/common/lib/SoundsTC.java:112` |
| sound_event | `thaumcraft:pump` | `thaumcraft:pump` | `thaumcraft/common/lib/SoundsTC.java:111` |
| sound_event | `thaumcraft:rumble` | `thaumcraft:rumble` | `thaumcraft/common/lib/SoundsTC.java:124` |
| sound_event | `thaumcraft:runicshieldcharge` | `thaumcraft:runicshieldcharge` | `thaumcraft/common/lib/SoundsTC.java:140` |
| sound_event | `thaumcraft:runicshieldeffect` | `thaumcraft:runicshieldeffect` | `thaumcraft/common/lib/SoundsTC.java:139` |
| sound_event | `thaumcraft:scan` | `thaumcraft:scan` | `thaumcraft/common/lib/SoundsTC.java:137` |
| sound_event | `thaumcraft:shock` | `thaumcraft:shock` | `thaumcraft/common/lib/SoundsTC.java:135` |
| sound_event | `thaumcraft:spill` | `thaumcraft:spill` | `thaumcraft/common/lib/SoundsTC.java:100` |
| sound_event | `thaumcraft:squeek` | `thaumcraft:squeek` | `thaumcraft/common/lib/SoundsTC.java:103` |
| sound_event | `thaumcraft:swarm` | `thaumcraft:swarm` | `thaumcraft/common/lib/SoundsTC.java:105` |
| sound_event | `thaumcraft:swarmattack` | `thaumcraft:swarmattack` | `thaumcraft/common/lib/SoundsTC.java:106` |
| sound_event | `thaumcraft:tentacle` | `thaumcraft:tentacle` | `thaumcraft/common/lib/SoundsTC.java:144` |
| sound_event | `thaumcraft:ticks` | `thaumcraft:ticks` | `thaumcraft/common/lib/SoundsTC.java:109` |
| sound_event | `thaumcraft:tool` | `thaumcraft:tool` | `thaumcraft/common/lib/SoundsTC.java:142` |
| sound_event | `thaumcraft:upgrade` | `thaumcraft:upgrade` | `thaumcraft/common/lib/SoundsTC.java:145` |
| sound_event | `thaumcraft:urnbreak` | `thaumcraft:urnbreak` | `thaumcraft/common/lib/SoundsTC.java:159` |
| sound_event | `thaumcraft:wand` | `thaumcraft:wand` | `thaumcraft/common/lib/SoundsTC.java:122` |
| sound_event | `thaumcraft:wandfail` | `thaumcraft:wandfail` | `thaumcraft/common/lib/SoundsTC.java:123` |
| sound_event | `thaumcraft:whispers` | `thaumcraft:whispers` | `thaumcraft/common/lib/SoundsTC.java:146` |
| sound_event | `thaumcraft:wind` | `thaumcraft:wind` | `thaumcraft/common/lib/SoundsTC.java:141` |
| sound_event | `thaumcraft:wispdead` | `thaumcraft:wispdead` | `thaumcraft/common/lib/SoundsTC.java:120` |
| sound_event | `thaumcraft:wisplive` | `thaumcraft:wisplive` | `thaumcraft/common/lib/SoundsTC.java:121` |
| sound_event | `thaumcraft:write` | `thaumcraft:write` | `thaumcraft/common/lib/SoundsTC.java:116` |
| sound_event | `thaumcraft:zap` | `thaumcraft:zap` | `thaumcraft/common/lib/SoundsTC.java:98` |
| theory_aid | `AidBasicAlchemy` | `BasicAlchemy` | `thaumcraft/common/config/ConfigResearch.java:264` |
| theory_aid | `AidBasicArtifice` | `BasicArtifice` | `thaumcraft/common/config/ConfigResearch.java:265` |
| theory_aid | `AidBasicAuromancy` | `BasicAuromancy` | `thaumcraft/common/config/ConfigResearch.java:267` |
| theory_aid | `AidBasicEldritch` | `BasicEldritch` | `thaumcraft/common/config/ConfigResearch.java:269` |
| theory_aid | `AidBasicGolemancy` | `BasicGolemancy` | `thaumcraft/common/config/ConfigResearch.java:268` |
| theory_aid | `AidBasicInfusion` | `BasicInfusion` | `thaumcraft/common/config/ConfigResearch.java:266` |
| theory_aid | `AidBeacon` | `Beacon` | `thaumcraft/common/config/ConfigResearch.java:271` |
| theory_aid | `AidBookshelf` | `Bookshelf` | `thaumcraft/common/config/ConfigResearch.java:258` |
| theory_aid | `AidBrainInAJar` | `BrainInAJar` | `thaumcraft/common/config/ConfigResearch.java:259` |
| theory_aid | `AidEnchantmentTable` | `EnchantmentTable` | `thaumcraft/common/config/ConfigResearch.java:270` |
| theory_aid | `AidGlyphedStone` | `GlyphedStone` | `thaumcraft/common/config/ConfigResearch.java:260` |
| theory_aid | `AidPortal.AidPortalCrimson` | `PortalCrimson` | `thaumcraft/common/config/ConfigResearch.java:263` |
| theory_aid | `AidPortal.AidPortalEnd` | `PortalEnd` | `thaumcraft/common/config/ConfigResearch.java:261` |
| theory_aid | `AidPortal.AidPortalNether` | `PortalNether` | `thaumcraft/common/config/ConfigResearch.java:262` |
| theory_card | `CardAnalyze` | `Analyze` | `thaumcraft/common/config/ConfigResearch.java:273` |
| theory_card | `CardAwareness` | `Awareness` | `thaumcraft/common/config/ConfigResearch.java:295` |
| theory_card | `CardBalance` | `Balance` | `thaumcraft/common/config/ConfigResearch.java:274` |
| theory_card | `CardBeacon` | `Beacon` | `thaumcraft/common/config/ConfigResearch.java:283` |
| theory_card | `CardCalibrate` | `Calibrate` | `thaumcraft/common/config/ConfigResearch.java:288` |
| theory_card | `CardCelestial` | `Celestial` | `thaumcraft/common/config/ConfigResearch.java:284` |
| theory_card | `CardChannel` | `Channel` | `thaumcraft/common/config/ConfigResearch.java:292` |
| theory_card | `CardConcentrate` | `Concentrate` | `thaumcraft/common/config/ConfigResearch.java:285` |
| theory_card | `CardCurio` | `Curio` | `thaumcraft/common/config/ConfigResearch.java:280` |
| theory_card | `CardDarkWhispers` | `DarkWhispers` | `thaumcraft/common/config/ConfigResearch.java:300` |
| theory_card | `CardEnchantment` | `Enchantment` | `thaumcraft/common/config/ConfigResearch.java:282` |
| theory_card | `CardExperimentation` | `Experimentation` | `thaumcraft/common/config/ConfigResearch.java:279` |
| theory_card | `CardFocus` | `Focus` | `thaumcraft/common/config/ConfigResearch.java:294` |
| theory_card | `CardGlyphs` | `Glyphs` | `thaumcraft/common/config/ConfigResearch.java:301` |
| theory_card | `CardInfuse` | `Infuse` | `thaumcraft/common/config/ConfigResearch.java:293` |
| theory_card | `CardInspired` | `Inspired` | `thaumcraft/common/config/ConfigResearch.java:281` |
| theory_card | `CardMeasure` | `Measure` | `thaumcraft/common/config/ConfigResearch.java:291` |
| theory_card | `CardMindOverMatter` | `MindOverMatter` | `thaumcraft/common/config/ConfigResearch.java:289` |
| theory_card | `CardNotation` | `Notation` | `thaumcraft/common/config/ConfigResearch.java:275` |
| theory_card | `CardPonder` | `Ponder` | `thaumcraft/common/config/ConfigResearch.java:276` |
| theory_card | `CardPortal` | `Portal` | `thaumcraft/common/config/ConfigResearch.java:302` |
| theory_card | `CardReactions` | `Reactions` | `thaumcraft/common/config/ConfigResearch.java:286` |
| theory_card | `CardRealization` | `Realization` | `thaumcraft/common/config/ConfigResearch.java:304` |
| theory_card | `CardReject` | `Reject` | `thaumcraft/common/config/ConfigResearch.java:278` |
| theory_card | `CardRethink` | `Rethink` | `thaumcraft/common/config/ConfigResearch.java:277` |
| theory_card | `CardRevelation` | `Revelation` | `thaumcraft/common/config/ConfigResearch.java:303` |
| theory_card | `CardScripting` | `Scripting` | `thaumcraft/common/config/ConfigResearch.java:298` |
| theory_card | `CardSculpting` | `Sculpting` | `thaumcraft/common/config/ConfigResearch.java:297` |
| theory_card | `CardSpellbinding` | `Spellbinding` | `thaumcraft/common/config/ConfigResearch.java:296` |
| theory_card | `CardStudy` | `Study` | `thaumcraft/common/config/ConfigResearch.java:272` |
| theory_card | `CardSynergy` | `Synergy` | `thaumcraft/common/config/ConfigResearch.java:299` |
| theory_card | `CardSynthesis` | `Synthesis` | `thaumcraft/common/config/ConfigResearch.java:287` |
| theory_card | `CardTinker` | `Tinker` | `thaumcraft/common/config/ConfigResearch.java:290` |
| worldgen/biome | `thaumcraft:eerie` |  | `thaumcraft/Registrar.java:133` |
| worldgen/biome | `thaumcraft:eldritch` |  | `thaumcraft/Registrar.java:135` |
| worldgen/biome | `thaumcraft:magical_forest` | `thaumcraft:magical_forest` | `thaumcraft/Registrar.java:131` |

Варианты metadata, выражения всех регистраций рецептов, хеши исходников и полный список медиа находятся в `reference/content-inventory.json`.
