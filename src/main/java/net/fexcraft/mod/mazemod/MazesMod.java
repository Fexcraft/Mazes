package net.fexcraft.mod.mazemod;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.core.registries.Registries.DIMENSION;
import static net.minecraft.world.level.Level.OVERWORLD;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.fexcraft.app.json.JsonHandler;
import net.fexcraft.app.json.JsonHandler.PrintOption;
import net.fexcraft.app.json.JsonMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MazesMod.MODID)
public class MazesMod {

    public static final String MODID = "mazesmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static File CFG_FOLDER;
    public static int CFG_DISTANCE;
    //
    public static final ResourceKey<Level> MAZES_LEVEL = ResourceKey.create(DIMENSION, new ResourceLocation(MODID, "mazes"));

    public static final RegistryObject<Block> ENTRY_BLOCK = BLOCKS.register("entry", () -> new EnrxitBlock(false));
    public static final RegistryObject<Item> ENTRY_ITEM = ITEMS.register("entry", () -> new BlockItem(ENTRY_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Block> EXIT_BLOCK = BLOCKS.register("exit", () -> new EnrxitBlock(true));
    public static final RegistryObject<Item> EXIT_ITEM = ITEMS.register("exit", () -> new BlockItem(EXIT_BLOCK.get(), new Item.Properties()));
    //public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
    //        .alwaysEat().nutrition(1).saturationMod(2f).build())));

    public MazesMod(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
        File file = new File(FMLPaths.CONFIGDIR.get().toFile(), "/mazemod.json");
        if(!file.exists()){
            JsonMap map = new JsonMap();
            map.add("instance_distance", 8);
            JsonHandler.print(file, map, PrintOption.DEFAULT);
        }
        JsonMap config = JsonHandler.parse(file);
        CFG_DISTANCE = config.getInteger("instance_distance", 8);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        event.accept(ENTRY_ITEM);
        event.accept(EXIT_ITEM);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event){
        MazeManager.load();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event){
        MazeManager.save();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event){
            //
        }

    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class Events {

        @SubscribeEvent
        public static void onPlayerJoin(PlayerLoggedInEvent event){
            if(event.getEntity().level().isClientSide) return;
            MazeManager.PLAYERS.put(event.getEntity().getGameProfile().getId(), new PlayerData());
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerLoggedOutEvent event){
            if(event.getEntity().level().isClientSide) return;
            MazeManager.PLAYERS.remove(event.getEntity().getGameProfile().getId());
            UUID uuid = event.getEntity().getGameProfile().getId();
            if(Parties.PARTYIN.containsKey(uuid)){
                Parties.leave(event.getEntity());
            }
            if(Parties.PARTIES.containsKey(uuid)){
                Parties.disband(event.getEntity(), uuid);
            }
        }

        @SubscribeEvent
        public static void onInteract(PlayerInteractEvent.RightClickBlock event){
            if(event.getSide().isClient() || event.getHand() != InteractionHand.MAIN_HAND || event.getItemStack().getItem() != Items.STICK) return;
            if(!isOp(event.getEntity())) return;
            SelectionCache sel = MazeManager.getPlayerData(event.getEntity()).selcache;
            if(sel.first == null){
                sel.first = event.getPos();
                event.getEntity().sendSystemMessage(Component.literal("First selection point set."));
                event.getEntity().sendSystemMessage(Component.literal(sel.first.toShortString()));
            }
            else{
                if(sel.second != null){
                    event.getEntity().sendSystemMessage(Component.literal("(Use '/mz-desel' to reset selection.)"));
                }
                sel.second = event.getPos();
                event.getEntity().sendSystemMessage(Component.literal("Second selection point set."));
                event.getEntity().sendSystemMessage(Component.literal(sel.second.toShortString()));
            }
        }

        @SubscribeEvent
        public static void onCmdReg(RegisterCommandsEvent event){
            event.getDispatcher().register(literal("mz").requires(con -> isOp(con))
                .then(literal("desel").executes(context -> {
                    SelectionCache cache = MazeManager.getPlayerData(context.getSource().getPlayer()).selcache;
                    cache.first = cache.second = null;
                    context.getSource().sendSystemMessage(Component.literal("Selection reset."));
                    return 0;
                }))
                .then(literal("register")
                    .then(argument("id", StringArgumentType.word())
                    .executes(context -> {
                        try{
                            Player player = context.getSource().getPlayer();
                            if(player.level().dimension().equals(MazesMod.MAZES_LEVEL)){
                                context.getSource().sendFailure(Component.literal("Please change dimension."));
                                return 0;
                            }
                            MazeManager.register(context, false);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                            context.getSource().sendFailure(Component.literal("Error during command execution. Check log for details."));
                        }
                        return 0;
                    })
                ))
                .then(literal("update")
                    .then(argument("id", StringArgumentType.word()).suggests(MAZE_TEMP_SUGGESTER)
                    .executes(context -> {
                        try{
                            Player player = context.getSource().getPlayer();
                            if(player.level().dimension().equals(MazesMod.MAZES_LEVEL)){
                                context.getSource().sendFailure(Component.literal("Please change dimension."));
                                return 0;
                            }
                            MazeManager.register(context, true);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                            context.getSource().sendFailure(Component.literal("Error during command execution. Check log for details."));
                        }
                        return 0;
                    })
                ))
                .then(literal("link")
                    .then(argument("id", StringArgumentType.word()).suggests(MAZE_TEMP_SUGGESTER)
                    .executes(context -> {
                        Maze maze = MazeManager.MAZES.get(context.getArgument("id", String.class));
                        PlayerData data = MazeManager.getPlayerData(context.getSource().getPlayer());
                        if(data.lexit){
                            maze.gate_out = data.last;
                            context.getSource().sendSystemMessage(Component.literal("Maze exit Gate set."));
                            context.getSource().sendSystemMessage(Component.literal(maze.gate_out.toShortString()));
                        }
                        else{
                            maze.gate_in = data.last;
                            context.getSource().sendSystemMessage(Component.literal("Maze entry Gate set."));
                            context.getSource().sendSystemMessage(Component.literal(maze.gate_in.toShortString()));
                        }
                        if(!data.lexit) data.last = null;
                        return 0;
                    })
                ))
                .then(literal("config")
                    .then(argument("id", StringArgumentType.word()).suggests(MAZE_TEMP_SUGGESTER)
                        .then(literal("instances")
                        .then(argument("value", IntegerArgumentType.integer(1, 64))
                        .executes(context -> {
                            Maze maze = MazeManager.MAZES.get(context.getArgument("id", String.class));
                            if(maze != null){
                                maze.instances = context.getArgument("value", Integer.class);
                                context.getSource().sendSystemMessage(Component.literal("Maze max instances updated to " + maze.instances));
                            }
                            else{
                                context.getSource().sendFailure(Component.literal("Maze template not found."));
                            }
                            return 0;
                        })))
                        .then(literal("cooldown")
                        .then(argument("value", IntegerArgumentType.integer(1, 86400))
                        .executes(context -> {
                            Maze maze = MazeManager.MAZES.get(context.getArgument("id", String.class));
                            if(maze != null){
                                maze.cooldown = context.getArgument("value", Integer.class);
                                context.getSource().sendSystemMessage(Component.literal("Maze cooldown (seconds) updated to " + maze.instances));
                            }
                            else{
                                context.getSource().sendFailure(Component.literal("Maze template not found."));
                            }
                            return 0;
                        })))
                        .then(literal("loottable")
                        .then(argument("value", StringArgumentType.word())
                        .executes(context -> {
                            Maze maze = MazeManager.MAZES.get(context.getArgument("id", String.class));
                            if(maze != null){
                                maze.loottable = context.getArgument("value", String.class);
                                context.getSource().sendSystemMessage(Component.literal("Maze loot-table updated to " + maze.loottable));
                            }
                            else{
                                context.getSource().sendFailure(Component.literal("Maze template not found."));
                            }
                            return 0;
                        })))
                        .then(literal("def-loottable")
                        .executes(context -> {
                            Maze maze = MazeManager.MAZES.get(context.getArgument("id", String.class));
                            if(maze != null){
                                maze.loottable = null;
                                context.getSource().sendSystemMessage(Component.literal("Maze loot-table reset to " + maze.id));
                            }
                            else{
                                context.getSource().sendFailure(Component.literal("Maze template not found."));
                            }
                            return 0;
                        }))
                ))
                .then(literal("delete")
                    .then(argument("id", StringArgumentType.word()).suggests(MAZE_TEMP_SUGGESTER)
                    .executes(context -> {
                        String id = context.getArgument("id", String.class);
                        Maze maze = MazeManager.MAZES.remove(id);
                        if(maze == null){
                            context.getSource().sendFailure(Component.literal("Maze template not found."));
                        }
                        else{
                            //TODO remove instances
                            context.getSource().sendSystemMessage(Component.literal("Maze template removed."));
                            try{
                                maze.getStatesFile().delete();
                                maze.getTemplateFile().delete();
                            }
                            catch(Exception e){
                                e.printStackTrace();
                                context.getSource().sendFailure(Component.literal("Error during file deletion, check log for details."));
                            }
                        }
                        return 0;
                    })
                ))
                .then(literal("tpdim").executes(context -> {
                    try{
                        Player player = context.getSource().getPlayer();
                        ServerLevel lvl = context.getSource().getServer().getLevel(player.level().dimension().equals(MazesMod.MAZES_LEVEL) ? OVERWORLD : MAZES_LEVEL);
                        context.getSource().getPlayer().changeDimension(lvl, new ITeleporter(){});
                    }
                    catch(Throwable e){
                        e.printStackTrace();
                    }
                    return 0;
                }))
                .then(literal("tp")
                    .then(argument("id", StringArgumentType.word()).suggests(MAZE_TEMP_SUGGESTER)
                    .executes(context -> {
                        String id = context.getArgument("id", String.class);
                        Maze maze = MazeManager.MAZES.get(id);
                        if(maze == null){
                            context.getSource().sendFailure(Component.literal("Maze template not found."));
                        }
                        else{
                            try{
                                Player player = context.getSource().getPlayer();
                                if(!player.level().dimension().equals(OVERWORLD)){
                                    ServerLevel lvl = context.getSource().getServer().getLevel(OVERWORLD);
                                    context.getSource().getPlayer().changeDimension(lvl, new Teleporter(maze.orgpos.getCenter()));
                                }
                                else{
                                    player.moveTo(maze.orgpos.getCenter());
                                }
                            }
                            catch(Throwable e){
                                e.printStackTrace();
                            }
                        }
                        return 0;
                    })
                ))
                .then(literal("templates").executes(context -> {
                    if(MazeManager.MAZES.isEmpty()){
                        context.getSource().sendFailure(Component.literal("No maze templates on this server."));
                        context.getSource().sendFailure(Component.literal("Use '/mz register <id>' register one!'"));
                        return 0;
                    }
                    context.getSource().sendSystemMessage(Component.literal("Maze Templates:"));
                    for(Map.Entry<String, Maze> entry : MazeManager.MAZES.entrySet()){
                        Maze temp = entry.getValue();
                        context.getSource().sendSystemMessage(Component.literal("ID: " + entry.getKey()));
                        context.getSource().sendSystemMessage(Component.literal("R-Size: " + temp.rawsize.toShortString()));
                        context.getSource().sendSystemMessage(Component.literal("C-Size: " + temp.size.toShortString()));
                        context.getSource().sendSystemMessage(Component.literal("Chests: " + temp.chests.size()));
                        context.getSource().sendSystemMessage(Component.literal("Entry: " + (temp.gate_in == null ? "not set" : temp.gate_in.toShortString())));
                        context.getSource().sendSystemMessage(Component.literal("Exit: " + (temp.gate_out == null ? "not set" : temp.gate_out.toShortString())));
                        context.getSource().sendSystemMessage(Component.literal("Max-Inst: " + temp.instances));
                        context.getSource().sendSystemMessage(Component.literal("Cooldown: " + temp.cooldown + "s"));
                    }
                    return 0;
                }))
                .then(literal("inst")
                    .then(literal("list").executes(context -> {
                        if(MazeManager.INSTANCES.isEmpty()){
                            context.getSource().sendFailure(Component.literal("No maze instances on this server."));
                            return 0;
                        }
                        context.getSource().sendSystemMessage(Component.literal("Maze Instances:"));
                        for(MazeInst inst : MazeManager.INSTANCES.values()){
                            context.getSource().sendSystemMessage(Component.literal("---- ----- -----"));
                            context.getSource().sendSystemMessage(Component.literal("UUID: " + inst.uuid));
                            context.getSource().sendSystemMessage(Component.literal("Root: " + inst.root.id));
                            context.getSource().sendSystemMessage(Component.literal("Start: " + inst.start.x + ", " + inst.start.z));
                            context.getSource().sendSystemMessage(Component.literal("End: " + inst.end.x + ", " + inst.end.z));
                            context.getSource().sendSystemMessage(Component.literal("Players (inside): " + inst.players.size()));
                        }
                        return 0;
                    }))
                    .then(literal("delete")
                        .then(argument("idx", StringArgumentType.word()).suggests(MAZE_INST_SUGGESTER)
                        .executes(context -> {
                            UUID idx = UUID.fromString(context.getArgument("idx", String.class));
                            if(!MazeManager.INSTANCES.containsKey(idx)){
                                context.getSource().sendFailure(Component.literal("Invalid UUID specified."));
                                return 0;
                            }
                            MazeInst inst = MazeManager.INSTANCES.get(idx);
                            if(inst.players.size() > 0){
                                context.getSource().sendSystemMessage(Component.literal("There are still players in the maze."));
                                context.getSource().sendSystemMessage(Component.literal("You can stop it using '/mz inst pause " + idx + "'"));
                                return 0;
                            }
                            MazeManager.INSTANCES.remove(idx);
                            inst.getFile().delete();
                            context.getSource().sendSystemMessage(Component.literal("Instance with index '" + idx + "' removed."));
                            return 0;
                        })
                    ))
                    .then(literal("pause")
                        .then(argument("idx", StringArgumentType.word()).suggests(MAZE_INST_SUGGESTER)
                        .executes(context -> {
                            UUID idx = UUID.fromString(context.getArgument("idx", String.class));
                            MazeInst inst = MazeManager.INSTANCES.get(idx);
                            if(inst == null){
                                context.getSource().sendFailure(Component.literal("Instance not found."));
                                return 0;
                            }
                            for(Player player : inst.players){
                                PlayerData data = MazeManager.getPlayerData(player);
                                data.teleport(player);
                                player.sendSystemMessage(Component.literal("Instance closed down for maintenance."));
                            }
                            inst.players.clear();
                            inst.paused = true;
                            context.getSource().sendSystemMessage(Component.literal("Instance paused. Note it resumes automatically on server restart."));
                            return 0;
                        })
                    ))
                    .then(literal("resume")
                        .then(argument("idx", StringArgumentType.word()).suggests(MAZE_INST_SUGGESTER)
                        .executes(context -> {
                            UUID idx = UUID.fromString(context.getArgument("idx", String.class));
                            MazeInst inst = MazeManager.INSTANCES.get(idx);
                            if(inst == null){
                                context.getSource().sendFailure(Component.literal("Instance not found."));
                                return 0;
                            }
                            inst.paused = false;
                            context.getSource().sendSystemMessage(Component.literal("Instance is back in service."));
                            return 0;
                        })
                    ))
                )
            );
            event.getDispatcher().register(literal("mz-party").requires(con -> con.getPlayer() != null)
                .then(literal("join").then(argument("code", StringArgumentType.word()).executes(context -> {
                    try{
                        Parties.PartyLead in = Parties.PARTYIN.get(context.getSource().getPlayer().getGameProfile().getId());
                        if(in != null){
                            Parties.leave(context.getSource().getPlayer());
                        }
                        var opart = Parties.PARTIES.get(context.getSource().getPlayer().getGameProfile().getId());
                        if(opart != null && opart.size() > 0){
                            context.getSource().sendSystemMessage(Component.literal("Please disband your own party first!"));
                            return 0;
                        }
                        String code = context.getArgument("code", String.class);
                        UUID party = Parties.get(code);
                        if(party == null){
                            context.getSource().sendSystemMessage(Component.literal("Party not found. Is the code correct?"));
                        }
                        else{
                            Parties.join(context.getSource().getServer(), context.getSource().getPlayer(), party);
                            context.getSource().sendSystemMessage(Component.literal("Ýou joined the party."));
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    return 0;
                })))
                .then(literal("newcode").executes(context -> {
                    Parties.putNewCode(context.getSource().getPlayer().getGameProfile().getId());
                    String code = Parties.CODES.get(context.getSource().getPlayer().getGameProfile().getId());
                    context.getSource().sendSystemMessage(Component.literal("Your New Invite Code: " + code));
                    return 0;
                }))
                .then(literal("disband").executes(context -> {
                    ArrayList<UUID> party = Parties.PARTIES.get(context.getSource().getPlayer().getGameProfile().getId());
                    if(party == null || party.size() == 0){
                        context.getSource().sendSystemMessage(Component.literal("No party members to disband."));
                    }
                    else{
                        Parties.disband(context.getSource().getPlayer(), context.getSource().getPlayer().getGameProfile().getId());
                        context.getSource().sendSystemMessage(Component.literal("Party disbanded. Invite code removed."));
                    }
                    return 0;
                }))
                .then(literal("leave").executes(context -> {
                    Parties.PartyLead in = Parties.PARTYIN.get(context.getSource().getPlayer().getGameProfile().getId());
                    if(in == null){
                        context.getSource().sendSystemMessage(Component.literal("You are not in a party!"));
                    }
                    else{
                        Parties.leave(context.getSource().getPlayer());
                    }
                    return 0;
                }))
                .then(literal("status").executes(context -> {
                    UUID uuid = context.getSource().getPlayer().getGameProfile().getId();
                    ArrayList<UUID> party = Parties.PARTIES.get(uuid);
                    PlayerData data = MazeManager.getPlayerData(context.getSource().getPlayer());
                    String code = Parties.CODES.get(uuid);
                    if(code == null && !data.informed){
                        context.getSource().sendSystemMessage(Component.literal("You do not have a party invite code."));
                        context.getSource().sendSystemMessage(Component.literal("Use '/mz-party newcode' to generate a new code."));
                        data.informed = true;
                    }
                    if(code != null) context.getSource().sendSystemMessage(Component.literal("Your Invite Code: " + code));
                    if(party == null){
                        Parties.PartyLead in = Parties.PARTYIN.get(uuid);
                        if(in == null){
                            context.getSource().sendSystemMessage(Component.literal("No Party Members."));
                        }
                        else{
                            context.getSource().sendSystemMessage(Component.literal("Currently in " + in.name() + "'s Party."));
                        }
                    }
                    else{
                        context.getSource().sendSystemMessage(Component.literal("Party Members: " + party.size()));
                        context.getSource().sendSystemMessage(Component.literal("Use '/mz-party disband' to disband."));
                        for(UUID player : party){
                            Player ply = context.getSource().getServer().getPlayerList().getPlayer(player);
                            context.getSource().sendSystemMessage(Component.literal("- " + (ply == null ? player : ply.getGameProfile().getName())));
                        }
                    }
                    return 0;
                })));
        }

        private static boolean isOp(CommandSourceStack con){
            if(con.getPlayer() == null) return false;
            if(con.getServer().isSingleplayer()) return true;
            return con.getServer().getPlayerList().isOp(con.getPlayer().getGameProfile());
        }

        private static boolean isOp(Player player){
            if(player == null) return false;
            if(ServerLifecycleHooks.getCurrentServer().isSingleplayer()) return true;
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().isOp(player.getGameProfile());
        }

        public static SuggestionProvider<CommandSourceStack> MAZE_TEMP_SUGGESTER = new SuggestionProvider<>() {
            @Override
            public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException{
                for(String id : MazeManager.MAZES.keySet()) builder.suggest(id);
                return builder.buildFuture();
            }
        };

        public static SuggestionProvider<CommandSourceStack> MAZE_INST_SUGGESTER = new SuggestionProvider<>() {
            @Override
            public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException{
                for(UUID uuid : MazeManager.INSTANCES.keySet()) builder.suggest(uuid.toString());
                return builder.buildFuture();
            }
        };

    }

}
