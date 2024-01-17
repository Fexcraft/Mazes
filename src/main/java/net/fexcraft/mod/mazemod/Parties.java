package net.fexcraft.mod.mazemod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Parties {

	public static HashMap<UUID, String> CODES = new HashMap<>();
	public static HashMap<UUID, ArrayList<UUID>> PARTIES = new HashMap<>();
	public static HashMap<UUID, PartyLead> PARTYIN = new HashMap<>();

	public static void putNewCode(UUID id){
		String newcode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		for(Entry<UUID, String> entry : CODES.entrySet()){
			if(entry.getValue().equals(newcode)){
				putNewCode(id);
				return;
			}
		}
		CODES.put(id, newcode);
	}

	public static void disband(ServerPlayer lead, UUID id){
		var party = Parties.PARTIES.remove(id);
		Player ply = null;
		for(UUID player : party){
			PARTYIN.remove(player);
			ply = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player);
			if(ply != null) ply.sendSystemMessage(Component.literal(lead.getDisplayName() + " disbanded the party."));
		}
		CODES.remove(id);
	}

	public static void leave(ServerPlayer player){
		PartyLead in = PARTYIN.get(player.getGameProfile().getId());
		Player mem = null;
		var party = PARTIES.get(in.uuid);
		party.remove(player);
		for(UUID member : party){
			mem = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(member);
			if(mem != null) mem.sendSystemMessage(Component.literal(player.getDisplayName() + " left the party."));
		}
		player.sendSystemMessage(Component.literal("You left " + in.name + "'s party."));
	}

	public static UUID get(String code){
		for(Entry<UUID, String> entry : CODES.entrySet()){
			if(entry.getValue().equals(code)){
				return entry.getKey();
			}
		}
		return null;
	}

	public static void join(MinecraftServer server, ServerPlayer player, UUID lead){
		var party = PARTIES.get(lead);
		Player mem = null;
		for(UUID member : party){
			mem = server.getPlayerList().getPlayer(member);
			if(mem != null) mem.sendSystemMessage(Component.literal(player.getDisplayName() + " joins the party."));
		}
		party.add(player.getGameProfile().getId());
		Player pead = server.getPlayerList().getPlayer(lead);
		PARTYIN.put(player.getGameProfile().getId(), new PartyLead(lead, pead.getGameProfile().getName()));
	}

	public record PartyLead(UUID uuid, String name){}

}
