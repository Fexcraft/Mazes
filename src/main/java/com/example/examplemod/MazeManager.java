package com.example.examplemod;

import java.util.HashMap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class MazeManager {

	public static HashMap<String, Maze> MAZES = new HashMap<>();
	public static HashMap<Integer, MazeInst> INSTANCES = new HashMap<>();
	private static BlockPos CENTER = new BlockPos(0, 0 ,0);

	public static void register(CommandContext<CommandSourceStack> context, boolean update) throws Exception{
		String id = context.getArgument("id", String.class);
		BlockPos as = Vec3Argument.getCoordinates(context, "start").getBlockPos(context.getSource());
		BlockPos ae = Vec3Argument.getCoordinates(context, "end").getBlockPos(context.getSource());
		BlockPos st = new BlockPos(ae.getX() < as.getX() ? ae.getX() : as.getX(), ae.getY() < as.getY() ? ae.getY() : as.getY(), ae.getZ() < as.getZ() ? ae.getZ() : as.getZ());
		BlockPos en = new BlockPos(ae.getX() > as.getX() ? ae.getX() : as.getX(), ae.getY() > as.getY() ? ae.getY() : as.getY(), ae.getZ() > as.getZ() ? ae.getZ() : as.getZ());
		if(MAZES.containsKey(id) && !update){
			context.getSource().sendFailure(Component.literal("A Maze with that ID exists already."));
			context.getSource().sendFailure(Component.literal("Use /mz-upd instead if you wish to update."));
			context.getSource().sendFailure(Component.literal("Use /mz-del to delete it."));
			return;
		}
		Maze maze = MAZES.containsKey(id) ? MAZES.get(id) : new Maze(id);
		if(update) context.getSource().sendSystemMessage(Component.literal("Starting update of maze '" + id + "' ..."));
		else context.getSource().sendSystemMessage(Component.literal("Starting registration of maze '" + id + "' ..."));
		maze.rawsize = new Vec3i(Math.abs(st.getX()) + Math.abs(en.getX()), Math.abs(st.getY()) + Math.abs(en.getY()), Math.abs(st.getZ()) + Math.abs(en.getZ()));
		context.getSource().sendSystemMessage(Component.literal("Raw Size: " + maze.rawsize.getX() + ", " + maze.rawsize.getY() + ", " + maze.rawsize.getZ() + ", "));
		int rx = maze.rawsize.getX();
		int ry = maze.rawsize.getY();
		int rz = maze.rawsize.getZ();
		int x = rx / 16;
		int y = ry / 16;
		int z = rz / 16;
		if(rx % 16 < 8) x++;
		if(ry % 16 < 8) y++;
		if(rz % 16 < 8) z++;
		maze.size = new Vec3i(x, y, z);
		context.getSource().sendSystemMessage(Component.literal("Rounded Size: " + (x * 16) + ", " + (y * 16) + ", " + (z * 16) + ", "));
		//
	}

}
