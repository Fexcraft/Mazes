package net.fexcraft.mod.mazemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Teleporter implements ITeleporter {

	private final Vec3 vec;

	public Teleporter(Vec3 vecto){
		vec = vecto;
	}

    @Nullable
    public PortalInfo getPortalInfo(Entity entity, ServerLevel world, Function<ServerLevel, PortalInfo> definfo){
        return new PortalInfo(vec, Vec3.ZERO, entity.getYRot(), entity.getXRot());
    }

}
