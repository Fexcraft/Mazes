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

	public Vec3i rawsize, size;
	public BlockPos entry;
	public BlockPos exit;
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
		JsonArray array = map.getArray("raw");
		rawsize = new Vec3i(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		array = map.getArray("size");
		size = new Vec3i(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		if(map.has("entry")){
			array = map.getArray("entry");
			entry = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("exit")){
			array = map.getArray("exit");
			exit = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("dim_pos")){
			array = map.getArray("dim_pos");
			orgpos = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		if(map.has("tp_pos")){
			array = map.getArray("tp_pos");
			orgpos = new BlockPos(array.get(0).integer_value(), array.get(1).integer_value(), array.get(2).integer_value());
		}
		dimid = ResourceKey.create(DIMENSION, new ResourceLocation(map.getString("dim_id", null)));
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
		if(entry != null){
			map.add("entry", new JsonArray(entry.getX(), entry.getY(), entry.getZ()));
		}
		if(exit != null){
			map.add("exit", new JsonArray(exit.getX(), exit.getY(), exit.getZ()));
		}
		if(orgpos != null){
			map.add("dim_pos", new JsonArray(orgpos.getX(), orgpos.getY(), orgpos.getZ()));
		}
		if(tppos != null){
			map.add("tp_pos", new JsonArray(tppos.getX(), tppos.getY(), tppos.getZ()));
		}
		if(dimid != null){
			map.add("dim_id", dimid.location().toString());
		}
		if(chests.size() > 0){
			JsonArray array = new JsonArray();
			for(BlockPos pos : chests) array.add(pos.asLong());
			map.add("chests", array);
		}
		return map;
	}

}
