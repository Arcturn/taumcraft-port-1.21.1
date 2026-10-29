package thaumcraft.port.research;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned data in the overworld; independent of player entity clones and dimensions. */
public final class ResearchStorage extends SavedData {
    private final Map<UUID, ResearchState> players = new HashMap<>();

    public static ResearchStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ResearchStorage::new, ResearchStorage::load, null), "thaumcraft_research_v1");
    }

    public ResearchState player(UUID id) {
        return players.computeIfAbsent(id, ignored -> new ResearchState());
    }

    public static ResearchStorage load(CompoundTag root, HolderLookup.Provider registries) {
        ResearchStorage result = new ResearchStorage();
        for (Tag tag : root.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            if (!entry.hasUUID("uuid")) continue;
            result.players.put(entry.getUUID("uuid"), ResearchCodec.read(entry));
        }
        if(root.getInt("schema")<ResearchCodec.SCHEMA)result.setDirty();
        return result;
    }

    private static List<String> strings(CompoundTag tag, String key) {
        return tag.getList(key, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        players.forEach((uuid, state) -> {
            CompoundTag entry = ResearchCodec.write(state);
            entry.putUUID("uuid", uuid);
            entries.add(entry);
        });
        root.putInt("schema", ResearchCodec.SCHEMA);
        root.put("players", entries);
        return root;
    }
}
