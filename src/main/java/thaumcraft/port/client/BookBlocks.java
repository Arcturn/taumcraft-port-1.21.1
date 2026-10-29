package thaumcraft.port.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.port.research.ResearchCatalog;

/** Blueprint cells use world geometry; an item sprite is not a block preview. */
final class BookBlocks {
    private static final JsonObject MODELS = ResearchCatalog.read("/thaumcraft-port/book-block-model-index.json");
    private static final JsonObject VANILLA = ResearchCatalog.read("/thaumcraft-port/book-vanilla-block-model-index.json");
    private static final JsonObject CONTEXT = ResearchCatalog.read("/thaumcraft-port/book-block-states.json");
    private static final Map<String, BlockState> STATES = new HashMap<>();
    private record PreparedModel(BakedModel source, BakedModel world) {}
    private static final Map<String, PreparedModel> PREPARED = new HashMap<>();

    static void registerModels(ModelEvent.RegisterAdditional event) {
        var models = new HashSet<String>();
        MODELS.entrySet().forEach(entry -> {
            var definition = entry.getValue().getAsJsonObject();
            models.add(definition.get("model").getAsString());
            if (definition.has("variants")) definition.getAsJsonArray("variants").forEach(
                    variant -> models.add(variant.getAsJsonObject().get("model").getAsString()));
        });
        models.addAll(vanillaModels());
        models.forEach(model -> event.register(ModelResourceLocation.standalone(ResourceLocation.parse(model))));
    }

    static List<String> vanillaModels() {
        return ResearchCatalog.strings(VANILLA, "models");
    }

    /** Original 1.12 metadata and multipart conditions, scoped to blueprint-only assets. */
    static List<JsonObject> vanillaParts(String source, BlockState state) {
        String[] parts = source.split(";", 4);
        var blocks = VANILLA.getAsJsonObject("blocks");
        if (!blocks.has(parts[0])) return List.of();
        var definition = blocks.getAsJsonObject(parts[0]);
        if (definition.has("variants")) {
            String metadata = parts.length > 2 ? parts[2] : "0";
            var variants = definition.getAsJsonObject("variants");
            return variants.has(metadata) ? List.of(variants.getAsJsonObject(metadata)) : List.of();
        }
        var selected = new java.util.ArrayList<JsonObject>();
        for (var element : definition.getAsJsonArray("multipart")) {
            var part = element.getAsJsonObject();
            boolean matches = true;
            if (part.has("when")) for (var condition : part.getAsJsonObject("when").entrySet()) {
                var property = switch (condition.getKey()) {
                    case "north" -> CrossCollisionBlock.NORTH; case "south" -> CrossCollisionBlock.SOUTH;
                    case "east" -> CrossCollisionBlock.EAST; case "west" -> CrossCollisionBlock.WEST;
                    default -> throw new IllegalArgumentException("Unknown original bars condition: " + condition.getKey());
                };
                matches &= state.hasProperty(property) && state.getValue(property) == condition.getValue().getAsBoolean();
            }
            if (matches) selected.add(part.getAsJsonObject("apply"));
        }
        return List.copyOf(selected);
    }

    static void render(GuiGraphics graphics, String source) {
        render(graphics, source, view(Map.of(BlockPos.ZERO, source)), BlockPos.ZERO);
    }

    static void render(GuiGraphics graphics, String source, BlockAndTintGetter view, BlockPos pos) {
        String[] parts = source.split(";", 4);
        String id = parts[0];
        if (id.equals("minecraft:air") || id.startsWith("thaumcraft:placeholder_")
                || id.equals("thaumcraft:thaumatorium_top")) return;
        if (id.equals("thaumcraft:infusion_matrix")) {
            MatrixBlueprintRenderer.render(graphics);
            return;
        }
        int metadata = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        var game = Minecraft.getInstance();
        BlockState state = view.getBlockState(pos);
        var vanilla = vanillaParts(source, state);
        if (!vanilla.isEmpty()) {
            for (var definition : vanilla) renderModel(graphics, definition, state, view, pos, "vanilla", false);
            return;
        }
        String key = id.substring(id.indexOf(':') + 1) + "_" + metadata;
        if (id.startsWith("thaumcraft:") && MODELS.has(key)) {
            JsonObject definition = variant(MODELS.getAsJsonObject(key), pos);
            renderModel(graphics, definition, state, view, pos, key, id.equals("thaumcraft:golem_builder"));
            return;
        }
        if (state.isAir()) return;
        renderWorld(graphics, state.is(Blocks.LAVA) ? null : game.getBlockRenderer().getBlockModel(state), state, view, pos);
    }

