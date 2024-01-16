package net.fexcraft.mod.mazemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.function.Function;

import static net.fexcraft.mod.mazemod.MazesMod.MAZES_LEVEL;
import static net.minecraft.world.level.Level.OVERWORLD;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class PlayerData {

	public SelectionCache selcache = new SelectionCache();
	public MazeInst instin;
	public int cooldown;
	public BlockPos last;
	public boolean lexit;
	public boolean informed;

	public void teleport(Player player, MazeInst to){
		try{
			ServerLevel lvl = ServerLifecycleHooks.getCurrentServer().getLevel(MAZES_LEVEL);
			player.moveTo(to.zeropos.offset(to.root.entry.get(0)).getCenter());
			player.changeDimension(lvl, new ITeleporter() {
				@Override
				public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity){
					entity = ITeleporter.super.placeEntity(entity, currentWorld, destWorld, yaw, repositionEntity);
					instin = to;;
					to.players.add((Player)entity);
					return entity;
				}
				@Override
				public boolean isVanilla(){
					return false;
				}
			});
		}
		catch(Throwable e){
			e.printStackTrace();
		}
	}

	public void teleport(Player player){
		try{
			ServerLevel lvl = player.getServer().getLevel(OVERWORLD);
			BlockPos pos = instin.root.gate_out == null ? instin.root.gate_in : instin.root.gate_out;
			instin.players.remove(player);
			player.moveTo(pos.getCenter());
			player.changeDimension(lvl, new ITeleporter(){});
		}
		catch(Throwable e){
			e.printStackTrace();
		}
	}

}
