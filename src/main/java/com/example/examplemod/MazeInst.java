package com.example.examplemod;

import java.util.ArrayList;

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

}
