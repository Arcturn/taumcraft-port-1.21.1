Актуальный единый чек-лист **alpha.9.9**: `docs/TESTING-alpha9.9.ru.md` и `docs/Thaumcraft-alpha9.9-testing-ru.docx`. Следующие разделы — исторические инструкции предыдущих альф; при расхождении использовать новый документ.

Для alpha.9.8 сначала **PARITY-CHECK.ru.md**: новые команды аудита и парный стенд. Далее действуют проверки alpha.9.7 ниже. Установить alpha.9.8 на обе стороны; протокол 5.

Для alpha.9.7 сначала выполнить **ORIGINAL-ICONS-CHECK.ru.md**, затем **THEORY-CHECK.ru.md**, затем **ADVANCEMENTS-CHECK.ru.md**, проверки интерфейсов и общий список ниже. Установить alpha.9.7 на клиент и сервер; протокол 5.


Дополнение alpha.8 с командами для распорок, бирок, заполнения банок и ванильной печи: `JARS-BELLOWS-CHECK.ru.md`.

> Дополнение alpha.6: см. `ESSENTIA-CHECK.ru.md` — готовый стенд эссенции, колбы, бирки, трубы и центрифуга. Следующий список описывает ранее перенесённое в alpha.5.

# Проверка промежуточной alpha.5

Этот список относится к реально добавленному содержимому. Это не финальный чек-ап всех 16 этапов. Команды требуют разрешённых читов/OP. Не смешивать JAR alpha.4 и alpha.5.

## Быстрый старт

```mcfunction
/gamemode creative
/difficulty normal
/gamerule keepInventory true
/give @s thaumcraft:thaumonomicon
/give @s thaumcraft:thaumometer
/give @s thaumcraft:salis_mundus 16
```

- [ ] В творческом меню нет чёрно-фиолетовых предметов и блоков.
- [ ] После F3+T текстуры и звуки остаются рабочими.
- [ ] Книга открывает дерево; его можно перетаскивать, масштаб дерева меняется колесом, страницы переключаются стрелками, Escape возвращает из страницы к дереву.
- [ ] Левая вкладка аспектов внутри страницы появляется после FIRSTSTEPS. Закрыть и снова использовать книгу — вернуться к дереву.
- [ ] Сканирование новых предметов даёт наблюдения; повторное — не даёт. Предмет выбросить на землю и навести на него таумометр; содержимое сундука сканировать Shift+ПКМ по сундуку.

## Обычная начальная цепочка

- [ ] Применить Salis Mundus к книжной полке, получить книгу.
- [ ] Открыть FIRSTSTEPS и начать исследование.
- [ ] Применить Salis Mundus к обычному верстаку. В книге сдать этап изготовления верстака.
- [ ] Скрафтить таумометр в магическом верстаке: четыре золотых слитка крестом, стеклянная панель в центре; по одному кристаллу каждого первичного аспекта; стоимость 20 Vis.
- [ ] Проверить запрет крафта без исследования, без одного кристалла и при нехватке Vis.
- [ ] В обычном рецепте Salis Mundus на магическом верстаке миска и кремень возвращаются.
- [ ] Предметы в верстаке переживают закрытие меню/выход из мира. При разрушении выпадает содержимое.
- [ ] Два игрока одновременно берут один результат: лишнего предмета и двойного расхода быть не должно. Это ещё не проверено в игровом runtime.

Отладочная подготовка верстака:

```mcfunction
/thaumcraft research grant FIRSTSTEPS
/thaumcraft aura vis 200
/give @s minecraft:gold_ingot 16
/give @s minecraft:glass_pane 4
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="aer"] 8
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="terra"] 8
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="ignis"] 8
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="aqua"] 8
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="ordo"] 8
/give @s thaumcraft:crystal_essence[thaumcraft:crystal_aspect="perditio"] 8
```

Команда `grant` — отладочная, она завершает выбранное исследование. Она не добавляет отсутствующие механики. Текущее состояние: `/thaumcraft research`. Наблюдения: `/thaumcraft research knowledge observation BASICS 16`; теория: `/thaumcraft research knowledge theory ALCHEMY 32`. Числа — сырые очки: 16 за единицу наблюдения, 32 за единицу теории.

