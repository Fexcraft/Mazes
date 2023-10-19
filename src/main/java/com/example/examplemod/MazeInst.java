package com.example.examplemod;

import java.util.ArrayList;

import net.fexcraft.app.json.JsonArray;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.lib.common.math.Time;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class MazeInst {

	public Maze root;
	public ChunkPos start;
	public ChunkPos end;
	public ArrayList<Player> players = new ArrayList<>();

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
	}

	public JsonMap save(){
		JsonMap map = new JsonMap();
		map.add("saved", Time.getAsString(Time.getDate()));
		map.add("root", root.id);
		map.add("start", new JsonArray(start.x, start.z));
		map.add("end", new JsonArray(end.x, end.z));
		return map;
	}

}
