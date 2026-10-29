package thaumcraft.port.research;

import java.util.*;

/** Read-only discovery predicates shared by the scanner, inventory scans and client highlighting. */
public final class ScanDiscoveries {
    private static final Map<String,Set<String>> FLAGS=new LinkedHashMap<>();
    private static void add(String subject,String... flags){FLAGS.computeIfAbsent(subject,k->new LinkedHashSet<>()).addAll(List.of(flags));}
    static {
        add("entity:thaumcraft:wisp","!Wisp","f_FLY");
        add("entity:thaumcraft:thaumic_slime","!ThaumSlime");
        add("entity:thaumcraft:cultist_knight","!CrimsonCultist");add("entity:thaumcraft:cultist_cleric","!CrimsonCultist");
        add("entity:thaumcraft:taint_seed","!TaintSeed");add("entity:thaumcraft:taint_seed_prime","!TaintSeed");
        add("entity:thaumcraft:flux_rift","!FluxRift","f_toomuchflux");
        for(String id:List.of("spider","cave_spider"))add("entity:minecraft:"+id,"f_SPIDER");
        for(String id:List.of("bat","parrot","ghast","blaze"))add("entity:minecraft:"+id,"f_FLY");
        add("entity:minecraft:bat","f_BAT","f_FLY");
        add("entity:minecraft:arrow","f_arrow");add("entity:minecraft:small_fireball","f_fireball");add("entity:minecraft:fireball","f_fireball");add("entity:minecraft:llama_spit","f_spit");
        for(String id:List.of("nether_portal","end_portal","end_portal_frame"))add("block:minecraft:"+id,"f_TELEPORT");
        add("item:minecraft:end_portal_frame","f_TELEPORT");
        add("entity:minecraft:enderman","f_TELEPORT");add("entity:minecraft:iron_golem","f_golem");
        add("item:minecraft:dispenser","f_DISPENSER");add("item:minecraft:arrow","f_arrow");
        add("item:minecraft:ender_pearl","f_TELEPORT");add("item:thaumcraft:void_seed","f_VOIDSEED");
        add("item:thaumcraft:brain","f_BRAIN");
        add("item:minecraft:dragon_breath","!DRAGONBREATH");add("item:minecraft:totem_of_undying","!TOTEMUNDYING");
        for(String id:List.of("clay_ball","terracotta","white_terracotta"))add("item:minecraft:"+id,"f_MATCLAY");
        for(String id:List.of("iron_ingot","iron_block","iron_ore","deepslate_iron_ore"))add("item:minecraft:"+id,"f_MATIRON");
        for(String metal:List.of("brass","thaumium","void"))for(String type:List.of("ingot_","metal_","plate_"))add("item:thaumcraft:"+type+metal,"f_MAT"+metal.toUpperCase(Locale.ROOT));
        for(String prefix:List.of("ore_","deepslate_ore_")){
            add("item:thaumcraft:"+prefix+"amber","ORE","!OREAMBER");
            add("item:thaumcraft:"+prefix+"cinnabar","ORE","!ORECINNABAR");
        }
        for(String aspect:List.of("aer","terra","ignis","aqua","ordo","perditio","vitium"))add("item:thaumcraft:crystal_"+aspect,"ORE","!ORECRYSTAL");
        for(String wood:List.of("greatwood","silverwood"))for(String type:List.of("log_","sapling_"))add("item:thaumcraft:"+type+wood,"PLANTS","!PLANTWOOD");
        for(String plant:List.of("shimmerleaf","cinderpearl","vishroom"))add("item:thaumcraft:"+plant,"PLANTS","!PLANT"+plant.toUpperCase(Locale.ROOT));
        add("item:thaumcraft:primordial_pearl","PRIMPEARL");
    }
    public static Set<String> subjectFlags(String subject) {
        if (subject.startsWith("entity:thaumcraft:wisp:")) subject="entity:thaumcraft:wisp";
        return Collections.unmodifiableSet(FLAGS.getOrDefault(subject, Set.of()));
    }

    public static boolean apply(ResearchState state, String subject) {
        boolean changed=false;
        for (String key:subjectFlags(subject)) if (!state.knows(key)) changed|=state.flag(key);
        return changed;
    }

