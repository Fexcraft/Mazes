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

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class EnrxitBlock extends Block {

	private boolean exit;

	public EnrxitBlock(boolean isexit){
		super(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.forceSolidOn()
			.noCollission()
			.strength(64)
			.pushReaction(PushReaction.IGNORE));
		exit = isexit;
	}

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity){
		if(entity instanceof Player == false) return;
		Player player = (Player)entity;
		player.sendSystemMessage(Component.literal(pos.toShortString()));
	}


}
