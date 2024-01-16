package net.fexcraft.mod.mazemod;

import java.util.ArrayList;
import java.util.Random;

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
import net.minecraft.world.level.storage.loot.LootDataResolver;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MazeInst {

	public Maze root;
	public ChunkPos start;
	public ChunkPos end;
	public ArrayList<Player> players = new ArrayList<>();
	public BlockPos zeropos;

	public MazeInst(Maze maze, ChunkPos s, ChunkPos e){
		root = maze;
		start = s;
		end = e;
	}

	public MazeInst(JsonMap map){
		root = MazeManager.MAZES.get(map.get("root").string_value());
		JsonArray arr = map.getArray("start");
		start = new ChunkPos(arr.get(0).integer_value(), arr.get(1).integer_value());
		arr = map.getArray("end");
		end = new ChunkPos(arr.get(0).integer_value(), arr.get(1).integer_value());
		arr = map.getArray("zeropos");
		zeropos = new BlockPos(arr.get(0).integer_value(), arr.get(1).integer_value(), arr.get(2).integer_value());
	}

	public JsonMap save(){
		JsonMap map = new JsonMap();
		map.add("saved", Time.getAsString(Time.getDate()));
		map.add("root", root.id);
		map.add("start", new JsonArray(start.x, start.z));
		map.add("end", new JsonArray(end.x, end.z));
		map.add("zeropos", new JsonArray(zeropos.getX(), zeropos.getY(), zeropos.getZ()));
		return map;
	}

	public void refill(){
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		ServerLevel level = server.getLevel(MazesMod.MAZES_LEVEL);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		LootDataResolver res = server.getLootData();
		LootTable table = res.getLootTable(new ResourceLocation("mazesmod:test"));
		LootParams param = new LootParams.Builder(level).create(LootContextParamSet.builder().build());
		for(BlockPos c : root.chests){
			pos = pos.set(c.getX() + zeropos.getX(), c.getY() + zeropos.getY(), c.getZ() + zeropos.getZ());
			level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
			ChestBlockEntity chest = (ChestBlockEntity)level.getBlockEntity(pos);
			if(chest != null) table.fill(chest, param, 0);
			else MazesMod.LOGGER.info(pos.toShortString() + " / " + level.dimension().location() + " has no chest.");
			level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
		}
	}

}
