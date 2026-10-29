# Портирование — alpha.9.7

Цель: пользовательский Thaumcraft 6.1.BETA26 → Minecraft 1.21.1 / NeoForge / Curios. Реализован основной модуль генерации и зависимые механики; полный оригинальный мод не завершён. Список ограничений — в README.ru.md.

## Архитектура

* DeferredRegister: блоки, предметы, существа, эффекты, features, тип структуры и части кургана.
* JSON-реестры 1.21.1: configured/placed features, biome modifiers, biome, structure/structure_set, loot_table, recipe, structure NBT. Названия каталогов соответствуют 1.21.1.
* TerraBlender 4.1.0.8 добавляет регион магического леса с климатическими точками леса/берёзового леса. Лес регистрируется в common setup; этот этап не подтверждён полноценной загрузкой мира.
* Аура: сериализуемый NeoForge Attachment каждого LevelChunk. Серверный поток, шаг раз в 20 тиков, только getChunkNow для соседей. Никакого фонового изменения блоков или принудительной загрузки чанков. Чанк помечается изменённым, когда значения поменялись.
* AuraState изолирует исходные уравнения; AuraCodec сохраняет дробные значения и ограничивает повреждённые входные числа. Seed детерминирован по миру и координатам. Состояние создаётся лениво, в том числе в уже существующем чанке.
* Генераторы деревьев адаптированы из исходных алгоритмов; листва получает расстояние до ствола, чтобы работать с современным decay. Деревья не заменяют непрозрачные препятствия.
* Курган переведён из последовательности setBlock в два палитровых NBT-шаблона. Имя template сохраняется с namespace, включая загрузку после перезапуска. Старая metadata лестниц/ступеней и block entities переведена в современные состояния.
* Разломы синхронизируют seed/size/stability/collapse; геометрия пересчитывается детерминированно на клиенте. Renderer использует современный RenderType. Старые GL11/Tessellator-вызовы не копировались.
* Теги биомов заменяют Forge BiomeDictionary. Состав аспектов мира хранится в отдельном world-aspect-tags.json; происхождение записано в reference/world-aspect-migration.json.
* Новый импортёр tools/import_worldgen.py повторяемо восстанавливает ресурсы из пользовательского JAR, декомпилированного исходника, MCP stable 39 и локальных данных ванили. Сначала запускать import_legacy.py, затем import_worldgen.py: базовый импортёр перезаписывает локализации/базовые loot tables.
* Игровые ресурсы используют singular item/block atlas paths; alpha.9 дополнительно подключает исходные items/blocks для моделей книги. Исходные изображения сохраняются побайтно, кроме явно отмеченных точных вырезок иконок эффектов.
* Datagen-классы подписаны только на клиентское окружение через EventBusSubscriber; общий конструктор мода не ссылается на PackOutput/DataGenerator, отсутствующие в серверном runtime.
* Runtime не содержит классов Baubles/Forge 1.12.2. Curios подключён отдельно; TerraBlender — заменяемый nested JAR. Лицензии и точный снимок исходников TerraBlender вложены в META-INF/third-party.

## Дальнейшая проверка

1. Клиент: выпекание OBJ и multipart-моделей, все текстуры, траектории снарядов, вид разлома, GUI и разные ресурс-паки.
2. Новый мир: TerraBlender действительно распределяет биом; реальные чанки создаются без cascading generation; структуры и порталы сохраняются после перезапуска.
3. Сервер: сохранность ауры при выгрузке, путешествия/смерть, изменения конфигурации, права команд, удалённое подключение. Модульные тесты и datagen этого не доказывают.
4. Нормальная Gradle-сборка и подготовленные GameTest. В этой сессии Minecraft EULA автоматически не принималась, серверные GameTest не выполнялись.
5. Закрывать описанные в README пробелы исходного поведения: пехи, заражённая фауна/наросты, багровая экипировка/исследования, инструменты работы с аурой и разломами. Статус новых систем и оставшийся объём — в PORTING-16.md.


