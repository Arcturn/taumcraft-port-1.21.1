/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.multiplayer.WorldClient
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.client.shader.ShaderGroup
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityNote
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.RayTraceResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraftforge.client.event.DrawBlockHighlightEvent
 *  net.minecraftforge.client.event.EntityViewRenderEvent$RenderFogEvent
 *  net.minecraftforge.client.event.RenderGameOverlayEvent
 *  net.minecraftforge.client.event.RenderGameOverlayEvent$ElementType
 *  net.minecraftforge.client.event.RenderGameOverlayEvent$Pre
 *  net.minecraftforge.client.event.RenderLivingEvent$Pre
 *  net.minecraftforge.client.event.RenderTooltipEvent$PostBackground
 *  net.minecraftforge.client.event.RenderWorldLastEvent
 *  net.minecraftforge.client.event.TextureStitchEvent$Pre
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingUpdateEvent
 *  net.minecraftforge.event.entity.player.ItemTooltipEvent
 *  net.minecraftforge.fml.client.FMLClientHandler
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ClientTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$Phase
 *  net.minecraftforge.fml.common.gameevent.TickEvent$PlayerTickEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$RenderTickEvent
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessage
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package thaumcraft.client.lib.events;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.items.IArchitect;
import thaumcraft.api.items.IGogglesDisplayExtended;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.client.lib.events.ShaderHandler;
import thaumcraft.client.lib.events.WandRenderingHandler;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketNote;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.devices.TileArcaneEar;
import thaumcraft.common.tiles.devices.TileRedstoneRelay;

