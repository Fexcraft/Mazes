package net.fexcraft.mod.mazemod;

import net.fexcraft.app.json.JsonArray;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.lib.common.math.Time;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Maze {

	public Vec3i rawsize, size;
	public BlockPos entry_min, entry_max;
	public BlockPos exit_min, exit_max;
	public final String id;
	public final ArrayList<BlockPos> chests = new ArrayList<>();

	public Maze(String nid){
		id = nid;
	}

	public Maze(JsonMap map){
		id = map.get("id").string_value();
		JsonArray array = map.getArray("raw");
		rawsize = new Vec3i(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		array = map.getArray("size");
		size = new Vec3i(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		if(map.has("entry_min")){
			array = map.getArray("entry_min");
			exit_min = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("entry_max")){
			array = map.getArray("entry_max");
			entry_max = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("exit_min")){
			array = map.getArray("exit_min");
			exit_min = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("entry_max")){
			array = map.getArray("entry_max");
			exit_min = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("chests")){
			array = map.getArray("chests");
			array.value.forEach(val -> {
				chests.add(BlockPos.of(val.long_value()));
			});
		}
	}

	public File getStatesFile(){
		return new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazes/" + id + ".nbt");
	}

	public File getTemplateFile(){
		return new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazes/" + id + ".json");
	}

	public JsonMap save(){
		JsonMap map = new JsonMap();
		map.add("id", id);
		map.add("saved", Time.getAsString(Time.getDate()));
		map.add("raw", new JsonArray(rawsize.getX(), rawsize.getY(), rawsize.getZ()));
		map.add("size", new JsonArray(size.getX(), size.getY(), size.getZ()));
		if(entry_min != null){
			map.add("entry_min", new JsonArray(entry_min.getX(), entry_min.getY(), entry_min.getZ()));
		}
		if(entry_max != null){
			map.add("entry_max", new JsonArray(entry_max.getX(), entry_max.getY(), entry_max.getZ()));
		}
		if(exit_min != null){
			map.add("exit_min", new JsonArray(exit_min.getX(), exit_min.getY(), exit_min.getZ()));
		}
		if(entry_max != null){
			map.add("entry_max", new JsonArray(entry_max.getX(), entry_max.getY(), entry_max.getZ()));
		}
		if(chests.size() > 0){
			JsonArray array = new JsonArray();
			for(BlockPos pos : chests) array.add(pos.asLong());
			map.add("chests", array);
		}
		return map;
	}

}