## Изменения архитектуры alpha.5

- `PortSounds` регистрирует исходный sound bank; `DeferredSoundType` откладывает обращение к звуковому реестру до его заполнения. Исправлено расхождение имени `runicshieldcharge`/`runicshieldecharge` в оригинале.
- `PortParticles`, `LegacyParticle` и `WorldFxPayload`: оригинальный лист 1024×1024, исходные координаты кадров/сроки жизни базовых эффектов, ограниченный S2C-пакет для вспышек/дуг/цветных частиц. Нет C2S-пакета произвольного эффекта.
- `ResearchCatalog` сохраняет исходные JSON; `ResearchState` и `ResearchCodec` расширяют серверное SavedData до schema 2 с миграцией schema 1. `ResearchActionPayload` содержит только ключ и ожидаемый этап; сервер проверяет книгу в руке, все условия и платежи. Повтор с устаревшим этапом не расходует ресурсы.
- Глубокое исследование адаптировано к нижней границе современного мира: minBuildHeight+10; высотное — maxBuildHeight×0,4. Расстояния и прыжки берутся из серверной статистики.
- `RecipeAspects` выводит состав через реальные рецепты; 32 уровня/512 рекурсивных обращений ограничивают циклы. Кеш очищается при синхронизации datapack. NBT/зелья/зачарования и полная совместимость с чужими динамическими рецептами пока не покрыты.
- `ArcaneRecipe` и `CrucibleRecipe` — современные типы рецептов с MapCodec/StreamCodec. Серверное меню верстака повторно ищет рецепт на каждый клик результата, проверяет оплату и возвращает recipe-specific остатки. Нельзя взять результат через обычный Slot без проверки.
- `WorkbenchEntity` сохраняет 15 слотов через ContainerHelper с компонентами ItemStack. `CrucibleState` отделяет расчёты от мира; BlockEntity сохраняет воду, тепло, таймер и аспекты. Клиентский BER рисует поверхность воды из стандартного атласа.
- Предметы, выброшенные тиглем, помечены только на ItemEntity: они не растворяются тут же повторно; после подбора и нового броска метки уже нет. Исследование катализатора проверяется по реальному владельцу брошенного предмета.
- `MachineFixtures` в datagen проверяет настоящие зарегистрированные рецепты, block entities, сохранение компонентов и все 128 новых имён ванильных объектов.

### Повторяемый импорт

Последовательно: `import_legacy.py`, `import_worldgen.py`, `import_presentation.py`, `import_research.py`, `import_vanilla_aspects.py`, `import_machines.py`. Передать исходный пользовательский JAR и исходники/MCP там, где требует аргумент `--help`. Старые импортёры перезаписывают общие локализации; порядок имеет значение. Новые локализации интерфейса, написанные вручную, следует сохранять при полном восстановлении проекта; это ещё не единый idempotent datagen-процесс.

`reference/*provenance.json` фиксирует точные копии и адаптации. Все модельные PNG идут в singular `textures/block`/`textures/item`; частицы — в `textures/particle`. Содержимое изображений и звуков не перерисовано.


## Alpha.6: essentia

`essentia/` содержит одноаспектный EssentiaTank, чистые SuctionRules/SmelterState, sided capability с симуляцией и серверными транзакциями, block entities печей/банок/труб/центрифуги. Поиск соседей проверяет `hasChunkAt` и не подгружает чанки. Колбы используют `thaumcraft:essentia_aspect`; предметы банок — `minecraft:block_entity_data`. Транспорт проверяет источник и получателя до списания. При нарушении контракта сторонней capability после успешной симуляции выбрасывается ошибка, а не выполняется скрытое восстановление/дублирование.

