package net.fexcraft.mod.mazemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class EnrxitBlock extends Block {

	public final boolean exit;

	public EnrxitBlock(boolean isexit){
		super(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.forceSolidOn()
			.noCollission()
			.strength(64)
			.noOcclusion()
			.pushReaction(PushReaction.IGNORE));
		exit = isexit;
	}

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity){
		if(entity instanceof Player == false) return;
		Player player = (Player)entity;
		PlayerData data = MazeManager.getPlayerData(player);
		if(data.cooldown > 0){
			data.cooldown--;
			return;
		}
		if(world.dimension().equals(MazesMod.MAZES_LEVEL)){
			if(exit) data.teleport(player);
		}
		else{
			Maze maze = MazeManager.getMazeFor(pos, exit);
			if(maze == null){
				data.last = new BlockPos(pos);
				data.lexit = exit;
				player.sendSystemMessage(Component.literal("Gate not linked. (" + pos.toShortString() + ")"));
				player.sendSystemMessage(Component.literal("Use '/mz link <id>' to link to a template."));
			}
			else if(maze.gate_out == null){
				player.sendSystemMessage(Component.literal("Exit gate is missing."));
			}
			else if(maze.gate_in == null){
				player.sendSystemMessage(Component.literal("Entry gate is missing."));
			}
			else if(!exit){
				MazeInst inst = MazeManager.getFreeInst(player, maze);
				if(inst == null){
					player.sendSystemMessage(Component.literal("All instances are occupied."));
				}
				else{
					inst.refill();
					data.teleport(player, inst);
					ArrayList<Player> party = Parties.PARTIES.get(player.getGameProfile().getId());
				}
			}
		}
		data.cooldown += 100;
	}


}
