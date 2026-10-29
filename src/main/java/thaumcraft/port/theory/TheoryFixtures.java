package thaumcraft.port.theory;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;
import thaumcraft.port.research.*;

/** Real registries, inventories, data components, block-entity NBT and wire codecs during datagen. */
public final class TheoryFixtures {
    public static int validate(RegistryAccess registries){
        int checks=0;var table=new ResearchTableEntity(BlockPos.ZERO,TheoryContent.TABLE.get().defaultBlockState());
        var ink=new ItemStack(PortContent.SCRIBING_TOOLS.get());ink.setDamageValue(98);table.setItem(0,ink);table.setItem(1,new ItemStack(Items.PAPER,7));
        require(table.ink()==2&&table.paper(7)&&!table.paper(8),"Exact paper and ink quantities");checks++;
        var menu=new TheoryMenu(12,new Inventory(null),table,BlockPos.ZERO);
        require(menu.slots.size()==38&&menu.getSlot(0).x==16&&menu.getSlot(0).y==15&&menu.getSlot(1).x==224&&menu.getSlot(1).y==16,"Original table slot coordinates");checks++;
        require(menu.getSlot(0).mayPlace(ink)&&!menu.getSlot(0).mayPlace(new ItemStack(Items.PAPER))&&menu.getSlot(0).getMaxStackSize()==1&&menu.getSlot(1).mayPlace(new ItemStack(Items.PAPER))&&!menu.getSlot(1).mayPlace(ink),"Ink and paper filters");checks++;
        for(int i=0;i<27;i++){var slot=menu.getSlot(i+2);require(slot.x==77+i%9*18&&slot.y==190+i/9*18&&slot.getSlotIndex()==9+i,"TC6 main inventory layout");checks++;}
        for(int i=0;i<9;i++){var slot=menu.getSlot(i+29);require(slot.x==20+i%3*18&&slot.y==190+i/3*18&&slot.getSlotIndex()==i,"TC6 3x3 hotbar layout");checks++;}
        var state=new ResearchState();state.completeCatalog();
        var ctx=new TheoryCards.Context(){public ResearchState research(){return state;}public int levels(){return 20;}public List<String> curios(){return List.of("thaumcraft:curio_arcane");}public int aspectTotal(String id){return 25;}};
        table.session=new TheorySession(UUID.fromString("55b20568-46de-4452-a9b3-f0bd47f8d03a"),10,List.of("Bookshelf","BasicAlchemy"));table.session.add("ALCHEMY",50);table.session.add("INFUSION",30);table.session.add("ARTIFICE",10);table.revision=17;
        for(String kind:TheoryCards.KINDS){
            if(kind.equals("Analyze"))continue;
            table.session.choices.clear();table.session.selected=-1;var card=TheoryCards.initialize(kind,82,true,table.session,ctx);require(card!=null,"Valid mature-theory card "+kind);checks++;
            table.session.choices.add(card);table.session.selected=0;
            var copy=new ResearchTableEntity(BlockPos.ZERO,table.getBlockState());copy.loadCustomOnly(table.saveCustomOnly(registries),registries);
            require(copy.session.encode().equals(table.session.encode())&&copy.revision==17&&copy.ink()==2&&copy.getItem(1).getCount()==7,"Exact pending-card, inventory and owner save "+kind);checks++;
            var update=table.getUpdateTag(registries);require(!update.contains("TheoryV1")&&update.getBoolean("Active"),"Chunk broadcast has visual state only");checks++;
            for(var q:card.required()){var stack=TheoryItems.stack(q);require(!stack.isEmpty()&&TheoryItems.matches(stack,q),"Registered card requirement "+q);checks++;}
        }
        for(String id:java.util.stream.Stream.concat(TheoryCards.TINKER.stream(),TheoryCards.INFUSE.stream()).distinct().toList()){require(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)),"Original card option exists: "+id);checks++;}
        var inv=new SimpleContainer(4);var a=TheoryCards.Requirement.crystal(Aspect.METAL,true);var b=TheoryCards.Requirement.crystal(Aspect.METAL,false);inv.setItem(0,CrystalItem.stack(Aspect.METAL));
        require(TheoryItems.reserve(inv,List.of(a,b))==null&&inv.getItem(0).getCount()==1,"One item cannot meet two requirements, failed reservation is read-only");checks++;
        inv.getItem(0).setCount(2);var reserved=TheoryItems.reserve(inv,List.of(a,b));require(reserved!=null&&reserved[0]==1&&inv.getItem(0).getCount()==2,"Presence-only requirement is reserved but not consumed");checks++;
        inv.setItem(0,CrystalItem.stack(Aspect.AIR));require(TheoryItems.reserve(inv,List.of(a))==null,"Crystal components are checked");checks++;
        var bow=new ItemStack(Items.BOW);bow.setDamageValue(1);require(!TheoryItems.matches(bow,TheoryCards.Requirement.item("minecraft:bow",true)),"Original exact-damage tool requirement");checks++;
        var playerInventory=new Inventory(null);playerInventory.setItem(40,CrystalItem.stack(Aspect.METAL));require(TheoryItems.reserve(playerInventory,List.of(a))==null,"Original card costs search main inventory, not offhand or armor");checks++;
        var phial=TheoryCards.Requirement.phial(Aspect.MAGIC,true);require(!TheoryItems.matches(new ItemStack(thaumcraft.port.essentia.EssentiaContent.PHIAL.get()),phial)&&TheoryItems.matches(TheoryItems.stack(phial),phial),"Empty phial is not a filled research requirement");checks++;
        for(int i=0;i<13;i++){var q=TheoryCards.Requirement.note(i);require(TheoryItems.matches(CelestialNotesItem.stack(i),q)&&!TheoryItems.matches(CelestialNotesItem.stack((i+1)%13),q),"Celestial note metadata "+i);checks++;require(thaumcraft.port.client.BookAssets.stack("thaumcraft:celestial_notes;1;"+i).getOrDefault(PortContent.CELESTIAL_KIND.get(),0)==i,"Book preserves celestial note kind "+i);checks++;}
        for(int i=0;i<7;i++){var stack=TheoryItems.stack(TheoryCards.Requirement.item("thaumcraft:curio_"+TheoryCards.CURIOS.get(i),true));require(LegacyRequirement.parse("thaumcraft:curio;1;"+i).matches().test(stack),"Book curio metadata flattening "+i);checks++;}
        var request=new TheoryActionPayload(12,UUID.randomUUID().toString(),17,2,"Bookshelf,BasicAlchemy");var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),registries);
        try{TheoryActionPayload.STREAM_CODEC.encode(buffer,request);require(TheoryActionPayload.STREAM_CODEC.decode(buffer).equals(request),"C2S choice/revision/nonce wire round-trip");checks++;}finally{buffer.release();}
        var payload=new TheorySyncPayload(12,request.token(),table.saveCustomOnly(registries));buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),registries);
        try{TheorySyncPayload.STREAM_CODEC.encode(buffer,payload);require(TheorySyncPayload.STREAM_CODEC.decode(buffer).equals(payload),"S2C complete card state wire round-trip");checks++;}finally{buffer.release();}
        return checks;
    }
    private static void require(boolean valid,String label){if(!valid)throw new IllegalStateException(label);}
    private TheoryFixtures(){}
}