Добавьте последним в цепочку импортов: `python tools/import_essentia.py --jar <исходный-TC6.jar>`. Он сохраняет исходные PNG и UV, переносит texture paths в `block/`/`item/`, создаёт блоковые состояния, рецепты и локализацию. Обычный повторный импорт предыдущих этапов затем требует повторного запуска новых импортёров (они дополняют локали). Для реконструкции 16 этапов используйте ведомость и исходный архив, а не количество зарегистрированных объектов.

Исторические ограничения alpha.6 (бирки/распорки/мехи дополнены в alpha.7–8): закрытый клапан запрещает оба направления, направленная труба ограничивает реальные output-порты; это предотвращает обход направления сторонними capability. Вместо перчатки, ещё не перенесённой, стороны труб переключаются Shift+таумометром. Бирки пока отображаются на северной стороне; крышки/распорки банок, полная исходная геометрия соединений и точная частица выпуска пара отсутствуют. Мехи и расширения печи пока не учитываются. Выбор между одинаковыми магическими раскладками учитывает доступные кристаллы, затем стабильный идентификатор рецепта.

Историческая сборка alpha.6–7: в локальном javac дополнительно использовался реальный `ItemProperties.class` из NeoForge 21.1.264 dev-runtime: он содержит публичный overload регистрации ItemPropertyFunction, отсутствующий в непатченом vanilla JAR. Выдуманных stub-классов нет; этот класс не включается в мод. Основной classpath — реальные NeoForge 21.1.259/Curios/MC 1.21.1. Datagen alpha.6–7 выполнялся на 21.1.264. Alpha.8 использует Gradle и настоящий patched API 21.1.259 без подстановки ItemProperties. Полноценную совместимость с установленной сборкой пользователя ещё необходимо проверить в игре.


## Alpha.7: essentia extensions

MixedEssentia is a bounded multi-aspect store. BufferEntity shares the tube type/factory and preserves all face settings. Pull requests the sink's typed aspect before consulting a mixed source's arbitrary advertised aspect. Buffer priorities compare compatible consumers, suction and open ports. Bellows are evaluated every 20 ticks at buffers. Smelter upgrades preserve the original timing and independent 0.333 vent trials; only loaded neighboring blocks are examined.

Thaumatorium uses both registered halves and redirects the upper item handler to the lower inventory. Salis Mundus installs both states before neighbor notification. Breaking a half restores the other construct; destroying the crucible dismantles both. The server catalog uses stable recipe IDs instead of legacy integer hashes. Preview slots cannot be picked up, quick-moved, cloned or dragged into. Selection revalidates distance, catalyst and research. Crafting checks the saved author research even offline, requires every aspect and emits the output before charging resources. Like TC6, completing a recipe clears accumulated essentia, including leftovers after switching recipes. Limits and persisted data are bounded.

Original bellows sound mapping: MCP stable fields.csv field_187557_bK = ENTITY_GHAST_SHOOT; volume 0.01, pitch 0.5 ± original randomized offset. Clockwork mind recipe field_151132_bS = COMPARATOR. These mappings were verified locally. Thaumatorium OBJ is byte-identical; MTL absolute Windows texture paths were normalized to thaumcraft:block/thaumatorium. Historical alpha.7 presentation differences (nozzle/furnace added in alpha.8): modern GUI layout, vanilla cloud steam, simplified buffer choke markers, missing bellows-buffer extension nozzle. Vanilla furnace acceleration is not implemented. Existing research progression gaps remain; debug research grants are required for blocked research branches.

See verification/STATUS.md for the exact boundary between codec/algorithm tests and unexecuted world/client tests.

## Alpha.8: банки и ванильная печь

Следующий импортёр: `python tools/import_essentia_refinements.py --jar <TC6.jar>` после импортёров alpha.7. Исходные PNG копируются без изменения, происхождение хранится в `reference/essentia-refinements-provenance.json`.

