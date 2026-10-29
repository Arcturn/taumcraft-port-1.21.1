/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.registries.GameData
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package thaumcraft.api;

import java.util.Arrays;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistryEntry;
import thaumcraft.api.aspects.AspectEventProxy;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.Part;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.internal.DummyInternalMethodHandler;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.api.internal.WeightedRandomLoot;

public class ThaumcraftApi {
    public static IInternalMethodHandler internalMethods = new DummyInternalMethodHandler();

    public static void registerResearchLocation(ResourceLocation loc) {
        if (!CommonInternals.jsonLocs.containsKey(loc.toString())) {
            CommonInternals.jsonLocs.put(loc.toString(), loc);
        }
    }

    public static void addSmeltingBonus(Object in, ItemStack out, float chance) {
        if (in instanceof ItemStack || in instanceof String) {
            CommonInternals.smeltingBonus.add(new SmeltBonus(in, out, chance));
        }
    }

    public static void addSmeltingBonus(Object in, ItemStack out) {
        if (in instanceof ItemStack || in instanceof String) {
            CommonInternals.smeltingBonus.add(new SmeltBonus(in, out, 0.33f));
        }
    }

    public static HashMap<ResourceLocation, IThaumcraftRecipe> getCraftingRecipes() {
        return CommonInternals.craftingRecipeCatalog;
    }

    public static HashMap<ResourceLocation, Object> getCraftingRecipesFake() {
        return CommonInternals.craftingRecipeCatalogFake;
    }

    public static void addFakeCraftingRecipe(ResourceLocation registry, Object recipe) {
        ThaumcraftApi.getCraftingRecipesFake().put(registry, recipe);
    }

    public static void addMultiblockRecipeToCatalog(ResourceLocation registry, BluePrint recipe) {
        ThaumcraftApi.getCraftingRecipes().put(registry, recipe);
    }

    public static void addArcaneCraftingRecipe(ResourceLocation registry, IArcaneRecipe recipe) {
        recipe.setRegistryName(registry);
        GameData.register_impl((IForgeRegistryEntry)recipe);
    }

    public static void addInfusionCraftingRecipe(ResourceLocation registry, InfusionRecipe recipe) {
        ThaumcraftApi.getCraftingRecipes().put(registry, recipe);
    }

    public static InfusionRecipe getInfusionRecipe(ItemStack res) {
        for (IThaumcraftRecipe r : ThaumcraftApi.getCraftingRecipes().values()) {
            if (!(r instanceof InfusionRecipe) || !(((InfusionRecipe)r).getRecipeOutput() instanceof ItemStack) || !((ItemStack)((InfusionRecipe)r).getRecipeOutput()).func_77969_a(res)) continue;
            return (InfusionRecipe)r;
        }
        return null;
    }

    public static void addCrucibleRecipe(ResourceLocation registry, CrucibleRecipe recipe) {
        ThaumcraftApi.getCraftingRecipes().put(registry, recipe);
    }