    private static void renderModel(GuiGraphics graphics, JsonObject definition, BlockState state,
                                    BlockAndTintGetter view, BlockPos pos, String key, boolean golem) {
        var model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(
                ResourceLocation.parse(definition.get("model").getAsString())));
        int x = definition.has("x") ? definition.get("x").getAsInt() : 0;
        int y = definition.has("y") ? definition.get("y").getAsInt() : 0;
        String cacheKey = key + ":" + definition.get("model").getAsString() + ":" + x + ":" + y;
        var prepared = PREPARED.get(cacheKey);
        // Resource reload supplies new baked model instances; never retain its old quads.
        if (prepared == null || prepared.source() != model) {
            prepared = new PreparedModel(model, orient(model, x, y, golem));
            PREPARED.put(cacheKey, prepared);
        }
        renderWorld(graphics, prepared.world(), state, view, pos);
    }

    /** Minecraft 1.12 MathHelper.getPositionRandom, including the int multiply. */
    static long positionSeed(BlockPos pos) {
        long seed = (long) (pos.getX() * 3129871) ^ (long) pos.getZ() * 116129781L ^ (long) pos.getY();
        return seed * seed * 42317861L + seed * 11L;
    }

    /** The source builder has already stably sorted variants by descending weight. */
    static JsonObject variant(JsonObject definition, BlockPos pos) {
        if (!definition.has("variants")) return definition;
        var variants = definition.getAsJsonArray("variants");
        int total = 0;
        for (var value : variants) total = Math.addExact(total, value.getAsJsonObject().get("weight").getAsInt());
        int choice = Math.abs((int) positionSeed(pos) >> 16) % total;
        for (var value : variants) {
            var selected = value.getAsJsonObject();
            choice -= selected.get("weight").getAsInt();
            if (choice < 0) return selected;
        }
        throw new IllegalStateException("Invalid blueprint model weights");
    }

    static BakedModel orient(BakedModel original, int x, int y, boolean golem) {
        if (x == 0 && y == 0 && !golem) return original;
        Transformation rotation = BlockModelRotation.by(x, y).getRotation();
        Transformation geometry = rotation.applyOrigin(new Vector3f(.5f, .5f, .5f));
        // The original GolemBuilder TESR anchors its raw OBJ at x/z + .5.
        if (golem) geometry = geometry.compose(new Transformation(new Vector3f(.5f, 0, .5f), null, null, null));
        var transformer = QuadTransformers.applying(geometry);
        var forward = rotation.getMatrix();
        var inverse = new Matrix4f(forward).invert();
        return new BakedModelWrapper<BakedModel>(original) {
            private final Map<BakedQuad, BakedQuad> quads = new IdentityHashMap<>();
            @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
                Direction sourceSide = side == null ? null : Direction.rotate(inverse, side);
                return original.getQuads(state, sourceSide, random, data, type).stream().map(quad -> quads.computeIfAbsent(quad, source -> {
                    var transformed = transformer.process(source);
                    // QuadTransformers rotates vertices/normals but retains direction.
                    // World AO/diffuse shading must see the rotated blockstate face.
                    return new BakedQuad(transformed.getVertices(), transformed.getTintIndex(),
                            Direction.rotate(forward, source.getDirection()), transformed.getSprite(),
                            transformed.isShade(), transformed.hasAmbientOcclusion());
                })).toList();
            }
            @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
                return getQuads(state, side, random, ModelData.EMPTY, null);
            }
        };
    }

    private static void renderWorld(GuiGraphics graphics, BakedModel model, BlockState state, BlockAndTintGetter view, BlockPos pos) {
        // TC6 uses world tessellation with standard item lighting disabled.
        // The block shaders consume its AO/face shading without a second GUI
        // diffuse-light pass from the entity/item shaders.
        graphics.flush();
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        graphics.pose().pushPose();
        graphics.pose().translate(-.5, -.5, -.5);
        try {
            if (state.is(Blocks.LAVA)) renderLava(graphics, state, view, pos);
            else {
                var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
                for (RenderType type : model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY)) {
                    renderer.tesselateBlock(view, model, state, pos, graphics.pose(), graphics.bufferSource().getBuffer(type),
                            true, RandomSource.create(42), state.getSeed(pos),
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
                }
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
        }
    }

    static BlockState state(String source) {
        String[] parts = source.split(";", 4);
        String id = parts[0];
        if (id.startsWith("thaumcraft:")) {
            String name = id.substring("thaumcraft:".length());
            if (CONTEXT.has(name)) return STATES.computeIfAbsent(name,
                    ignored -> new ContextState(CONTEXT.getAsJsonObject(name)));
        }
        int metadata = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        // This legacy ID denotes the block in a blueprint, not the modern brick item.
        String blockId = switch (id) {
            case "minecraft:nether_brick" -> "minecraft:nether_bricks";
            case "thaumcraft:stone_arcane" -> "thaumcraft:arcane_stone";
            case "thaumcraft:stone_arcane_brick" -> "thaumcraft:arcane_brick";
            default -> id;
        };
        var location = ResourceLocation.tryParse(blockId);
        BlockState state = location != null && BuiltInRegistries.BLOCK.containsKey(location)
                ? BuiltInRegistries.BLOCK.get(location).defaultBlockState() : null;
        if (state == null) {
            var stack = BookAssets.stack(source);
            if (stack.getItem() instanceof BlockItem item) state = item.getBlock().defaultBlockState();
        }
        if (state == null) return Blocks.AIR.defaultBlockState();
        if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON))
            state = state.setValue(DirectionalBlock.FACING, Direction.from3DDataValue(metadata & 7));
        if (state.is(Blocks.ANVIL))
            state = state.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(metadata & 3));
        return state;
    }

    /**
     * Read-only rendering state. The existing STONE owner avoids creating or
     * registering a gameplay block after registries have frozen; every property
     * consulted by blueprint tessellation comes from the original TC6 state.
     * Do not initialize vanilla's owner-derived state cache for these instances.
     */
    private static final class ContextState extends BlockState {
        private final boolean opaque, fullCube, materialSolid, solidFaces;
        private final int light;
        private final VoxelShape shape, collision;

        private ContextState(JsonObject source) {
            super(Blocks.STONE, new Reference2ObjectArrayMap<>(), MapCodec.unit(Blocks.STONE.defaultBlockState()));
            opaque = source.get("opaque").getAsBoolean();
            fullCube = source.get("full_cube").getAsBoolean();
            materialSolid = source.get("material_solid").getAsBoolean();
            solidFaces = source.get("face_shape").getAsString().equals("solid");
            light = source.get("light").getAsInt();
            shape = box(source.getAsJsonArray("shape"));
            VoxelShape combined = Shapes.empty();
            for (var part : source.getAsJsonArray("collision")) combined = Shapes.or(combined, box(part.getAsJsonArray()));
            collision = combined;
        }

        private static VoxelShape box(com.google.gson.JsonArray values) {
            return Shapes.box(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble(),
                    values.get(3).getAsDouble(), values.get(4).getAsDouble(), values.get(5).getAsDouble());
        }

        @Override public boolean isAir() { return false; }
        @Override public boolean isSolid() { return materialSolid; }
        @Override public boolean canOcclude() { return opaque; }
        @Override public boolean isSolidRender(BlockGetter view, BlockPos pos) { return opaque; }
        @Override public boolean isViewBlocking(BlockGetter view, BlockPos pos) { return opaque; }
        @Override public boolean isSuffocating(BlockGetter view, BlockPos pos) { return fullCube; }
        @Override public boolean propagatesSkylightDown(BlockGetter view, BlockPos pos) { return !opaque; }
        @Override public boolean useShapeForLightOcclusion() { return false; }
        @Override public int getLightBlock(BlockGetter view, BlockPos pos) { return opaque ? 15 : 0; }
        @Override public int getLightEmission() { return light; }
        @Override public int getLightEmission(BlockGetter view, BlockPos pos) { return light; }
        @Override public float getShadeBrightness(BlockGetter view, BlockPos pos) { return fullCube ? .2f : 1; }
        @Override public FluidState getFluidState() { return Fluids.EMPTY.defaultFluidState(); }
        @Override public VoxelShape getShape(BlockGetter view, BlockPos pos, CollisionContext context) { return shape; }
        @Override public VoxelShape getCollisionShape(BlockGetter view, BlockPos pos, CollisionContext context) { return collision; }
        @Override public VoxelShape getOcclusionShape(BlockGetter view, BlockPos pos) { return opaque ? Shapes.block() : Shapes.empty(); }
        @Override public VoxelShape getFaceOcclusionShape(BlockGetter view, BlockPos pos, Direction face) { return opaque ? Shapes.block() : Shapes.empty(); }
        @Override public boolean isFaceSturdy(BlockGetter view, BlockPos pos, Direction face, SupportType support) { return solidFaces; }
        @Override public boolean isCollisionShapeFullBlock(BlockGetter view, BlockPos pos) { return net.minecraft.world.level.block.Block.isShapeFullBlock(collision); }
        @Override public long getSeed(BlockPos pos) { return positionSeed(pos); }
    }

    static BlockAndTintGetter view(Map<BlockPos, String> cells) {
        var states = new HashMap<BlockPos, BlockState>();
        cells.forEach((pos, source) -> states.put(pos.immutable(), state(source)));
        var original = Map.copyOf(states);
        BlockAndTintGetter view = new BlockAndTintGetter() {
            @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
            @Override public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
            @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
            @Override public int getHeight() { return 256; }
            @Override public int getMinBuildHeight() { return 0; }
            @Override public float getShade(Direction face, boolean shaded) {
                if (!shaded) return 1;
                return switch (face) { case DOWN -> .5f; case UP -> 1; case NORTH, SOUTH -> .8f; case EAST, WEST -> .6f; };
            }
            @Override public LevelLightEngine getLightEngine() { return Minecraft.getInstance().level.getLightEngine(); }
            @Override public int getBlockTint(BlockPos pos, ColorResolver resolver) { return 0xffffff; }
            @Override public int getBrightness(LightLayer layer, BlockPos pos) { return 15; }
            @Override public int getRawBrightness(BlockPos pos, int amount) { return 15; }
        };
        // Match IronBarsBlock placement against the complete blueprint, including
        // solid neighbors. All cells exist before this pass; drawing order cannot
        // turn a connected furnace grille into an isolated center post.
        original.forEach((pos, raw) -> {
            if (!(raw.getBlock() instanceof IronBarsBlock bars)) return;
            BlockState connected = raw;
            for (Direction face : Direction.Plane.HORIZONTAL) {
                BlockPos adjacent = pos.relative(face);
                BlockState neighbor = original.getOrDefault(adjacent, Blocks.AIR.defaultBlockState());
                var property = switch (face) {
                    case NORTH -> CrossCollisionBlock.NORTH; case SOUTH -> CrossCollisionBlock.SOUTH;
                    case EAST -> CrossCollisionBlock.EAST; case WEST -> CrossCollisionBlock.WEST;
                    default -> throw new IllegalArgumentException("Expected a horizontal connection");
                };
                connected = connected.setValue(property,
                        bars.attachsTo(neighbor, neighbor.isFaceSturdy(view, adjacent, face.getOpposite())));
            }
            states.put(pos, connected);
        });
        return view;
    }

    private static void renderLava(GuiGraphics graphics, BlockState state, BlockAndTintGetter view, BlockPos pos) {
        var game = Minecraft.getInstance();
        var target = graphics.bufferSource().getBuffer(RenderType.translucent());
        var pose = graphics.pose().last();
        // LiquidBlockRenderer has no PoseStack argument; transform its actual fluid vertices.
        VertexConsumer transformed = new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) {
                // LiquidBlockRenderer emits section-relative coordinates, while
                // the current pose already points at this cell's local corner.
                target.addVertex(pose, x - (pos.getX() & 15), y - (pos.getY() & 15), z - (pos.getZ() & 15))
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                return this;
            }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(r,g,b,a); return this; }
            @Override public VertexConsumer setUv(float u, float v) { target.setUv(u,v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u,v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(u,v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(pose,x,y,z); return this; }
        };
        game.getBlockRenderer().renderLiquid(pos, view, transformed, state, state.getFluidState());
    }

    private BookBlocks() {}
}
