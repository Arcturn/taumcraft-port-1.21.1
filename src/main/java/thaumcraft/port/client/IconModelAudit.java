package thaumcraft.port.client;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.client.event.ModelEvent;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.ResearchCatalog;

/** Inspect the actual client bake (including OBJ and resource packs) on startup/F3+T.
 * Reports affected item/research IDs instead of silently hiding missing models.
 * No model replacement, file writes, or changes to player research.
 */
public final class IconModelAudit {
    public static void check(ModelEvent.BakingCompleted event) {
        var missing=event.getModelManager().getMissingModel();
        var models=event.getModels();
        var issues=new ArrayList<String>();
        int checked=0;
        for(var item:PortContent.ITEMS.getEntries()) {
            var id=ModelResourceLocation.inventory(item.getId());
            check(item.getId().toString(),models.get(id),new ItemStack(item.get()),missing,issues);
            checked++;
        }
        var carrier=models.get(ModelResourceLocation.inventory(PortContent.RESEARCH_ICON.getId()));
        var icons=ResearchCatalog.read("/thaumcraft-port/advancement-icon-index.json");
        for(var entry:icons.entrySet()) {
            var stack=new ItemStack(PortContent.RESEARCH_ICON.get());
            stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(Integer.parseInt(entry.getKey())));
            check("advancement custom_model_data="+entry.getKey()+" source="+entry.getValue().getAsJsonObject().get("source").getAsString(),carrier,stack,missing,issues);
            checked++;
        }
        var book=ResearchCatalog.read("/thaumcraft-port/book-model-index.json");
        for(var definition:ResearchCatalog.read("/thaumcraft-port/research-display-icons.json").entrySet()) {
            int frame=0;
            for(var element:definition.getValue().getAsJsonObject().getAsJsonArray("variants")) {
                var variant=element.getAsJsonObject();
                var id=ModelResourceLocation.standalone(ResourceLocation.parse(variant.get("model").getAsString()));
                check("original research "+definition.getKey()+" frame="+(frame++)+" source="+variant.get("source").getAsString(),models.get(id),ItemStack.EMPTY,missing,issues);
                checked++;
            }
        }
        for(var entry:book.entrySet()) {
            var id=ModelResourceLocation.standalone(ResourceLocation.parse(entry.getValue().getAsString()));
            check("book "+entry.getKey()+" model="+id,models.get(id),ItemStack.EMPTY,missing,issues);
            checked++;
        }
        for(var entry:ResearchCatalog.read("/thaumcraft-port/book-block-model-index.json").entrySet()) {
            var id=ModelResourceLocation.standalone(ResourceLocation.parse(entry.getValue().getAsJsonObject().get("model").getAsString()));
            check("blueprint "+entry.getKey()+" model="+id,models.get(id),ItemStack.EMPTY,missing,issues);
            checked++;
        }
        for(String model:BookBlocks.vanillaModels()) {
            var id=ModelResourceLocation.standalone(ResourceLocation.parse(model));
            check("original vanilla blueprint model="+id,models.get(id),ItemStack.EMPTY,missing,issues);
            checked++;
        }
        var logger=LogUtils.getLogger();
        for(String issue:issues)logger.warn("[Thaumcraft icon audit] {}",issue);
        logger.info("[Thaumcraft icon audit] Checked {} item/book/advancement model selections; {} problems",checked,issues.size());
    }

    private static void check(String label,BakedModel model,ItemStack stack,BakedModel missing,List<String> issues) {
        try {
            if(model==null||model==missing){issues.add(label+": model not loaded");return;}
            if(!stack.isEmpty())model=model.getOverrides().resolve(model,stack,null,null,0);
            if(model==null||model==missing){issues.add(label+": selected override is missing");return;}
            // Built-in renderers do not draw the model's quads.
            if(model.isCustomRenderer())return;
            List<Direction> faces=new ArrayList<>(Arrays.asList(Direction.values()));faces.add(null);
            // NeoForge composite/OBJ models expose item passes and extended quads;
            // the old vanilla-only overload can be empty even while a bad face is visible.
            for(var pass:model.getRenderPasses(stack,false)) {
                if(pass==missing){issues.add(label+": render pass is missing");return;}
                for(var face:faces)for(var quad:pass.getQuads(null,face,RandomSource.create(0),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,null)) {
                    if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                        issues.add(label+": face uses missing texture");return;
                    }
                }
            }
        }catch(RuntimeException error){issues.add(label+": cannot inspect model: "+error);}
    }
    private IconModelAudit(){}
}