`EssentiaSource` — отдельный несайдовый интерфейс будущих дистанционных потребителей; распорка не меняет `EssentiaTransport`. Перегонный куб источником этого API не является. Полный поиск источников/лучи/потребители пока отсутствуют. Portable NBT сохраняет содержимое и фильтр, но исключает распорку/ориентацию; распорка выпадает отдельным предметом.

Три поля `AbstractFurnaceBlockEntity` открыты через стандартный `META-INF/accesstransformer.cfg`. ModDev 2.0.148 обнаруживает этот путь автоматически; включена проверка целей AT. Мехи меняют только незавершённый прогресс обычной горящей печи и не создают выход/XP. Жёсткая граница TC6 199 заменена `cookingTotalTime - 1`, погасшая печь не ускоряется. Документация: https://github.com/neoforged/ModDevGradle#access-transformers и https://docs.neoforged.net/docs/1.21.1/advanced/accesstransformers/ .

Сценарии проверки — `JARS-BELLOWS-CHECK.ru.md`; фактические проверки и ограничения — `verification/STATUS.md`.


## Alpha.8: визуальные исправления

Запускать `python tools/import_visual_fixes.py --jar <TC6.jar>` **после остальных импортёров alpha.8**, после `import_essentia_refinements.py`. Ранние импортёры worldgen/машин создают старые модели, поэтому после их запуска этот шаг обязателен. Происхождение ресурсов и преобразования MTL — `reference/visual-fixes-provenance.json`. Исходная crystal.obj копируется побайтно, PNG не перерисовываются.

`CrystalModel` использует NeoForge OBJ-loader: восемь исходных групп, отдельное запекание для шести направлений, от одного до четырёх осколков на сторону. Выбор зависит от позиции; при увеличении размера прежние осколки сохраняются. MTL переведён на atlas path и tint index. Полный клиентский bake пока не запускался.

`NitorEntity` испускает пламя каждый клиентский тик, белое ядро — каждые десять. `NitorParticle` использует исходный лист 64×64 кадров: 264–271 для пламени, 457 для ядра. Пламя использует SRC_ALPHA/ONE, ядро обычное alpha-смешивание. Блок не рисует статическую модель. Ленивый запрос block entity через animateTick обслуживает нитор из старых alpha-миров; эту миграцию ещё надо проверить в игре.

В Minecraft 1.21.1 `Screen.render` вызывает `renderBackground`. После отрисовки содержимого `ResearchScreen` и `ThaumonomiconScreen` теперь вызывают только render виджетов, поэтому фон не размывает текст повторно.

Повороты труб учитывают отрицательные углы кватерниона `BlockModelRotation`. Проверки применяют реальное преобразование Minecraft к концу каждого соединения. `TubeEntity` и `BufferEntity` обновляют соединения при первом тике и раз в 20 тиков, не загружая соседние чанки принудительно.

Alpha.8 собрана Gradle 9.2.1 / ModDev 2.0.148 с настоящими patched API NeoForge 21.1.259. Для ограниченной среды использованы предварительная загрузка Maven и fallback пути Java в инструменте NeoForm Runtime; это подробно зафиксировано в `verification/alpha8/build-environment.json`. Итоговые проверки и их границы — `verification/STATUS.md`.


## Alpha.8.1: отображение тигля и верстака

У `models/block/arcane_workbench.json` и `crucible.json` добавлен родитель `minecraft:block/block`. Предметы уже наследуют эти модели. Родитель предоставляет ванильные ракурсы GUI, первой/третьей персоны, выброшенного предмета и рамки, а дочерние модели сохраняют исходные элементы и UV. Импортёр `import_machines.py` устанавливает этот родитель при повторном импорте. Проверки текущего выпуска — `verification/alpha8_1/`.


## Alpha.8.2: имена аспектов

