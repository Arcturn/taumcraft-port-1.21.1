/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.world.World
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.client.model.obj.OBJLoader
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.fml.client.FMLClientHandler
 *  net.minecraftforge.fml.common.event.FMLInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLPostInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLPreInitializationEvent
 */
package thaumcraft.proxies;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import thaumcraft.client.ColorHandler;
import thaumcraft.client.lib.ender.ShaderHelper;
import thaumcraft.common.lib.events.KeyHandler;
import thaumcraft.proxies.CommonProxy;
import thaumcraft.proxies.ProxyEntities;
import thaumcraft.proxies.ProxyTESR;

public class ClientProxy
extends CommonProxy {
    ProxyEntities proxyEntities = new ProxyEntities();
    ProxyTESR proxyTESR = new ProxyTESR();
    KeyHandler kh = new KeyHandler();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        OBJLoader.INSTANCE.addDomain("thaumcraft".toLowerCase());
        ShaderHelper.initShaders();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ColorHandler.registerColourHandlers();
        this.registerKeyBindings();
        this.proxyEntities.setupEntityRenderers();
        this.proxyTESR.setupTESR();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    public void registerKeyBindings() {
        MinecraftForge.EVENT_BUS.register((Object)this.kh);
    }

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance().getClient().field_71441_e;
    }

    @Override
    public World getWorld(int dim) {
        return this.getClientWorld();
    }

    @Override
    public boolean getSingleplayer() {
        return Minecraft.func_71410_x().func_71356_B();
    }

    @Override
    public boolean isShiftKeyDown() {
        return GuiScreen.func_146272_n();
    }

    public void setOtherBlockRenderers() {
    }

    @Override
    public void registerModel(ItemBlock itemBlock) {
        ModelLoader.setCustomModelResourceLocation((Item)itemBlock, (int)0, (ModelResourceLocation)new ModelResourceLocation(itemBlock.getRegistryName(), "inventory"));
    }
}

