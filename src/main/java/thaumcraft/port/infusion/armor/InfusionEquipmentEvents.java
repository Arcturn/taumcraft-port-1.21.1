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

/** TC6's absorption-based runic shield and fortress equipment effects. */
public final class InfusionEquipmentEvents {
    private static final ResourceLocation RUNIC=ResourceLocation.parse("thaumcraft:runic_capacity");
    private static final ResourceLocation ARMOR=ResourceLocation.parse("thaumcraft:fortress_set_armor");
    private static final ResourceLocation TOUGHNESS=ResourceLocation.parse("thaumcraft:fortress_set_toughness");
    private static final Map<LivingEntity,net.neoforged.neoforge.common.damagesource.DamageContainer> FORTRESS_DAMAGE=new WeakHashMap<>();
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
    public static int gearWarp(Player player){int[] total={InfusionAugments.getWarp(player.getMainHandItem())};worn(player,stack->total[0]+=InfusionAugments.getWarp(stack));return total[0];}
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
        var victim=event.getEntity();FORTRESS_DAMAGE.remove(victim);
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
        if(!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)){
            boolean hasFortress=false;for(var item:victim.getArmorSlots())if(item.getItem() instanceof FortressArmorItem){hasFortress=true;break;}
            if(hasFortress){
                FORTRESS_DAMAGE.put(victim,event.getContainer());
                final int originalArmor=victim.getArmorValue();final float originalToughness=(float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                var properties=new ArrayList<float[]>();for(var stack:victim.getArmorSlots())if(stack.getItem() instanceof FortressArmorItem armor)properties.add(new float[]{armor.getDefense()/denominator(event.getSource()),stack.getMaxDamage()+1-stack.getDamageValue()});
                // Forge1.12 ISpecialArmor applies proportional absorption first, then the
                // ordinary armor/toughness attributes (including the per-piece set bonus).
                event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR,(container,reduction)->{
                    float raw=container.getNewDamage(),ratio=0;for(var property:properties)ratio+=Math.min(property[0],property[1]/Math.max(.001f,raw));ratio=Math.min(1,ratio);
                    float remaining=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(victim,raw*(1-ratio),event.getSource(),originalArmor,originalToughness);
                    return raw-remaining;
                });
            }
        }
    }
    private static float denominator(net.minecraft.world.damagesource.DamageSource source){
        if(source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC))return 35;
        return source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION)?20:25;
    }
    private static float fortressRatio(LivingEntity entity,net.minecraft.world.damagesource.DamageSource source,float damage){float ratio=0;for(var stack:entity.getArmorSlots())if(stack.getItem() instanceof FortressArmorItem armor)ratio+=Math.min(armor.getDefense()/denominator(source),(stack.getMaxDamage()+1-stack.getDamageValue())/Math.max(.001f,damage));return Math.min(1,ratio);}
    private static void armorHurt(net.neoforged.neoforge.event.entity.living.ArmorHurtEvent event){
        // Falling blocks/anvils hurt only the helmet before their ordinary armor pass.
        if(event.getDamageSource().is(DamageTypeTags.DAMAGES_HELMET)&&event.getArmorMap().size()==1&&event.getArmorMap().containsKey(EquipmentSlot.HEAD))return;
        var context=FORTRESS_DAMAGE.remove(event.getEntity());if(context==null||context.getSource()!=event.getDamageSource()||event.getDamageSource().is(DamageTypeTags.BYPASSES_ARMOR))return;
        float raw=context.getNewDamage();
        float ratio=fortressRatio(event.getEntity(),event.getDamageSource(),raw),normal=raw*(1-ratio);
        for(var entry:event.getArmorMap().entrySet()){
            float damage=normal>0?Math.max(1,(int)(normal/4)):0;
            if(entry.getValue().armorItemStack.getItem() instanceof FortressArmorItem armor)damage+=Math.max(1,(int)(raw*armor.getDefense()/denominator(event.getDamageSource())));
            event.setNewDamage(entry.getKey(),damage);
        }
    }
    private static void damaged(LivingDamageEvent.Post event){
        FORTRESS_DAMAGE.remove(event.getEntity());
        if(!(event.getEntity() instanceof ServerPlayer player)||player.getPersistentData().getInt("tcRunicMaximum")==0)return;
        if(event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION)<=0)return;
        InfusionFxPayload.shield(player,event.getSource());
        if(player.getAbsorptionAmount()<1){player.getPersistentData().putInt("tcRunicLastCharge",0);player.getPersistentData().putLong("tcRunicNext",player.level().getGameTime()+80);}
    }
    private static void tooltip(ItemTooltipEvent event){
        var stack=event.getItemStack();int shield=InfusionAugments.getRunic(stack),warp=InfusionAugments.getWarp(stack);
        if(shield>0)event.getToolTip().add(Component.translatable("item.runic.charge").append(" +"+shield).withStyle(ChatFormatting.GOLD));
        InfusionAugments.enchantments(stack).forEach((id,level)->{var text=Component.translatable("enchantment.infusion."+id.toUpperCase(Locale.ROOT));if(InfusionAugments.maxLevel(id)>1)text.append(" ").append(Component.translatable("enchantment.level."+level));event.getToolTip().add(text.withStyle(ChatFormatting.GOLD));});
        if(warp>0)event.getToolTip().add(Component.translatable("item.warping").append(" "+warp).withStyle(ChatFormatting.DARK_PURPLE));
    }
    private InfusionEquipmentEvents(){}
}
