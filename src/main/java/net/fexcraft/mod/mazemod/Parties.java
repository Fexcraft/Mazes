package net.fexcraft.mod.mazemod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * @author Ferdinand Calo' (FEX___96)
 */
public class Parties {

	public static HashMap<UUID, String> CODES = new HashMap<>();
	public static HashMap<UUID, ArrayList<Player>> PARTIES = new HashMap<>();
	public static HashMap<UUID, Player> PARTYIN = new HashMap<>();

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
		for(Player player : party){
			PARTYIN.remove(player.getGameProfile().getId());
			player.sendSystemMessage(Component.literal(lead.getDisplayName() + " disbanded the party."));
		}
		CODES.remove(id);
	}

	public static void leave(ServerPlayer player){
		Player in = PARTYIN.get(player.getGameProfile().getId());
		var party = PARTIES.get(in.getGameProfile().getId());
		party.remove(player);
		for(Player member : party){
			member.sendSystemMessage(Component.literal(player.getDisplayName() + " left the party."));
		}
		player.sendSystemMessage(Component.literal("You left " + in.getDisplayName() + "'s party."));
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
		for(Player member : party){
			member.sendSystemMessage(Component.literal(player.getDisplayName() + " joins the party."));
		}
		party.add(player);
		PARTYIN.put(player.getGameProfile().getId(), server.getPlayerList().getPlayer(lead));
	}

}
