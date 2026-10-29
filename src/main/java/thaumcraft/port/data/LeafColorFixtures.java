package thaumcraft.port.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.port.client.ClientEvents;
import thaumcraft.port.world.WorldContent;

/** Exercises the real client event handlers and color registries without an OpenGL context. */
final class LeafColorFixtures {
    private LeafColorFixtures() {}

    static int validate(ResourceManager resources) throws Exception {
        var blocks=new BlockColors();
        var items=new ItemColors();
        var greatwood=WorldContent.block("leaves_greatwood");
        var silverwood=WorldContent.block("leaves_silverwood");
        var greatState=greatwood.defaultBlockState();
        var silverState=silverwood.defaultBlockState();
        var greatItem=new ItemStack(greatwood);
        var silverItem=new ItemStack(silverwood);
        int checks=0;
        check(blocks.getColor(greatState,null,null,0)==-1,"Fixture must start without the leaf block handler");checks++;
        check(items.getColor(greatItem,0)==-1,"Fixture must start without the leaf item handler");checks++;
        ClientEvents.blocks(new RegisterColorHandlersEvent.Block(blocks));
        ClientEvents.colors(new RegisterColorHandlersEvent.Item(items,blocks));

        // ColorizerFoliage.func_77468_c() in the original 1.12.2 client returns RGB 0x48B518.
        int inventoryColor=0xff48b518;
        BlockPos pos=new BlockPos(14,80,-31);
        check(blocks.getColor(greatState,null,null,0)==inventoryColor,"Greatwood null-world fallback differs from TC6");checks++;
        check(blocks.getColor(greatState,null,pos,0)==inventoryColor,"Missing world must use the default leaf color");checks++;
        check(items.getColor(greatItem,0)==inventoryColor,"Greatwood inventory tint was not registered");checks++;
        check(blocks.getColor(silverState,null,null,0)==0xffffffff,"Silverwood must preserve its original colored pixels");checks++;
        check(items.getColor(silverItem,0)==0xffffffff,"Silverwood inventory must stay white-tinted");checks++;

        for(int foliage:new int[]{0x204060,0x8cae24,0x55ff01}){
            int[] lookups={0};
            var world=(BlockAndTintGetter)Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),
                    new Class<?>[]{BlockAndTintGetter.class},(proxy,method,args)->{
                        if(method.getName().equals("getBlockTint")){
                            check(pos.equals(args[0]),"Leaf color sampled the wrong position");
                            check(args[1]==BiomeColors.FOLIAGE_COLOR_RESOLVER,"Leaf color used the grass or water resolver");
                            lookups[0]++;
                            return foliage;
                        }
                        throw new AssertionError("Unexpected leaf tint world access: "+method.getName());
                    });
            check(blocks.getColor(greatState,world,pos,0)==(0xff000000|foliage),"Greatwood did not follow the world's foliage tint");checks++;
            check(blocks.getColor(greatState.setValue(LeavesBlock.DISTANCE,1).setValue(LeavesBlock.PERSISTENT,true),world,pos,0)==(0xff000000|foliage),"Placed leaves lost biome coloring");checks++;
            check(blocks.getColor(greatState,world,null,0)==inventoryColor,"Missing position must use the default leaf color");checks++;
            check(blocks.getColor(silverState,world,pos,0)==0xffffffff,"Silverwood inherited the biome's green tint");checks++;
            check(lookups[0]==2,"Silverwood or a null-position lookup queried biome tint");checks++;
            check(items.getColor(greatItem,0)==inventoryColor,"World tint leaked into the inventory color");checks++;
        }

        String[] woods={"greatwood","silverwood"};
        String[] hashes={"355e179437bffb8a5709650c3cb46ae3202be894fedf4ca8fc7300d07af717cb",
                "9e7b6e0f3af18372797dc3df7b80427c2e0145461f75c65d167b5e8ab41dd605"};
        for(int i=0;i<woods.length;i++){
            String wood=woods[i];
            try(var stream=resources.getResourceOrThrow(id("thaumcraft:textures/block/leaves_"+wood+".png")).open()){
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
                check(hashes[i].equals(hash),"Original leaf pixels were changed: "+wood);checks++;
            }
            JsonObject block=json(resources,"thaumcraft:models/block/leaves_"+wood+".json");
            check(block.get("parent").getAsString().equals("minecraft:block/leaves"),"Leaf model lost its tinted parent: "+wood);checks++;
            check(block.getAsJsonObject("textures").get("all").getAsString().equals("thaumcraft:block/leaves_"+wood),"Wrong leaf texture: "+wood);checks++;
            JsonObject item=json(resources,"thaumcraft:models/item/leaves_"+wood+".json");
            check(item.get("parent").getAsString().equals("thaumcraft:block/leaves_"+wood),"Inventory leaves lost the same tinted model: "+wood);checks++;
        }
        var faces=json(resources,"minecraft:models/block/leaves.json").getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        for(String face:new String[]{"down","up","north","south","west","east"}){
            check(faces.getAsJsonObject(face).get("tintindex").getAsInt()==0,"Leaf face is not tinted: "+face);checks++;
        }
        return checks;
    }

    private static ResourceLocation id(String id){return ResourceLocation.parse(id);}
    private static JsonObject json(ResourceManager resources,String id) throws Exception {
        try(var reader=resources.getResourceOrThrow(id(id)).openAsReader()){
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
