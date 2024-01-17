package net.fexcraft.mod.mazemod;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import net.fexcraft.app.json.JsonArray;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.lib.common.math.Time;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.storage.loot.LootDataResolver;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MazeInst {

	public Maze root;
	public ChunkPos start;
	public ChunkPos end;
	public ArrayList<Player> players = new ArrayList<>();
	public BlockPos zeropos;
	public final UUID uuid;
	public boolean paused;

	public MazeInst(UUID id, Maze maze, ChunkPos s, ChunkPos e){
		root = maze;
		start = s;
		end = e;
		uuid = id;
	}

	public MazeInst(JsonMap map){
		root = MazeManager.MAZES.get(map.get("root").string_value());
		JsonArray arr = map.getArray("start");
		start = new ChunkPos(arr.get(0).integer_value(), arr.get(1).integer_value());
		arr = map.getArray("end");
		end = new ChunkPos(arr.get(0).integer_value(), arr.get(1).integer_value());
		zeropos = Maze.frArray(map, "zeropos");
		uuid = UUID.fromString(map.get("uuid").string_value());
	}

	public JsonMap save(){
		JsonMap map = new JsonMap();
		map.add("saved", Time.getAsString(Time.getDate()));
		map.add("root", root.id);
		map.add("start", new JsonArray.Flat(start.x, start.z));
		map.add("end", new JsonArray.Flat(end.x, end.z));
		Maze.toArray(map, "zeropos", zeropos);
		map.add("uuid", uuid.toString());
		return map;
	}

	public void refill(){
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		ServerLevel level = server.getLevel(MazesMod.MAZES_LEVEL);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		ArrayList<ChunkPos> ckpos = new ArrayList<>();
		for(BlockPos c : root.chests){
			pos = pos.set(c.getX() + zeropos.getX(), c.getY() + zeropos.getY(), c.getZ() + zeropos.getZ());
			ChunkPos ckp = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
			if(!ckpos.contains(ckp)) level.setChunkForced(ckp.x, ckp.z, true);
			ChestBlockEntity chest = (ChestBlockEntity)level.getExistingBlockEntity(pos);
			if(chest != null){
				chest.clearContent();
				chest.setLootTable(new ResourceLocation("mazesmod", root.id), 0);
			}
			else MazesMod.LOGGER.info(pos.toShortString() + " / " + level.dimension().location() + " has no chest.");
		}
		for(ChunkPos ckp : ckpos){
			try{
				level.setChunkForced(ckp.x, ckp.z, false);
			}
			catch(Throwable e){
				e.printStackTrace();
			}
		}
	}

	public File getFile(){
		return new File(FMLPaths.CONFIGDIR.get().toFile(), "/maze_instances/" + uuid + ".json");
	}
}