    public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
        for (IThaumcraftRecipe r : ThaumcraftApi.getCraftingRecipes().values()) {
            if (!(r instanceof CrucibleRecipe) || !((CrucibleRecipe)r).getRecipeOutput().func_77969_a(stack)) continue;
            return (CrucibleRecipe)r;
        }
        return null;
    }

    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        for (IThaumcraftRecipe recipe : ThaumcraftApi.getCraftingRecipes().values()) {
            if (!(recipe instanceof CrucibleRecipe) || ((CrucibleRecipe)recipe).hash != hash) continue;
            return (CrucibleRecipe)recipe;
        }
        return null;
    }

    public static boolean exists(ItemStack item) {
        ItemStack stack = item.func_77946_l();
        stack.func_190920_e(1);
        AspectList tmp = CommonInternals.objectTags.get(stack.serializeNBT().toString());
        if (tmp == null) {
            try {
                stack.func_77964_b(Short.MAX_VALUE);
                tmp = CommonInternals.objectTags.get(stack.serializeNBT().toString());
                if (item.func_77952_i() == Short.MAX_VALUE && tmp == null) {
                    int index = 0;
                    do {
                        stack.func_77964_b(index);
                        tmp = CommonInternals.objectTags.get(stack.serializeNBT().toString());
                    } while (++index < 16 && tmp == null);
                }
                if (tmp == null) {
                    return false;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return true;
    }

    @Deprecated
    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        new AspectEventProxy().registerObjectTag(item, aspects);
    }

    @Deprecated
    public static void registerObjectTag(ItemStack item, int[] meta, AspectList aspects) {
    }

    @Deprecated
    public static void registerObjectTag(String oreDict, AspectList aspects) {
        new AspectEventProxy().registerObjectTag(oreDict, aspects);
    }

    @Deprecated
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        new AspectEventProxy().registerComplexObjectTag(item, aspects);
    }

    @Deprecated
    public static void registerComplexObjectTag(String oreDict, AspectList aspects) {
        new AspectEventProxy().registerComplexObjectTag(oreDict, aspects);
    }

    @Deprecated
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT ... nbt) {
        CommonInternals.scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    public static void addWarpToItem(ItemStack craftresult, int amount) {
        CommonInternals.warpMap.put(Arrays.asList(craftresult.func_77973_b(), craftresult.func_77952_i()), amount);
    }

    public static int getWarp(ItemStack in) {
        if (in == null) {
            return 0;
        }
        if (in instanceof ItemStack && CommonInternals.warpMap.containsKey(Arrays.asList(in.func_77973_b(), in.func_77952_i()))) {
            return CommonInternals.warpMap.get(Arrays.asList(in.func_77973_b(), in.func_77952_i()));
        }
        return 0;
    }

    public static void addLootBagItem(ItemStack item, int weight, int ... bagTypes) {
        if (bagTypes == null || bagTypes.length == 0) {
            WeightedRandomLoot.lootBagCommon.add(new WeightedRandomLoot(item, weight));
        } else {
            block5: for (int rarity : bagTypes) {
                switch (rarity) {
                    case 0: {
                        WeightedRandomLoot.lootBagCommon.add(new WeightedRandomLoot(item, weight));
                        continue block5;
                    }
                    case 1: {
                        WeightedRandomLoot.lootBagUncommon.add(new WeightedRandomLoot(item, weight));
                        continue block5;
                    }
                    case 2: {
                        WeightedRandomLoot.lootBagRare.add(new WeightedRandomLoot(item, weight));
                    }
                }
            }
        }
    }

    public static void registerSeed(Block block, ItemStack seed) {
        CommonInternals.seedList.put(block.func_149739_a(), seed);
    }

    public static ItemStack getSeed(Block block) {
        return CommonInternals.seedList.get(block.func_149739_a());
    }

    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;

        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT ... nbts) {
            this.entityName = entityName;
            this.nbts = nbts;
            this.aspects = aspects;
        }
    }

    public static class EntityTagsNBT {
        public String name;
        public Object value;

        public EntityTagsNBT(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class BluePrint
    implements IThaumcraftRecipe {
        Part[][][] parts;
        String research;
        ItemStack displayStack;
        ItemStack[] ingredientList;
        private String group;

        public BluePrint(String research, Part[][][] parts, ItemStack ... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.ingredientList = ingredientList;
        }

        public BluePrint(String research, ItemStack display, Part[][][] parts, ItemStack ... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.displayStack = display;
            this.ingredientList = ingredientList;
        }

        public Part[][][] getParts() {
            return this.parts;
        }

        @Override
        public String getResearch() {
            return this.research;
        }

        public ItemStack[] getIngredientList() {
            return this.ingredientList;
        }

        public ItemStack getDisplayStack() {
            return this.displayStack;
        }

        @Override
        public String getGroup() {
            return this.group;
        }

        public BluePrint setGroup(ResourceLocation loc) {
            this.group = loc.toString();
            return this;
        }
    }

    public static class SmeltBonus {
        public Object in;
        public ItemStack out;
        public float chance;

        public SmeltBonus(Object in, ItemStack out, float chance) {
            this.in = in;
            this.out = out;
            this.chance = chance;
        }
    }
}

