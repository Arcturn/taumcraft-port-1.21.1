/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.settings.KeyBinding
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraftforge.fml.client.FMLClientHandler
 *  net.minecraftforge.fml.client.registry.ClientRegistry
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.common.gameevent.TickEvent$PlayerTickEvent
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessage
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.input.Keyboard
 */
package thaumcraft.common.lib.events;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.golems.ItemGolemBell;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;

@Mod.EventBusSubscriber(value={Side.CLIENT})
public class KeyHandler {
    public static KeyBinding keyF = new KeyBinding("Change Caster Focus", 33, "key.categories.thaumcraft");
    public static KeyBinding keyG = new KeyBinding("Misc Caster Toggle", 34, "key.categories.thaumcraft");
    private static boolean keyPressedF = false;
    private boolean keyPressedH = false;
    private static boolean keyPressedG = false;
    public static boolean radialActive = false;
    public static boolean radialLock = false;
    public static long lastPressF = 0L;
    public static long lastPressH = 0L;
    public static long lastPressG = 0L;

    public KeyHandler() {
        ClientRegistry.registerKeyBinding((KeyBinding)keyF);
        ClientRegistry.registerKeyBinding((KeyBinding)keyG);
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == Side.SERVER) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            EntityPlayer player;
            if (keyF.func_151470_d()) {
                if (FMLClientHandler.instance().getClient().field_71415_G) {
                    player = event.player;
                    if (player != null) {
                        if (!keyPressedF) {
                            lastPressF = System.currentTimeMillis();
                            radialLock = false;
                        }
                        if (!radialLock && (player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof ICaster || player.func_184592_cb() != null && player.func_184592_cb().func_77973_b() instanceof ICaster)) {
                            if (player.func_70093_af()) {
                                PacketHandler.INSTANCE.sendToServer((IMessage)new PacketFocusChangeToServer("REMOVE"));
                            } else {
                                radialActive = true;
                            }
                        } else if (player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof ItemGolemBell && !keyPressedF) {
                            PacketHandler.INSTANCE.sendToServer((IMessage)new PacketItemKeyToServer(0));
                        }
                    }
                    keyPressedF = true;
                }
            } else {
                radialActive = false;
                if (keyPressedF) {
                    lastPressF = System.currentTimeMillis();
                }
                keyPressedF = false;
            }
            if (keyG.func_151470_d()) {
                if (FMLClientHandler.instance().getClient().field_71415_G) {
                    player = event.player;
                    if (player != null && !keyPressedG) {
                        lastPressG = System.currentTimeMillis();
                        PacketHandler.INSTANCE.sendToServer((IMessage)new PacketItemKeyToServer(1, Keyboard.isKeyDown((int)29) ? 1 : (Keyboard.isKeyDown((int)42) ? 2 : 0)));
                    }
                    keyPressedG = true;
                }
            } else {
                if (keyPressedG) {
                    lastPressG = System.currentTimeMillis();
                }
                keyPressedG = false;
            }
        }
    }
}

