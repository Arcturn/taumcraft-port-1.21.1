package thaumcraft.port.infusion.armor;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;
import top.theillusivec4.curios.api.CuriosApi;

/** TC6's runic shield, special armor absorption and infusion equipment effects. */
public final class InfusionEquipmentEvents {
    private static final ResourceLocation RUNIC=ResourceLocation.parse("thaumcraft:runic_capacity");
    private static final ResourceLocation ARMOR=ResourceLocation.parse("thaumcraft:fortress_set_armor");
    private static final ResourceLocation TOUGHNESS=ResourceLocation.parse("thaumcraft:fortress_set_toughness");
    private record ArmorProperty(EquipmentSlot slot,ItemStack stack,int priority,float ratio,float maximum) {}
    private record ArmorContext(net.neoforged.neoforge.common.damagesource.DamageContainer damage,List<ArmorProperty> properties) {}
    private record ArmorAbsorption(float remaining,Map<EquipmentSlot,Float> absorbed) {}
    private static final Map<LivingEntity,ArmorContext> SPECIAL_DAMAGE=new WeakHashMap<>();
    public static void register(){
        NeoForge.EVENT_BUS.addListener(InfusionEquipmentEvents::tick);
        NeoForge.EVENT_BUS.addListener(InfusionEquipmentEvents::hurt);
        NeoForge.EVENT_BUS.addListener(InfusionEquipmentEvents::armorHurt);
        NeoForge.EVENT_BUS.addListener(InfusionEquipmentEvents::damaged);
        NeoForge.EVENT_BUS.addListener(InfusionEquipmentEvents::tooltip);
    }
    public static void worn(Player player,Consumer<ItemStack> consumer){
        player.getArmorSlots().forEach(consumer);
        CuriosApi.getCuriosInventory(player).ifPresent(inventory->{for(var handler:inventory.getCurios().values())for(int slot=0;slot<handler.getStacks().getSlots();slot++)if(inventory.isSlotActive(handler.getIdentifier(),slot))consumer.accept(handler.getStacks().getStackInSlot(slot));});
    }
    public static int runicCapacity(Player player){int[] n={0};worn(player,stack->n[0]+=InfusionAugments.getRunic(stack));return n[0];}
    /** Original WarpEvents includes the main hand, armor and functional baubles, but not the offhand. */
    public static int gearWarp(Player player){int[] total={gearWarp(player.getMainHandItem(),player)};worn(player,stack->total[0]+=gearWarp(stack,player));return total[0];}
    private static int gearWarp(ItemStack stack,Player player){return InfusionAugments.getWarp(stack)+(stack.getItem() instanceof thaumcraft.port.infusion.content.WarpingGear gear?gear.gearWarp(stack,player):0);}
    /** TC6 checks this before warp severity selection, rather than reducing permanent warp. */
    public static int mitigateWarpSeverity(Player player,int severity){
        return mask(player)==0?Math.max(0,severity-2-player.getRandom().nextInt(4)):severity;
    }
    public static int mask(LivingEntity entity){var head=entity.getItemBySlot(EquipmentSlot.HEAD);return head.getItem() instanceof FortressArmorItem?InfusionAugments.mask(head):-1;}
    public static void updateShield(ServerPlayer player){
        var data=player.getPersistentData();int maximum=runicCapacity(player),old=data.getInt("tcRunicMaximum");
        float previousAbsorption=player.getAbsorptionAmount();
        var attribute=player.getAttribute(Attributes.MAX_ABSORPTION);
        if(attribute!=null){var modifier=attribute.getModifier(RUNIC);if(modifier==null||modifier.amount()!=maximum){attribute.removeModifier(RUNIC);if(maximum>0)attribute.addTransientModifier(new AttributeModifier(RUNIC,maximum,AttributeModifier.Operation.ADD_VALUE));}}
        player.setAbsorptionAmount(Math.max(0,previousAbsorption-Math.max(0,old-maximum)));
        data.putInt("tcRunicMaximum",maximum);
        int charge=(int)player.getAbsorptionAmount();long now=player.level().getGameTime();
        if(charge==0&&data.getInt("tcRunicLastCharge")>0)data.putLong("tcRunicNext",now+80);
        data.putInt("tcRunicLastCharge",charge);
        if(charge>=maximum||now<data.getLong("tcRunicNext"))return;
        var aura=AuraSystem.get(player.serverLevel(),player.blockPosition());if(aura==null||aura.vis()<1)return;
        if(aura.base()>0&&aura.vis()/aura.base()<.1f&&ResearchStorage.get(player.getServer()).player(player.getUUID()).knows("AURAPRESERVE"))return;
        if(AuraSystem.drain(player.serverLevel(),player.blockPosition(),1,false,false)>=1){
            player.setAbsorptionAmount(charge+1);data.putInt("tcRunicLastCharge",charge+1);data.putLong("tcRunicNext",now+40);
        }
    }
    private static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        updateShield(player);
        if(player.tickCount%20==0)fortressBonuses(player);
    }
    private static void fortressBonuses(Player player){
        int pieces=0,masks=0;for(var item:player.getArmorSlots())if(item.getItem() instanceof FortressArmorItem){pieces++;if(InfusionAugments.mask(item)>=0)masks++;}
        // ISpecialArmor adds the set bonus for every participating fortress piece.
        modifier(player,Attributes.ARMOR,ARMOR,pieces*(1+masks));modifier(player,Attributes.ARMOR_TOUGHNESS,TOUGHNESS,pieces);
    }
    private static void modifier(Player player,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type,ResourceLocation id,double value){var attribute=player.getAttribute(type);if(attribute==null)return;var old=attribute.getModifier(id);if(old!=null&&old.amount()==value)return;attribute.removeModifier(id);if(value>0)attribute.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE));}
    private static void hurt(LivingIncomingDamageEvent event){
        var victim=event.getEntity();SPECIAL_DAMAGE.remove(victim);
        // The incoming event precedes shield/iframe rejection. ARMOR callbacks run
        // only for an accepted hit and expose its actual pre-armor damage.
        event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR,(container,reduction)->{
            if(event.getSource().getEntity() instanceof LivingEntity attacker){
                float amount=container.getNewDamage();
                if(attacker instanceof Player&&mask(attacker)==2&&attacker.getRandom().nextFloat()<amount/12f)attacker.heal(1);
                if(victim instanceof Player&&mask(victim)==1&&victim.getRandom().nextFloat()<amount/10f)attacker.addEffect(new MobEffectInstance(MobEffects.WITHER,80,0));
            }
            return reduction;
        });
        if(victim instanceof Player player)fortressBonuses(player);
        var properties=armorProperties(victim,event.getSource());
        if(!properties.isEmpty()){
            SPECIAL_DAMAGE.put(victim,new ArmorContext(event.getContainer(),properties));
            final int originalArmor=victim.getArmorValue();final float originalToughness=(float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            // Source ISpecialArmor consumes higher-priority protection first: fortress
            // fire protection precedes ordinary void robes in a mixed set.
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR,(container,reduction)->{
                float raw=container.getNewDamage();var absorption=absorb(properties,raw);float remaining=absorption.remaining();
                remaining=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(victim,remaining,event.getSource(),originalArmor,originalToughness);
                return raw-remaining;
            });
        }
    }
    private static List<ArmorProperty> armorProperties(LivingEntity entity,net.minecraft.world.damagesource.DamageSource source){
        var properties=new ArrayList<ArmorProperty>();
        // Forge 1.12 calls handleUnblockableDamage before getProperties. Both
        // source items inherit its false default, including for vanilla magic.
        if(source.is(DamageTypeTags.BYPASSES_ARMOR))return properties;
        boolean magic=source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC)||source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC);
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var stack=entity.getItemBySlot(slot);boolean fortress=stack.getItem() instanceof FortressArmorItem;
            boolean robe=stack.getItem() instanceof thaumcraft.port.infusion.content.VoidArmorItem armor&&armor.isRobe();
            if(!fortress&&!robe)continue;
            boolean elemental=fortress&&(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION));
            float denominator=magic?35:elemental?20:25;
            properties.add(new ArmorProperty(slot,stack,magic||elemental?1:0,((net.minecraft.world.item.ArmorItem)stack.getItem()).getDefense()/denominator,
                    stack.getMaxDamage()+1-stack.getDamageValue()));
        }
        return properties;
    }
    private static ArmorAbsorption absorb(List<ArmorProperty> properties,float raw){
        float remaining=raw;var absorbed=new EnumMap<EquipmentSlot,Float>(EquipmentSlot.class);
        for(int priority=1;priority>=0;priority--){
            float ratio=0;for(var property:properties)if(property.priority()==priority)ratio+=Math.min(property.ratio(),property.maximum()/Math.max(.001f,remaining));
            if(ratio<=0)continue;
            float normalization=ratio>1?1/ratio:1;
            for(var property:properties)if(property.priority()==priority){
                float part=Math.min(property.ratio(),property.maximum()/Math.max(.001f,remaining))*normalization;
                if(part>0)absorbed.put(property.slot(),remaining*part);
            }
            remaining*=1-Math.min(1,ratio);
        }
        return new ArmorAbsorption(remaining,absorbed);
    }
    private static void armorHurt(net.neoforged.neoforge.event.entity.living.ArmorHurtEvent event){
        // Falling blocks/anvils hurt only the helmet before their ordinary armor pass.
        if(event.getDamageSource().is(DamageTypeTags.DAMAGES_HELMET)&&event.getArmorMap().size()==1&&event.getArmorMap().containsKey(EquipmentSlot.HEAD))return;
        var context=SPECIAL_DAMAGE.remove(event.getEntity());if(context==null||context.damage().getSource()!=event.getDamageSource()||event.getDamageSource().is(DamageTypeTags.BYPASSES_ARMOR))return;
        var absorption=absorb(context.properties(),context.damage().getNewDamage());float normal=absorption.remaining();
        for(var entry:event.getArmorMap().entrySet()){
            float damage=normal>0?Math.max(1,(int)(normal/4)):0;
            float absorbed=absorption.absorbed().getOrDefault(entry.getKey(),0f);
            if(absorbed>0&&!event.getDamageSource().is(DamageTypes.FALL))damage+=Math.max(1,(int)absorbed);
            event.setNewDamage(entry.getKey(),damage);
        }
    }
    private static void damaged(LivingDamageEvent.Post event){
        SPECIAL_DAMAGE.remove(event.getEntity());
        if(!(event.getEntity() instanceof ServerPlayer player)||player.getPersistentData().getInt("tcRunicMaximum")==0)return;
        if(event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION)<=0)return;
        InfusionFxPayload.shield(player,event.getSource());
        if(player.getAbsorptionAmount()<1){player.getPersistentData().putInt("tcRunicLastCharge",0);player.getPersistentData().putLong("tcRunicNext",player.level().getGameTime()+80);}
    }
    private static void tooltip(ItemTooltipEvent event){
        var stack=event.getItemStack();int shield=InfusionAugments.getRunic(stack),warp=gearWarp(stack,event.getEntity());
        if(shield>0)event.getToolTip().add(Component.translatable("item.runic.charge").append(" +"+shield).withStyle(ChatFormatting.GOLD));
        InfusionAugments.enchantments(stack).forEach((id,level)->{var text=Component.translatable("enchantment.infusion."+id.toUpperCase(Locale.ROOT));if(InfusionAugments.maxLevel(id)>1)text.append(" ").append(Component.translatable("enchantment.level."+level));event.getToolTip().add(text.withStyle(ChatFormatting.GOLD));});
        if(warp>0)event.getToolTip().add(Component.translatable("item.warping").append(" "+warp).withStyle(ChatFormatting.DARK_PURPLE));
    }
    private InfusionEquipmentEvents(){}
}