Исходный TC6 разделяет `getName()` (латинский tag с заглавной буквы) и `getLocalizedDescription()` (значение `tc.aspect.<id>`). В порте описания ошибочно использовались вместо имён. `Aspect.displayName()` повторяет правило исходного имени; `Aspect.describe()` форматирует наборы для сообщений устройств. Все соответствующие подсказки и экран аспектов используют имя. Ключи описаний сохранены для дальнейшего переноса; ID, цвета, компоненты, рецепты и формат сохранений не менялись. Генератор enum в `import_legacy.py` и названия скоплений в `import_worldgen.py` обновлены. Проверки — `verification/alpha8_2/`.

## Alpha.9: исходное представление книги и сканер

После всей цепочки alpha.8 выполнить `import_research_presentation.py --jar <TC6.jar>`, `import_book_recipes.py --source <декомпилированный-каталог-thaumcraft> --fields <MCP-fields.csv>`, `import_book_models.py --jar <TC6.jar>`. Для рецептов использован официальный MCP snapshot 20171003-1.12; отчёт сохраняет исходные выражения и отдельно раскрытые динамические варианты. Все модели книги — дополнительные клиентские модели; они не создают фиктивных игровых предметов. Исключения исходных recipe ID записаны в `reference/book-recipe-import.json`.

ResearchScreen / ResearchPageScreen / BookDocument / BookAssets разделяют дерево, страницы, разметку и отображение исходных ресурсов. RecipeBook использует отдельный презентационный каталог. BookReadPayload сохраняет только отметку чтения; ResearchActionPayload по-прежнему проверяет оплату и этап на сервере. JournalPayload может обновить локальное отображение без открытия книги; дополнительные scan/aura-поля не сохраняются в журнал игрока.

Scanner использует исходный допуск выбора существа, блоковую дальность игрока, проверку видимости и серверный cooldown. Небо проверяется по углам, фазе луны, доступу к небу и Overworld; дневные флаги используют gameTime. Сохраняется временная замена перчатки таумометром для труб/тигля из alpha.8: эти жесты не являются оригинальным управлением TC6. Показ известных аспектов у прицела пока является адаптацией порта; исходное размещение тегов над объектами в мире не восстановлено.

Проверки: `python tools/verify_book.py`, `python tools/verify_resources.py --jar dist/<JAR>`, `python tools/verify_presentation.py`, `./gradlew test`, `./gradlew runData`. Точный графический паритет требует клиента с OpenGL; самостоятельная загрузка PNG в атлас этого не доказывает.

## Alpha.9.1: GUI-текстуры и графическое состояние

Minecraft 1.21.1 NativeImage.read вызывает PngInfo.validateHeader до STB-декодирования. Поэтому прямые JPG-ссылки из TC6 дают missing texture, хотя файл присутствует в JAR. Importer presentation сохраняет исходные JPG и дополнительно создаёт PNG; RGB-пиксели сверяются verify_book.py, происхождение указано в converted_backgrounds. GUI-фоны не входят в sprite atlas: прежний тест атласа не мог обнаружить эту ошибку. GuiTextureSmokeTest воспроизводит отказ JPG и проверяет все GUI PNG настоящим NativeImage.

BookAssets.prepareImage завершает отложенную отрисовку и восстанавливает blend state перед каждым изображением: GuiGraphics.blit сам его не включает, а render types предметов/шрифта сбрасывают при завершении. Фоновый слой дерева отделён от атласа схем рецептов. Автоматического OpenGL-сравнения со скриншотами здесь нет.

Административная ветка grant all доступна только на уровне разрешений 2 и завершается на сервере. ResearchState.completeCatalog завершает все стадии записей, включая записи с уже установленными флагами; повтор не меняет данные. Она не выдаёт знания/предметы/искажение и не добавляет выдуманные сканирования.

## Alpha.9.2: меню магического верстака и печей