## Тигель, нитор и алюментум

```mcfunction
/thaumcraft research grant UNLOCKALCHEMY
/setblock ~2 ~-1 ~ minecraft:magma_block
/setblock ~2 ~ ~ thaumcraft:crucible
/give @s minecraft:water_bucket
/give @s minecraft:coal 8
/give @s minecraft:glowstone_dust 8
```

1. Наполнить тигель ведром. Подождать около 8 секунд; пустой рукой можно прочитать воду, нагрев и аспекты.
2. Бросить клавишей Q **один уголь**, затем **одну светопыль**, затем **ещё одну светопыль**. Последняя становится катализатором; должен появиться жёлтый нитор. Бросать от имени игрока, который открыл исследование.
3. Проверить звук пузырьков/вспышку, расход 50 мБ воды, остатки аспектов, свет и окраску нитора.
4. Для очистки — Shift + ПКМ таумометром по тиглю. Остатки превращаются в Flux; сравнить `/thaumcraft aura` до/после.
5. Выйти и зайти: вода, нагрев и аспекты должны сохраниться. Оставленные аспекты со временем загрязняют ауру.
6. `/give @s thaumcraft:alumentum 8`: проверить топливо (4800 тиков) и бросок ПКМ. Снаряд разрушает блоки при попадании, проверять вдали от построек.

## Портал культистов и поведение

```mcfunction
/difficulty normal
/summon thaumcraft:cultist_portal_lesser ~5 ~ ~
/give @s minecraft:diamond_sword
/give @s minecraft:diamond_chestplate
/gamemode survival
```

- [ ] В пределах 32 блоков портал активируется. Нужна видимость игрока; Creative/Spectator не активируют его.
- [ ] Первый призыв — после задержки около 5 секунд; последующие примерно через 2,5–5 секунд при выполнении условий.
- [ ] При Normal предел — четыре живых культиста рядом. Убийство культиста позволяет порталу продолжить призыв.
- [ ] Рыцарь преследует и бьёт; жрец использует огненные снаряды и красную самонаводящуюся сферу. Сферу можно отбить ударом.
- [ ] Работают исходная анимация портала, вспышка призыва и звуки. Призывы тратят здоровье портала.
- [ ] Присутствие второго игрока в Creative рядом не мешает находить игрока в Survival.

Отдельные сущности:

```mcfunction
/summon thaumcraft:cultist_knight ~5 ~ ~
/summon thaumcraft:cultist_cleric ~8 ~ ~
/summon thaumcraft:wisp ~5 ~2 ~ {aspect:"ignis"}
/summon thaumcraft:flux_rift ~8 ~3 ~ {size:40,seed:12345,stability:0.0f}
/summon thaumcraft:taint_seed ~8 ~ ~
```

Висп обычно нейтрален: ударить его для проверки ответа, цвета атаки и звука. Разлом и заражение могут разрушать мир. Голый `/summon thaumcraft:flux_rift` теперь сохраняет размер по умолчанию; для заметного тестового разлома лучше размер 40.

## Генерация и сохранение

```mcfunction
/locate biome thaumcraft:magical_forest
/locate structure thaumcraft:mound
/place feature thaumcraft:greatwood_sapling
/place feature thaumcraft:silverwood_sapling
/thaumcraft aura
```

- [ ] Новые чанки: лес, руды, растения, кристаллы, курганы.
- [ ] Великие саженцы 2×2 и одиночный серебряный саженец растут; листва не исчезает сразу.
- [ ] Урны/ящики и пористый камень дают добычу.
- [ ] Аура, существа, контейнеры и исследования переживают перезапуск и путешествие между измерениями.
- [ ] В `latest.log` нет missing texture/model/sound и исключений пакетов/меню.

Диагностика оригинальных звуков без условий механики:

```mcfunction
/playsound thaumcraft:scan master @s ~ ~ ~ 1 1
/playsound thaumcraft:evilportal master @s ~ ~ ~ 1 1
/playsound thaumcraft:wisplive master @s ~ ~ ~ 1 1
/particle thaumcraft:craft ~ ~1 ~ 0 0 0 0 1 force
```
