package net.fexcraft.mod.mazemod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class SelectionUtil {

    private static final ConcurrentHashMap<UUID, SelectionCache> CACHE = new ConcurrentHashMap<>();

    public static SelectionCache get(Player entity){
        return CACHE.get(entity.getGameProfile().getId());
    }

    public static void add(Player entity){
        CACHE.put(entity.getGameProfile().getId(), new SelectionCache());
    }

    public static void rem(Player entity){
        CACHE.remove(entity.getGameProfile().getId());
    }

    public static class SelectionCache {

        public BlockPos first;
        public BlockPos second;

    }

}
