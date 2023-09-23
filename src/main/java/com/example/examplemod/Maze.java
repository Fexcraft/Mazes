package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Maze {

	public Vec3i rawsize, size;
	public BlockPos entry_min, entry_max;
	public BlockPos exit_min, exit_max;
	public final String id;

	public Maze(String nid){
		id = nid;
	}

}