    private static final Map<String,List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>>> MATERIALS=materials();
    private static Map<String,List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>>> materials() {
        var result=new LinkedHashMap<String,List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>>>();
        for (String metal:List.of("iron","brass","thaumium","void")) {
            var tags=new ArrayList<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>>();
            for (String form:metal.equals("iron")?List.of("ores","ingots","storage_blocks","plates"):List.of("ingots","storage_blocks","plates"))
                tags.add(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.ResourceLocation.parse("c:"+form+"/"+metal)));
            result.put("f_MAT"+metal.toUpperCase(Locale.ROOT), List.copyOf(tags));
        }
        return Collections.unmodifiableMap(result);
    }

    public static Set<String> itemFlags(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return Set.of();
        var flags=new LinkedHashSet<>(subjectFlags("item:"+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())));
        MATERIALS.forEach((flag,tags)->{if(tags.stream().anyMatch(stack::is))flags.add(flag);});
        var enchantments=stack.getOrDefault(stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)
                ?net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS:net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var holder:enchantments.keySet()) holder.unwrapKey().ifPresent(key->{
            var contents=holder.value().description().getContents();
            String description=contents instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                    ?translated.getKey():"enchantment."+key.location().getNamespace()+"."+key.location().getPath();
            flags.add(LegacyScanKeys.enchantment(key.location().toString(),description));
        });
        var potions=stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        for(var effect:potions.getAllEffects()) flags.add(effectKey(effect.getEffect()));
        return Collections.unmodifiableSet(flags);
    }

    public static String effectKey(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        var id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        return LegacyScanKeys.effect(id==null?"":id.toString(),effect.value().getDescriptionId());
    }

    public static Set<String> blockFlags(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.item.ItemStack picked) {
        var flags=new LinkedHashSet<>(itemFlags(picked));
        flags.addAll(subjectFlags("block:"+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        // Material.CLAY no longer exists; this retains its vanilla block use, separately from item scans.
        if(state.is(net.minecraft.world.level.block.Blocks.CLAY)||state.is(net.minecraft.tags.BlockTags.TERRACOTTA))flags.add("f_MATCLAY");
        return Collections.unmodifiableSet(flags);
    }

    public static Set<String> entityTypeFlags(Class<? extends net.minecraft.world.entity.Entity> type,net.minecraft.resources.ResourceLocation id) {
        var flags=new LinkedHashSet<>(subjectFlags("entity:"+id));
        if(net.minecraft.world.entity.monster.Spider.class.isAssignableFrom(type))flags.add("f_SPIDER");
        if(net.minecraft.world.entity.ambient.Bat.class.isAssignableFrom(type)){flags.add("f_BAT");flags.add("f_FLY");}
        if(net.minecraft.world.entity.animal.Parrot.class.isAssignableFrom(type)||net.minecraft.world.entity.monster.Ghast.class.isAssignableFrom(type)
                ||net.minecraft.world.entity.monster.Blaze.class.isAssignableFrom(type))flags.add("f_FLY");
        if(net.minecraft.world.entity.animal.AbstractGolem.class.isAssignableFrom(type))flags.add("f_golem");
        if(net.minecraft.world.entity.monster.EnderMan.class.isAssignableFrom(type))flags.add("f_TELEPORT");
        if(net.minecraft.world.entity.projectile.AbstractArrow.class.isAssignableFrom(type))flags.add("f_arrow");
        if(net.minecraft.world.entity.projectile.Fireball.class.isAssignableFrom(type)||net.minecraft.world.entity.projectile.DragonFireball.class.isAssignableFrom(type)
                ||net.minecraft.world.entity.projectile.WitherSkull.class.isAssignableFrom(type))flags.add("f_fireball");
        if(net.minecraft.world.entity.projectile.LlamaSpit.class.isAssignableFrom(type))flags.add("f_spit");
        return Collections.unmodifiableSet(flags);
    }

    public static Set<String> entityFlags(net.minecraft.world.entity.Entity entity) {
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity item)return itemFlags(item.getItem());
        var flags=new LinkedHashSet<>(entityTypeFlags(entity.getClass(),net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
        if(entity instanceof net.minecraft.world.entity.LivingEntity living)
            for(var effect:living.getActiveEffects())flags.add(effectKey(effect.getEffect()));
        return Collections.unmodifiableSet(flags);
    }
    private ScanDiscoveries(){}
}
