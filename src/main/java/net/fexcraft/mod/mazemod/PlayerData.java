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
			instin = to;
			Entity entity = player.changeDimension(lvl, new Teleporter(to.zeropos.offset(to.root.entry.get(0)).getCenter()));
			if(entity != null) instin.players.add((Player)entity);
		}
		catch(Throwable e){
			e.printStackTrace();
		}
	}

	public void teleport(Player player){
		try{
			ServerLevel lvl = ServerLifecycleHooks.getCurrentServer().overworld();
			BlockPos pos = instin.root.gate_out == null ? instin.root.gate_in : instin.root.gate_out;
			instin.players.remove(player);
			player.changeDimension(lvl, new Teleporter(pos.getCenter()));
		}
		catch(Throwable e){
			e.printStackTrace();
		}
	}

}
