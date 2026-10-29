package thaumcraft.port.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

/** Real server-side player state with NeoForge's no-send connection, without a client login. */
final class GameTestPlayers {
    static ServerPlayer create(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        FakePlayer player = new FakePlayer(helper.getLevel(),
                new GameProfile(id, "tc-test-" + id.toString().substring(0, 8)));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        // A real login initializes these slots. Tests load the same datapack-defined slots
        // through Curios' public API without sending login synchronization packets.
        CuriosApi.getCuriosInventory(player).orElseThrow().reset();
        return player;
    }

    private GameTestPlayers() {}
}
