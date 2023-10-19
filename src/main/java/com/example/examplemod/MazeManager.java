package com.example.examplemod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import net.fexcraft.app.json.JsonHandler;
import net.fexcraft.app.json.JsonHandler.PrintOption;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class MazeManager {

	public static HashMap<String, Maze> MAZES = new HashMap<>();
	public static ArrayList<MazeInst> INSTANCES = new ArrayList<>();
	private static BlockPos CENTER = new BlockPos(0, 0 ,0);

	public static void register(CommandContext<CommandSourceStack> context, boolean update) throws Exception{
		String id = context.getArgument("id", String.class);
		BlockPos as = Vec3Argument.getCoordinates(context, "start").getBlockPos(context.getSource());
		BlockPos ae = Vec3Argument.getCoordinates(context, "end").getBlockPos(context.getSource());
		BlockPos st = new BlockPos(ae.getX() < as.getX() ? ae.getX() : as.getX(), ae.getY() < as.getY() ? ae.getY() : as.getY(), ae.getZ() < as.getZ() ? ae.getZ() : as.getZ());
		BlockPos en = new BlockPos(ae.getX() > as.getX() ? ae.getX() : as.getX(), ae.getY() > as.getY() ? ae.getY() : as.getY(), ae.getZ() > as.getZ() ? ae.getZ() : as.getZ());
		if(MAZES.containsKey(id) && !update){
			context.getSource().sendFailure(Component.literal("A Maze template with that ID exists already."));
			context.getSource().sendFailure(Component.literal("Use /mz-upd instead if you wish to update."));
			context.getSource().sendFailure(Component.literal("Use /mz-del to delete it."));
			return;
		}
		Maze maze = MAZES.containsKey(id) ? MAZES.get(id) : new Maze(id);
		if(update) context.getSource().sendSystemMessage(Component.literal("Starting update of maze '" + id + "' ..."));
		else context.getSource().sendSystemMessage(Component.literal("Starting registration of maze '" + id + "' ..."));
		maze.rawsize = new Vec3i(en.getX() - st.getX(), en.getY() - st.getY(), en.getZ() - st.getZ());
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
		CompoundTag com = new CompoundTag();
		com.putInt("rsize_x", maze.rawsize.getX());
		com.putInt("rsize_y", maze.rawsize.getY());
		com.putInt("rsize_z", maze.rawsize.getZ());
		com.putInt("size_x", maze.size.getX());
		com.putInt("size_y", maze.size.getY());
		com.putInt("size_z", maze.size.getZ());
		ListTag blks = new ListTag();
		Level world = context.getSource().getLevel();
		MutableBlockPos pos = new MutableBlockPos();
		ArrayList<BlockState> states = new ArrayList<>();
		for(int px = 0; px < maze.rawsize.getX(); px++){
			for(int py = 0; py < maze.rawsize.getX(); py++){
				for(int pz = 0; pz < maze.rawsize.getX(); pz++){
					BlockState state = world.getBlockState(pos.set(st.getX() + px, st.getY() + py, st.getZ() + pz));
					int idx = states.indexOf(state);
					if(idx < 0){
						idx = states.size();
						states.add(state);
					}
					CompoundTag tag = new CompoundTag();
					tag.putLong("p", pos.set(px, py, pz).asLong());
					tag.putInt("b", idx);
					blks.add(tag);
					if(state.getBlock() instanceof ChestBlock){
						maze.chests.add(pos);
					}
				}
			}
		}
		com.put("blocks", blks);
		CompoundTag sts = new CompoundTag();
		for(int i = 0; i < states.size(); i++){
			DataResult<Tag> o = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, states.get(i));
			sts.put(i + "", o.result().get());
		}
		com.put("states", sts);
		File file = maze.getStatesFile();
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		NbtIo.write(com, file);
		context.getSource().sendSystemMessage(Component.literal("File saved as: " + file.getPath()));
		if(!MAZES.containsKey(id)) MAZES.put(maze.id, maze);
	}

	public static void load(){
		MAZES.clear();
		File folder = new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazes");
		if(!folder.exists()) folder.mkdirs();
		for(File file : folder.listFiles()){
			if(file.getName().endsWith(".json")){
				try{
					Maze maze = new Maze(JsonHandler.parse(file));
					MAZES.put(maze.id, maze);
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		folder = new File(FMLPaths.CONFIGDIR.get().toFile(), "/maze_instances");
		if(!folder.exists()) folder.mkdirs();
		for(File file : folder.listFiles()){
			try{
				INSTANCES.add(new MazeInst(JsonHandler.parse(file)));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public static void save(){
		File folder = new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazes");
		if(!folder.exists()) folder.mkdirs();
		for(Maze maze : MAZES.values()){
			JsonHandler.print(maze.getTemplateFile(), maze.save(), PrintOption.DEFAULT);
		}
		folder = new File(FMLPaths.CONFIGDIR.get().toFile(), "/maze_instances");
		if(!folder.exists()) folder.mkdirs();
		for(int i = 0; i < INSTANCES.size(); i++){
			JsonHandler.print(new File(folder, "inst_" + i + ".json"), INSTANCES.get(i).save(), PrintOption.DEFAULT);
		}
	}

}
