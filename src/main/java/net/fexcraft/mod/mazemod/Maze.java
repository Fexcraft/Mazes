package net.fexcraft.mod.mazemod;

import net.fexcraft.app.json.JsonArray;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.lib.common.math.Time;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;

import static net.minecraft.core.registries.Registries.DIMENSION;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Maze {

	public BlockPos rawsize, size;
	public ArrayList<BlockPos> entry = new ArrayList<>();
	public ArrayList<BlockPos> exit = new ArrayList<>();
	public BlockPos gate_in;
	public BlockPos gate_out;
	public final String id;
	public final ArrayList<BlockPos> chests = new ArrayList<>();
	public ResourceKey<Level> dimid;
	public BlockPos orgpos;
	public BlockPos tppos;
	//
	public int instances = 1;
	public int cooldown = 60;

	public Maze(String nid){
		id = nid;
	}

	public Maze(JsonMap map){
		id = map.get("id").string_value();
		rawsize = frArray(map, "raw");
		size = frArray(map, "size");
		if(map.has("entry")){
			entry.clear();
			map.getArray("entry").value.forEach(val -> entry.add(frArray(val.asArray())));
		}
		if(map.has("exit")){
			exit.clear();
			map.getArray("exit").value.forEach(val -> exit.add(frArray(val.asArray())));
		}
		gate_in = frArray(map, "gate_in");
		gate_out = frArray(map, "gate_out");
		orgpos = frArray(map, "dim_pos");
		tppos = frArray(map, "tp_pos");
		dimid = ResourceKey.create(DIMENSION, new ResourceLocation(map.getString("dim_id", null)));
		if(map.has("chests")){
			map.getArray("chests").value.forEach(val -> {
				chests.add(frArray(val.asArray()));
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
		toArray(map, "raw", rawsize);
		toArray(map, "size", size);
		if(entry != null){
			JsonArray array = new JsonArray();
			entry.forEach(pos -> array.add(toArray(pos)));
			map.add("entry", array);
		}
		if(exit != null){
			JsonArray array = new JsonArray();
			exit.forEach(pos -> array.add(toArray(pos)));
			map.add("exit", array);
		}
		toArray(map, "gate_in", gate_in);
		toArray(map, "gate_out", gate_out);
		toArray(map, "dim_pos", orgpos);
		toArray(map, "tp_pos", tppos);
		if(dimid != null){
			map.add("dim_id", dimid.location().toString());
		}
		if(chests.size() > 0){
			JsonArray array = new JsonArray();
			for(BlockPos pos : chests) array.add(toArray(pos));
			map.add("chests", array);
		}
		return map;
	}

	public static void toArray(JsonMap map, String key, BlockPos pos){
		if(pos != null) map.add(key, toArray(pos));
	}

	public static JsonArray toArray(BlockPos pos){
		return new JsonArray.Flat(pos.getX(), pos.getY(), pos.getZ());
	}

	public static BlockPos frArray(JsonMap map, String key){
		if(!map.has(key)) return null;
		return frArray(map.getArray(key));
	}

	public static BlockPos frArray(JsonArray array){
		return new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
	}

}
