package thaumcraft.port.infusion.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ElementalToolPayload() implements CustomPacketPayload {
    public static final Type<ElementalToolPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:architect_orientation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ElementalToolPayload> CODEC=StreamCodec.unit(new ElementalToolPayload());
    @Override public Type<ElementalToolPayload> type() { return TYPE; }
    public static void handle(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.getMainHandItem().is(InfusionTools.SHOVEL.get())) return;
        ElementalShovelItem.cycleOrientation(player.getMainHandItem());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
    public record Placement(BlockPos pos,Direction face) implements CustomPacketPayload {
        public static final Type<Placement> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:architect_placement"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Placement> CODEC=StreamCodec.of(
                (buf,p) -> {buf.writeBlockPos(p.pos);buf.writeEnum(p.face);},buf -> new Placement(buf.readBlockPos(),buf.readEnum(Direction.class)));
        @Override public Type<Placement> type() { return TYPE; }
    }
    public static java.util.function.Consumer<Placement> visual=p -> {};
    public static void placement(Level level,BlockPos pos,Direction face) {
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            PacketDistributor.sendToPlayersNear(server,null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,32,new Placement(pos,face));
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("9").playToServer(TYPE,CODEC,(p,c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player) handle(player);
        }));
        event.registrar("9").playToClient(Placement.TYPE,Placement.CODEC,(p,c) -> c.enqueueWork(() -> visual.accept(p)));
    }
}
