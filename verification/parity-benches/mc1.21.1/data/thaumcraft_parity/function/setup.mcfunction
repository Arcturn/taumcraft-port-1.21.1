# TEST WORLD ONLY: replaces blocks in x/z -16..16, y 64..78.
# Execute as a player in the Overworld. No automatic load/tick functions.
gamemode creative @s
difficulty normal
gamerule doDaylightCycle false
gamerule doWeatherCycle false
time set 6000
weather clear
fill -16 64 -16 16 64 16 minecraft:stone
fill -16 65 -16 16 78 16 minecraft:air
tp @s 0.5 65 14.5 180 10
setblock -12 65 -12 thaumcraft:arcane_workbench
setblock -9 65 -12 thaumcraft:crucible
setblock -6 65 -12 thaumcraft:table_wood
setblock -3 65 -12 thaumcraft:research_table
setblock 0 65 -12 thaumcraft:smelter_basic
setblock 3 65 -12 thaumcraft:smelter_thaumium
setblock 6 65 -12 thaumcraft:smelter_void
setblock 9 65 -12 thaumcraft:alembic
setblock 12 65 -12 thaumcraft:jar_normal
setblock -12 65 -8 thaumcraft:jar_void
setblock -9 65 -8 thaumcraft:tube
setblock -6 65 -8 thaumcraft:tube_valve
setblock -3 65 -8 thaumcraft:tube_filter
setblock 0 65 -8 thaumcraft:tube_oneway
setblock 3 65 -8 thaumcraft:tube_restrict
setblock 6 65 -8 thaumcraft:tube_buffer
setblock 9 65 -8 thaumcraft:centrifuge
setblock 12 65 -8 thaumcraft:bellows
setblock -12 65 -4 thaumcraft:crucible
setblock -12 66 -4 thaumcraft:thaumatorium
setblock -12 67 -4 thaumcraft:thaumatorium_top
setblock -6 65 -4 thaumcraft:nitor_white
setblock -3 65 -4 thaumcraft:nitor_orange
setblock 0 65 -4 thaumcraft:nitor_magenta
setblock 3 65 -4 thaumcraft:nitor_light_blue
setblock 6 65 -4 thaumcraft:nitor_yellow
setblock 9 65 -4 thaumcraft:nitor_lime
setblock 12 65 -4 thaumcraft:nitor_pink
setblock -12 65 0 thaumcraft:nitor_gray
setblock -9 65 0 thaumcraft:nitor_light_gray
setblock -6 65 0 thaumcraft:nitor_cyan
setblock -3 65 0 thaumcraft:nitor_purple
setblock 0 65 0 thaumcraft:nitor_blue
setblock 3 65 0 thaumcraft:nitor_brown
setblock 6 65 0 thaumcraft:nitor_green
setblock 9 65 0 thaumcraft:nitor_red
setblock 12 65 0 thaumcraft:nitor_black
setblock -12 65 4 thaumcraft:crystal_aer
setblock -9 65 4 thaumcraft:crystal_ignis
setblock -6 65 4 thaumcraft:crystal_aqua
setblock -3 65 4 thaumcraft:crystal_terra
setblock 0 65 4 thaumcraft:crystal_ordo
setblock 3 65 4 thaumcraft:crystal_perditio
setblock 6 65 4 thaumcraft:crystal_vitium