Исходник GuiArcaneWorkbench использует arcaneworkbench.png, область 190×234, результат (160,64), сетку от (40,40) с шагом 24, инвентарь (16,151) и хотбар Y=209. Кристаллы размещены по ContainerArcaneWorkbench.xx/yy; порядок исходного ShardType (Aer, Ignis, Aqua, Terra, Ordo, Perditio) отличается от сохранённых слотов порта (Aer, Terra, Ignis, Aqua, Ordo, Perditio). Поэтому сохранён прежний порядок слотов, а позиции и фаза анимации используют отображение 0,3,1,2,4,5.

WorkbenchMenu передаёт восемь целых параметров: стоимость, доступный Vis и шесть расходов кристаллов. Протокол повышен до 4. Затемнённый результат вычисляется клиентом только для изображения; он не добавляется в Slot/Container. Выдача/расход остаются в существующей серверной транзакции. Скидки Vis не имитируются: соответствующая экипировка ещё не перенесена.

SmelterScreen использует исходные размеры/координаты, восстанавливает blend state и не добавляет чужих заголовков. Для нулевого срока топлива использован исходный знаменатель 200; значения шкал ограничены окном при рассинхронизации. Нижняя грань smelter_basic_on/off переведена с blocks/furnace_top на minecraft:block/furnace_top. Исправление включено в import_essentia.py, а import_machines.py теперь самостоятельно импортирует GUI-текстуру верстака.

verify_machine_presentation.py сравнивает геометрию/UV и текстуры с исходным JAR, разрешает родителей и ванильные ссылки по реальному Minecraft client JAR, проверяет все варианты поворота/горения. MachineFixtures добавляет 30 проверок с реальными Menu/Slot/ItemStack и round-trip сохранением, без сервера или игровых заглушек.

## Достижения alpha.9.4

В TC6 BETA26 нет исходных advancement JSON или собственного регистратора критериев. Новый `ResearchAdvancements` регистрирует `thaumcraft:research` через DeferredRegister; настоящий SimpleCriterionTrigger проверяет серверное ResearchState раз в секунду. Minecraft удаляет завершённые слушатели и сохраняет прогресс обычным способом. Старые сохранения, вход и /reload учитываются тем же путём, без нового C2S-пакета и без записи в исследовательский прогресс.

`tools/build_research_advancements.py --legacy-jar ORIGINAL.jar` запускается после импортёров книги/моделей. Он строит все 156 advancement JSON, значки с CustomModelData и явные источники новых спрайтов для атласа. 137 иконок используют скрытый из творческой вкладки `research_icon`, остальные — реальные зарегистрированные предметы. Цвета фокусов задаёт обработчик цветов клиента. Граф отображения использует одного родителя, как требует Minecraft; исходные множественные требования проверяет система исследований.

## Alpha.9.5: theorycraft host and persistence

`TheorySession` and `TheoryCards` retain the original registered set, weighted aid pools, category order, inspiration arithmetic, inclusive random ranges and raw-knowledge rounding. `TheoryItems` bridges the real item components, main inventory and recipe-derived aspects. `ResearchTableEntity` validates the entire payment before committing; `TheoryMenu` adds a per-open nonce, a persistent revision, menu/distance checks and UUID ownership. Protocol is now 5. Chunk updates only contain the visual active flag and the two slots; full cards are sent to menu viewers. Saved sheet history is capped at 256 visual entries.

The original Analyze null-category bug and the lack of registration for Truth/DragonEgg are explicitly retained. The source registration matrix and unchanged artwork hashes are in `reference/theory-parity.json`. Eight of fourteen original aid objects exist in gameplay; definitions for the other six await their real devices. Cross-mod ore-dictionary equivalence remains incomplete. Aspect-valued cards refresh after recipe reload without rerolling their required items.

The original GUI coordinates, paper UVs, deck motion, icons, sound cues, model pivots and glyph atlas rows are ported. Rendering has not been verified in a running client. `TheoryFixtures` exercises real NeoForge registries, NBT, slots, components and both wire codecs during datagen. The JUnit suite covers card guards, independent golden arithmetic, save/replay protection and recipe-reload valuation; it is not a substitute for client/server play tests.
