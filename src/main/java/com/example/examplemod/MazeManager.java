package com.example.examplemod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.types.templates.CompoundList;
import com.mojang.serialization.DataResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.fml.loading.FMLPaths;

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
		File file = new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazes/" + id + ".nbt");
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		NbtIo.write(com, file);
		context.getSource().sendSystemMessage(Component.literal("File saved as: " + file.getPath()));
	}

}
