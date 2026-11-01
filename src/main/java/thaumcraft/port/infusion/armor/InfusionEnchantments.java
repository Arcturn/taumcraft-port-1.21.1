package thaumcraft.port.infusion.armor;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;
import thaumcraft.port.infusion.recipe.InfusionAugments;

/** Real TC6 infusion enchantment effects; these are independent of vanilla enchantment slots. */
public final class InfusionEnchantments {
    public static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:ores"));
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    public static final java.util.function.Supplier<EntityType<FollowingItemEntity>> FOLLOWING_ITEM=ENTITIES.register("following_item",()->EntityType.Builder.<FollowingItemEntity>of(FollowingItemEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(1).build("thaumcraft:following_item"));
    public static final DeferredBlock<AirBlock> GLIMMER=PortContent.BLOCKS.register("effect_glimmer",()->new AirBlock(BlockBehaviour.Properties.of().noCollission().replaceable().air().noLootTable().strength(999).lightLevel(s->15)));
    private static final Map<ServerPlayer,Direction> FACES=new WeakHashMap<>();
    private static final Set<UUID> BREAKING=new HashSet<>(), BURROWING=new HashSet<>();
    private record Pending(BlockPos pos,ServerPlayer player,boolean destructive,boolean light,Direction face){}
    private static final Map<ServerLevel,List<Pending>> PENDING=new WeakHashMap<>();
    public static void register(IEventBus bus){ENTITIES.register(bus);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::left);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::right);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,InfusionEnchantments::breaking);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::drops);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::livingDrops);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::attack);NeoForge.EVENT_BUS.addListener(InfusionEnchantments::tick);}
    public static int level(ItemStack stack,String id){return InfusionAugments.getEnchantment(stack,id);}
    public static boolean ore(BlockState state){return state.is(ORES);}
    private static boolean effective(ItemStack tool,BlockState state){return !tool.isEmpty()&&(tool.isCorrectToolForDrops(state)||tool.getDestroySpeed(state)>1);}
    private static void left(PlayerInteractEvent.LeftClickBlock event){if(event.getEntity() instanceof ServerPlayer player)FACES.put(player,event.getFace());}
    private static void right(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!player.isShiftKeyDown())return;
        var tool=event.getItemStack();int rank=level(tool,"sounding");if(rank==0)return;
        tool.hurtAndBreak(5,player,event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        player.serverLevel().playSound(null,event.getPos(),PortSounds.get("wandfail"),SoundSource.BLOCKS,.2f,.2f+player.getRandom().nextFloat()*.2f);
        InfusionFxPayload.sounding(player,event.getPos(),rank);event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    private static void breaking(BlockEvent.BreakEvent event){
        if(event.isCanceled()||!(event.getPlayer() instanceof ServerPlayer player)||(BREAKING.contains(player.getUUID())||BURROWING.contains(player.getUUID()))||player.isShiftKeyDown())return;
        var tool=player.getMainHandItem();var state=event.getState();
        if(level(tool,"burrowing")==0||!effective(tool,state)||!(state.is(BlockTags.LOGS)||ore(state)))return;
        BlockPos furthest=furthest(player.serverLevel(),event.getPos(),state);
        if(furthest.equals(event.getPos()))return;
        event.setCanceled(true);BURROWING.add(player.getUUID());
        try{player.gameMode.destroyBlock(furthest);}finally{BURROWING.remove(player.getUUID());}
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(),event.getPos()));
    }
    /** Original monotonically outward walk: one farthest connected block per swing, not whole-vein mining. */
    public static BlockPos furthest(ServerLevel world,BlockPos origin,BlockState state){
        int reach=state.is(BlockTags.LOGS)?2:1;BlockPos current=origin;double distance=0;
        for(int iteration=0;iteration<4096;iteration++){
            BlockPos next=null;
            search:for(int x=-reach;x<=reach;x++)for(int y=reach;y>=-reach;y--)for(int z=-reach;z<=reach;z++){
                BlockPos pos=current.offset(x,y,z);int dx=pos.getX()-origin.getX(),dy=pos.getY()-origin.getY(),dz=pos.getZ()-origin.getZ();
                if(Math.abs(dx)>24||Math.abs(dy)>48||Math.abs(dz)>24)continue;
                if(!world.hasChunkAt(pos))continue;double d=origin.distSqr(pos);
                if(d>distance&&world.getBlockState(pos).equals(state)&&state.getDestroySpeed(world,pos)>=0){next=pos;distance=d;break search;}
            }
            if(next==null)break;current=next;
        }
        return current;
    }
    private static void drops(BlockDropsEvent event){
        if(!(event.getBreaker() instanceof ServerPlayer player))return;
        boolean silk=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),event.getTool())>0;
        if(!silk&&!effective(event.getTool(),event.getState()))return;
        var tool=event.getTool();int rank=level(tool,"refining");
        if(rank>0)for(var drop:event.getDrops()){var refined=refined(drop.getItem());if(!refined.isEmpty()&&event.getLevel().random.nextFloat()<(rank+1)*.125f)drop.setItem(refined);}
        if(player.isShiftKeyDown())return;
        if(level(tool,"collector")>0){var converted=new ArrayList<ItemEntity>();for(var drop:event.getDrops())converted.add(new FollowingItemEntity(drop,player));event.getDrops().clear();event.getDrops().addAll(converted);}
        boolean destructive=level(tool,"destructive")>0&&!BREAKING.contains(player.getUUID());boolean light=level(tool,"lamplight")>0;
        if(destructive||light)PENDING.computeIfAbsent(event.getLevel(),k->new ArrayList<>()).add(new Pending(event.getPos().immutable(),player,destructive,light,FACES.getOrDefault(player,player.getDirection().getOpposite())));
    }
    /** Modern raw ores take the same role as TC6 ore-block drops. Quartz is intentionally included. */
    public static ItemStack refined(ItemStack drop){
        String metal=null;var id=BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath();
        for(String candidate:List.of("iron","gold","copper","tin","silver","lead")){
            if(drop.is(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:raw_materials/"+candidate)))||drop.is(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ores/"+candidate)))){metal=candidate;break;}
        }
        if(drop.is(Items.QUARTZ))metal="quartz";
        if(id.equals("ore_cinnabar")||id.equals("deepslate_ore_cinnabar"))metal="cinnabar";
        if(metal==null)return ItemStack.EMPTY;var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:cluster_"+metal));return item==Items.AIR?ItemStack.EMPTY:new ItemStack(item,drop.getCount());
    }
    private static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel world))return;var pending=PENDING.remove(world);if(pending==null)return;
        for(var entry:pending){var player=entry.player;if(player==null||player.level()!=world||player.distanceToSqr(Vec3.atCenterOf(entry.pos))>1024)continue;
            if(entry.destructive&&level(player.getMainHandItem(),"destructive")>0){BREAKING.add(player.getUUID());try{for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                if(a==0&&b==0)continue;BlockPos pos=switch(entry.face.getAxis()){case X->entry.pos.offset(0,a,b);case Y->entry.pos.offset(a,0,b);case Z->entry.pos.offset(a,b,0);};
                var state=world.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(world,pos)<0||!effective(player.getMainHandItem(),state)||!world.mayInteract(player,pos))continue;
                player.gameMode.destroyBlock(pos);
            }}finally{BREAKING.remove(player.getUUID());}}
            if(entry.light&&world.getBlockState(entry.pos).isAir()&&world.getMaxLocalRawBrightness(entry.pos)<10&&world.mayInteract(player,entry.pos))world.setBlockAndUpdate(entry.pos,GLIMMER.get().defaultBlockState());
        }
    }
    private static void attack(AttackEntityEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getTarget() instanceof LivingEntity target))return;
        int rank=level(player.getMainHandItem(),"arcing");if(rank==0)return;int count=0;
        for(var other:player.serverLevel().getEntitiesOfClass(Mob.class,target.getBoundingBox().inflate(1.5+rank,1+rank/2d,1.5+rank))){
            if(other==target||!other.isAlive()||player.isAlliedTo(other)||other instanceof TamableAnimal pet&&pet.isOwnedBy(player))continue;
            if(other.hurt(player.damageSources().playerAttack(player),(float)player.getAttributeValue(Attributes.ATTACK_DAMAGE)*.5f)){
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(player.serverLevel(),other,player.damageSources().playerAttack(player));
                float yaw=player.getYRot()*(float)Math.PI/180;other.push(-Math.sin(yaw)*.5,.1,Math.cos(yaw)*.5);InfusionFxPayload.arc(player.serverLevel(),target,other);count++;
            }
            if(count>=rank)break;
        }
        if(count>0){InfusionFxPayload.arc(player.serverLevel(),player,target);player.serverLevel().playSound(null,player.blockPosition(),PortSounds.get("wind"),SoundSource.PLAYERS,1,.9f+player.getRandom().nextFloat()*.2f);}
    }
    private static void livingDrops(LivingDropsEvent event){
        if(!(event.getSource().getEntity() instanceof ServerPlayer player))return;var tool=player.getMainHandItem();boolean collect=level(tool,"collector")>0;
        if(collect){var converted=new ArrayList<ItemEntity>();for(var drop:event.getDrops())converted.add(new FollowingItemEntity(drop,player));event.getDrops().clear();event.getDrops().addAll(converted);}
        int rank=level(tool,"essence");if(rank==0)return;var random=event.getEntity().getRandom();int b=random.nextInt(5)<rank?0:99;
        var aspects=new LinkedHashMap<>(AspectTags.forEntity(event.getEntity()));
        while(b<rank&&!aspects.isEmpty()){
            var keys=new ArrayList<>(aspects.keySet());Aspect aspect=keys.get(random.nextInt(keys.size()));int left=aspects.get(aspect)-1;if(left<=0)aspects.remove(aspect);else aspects.put(aspect,left);
            var entity=event.getEntity();var drop=new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),CrystalItem.stack(aspect));event.getDrops().add(collect?new FollowingItemEntity(drop,player):drop);b++;
            if(random.nextInt(rank)==0)b+=1+random.nextInt(2);
        }
    }
    private InfusionEnchantments(){}
}