@Mod.EventBusSubscriber(value={Side.CLIENT})
public class RenderEventHandler {
    public static RenderEventHandler INSTANCE = new RenderEventHandler();
    @SideOnly(value=Side.CLIENT)
    public static HudHandler hudHandler = new HudHandler();
    @SideOnly(value=Side.CLIENT)
    public static WandRenderingHandler wandHandler = new WandRenderingHandler();
    @SideOnly(value=Side.CLIENT)
    static ShaderHandler shaderhandler = new ShaderHandler();
    public static List blockTags = new ArrayList();
    public static float tagscale = 0.0f;
    public static int tickCount = 0;
    static boolean checkedDate = false;
    private Random random = new Random();
    public static boolean resetShaders = false;
    private static int oldDisplayWidth = 0;
    private static int oldDisplayHeight = 0;
    public static Entity thaumTarget = null;
    static final ResourceLocation CFRAME = new ResourceLocation("thaumcraft", "textures/misc/frame_corner.png");
    static final ResourceLocation MIDDLE = new ResourceLocation("thaumcraft", "textures/misc/seal_area.png");
    static EnumFacing[][] rotfaces = new EnumFacing[][]{{EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.WEST}, {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.WEST}, {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST}, {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST}, {EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.EAST}, {EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.EAST}, {EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.WEST}, {EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.WEST}};
    static int[][] rotmat = new int[][]{{0, 270, 0}, {270, 180, 270}, {90, 0, 90}, {180, 90, 180}, {180, 180, 0}, {90, 270, 270}, {270, 90, 90}, {0, 0, 180}};
    public static HashMap<Integer, ShaderGroup> shaderGroups = new HashMap();
    public static boolean fogFiddled = false;
    public static float fogTarget = 0.0f;
    public static int fogDuration = 0;
    public static float prevVignetteBrightness = 0.0f;
    public static float targetBrightness = 1.0f;
    protected static final ResourceLocation vignetteTexPath = new ResourceLocation("thaumcraft", "textures/misc/vignette.png");

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        if (event.side == Side.SERVER || event.player.func_145782_y() != mc.field_71439_g.func_145782_y()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            try {
                shaderhandler.checkShaders(event, mc);
                if (ShaderHandler.warpVignette > 0) {
                    --ShaderHandler.warpVignette;
                    targetBrightness = 0.0f;
                } else {
                    targetBrightness = 1.0f;
                }
                if (fogFiddled) {
                    if (fogDuration < 100) {
                        fogTarget = 0.1f * ((float)fogDuration / 100.0f);
                    } else if (fogTarget < 0.1f) {
                        fogTarget += 0.001f;
                    }
                    if (--fogDuration < 0) {
                        fogFiddled = false;
                    }
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void clientWorldTick(TickEvent.ClientTickEvent event) {
        if (event.side == Side.SERVER) {
            return;
        }
        Minecraft mc = FMLClientHandler.instance().getClient();
        WorldClient world = mc.field_71441_e;
        if (event.phase == TickEvent.Phase.START) {
            ++tickCount;
            for (String fxk : ((ConcurrentHashMap.CollectionView)((Object)EssentiaHandler.sourceFX.keySet())).toArray(new String[0])) {
                EssentiaHandler.EssentiaSourceFX fx = EssentiaHandler.sourceFX.get(fxk);
                if (world == null) continue;
                int mod = 0;
                TileEntity tile = world.func_175625_s(fx.start);
                if (tile != null && tile instanceof TileInfusionMatrix) {
                    mod = -1;
                }
                FXDispatcher.INSTANCE.essentiaTrailFx(fx.end, fx.start.func_177981_b(mod), tickCount, fx.color, 0.1f, fx.ext);
                EssentiaHandler.sourceFX.remove(fxk);
            }
        } else {
            LinkedBlockingQueue<HudHandler.KnowledgeGainTracker> temp = new LinkedBlockingQueue<HudHandler.KnowledgeGainTracker>();
            if (RenderEventHandler.hudHandler.knowledgeGainTrackers.isEmpty()) {
                if (RenderEventHandler.hudHandler.kgFade > 0.0f) {
                    RenderEventHandler.hudHandler.kgFade -= 1.0f;
                }
            } else {
                RenderEventHandler.hudHandler.kgFade += 10.0f;
                if (RenderEventHandler.hudHandler.kgFade > 40.0f) {
                    RenderEventHandler.hudHandler.kgFade = 40.0f;
                }
                while (!RenderEventHandler.hudHandler.knowledgeGainTrackers.isEmpty()) {
                    HudHandler.KnowledgeGainTracker current = RenderEventHandler.hudHandler.knowledgeGainTrackers.poll();
                    if (current == null || current.progress <= 0) continue;
                    --current.progress;
                    temp.offer(current);
                }
                while (!temp.isEmpty()) {
                    RenderEventHandler.hudHandler.knowledgeGainTrackers.offer((HudHandler.KnowledgeGainTracker)temp.poll());
                }
            }
            if (mc.field_71441_e != null && !checkedDate) {
                checkedDate = true;
                Calendar calendar = mc.field_71441_e.func_83015_S();
                if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
                    ModConfig.isHalloween = true;
                }
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            UtilsFX.sysPartialTicks = event.renderTickTime;
        } else {
            Minecraft mc = FMLClientHandler.instance().getClient();
            if (Minecraft.func_71410_x().func_175606_aa() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)Minecraft.func_71410_x().func_175606_aa();
                long time = System.currentTimeMillis();
                if (player != null) {
                    hudHandler.renderHuds(mc, event.renderTickTime, player, time);
                }
                if (player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof IArchitect || player.func_184592_cb() != null && player.func_184592_cb().func_77973_b() instanceof IArchitect) {
                    RayTraceResult target2;
                    ItemStack stack;
                    ItemStack itemStack = stack = player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof IArchitect ? player.func_184614_ca() : player.func_184592_cb();
                    if (!((IArchitect)stack.func_77973_b()).useBlockHighlight(stack) && (target2 = ((IArchitect)stack.func_77973_b()).getArchitectMOP(stack, player.field_70170_p, (EntityLivingBase)player)) != null) {
                        wandHandler.handleArchitectOverlay(stack, player, event.renderTickTime, player.field_70173_aa, target2);
                    }
                }
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void tooltipEvent(ItemTooltipEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        GuiScreen gui = mc.field_71462_r;
        if (gui instanceof GuiContainer) {
            if (gui.func_146272_n() != ModConfig.CONFIG_GRAPHICS.showTags && !Mouse.isGrabbed() && event.getItemStack() != null) {
                int width;
                AspectList tags = ThaumcraftCraftingManager.getObjectTags(event.getItemStack());
                int index = 0;
                if (tags != null && tags.size() > 0) {
                    for (Aspect tag : tags.getAspects()) {
                        if (tag == null) continue;
                        ++index;
                    }
                }
                if ((width = index * 18) > 0) {
                    double sw = mc.field_71466_p.func_78256_a(" ");
                    int t = MathHelper.func_76143_f((double)((double)width / sw));
                    int l = MathHelper.func_76143_f((double)(18.0 / (double)mc.field_71466_p.field_78288_b));
                    for (int a = 0; a < l; ++a) {
                        event.getToolTip().add("                                                                                                                                            ".substring(0, Math.min(120, t)));
                    }
                }
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void tooltipEvent(RenderTooltipEvent.PostBackground event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        GuiScreen gui = mc.field_71462_r;
        if (gui instanceof GuiContainer) {
            if (gui.func_146272_n() != ModConfig.CONFIG_GRAPHICS.showTags && !Mouse.isGrabbed()) {
                int bot = event.getHeight();
                if (!event.getLines().isEmpty()) {
                    for (int a = event.getLines().size() - 1; a >= 0; --a) {
                        if (event.getLines().get(a) != null && !((String)event.getLines().get(a)).contains("    ")) {
                            bot -= 10;
                            continue;
                        }
                        if (a <= 0 || event.getLines().get(a - 1) == null || !((String)event.getLines().get(a - 1)).contains("    ")) continue;
                        hudHandler.renderAspectsInGui((GuiContainer)gui, (EntityPlayer)mc.field_71439_g, event.getStack(), bot, event.getX(), event.getY());
                        break;
                    }
                }
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        long time = System.nanoTime() / 1000000L;
        if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT) {
            wandHandler.handleFociRadial(mc, time, event);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            RenderEventHandler.renderVignette(targetBrightness, event.getResolution().func_78327_c(), event.getResolution().func_78324_d());
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void renderShaders(RenderGameOverlayEvent.Pre event) {
        if (!ModConfig.CONFIG_GRAPHICS.disableShaders && event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.func_71410_x();
            if (OpenGlHelper.field_148824_g && shaderGroups.size() > 0) {
                RenderEventHandler.updateShaderFrameBuffers(mc);
                GL11.glMatrixMode((int)5890);
                GL11.glLoadIdentity();
                for (ShaderGroup sg : shaderGroups.values()) {
                    GL11.glPushMatrix();
                    try {
                        sg.func_148018_a(event.getPartialTicks());
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    GL11.glPopMatrix();
                }
                mc.func_147110_a().func_147610_a(true);
            }
        }
    }

    private static void updateShaderFrameBuffers(Minecraft mc) {
        if (resetShaders || mc.field_71443_c != oldDisplayWidth || oldDisplayHeight != mc.field_71440_d) {
            for (ShaderGroup sg : shaderGroups.values()) {
                sg.func_148026_a(mc.field_71443_c, mc.field_71440_d);
            }
            oldDisplayWidth = mc.field_71443_c;
            oldDisplayHeight = mc.field_71440_d;
            resetShaders = false;
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void blockHighlight(DrawBlockHighlightEvent event) {
        int ticks = event.getPlayer().field_70173_aa;
        RayTraceResult target = event.getTarget();
        if (blockTags.size() > 0) {
            int x = (Integer)blockTags.get(0);
            int y = (Integer)blockTags.get(1);
            int z = (Integer)blockTags.get(2);
            AspectList ot = (AspectList)blockTags.get(3);
            String[] dir = EnumFacing.field_82609_l[(Integer)blockTags.get(4)];
            if (x == target.func_178782_a().func_177958_n() && y == target.func_178782_a().func_177956_o() && z == target.func_178782_a().func_177952_p()) {
                if (tagscale < 0.5f) {
                    tagscale += 0.031f - tagscale / 10.0f;
                }
                RenderEventHandler.drawTagsOnContainer((float)target.func_178782_a().func_177958_n() + (float)dir.func_82601_c() / 2.0f, (float)target.func_178782_a().func_177956_o() + (float)dir.func_96559_d() / 2.0f, (float)target.func_178782_a().func_177952_p() + (float)dir.func_82599_e() / 2.0f, ot, 220, (EnumFacing)dir, event.getPartialTicks());
            }
        }
        if (target != null && target.func_178782_a() != null) {
            RayTraceResult hit;
            TileEntity te = event.getPlayer().field_70170_p.func_175625_s(target.func_178782_a());
            if (te != null && te instanceof TileRedstoneRelay && (hit = RayTracer.retraceBlock(event.getPlayer().field_70170_p, event.getPlayer(), target.func_178782_a())) != null) {
                if (hit.subHit == 0) {
                    RenderEventHandler.drawTextInAir(target.func_178782_a().func_177958_n(), (double)target.func_178782_a().func_177956_o() + 0.3, target.func_178782_a().func_177952_p(), event.getPartialTicks(), "Out: " + ((TileRedstoneRelay)te).getOut());
                } else if (hit.subHit == 1) {
                    RenderEventHandler.drawTextInAir(target.func_178782_a().func_177958_n(), (double)target.func_178782_a().func_177956_o() + 0.3, target.func_178782_a().func_177952_p(), event.getPartialTicks(), "In: " + ((TileRedstoneRelay)te).getIn());
                }
            }
            if (EntityUtils.hasGoggles((Entity)event.getPlayer())) {
                float to = 0.0f;
                if (te instanceof IGogglesDisplayExtended) {
                    String[] sa;
                    GL11.glDisable((int)2929);
                    Vec3d v = ((IGogglesDisplayExtended)te).getIGogglesTextOffset();
                    for (String s : sa = ((IGogglesDisplayExtended)te).getIGogglesText()) {
                        RenderEventHandler.drawTextInAir((double)target.func_178782_a().func_177958_n() + v.field_72450_a, (double)target.func_178782_a().func_177956_o() + v.field_72448_b - (double)((to - (float)sa.length / 2.0f) / 5.5f), (double)target.func_178782_a().func_177952_p() + v.field_72449_c, event.getPartialTicks(), s);
                        to += 1.0f;
                    }
                    GL11.glEnable((int)2929);
                } else {
                    Block b = event.getPlayer().field_70170_p.func_180495_p(target.func_178782_a()).func_177230_c();
                    if (b instanceof IGogglesDisplayExtended) {
                        String[] sa;
                        GL11.glDisable((int)2929);
                        Vec3d v = ((IGogglesDisplayExtended)b).getIGogglesTextOffset();
                        for (String s : sa = ((IGogglesDisplayExtended)b).getIGogglesText()) {
                            RenderEventHandler.drawTextInAir((double)target.func_178782_a().func_177958_n() + v.field_72450_a, (double)target.func_178782_a().func_177956_o() + v.field_72448_b + (double)((to - (float)sa.length / 2.0f) / 5.5f), (double)target.func_178782_a().func_177952_p() + v.field_72449_c, event.getPartialTicks(), s);
                            to += 1.0f;
                        }
                        GL11.glEnable((int)2929);
                    }
                }
                boolean spaceAbove = event.getPlayer().field_70170_p.func_175623_d(target.func_178782_a().func_177984_a());
                if (te != null) {
                    int note = -1;
                    if (te instanceof TileEntityNote) {
                        note = ((TileEntityNote)te).field_145879_a;
                    } else if (te instanceof TileArcaneEar) {
                        note = ((TileArcaneEar)te).note;
                    } else if (te instanceof IAspectContainer && ((IAspectContainer)te).getAspects() != null && ((IAspectContainer)te).getAspects().size() > 0) {
                        float shift = 0.0f;
                        if (tagscale < 0.3f) {
                            tagscale += 0.031f - tagscale / 10.0f;
                        }
                        RenderEventHandler.drawTagsOnContainer(target.func_178782_a().func_177958_n(), (float)target.func_178782_a().func_177956_o() + (spaceAbove ? 0.4f : 0.0f) + shift, target.func_178782_a().func_177952_p(), ((IAspectContainer)te).getAspects(), 220, spaceAbove ? EnumFacing.UP : event.getTarget().field_178784_b, event.getPartialTicks());
                    }
                    if (note >= 0) {
                        if (ticks % 5 == 0) {
                            PacketHandler.INSTANCE.sendToServer((IMessage)new PacketNote(target.func_178782_a().func_177958_n(), target.func_178782_a().func_177956_o(), target.func_178782_a().func_177952_p(), event.getPlayer().field_70170_p.field_73011_w.getDimension()));
                        }
                        RenderEventHandler.drawTextInAir(target.func_178782_a().func_177958_n(), target.func_178782_a().func_177956_o() + 1, target.func_178782_a().func_177952_p(), event.getPartialTicks(), "Note: " + note);
                    }
                }
            }
        }
        if (target.field_72313_a == RayTraceResult.Type.BLOCK && (event.getPlayer().func_184614_ca() != null && event.getPlayer().func_184614_ca().func_77973_b() instanceof IArchitect || event.getPlayer().func_184592_cb() != null && event.getPlayer().func_184592_cb().func_77973_b() instanceof IArchitect)) {
            RayTraceResult target2;
            ItemStack stack;
            ItemStack itemStack = stack = event.getPlayer().func_184614_ca() != null && event.getPlayer().func_184614_ca().func_77973_b() instanceof IArchitect ? event.getPlayer().func_184614_ca() : event.getPlayer().func_184592_cb();
            if (((IArchitect)stack.func_77973_b()).useBlockHighlight(stack) && (target2 = ((IArchitect)stack.func_77973_b()).getArchitectMOP(stack, event.getPlayer().field_70170_p, (EntityLivingBase)event.getPlayer())) != null && wandHandler.handleArchitectOverlay(stack, event.getPlayer(), event.getPartialTicks(), event.getPlayer().field_70173_aa, target2)) {
                event.setCanceled(true);
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void renderLast(RenderWorldLastEvent event) {
        if (tagscale > 0.0f) {
            tagscale -= 0.005f;
        }
        float partialTicks = event.getPartialTicks();
        Minecraft mc = Minecraft.func_71410_x();
        if (Minecraft.func_71410_x().func_175606_aa() instanceof EntityPlayer) {
            AspectList ot;
            RayTraceResult target;
            EntityPlayer player = (EntityPlayer)mc.func_175606_aa();
            if (player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof ISealDisplayer) {
                RenderEventHandler.drawSeals(partialTicks, player);
            } else if (player.func_184592_cb() != null && player.func_184592_cb().func_77973_b() instanceof ISealDisplayer) {
                RenderEventHandler.drawSeals(partialTicks, player);
            }
            if (player.func_184614_ca() != null && player.func_184614_ca().func_77973_b() instanceof IArchitect) {
                target = ((IArchitect)player.func_184614_ca().func_77973_b()).getArchitectMOP(player.func_184614_ca(), player.field_70170_p, (EntityLivingBase)player);
                wandHandler.handleArchitectOverlay(player.func_184614_ca(), player, partialTicks, player.field_70173_aa, target);
            } else if (player.func_184592_cb() != null && player.func_184592_cb().func_77973_b() instanceof IArchitect) {
                target = ((IArchitect)player.func_184592_cb().func_77973_b()).getArchitectMOP(player.func_184592_cb(), player.field_70170_p, (EntityLivingBase)player);
                wandHandler.handleArchitectOverlay(player.func_184592_cb(), player, partialTicks, player.field_70173_aa, target);
            }
            if (thaumTarget != null && (ot = AspectHelper.getEntityAspects(thaumTarget)) != null && !ot.aspects.isEmpty()) {
                if (tagscale < 0.5f) {
                    tagscale += 0.031f - tagscale / 10.0f;
                }
                double iPX = RenderEventHandler.thaumTarget.field_70169_q + (RenderEventHandler.thaumTarget.field_70165_t - RenderEventHandler.thaumTarget.field_70169_q) * (double)partialTicks;
                double iPY = RenderEventHandler.thaumTarget.field_70167_r + (RenderEventHandler.thaumTarget.field_70163_u - RenderEventHandler.thaumTarget.field_70167_r) * (double)partialTicks;
                double iPZ = RenderEventHandler.thaumTarget.field_70166_s + (RenderEventHandler.thaumTarget.field_70161_v - RenderEventHandler.thaumTarget.field_70166_s) * (double)partialTicks;
                RenderEventHandler.drawTagsOnContainer(iPX, iPY + (double)RenderEventHandler.thaumTarget.field_70131_O, iPZ, ot, 220, null, event.getPartialTicks());
            }
        }
    }

    private static void drawSeals(float partialTicks, EntityPlayer player) {
        ConcurrentHashMap<SealPos, SealEntity> seals = SealHandler.sealEntities.get(player.field_70170_p.field_73011_w.getDimension());
        if (seals != null && seals.size() > 0) {
            GL11.glPushMatrix();
            if (player.func_70093_af()) {
                GL11.glDisable((int)2929);
            }
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glDisable((int)2884);
            double iPX = player.field_70169_q + (player.field_70165_t - player.field_70169_q) * (double)partialTicks;
            double iPY = player.field_70167_r + (player.field_70163_u - player.field_70167_r) * (double)partialTicks;
            double iPZ = player.field_70166_s + (player.field_70161_v - player.field_70166_s) * (double)partialTicks;
            GL11.glTranslated((double)(-iPX), (double)(-iPY), (double)(-iPZ));
            for (ISealEntity iSealEntity : seals.values()) {
                double dis = player.func_174831_c(iSealEntity.getSealPos().pos);
                if (!(dis <= 256.0)) continue;
                float alpha = 1.0f - (float)(dis / 256.0);
                boolean ia = false;
                if (iSealEntity.isStoppedByRedstone(player.field_70170_p)) {
                    ia = true;
                    if (player.field_70170_p.field_73012_v.nextFloat() < partialTicks / 12.0f) {
                        FXDispatcher.INSTANCE.spark((float)iSealEntity.getSealPos().pos.func_177958_n() + 0.5f + (float)iSealEntity.getSealPos().face.func_82601_c() * 0.66f, (float)iSealEntity.getSealPos().pos.func_177956_o() + 0.5f + (float)iSealEntity.getSealPos().face.func_96559_d() * 0.66f, (float)iSealEntity.getSealPos().pos.func_177952_p() + 0.5f + (float)iSealEntity.getSealPos().face.func_82599_e() * 0.66f, 2.0f, 0.8f - player.field_70170_p.field_73012_v.nextFloat() * 0.2f, 0.0f, 0.0f, 1.0f);
                        ia = false;
                    }
                }
                RenderEventHandler.renderSeal(iSealEntity.getSealPos().pos.func_177958_n(), iSealEntity.getSealPos().pos.func_177956_o(), iSealEntity.getSealPos().pos.func_177952_p(), alpha, iSealEntity.getSealPos().face, iSealEntity.getSeal().getSealIcon(), ia);
                RenderEventHandler.drawSealArea(player, iSealEntity, alpha, partialTicks);
            }
            GL11.glDisable((int)3042);
            GL11.glEnable((int)2884);
            if (player.func_70093_af()) {
                GL11.glEnable((int)2929);
            }
            GL11.glPopMatrix();
        }
    }

    private static void drawSealArea(EntityPlayer player, ISealEntity seal, float alpha, float partialTicks) {
        GL11.glPushMatrix();
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        if (seal.getColor() > 0) {
            Color c = new Color(EnumDyeColor.func_176764_b((int)(seal.getColor() - 1)).func_193350_e());
            r = (float)c.getRed() / 255.0f;
            g = (float)c.getGreen() / 255.0f;
            b = (float)c.getBlue() / 255.0f;
        } else {
            r = 0.7f + MathHelper.func_76126_a((float)(((float)player.field_70173_aa + partialTicks + (float)seal.getSealPos().pos.func_177958_n()) / 4.0f)) * 0.1f;
            g = 0.7f + MathHelper.func_76126_a((float)(((float)player.field_70173_aa + partialTicks + (float)seal.getSealPos().pos.func_177956_o()) / 5.0f)) * 0.1f;
            b = 0.7f + MathHelper.func_76126_a((float)(((float)player.field_70173_aa + partialTicks + (float)seal.getSealPos().pos.func_177952_p()) / 6.0f)) * 0.1f;
        }
        GL11.glPushMatrix();
        GL11.glTranslated((double)((double)seal.getSealPos().pos.func_177958_n() + 0.5), (double)((double)seal.getSealPos().pos.func_177956_o() + 0.5), (double)((double)seal.getSealPos().pos.func_177952_p() + 0.5));
        GL11.glRotatef((float)90.0f, (float)(-seal.getSealPos().face.func_96559_d()), (float)seal.getSealPos().face.func_82601_c(), (float)(-seal.getSealPos().face.func_82599_e()));
        if (seal.getSealPos().face.func_82599_e() < 0) {
            GL11.glTranslated((double)0.0, (double)0.0, (double)-0.51f);
        } else {
            GL11.glTranslated((double)0.0, (double)0.0, (double)0.51f);
        }
        GL11.glRotatef((float)((float)(player.field_70173_aa % 360) + partialTicks), (float)0.0f, (float)0.0f, (float)1.0f);
        UtilsFX.renderQuadCentered(MIDDLE, 0.9f, r, g, b, 200, 771, alpha * 0.8f);
        GL11.glPopMatrix();
        if (seal.getSeal() instanceof ISealConfigArea) {
            GL11.glDepthMask((boolean)false);
            AxisAlignedBB area = new AxisAlignedBB((double)seal.getSealPos().pos.func_177958_n(), (double)seal.getSealPos().pos.func_177956_o(), (double)seal.getSealPos().pos.func_177952_p(), (double)(seal.getSealPos().pos.func_177958_n() + 1), (double)(seal.getSealPos().pos.func_177956_o() + 1), (double)(seal.getSealPos().pos.func_177952_p() + 1)).func_72317_d((double)seal.getSealPos().face.func_82601_c(), (double)seal.getSealPos().face.func_96559_d(), (double)seal.getSealPos().face.func_82599_e()).func_72321_a(seal.getSealPos().face.func_82601_c() != 0 ? (double)((seal.getArea().func_177958_n() - 1) * seal.getSealPos().face.func_82601_c()) : 0.0, seal.getSealPos().face.func_96559_d() != 0 ? (double)((seal.getArea().func_177956_o() - 1) * seal.getSealPos().face.func_96559_d()) : 0.0, seal.getSealPos().face.func_82599_e() != 0 ? (double)((seal.getArea().func_177952_p() - 1) * seal.getSealPos().face.func_82599_e()) : 0.0).func_72314_b(seal.getSealPos().face.func_82601_c() == 0 ? (double)(seal.getArea().func_177958_n() - 1) : 0.0, seal.getSealPos().face.func_96559_d() == 0 ? (double)(seal.getArea().func_177956_o() - 1) : 0.0, seal.getSealPos().face.func_82599_e() == 0 ? (double)(seal.getArea().func_177952_p() - 1) : 0.0);
            double[][] locs = new double[][]{{area.field_72340_a, area.field_72338_b, area.field_72339_c}, {area.field_72340_a, area.field_72337_e - 1.0, area.field_72339_c}, {area.field_72336_d - 1.0, area.field_72338_b, area.field_72339_c}, {area.field_72336_d - 1.0, area.field_72337_e - 1.0, area.field_72339_c}, {area.field_72336_d - 1.0, area.field_72338_b, area.field_72334_f - 1.0}, {area.field_72336_d - 1.0, area.field_72337_e - 1.0, area.field_72334_f - 1.0}, {area.field_72340_a, area.field_72338_b, area.field_72334_f - 1.0}, {area.field_72340_a, area.field_72337_e - 1.0, area.field_72334_f - 1.0}};
            int q = 0;
            for (double[] loc : locs) {
                GL11.glPushMatrix();
                GL11.glTranslated((double)(loc[0] + 0.5), (double)(loc[1] + 0.5), (double)(loc[2] + 0.5));
                int w = 0;
                for (EnumFacing face : rotfaces[q]) {
                    GL11.glPushMatrix();
                    GL11.glRotatef((float)90.0f, (float)(-face.func_96559_d()), (float)face.func_82601_c(), (float)(-face.func_82599_e()));
                    if (face.func_82599_e() < 0) {
                        GL11.glTranslated((double)0.0, (double)0.0, (double)-0.49f);
                    } else {
                        GL11.glTranslated((double)0.0, (double)0.0, (double)0.49f);
                    }
                    GL11.glRotatef((float)90.0f, (float)0.0f, (float)0.0f, (float)-1.0f);
                    GL11.glRotatef((float)rotmat[q][w], (float)0.0f, (float)0.0f, (float)1.0f);
                    UtilsFX.renderQuadCentered(CFRAME, 1.0f, r, g, b, 200, 771, alpha * 0.7f);
                    GL11.glPopMatrix();
                    ++w;
                }
                GL11.glPopMatrix();
                ++q;
            }
            GL11.glDepthMask((boolean)true);
        }
        GL11.glPopMatrix();
    }

    static void renderSeal(int x, int y, int z, float alpha, EnumFacing face, ResourceLocation resourceLocation, boolean ia) {
        GL11.glPushMatrix();
        GL11.glColor4f((float)(ia ? 0.5f : 1.0f), (float)(ia ? 0.5f : 1.0f), (float)(ia ? 0.5f : 1.0f), (float)alpha);
        RenderEventHandler.translateSeal(x, y, z, face.ordinal(), -0.05f);
        GL11.glScalef((float)0.5f, (float)0.5f, (float)0.5f);
        UtilsFX.renderItemIn2D(resourceLocation.toString(), Minecraft.func_71410_x().func_175606_aa().func_70093_af() ? 0.0f : 0.1f);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glPopMatrix();
    }

    private static void translateSeal(float x, float y, float z, int orientation, float off) {
        if (orientation == 1) {
            GL11.glTranslatef((float)(x + 0.25f), (float)(y + 1.0f), (float)(z + 0.75f));
            GL11.glRotatef((float)-90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        } else if (orientation == 0) {
            GL11.glTranslatef((float)(x + 0.25f), (float)y, (float)(z + 0.25f));
            GL11.glRotatef((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        } else if (orientation == 3) {
            GL11.glTranslatef((float)(x + 0.25f), (float)(y + 0.25f), (float)(z + 1.0f));
        } else if (orientation == 2) {
            GL11.glTranslatef((float)(x + 0.75f), (float)(y + 0.25f), (float)z);
            GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else if (orientation == 5) {
            GL11.glTranslatef((float)(x + 1.0f), (float)(y + 0.25f), (float)(z + 0.75f));
            GL11.glRotatef((float)90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else if (orientation == 4) {
            GL11.glTranslatef((float)x, (float)(y + 0.25f), (float)(z + 0.25f));
            GL11.glRotatef((float)-90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        }
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)(-off));
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void fogDensityEvent(EntityViewRenderEvent.RenderFogEvent event) {
        if (fogFiddled && fogTarget > 0.0f) {
            GL11.glFogi((int)2917, (int)2048);
            GL11.glFogf((int)2914, (float)fogTarget);
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingUpdateEvent event) {
        Integer t;
        EntityCreature mob;
        if (event.getEntity().field_70170_p.field_72995_K && event.getEntity() instanceof EntityCreature && !event.getEntity().field_70128_L && (mob = (EntityCreature)event.getEntity()).func_110148_a(ThaumcraftApiHelper.CHAMPION_MOD) != null && (t = Integer.valueOf((int)mob.func_110148_a(ThaumcraftApiHelper.CHAMPION_MOD).func_111126_e())) != null && t >= 0 && t < ChampionModifier.mods.length) {
            ChampionModifier.mods[t.intValue()].effect.showFX((EntityLivingBase)mob);
        }
    }

    @SubscribeEvent
    public static void renderLivingPre(RenderLivingEvent.Pre event) {
        Integer t;
        EntityCreature mob;
        if (event.getEntity().field_70170_p.field_72995_K && event.getEntity() instanceof EntityCreature && !event.getEntity().field_70128_L && (mob = (EntityCreature)event.getEntity()).func_110148_a(ThaumcraftApiHelper.CHAMPION_MOD) != null && (t = Integer.valueOf((int)mob.func_110148_a(ThaumcraftApiHelper.CHAMPION_MOD).func_111126_e())) != null && t >= 0 && t < ChampionModifier.mods.length) {
            ChampionModifier.mods[t.intValue()].effect.preRender((EntityLivingBase)mob, event.getRenderer());
        }
    }

    public static void drawTagsOnContainer(double x, double y, double z, AspectList tags, int bright, EnumFacing dir, float partialTicks) {
        if (Minecraft.func_71410_x().func_175606_aa() instanceof EntityPlayer && tags != null && tags.size() > 0) {
            int fox = 0;
            int foy = 0;
            int foz = 0;
            if (dir != null) {
                fox = dir.func_82601_c();
                foy = dir.func_96559_d();
                foz = dir.func_82599_e();
            } else {
                x -= 0.5;
                z -= 0.5;
            }
            EntityPlayer player = (EntityPlayer)Minecraft.func_71410_x().func_175606_aa();
            double iPX = player.field_70169_q + (player.field_70165_t - player.field_70169_q) * (double)partialTicks;
            double iPY = player.field_70167_r + (player.field_70163_u - player.field_70167_r) * (double)partialTicks;
            double iPZ = player.field_70166_s + (player.field_70161_v - player.field_70166_s) * (double)partialTicks;
            int rowsize = 5;
            int current = 0;
            float shifty = 0.0f;
            int left = tags.size();
            for (Aspect tag : tags.getAspects()) {
                int div = Math.min(left, rowsize);
                if (current >= rowsize) {
                    current = 0;
                    shifty -= tagscale * 1.05f;
                    if ((left -= rowsize) < rowsize) {
                        div = left % rowsize;
                    }
                }
                float shift = ((float)current - (float)div / 2.0f + 0.5f) * tagscale * 4.0f;
                shift *= tagscale;
                Color color = new Color(tag.getColor());
                GL11.glPushMatrix();
                GL11.glDisable((int)2929);
                GL11.glTranslated((double)(-iPX + x + 0.5 + (double)(tagscale * 2.0f * (float)fox)), (double)(-iPY + y - (double)shifty + 0.5 + (double)(tagscale * 2.0f * (float)foy)), (double)(-iPZ + z + 0.5 + (double)(tagscale * 2.0f * (float)foz)));
                float xd = (float)(iPX - (x + 0.5));
                float zd = (float)(iPZ - (z + 0.5));
                float rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
                GL11.glRotatef((float)(rotYaw + 180.0f), (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glTranslated((double)shift, (double)0.0, (double)0.0);
                GL11.glRotatef((float)90.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                GL11.glScalef((float)tagscale, (float)tagscale, (float)tagscale);
                UtilsFX.renderQuadCentered(tag.getImage(), 1.0f, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, bright, 771, 0.75f);
                if (tags.getAmount(tag) >= 0) {
                    GL11.glRotatef((float)90.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                    String am = "" + tags.getAmount(tag);
                    GL11.glScalef((float)0.04f, (float)0.04f, (float)0.04f);
                    GL11.glTranslated((double)0.0, (double)6.0, (double)-0.1);
                    int sw = Minecraft.func_71410_x().field_71466_p.func_78256_a(am);
                    GL11.glEnable((int)3042);
                    GL11.glBlendFunc((int)770, (int)771);
                    Minecraft.func_71410_x().field_71466_p.func_78276_b(am, 14 - sw, 1, 0x111111);
                    GL11.glTranslated((double)0.0, (double)0.0, (double)-0.1);
                    Minecraft.func_71410_x().field_71466_p.func_78276_b(am, 13 - sw, 0, 0xFFFFFF);
                }
                GL11.glEnable((int)2929);
                GL11.glPopMatrix();
                ++current;
            }
        }
    }

    public static void drawTextInAir(double x, double y, double z, float partialTicks, String text) {
        if (Minecraft.func_71410_x().func_175606_aa() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)Minecraft.func_71410_x().func_175606_aa();
            double iPX = player.field_70169_q + (player.field_70165_t - player.field_70169_q) * (double)partialTicks;
            double iPY = player.field_70167_r + (player.field_70163_u - player.field_70167_r) * (double)partialTicks;
            double iPZ = player.field_70166_s + (player.field_70161_v - player.field_70166_s) * (double)partialTicks;
            GL11.glPushMatrix();
            GL11.glTranslated((double)(-iPX + x + 0.5), (double)(-iPY + y + 0.5), (double)(-iPZ + z + 0.5));
            float xd = (float)(iPX - (x + 0.5));
            float zd = (float)(iPZ - (z + 0.5));
            float rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
            GL11.glRotatef((float)(rotYaw + 180.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glScalef((float)0.0125f, (float)0.0125f, (float)0.0125f);
            int sw = Minecraft.func_71410_x().field_71466_p.func_78256_a(text);
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)771);
            Minecraft.func_71410_x().field_71466_p.func_175065_a(text, (float)(1 - sw / 2), 1.0f, 0xFFFFFF, true);
            GL11.glPopMatrix();
        }
    }

    protected static void renderVignette(float brightness, double sw, double sh) {
        int k = (int)sw;
        int l = (int)sh;
        if ((prevVignetteBrightness = (float)((double)prevVignetteBrightness + (double)((brightness = 1.0f - brightness) - prevVignetteBrightness) * 0.01)) > 0.0f) {
            float b = prevVignetteBrightness * (1.0f + MathHelper.func_76126_a((float)((float)Minecraft.func_71410_x().field_71439_g.field_70173_aa / 2.0f)) * 0.1f);
            GL11.glPushMatrix();
            GL11.glClear((int)256);
            GL11.glMatrixMode((int)5889);
            GL11.glLoadIdentity();
            GL11.glOrtho((double)0.0, (double)sw, (double)sh, (double)0.0, (double)1000.0, (double)3000.0);
            Minecraft.func_71410_x().func_110434_K().func_110577_a(vignetteTexPath);
            GL11.glMatrixMode((int)5888);
            GL11.glLoadIdentity();
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
            GL11.glDisable((int)2929);
            GL11.glDepthMask((boolean)false);
            OpenGlHelper.func_148821_a((int)0, (int)769, (int)1, (int)0);
            GL11.glColor4f((float)b, (float)b, (float)b, (float)1.0f);
            Tessellator tessellator = Tessellator.func_178181_a();
            tessellator.func_178180_c().func_181668_a(7, DefaultVertexFormats.field_181707_g);
            tessellator.func_178180_c().func_181662_b(0.0, (double)l, -90.0).func_187315_a(0.0, 1.0).func_181675_d();
            tessellator.func_178180_c().func_181662_b((double)k, (double)l, -90.0).func_187315_a(1.0, 1.0).func_181675_d();
            tessellator.func_178180_c().func_181662_b((double)k, 0.0, -90.0).func_187315_a(1.0, 0.0).func_181675_d();
            tessellator.func_178180_c().func_181662_b(0.0, 0.0, -90.0).func_187315_a(0.0, 0.0).func_181675_d();
            tessellator.func_78381_a();
            GL11.glDepthMask((boolean)true);
            GL11.glEnable((int)2929);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            OpenGlHelper.func_148821_a((int)770, (int)771, (int)1, (int)0);
            GL11.glPopMatrix();
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void textureStitchEventPre(TextureStitchEvent.Pre event) {
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "research/quill"));
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "blocks/crystal"));
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "blocks/taint_growth_1"));
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "blocks/taint_growth_2"));
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "blocks/taint_growth_3"));
        event.getMap().func_174942_a(new ResourceLocation("thaumcraft", "blocks/taint_growth_4"));
    }

    public static class ChargeEntry {
        public long time;
        public long tickTime;
        public ItemStack item;
        float charge = 0.0f;
        byte diff = 0;

        public ChargeEntry(long time, ItemStack item, float charge) {
            this.time = time;
            this.item = item;
            this.charge = charge;
        }
    }
}

