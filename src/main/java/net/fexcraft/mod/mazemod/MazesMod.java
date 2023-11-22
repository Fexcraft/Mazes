package net.fexcraft.mod.mazemod;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.core.registries.Registries.DIMENSION;
import static net.minecraft.world.level.Level.OVERWORLD;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fexcraft.app.json.JsonHandler;
import net.fexcraft.app.json.JsonHandler.PrintOption;
import net.fexcraft.app.json.JsonMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
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

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

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
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
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
            //
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerLoggedOutEvent event){
            if(event.getEntity().level().isClientSide) return;
            //
        }

        @SubscribeEvent
        public static void onCmdReg(RegisterCommandsEvent event){
            event.getDispatcher().register(literal("mz-reg").requires(con -> isOp(con))
                .then(argument("id", StringArgumentType.word())
                    .then(argument("start", Vec3Argument.vec3())
                        .then(argument("end", Vec3Argument.vec3())
                .executes(context ->{
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
                }
            )))));
            event.getDispatcher().register(literal("mz-upd").requires(con -> isOp(con))
                .then(argument("id", StringArgumentType.word())
                    .then(argument("start", Vec3Argument.vec3())
                        .then(argument("end", Vec3Argument.vec3())
                .executes(context ->{
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
                }
            )))));
            event.getDispatcher().register(literal("mz-del").requires(con -> isOp(con))
                .then(argument("id", StringArgumentType.word())
                .executes(context -> {
                    String id = context.getArgument("id", String.class);
                    Maze maze = MazeManager.MAZES.remove(id);
                    if(maze == null){
                        context.getSource().sendFailure(Component.literal("Maze template not found."));
                    }
                    else{
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
                }
            )));
            event.getDispatcher().register(literal("mz-tpdim").requires(con -> isOp(con)).executes(context ->{
                try{
                    Player player = context.getSource().getPlayer();
                    ServerLevel lvl = context.getSource().getServer().getLevel(player.level().dimension().equals(MazesMod.MAZES_LEVEL) ? OVERWORLD : MAZES_LEVEL);
                    context.getSource().getPlayer().changeDimension(lvl, new ITeleporter() {
                        @Override
                        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                            return ITeleporter.super.placeEntity(entity, currentWorld, destWorld, yaw, repositionEntity);
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
                return 0;
            }));
            event.getDispatcher().register(literal("mz-party").requires(con -> con.getPlayer() != null)
                .then(literal("join").then(argument("code", StringArgumentType.word()).executes(context -> {
                    try{
                        Player in = Parties.PARTYIN.get(context.getSource().getPlayer().getGameProfile().getId());
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
                    ArrayList<Player> party = Parties.PARTIES.get(context.getSource().getPlayer().getGameProfile().getId());
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
                    Player in = Parties.PARTYIN.get(context.getSource().getPlayer().getGameProfile().getId());
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
                    ArrayList<Player> party = Parties.PARTIES.get(uuid);
                    String code = Parties.CODES.get(uuid);
                    if(code == null){
                        context.getSource().sendSystemMessage(Component.literal("You do not have a party invite code."));
                        context.getSource().sendSystemMessage(Component.literal("Use '/mz-party newcode' to generate a new code."));
                        return 0;
                    }
                    context.getSource().sendSystemMessage(Component.literal("Your Invite Code: " + code));
                    if(party == null){
                        Player in = Parties.PARTYIN.get(uuid);
                        if(in == null){
                            context.getSource().sendSystemMessage(Component.literal("No Party Members."));
                        }
                        else{
                            context.getSource().sendSystemMessage(Component.literal("Currently in " + in.getDisplayName() + "'s Party."));
                        }
                    }
                    else{
                        context.getSource().sendSystemMessage(Component.literal("Party Members: " + party.size()));
                        context.getSource().sendSystemMessage(Component.literal("Use '/mz-party disband' to disband."));
                        for(Player player : party){
                            context.getSource().sendSystemMessage(Component.literal("- " + player.getDisplayName()));
                        }
                    }
                    return 0;
                })));

            event.getDispatcher().register(literal("mz-inst").requires(con -> isOp(con))
                .then(literal("create").then(argument("template", StringArgumentType.word()).executes(context -> {
                    MazeManager.create(context, context.getArgument("template", String.class));
                    return 0;
                })))
                .then(literal("list").executes(context -> {
                    if(MazeManager.INSTANCES.isEmpty()){
                        context.getSource().sendFailure(Component.literal("No maze instances on this server."));
                        context.getSource().sendFailure(Component.literal("Use '/mz-inst create <template-id> to create one!'"));
                        return 0;
                    }
                    context.getSource().sendSystemMessage(Component.literal("Maze Instances:"));
                    int idx = 0;
                    for(MazeInst inst : MazeManager.INSTANCES){
                        context.getSource().sendSystemMessage(Component.literal("Index: " + (idx++) + "Root: " + inst.root.id));
                        context.getSource().sendSystemMessage(Component.literal("Start: " + inst.start.x + ", " + inst.start.z));
                        context.getSource().sendSystemMessage(Component.literal("End: " + inst.end.x + ", " + inst.end.z));
                        context.getSource().sendSystemMessage(Component.literal("Players (inside): " + inst.players.size()));
                    }
                    return 0;
                }))
                .then(literal("templates").executes(context -> {
                    if(MazeManager.MAZES.isEmpty()){
                        context.getSource().sendFailure(Component.literal("No maze templates on this server."));
                        context.getSource().sendFailure(Component.literal("Use '/mz-reg' register one!'"));
                        return 0;
                    }
                    context.getSource().sendSystemMessage(Component.literal("Maze Templates:"));
                    for(Map.Entry<String, Maze> entry : MazeManager.MAZES.entrySet()){
                        context.getSource().sendSystemMessage(Component.literal("ID: " + entry.getKey()));
                        context.getSource().sendSystemMessage(Component.literal("R-Size: " + entry.getValue().rawsize.toShortString()));
                        context.getSource().sendSystemMessage(Component.literal("C-Size: " + entry.getValue().size.toShortString()));
                        context.getSource().sendSystemMessage(Component.literal("Chests: " + entry.getValue().chests.size()));
                    }
                    return 0;
                }))
                .then(literal("delete").then(argument("idx", IntegerArgumentType.integer(0, MazeManager.INSTANCES.size())).executes(context -> {
                    int idx = context.getArgument("idx", Integer.class);
                    MazeInst inst = MazeManager.INSTANCES.get(idx);
                    if(inst.players.size() > 0){
                        context.getSource().sendSystemMessage(Component.literal("There are still players in the maze."));
                        context.getSource().sendSystemMessage(Component.literal("You can stop it using '/mz-inst pause " + idx + "'"));
                    }
                    //
                    return 0;
                })))
                .then(literal("pause").then(argument("idx", IntegerArgumentType.integer(0, MazeManager.INSTANCES.size())).executes(context -> {
                    int idx = context.getArgument("idx", Integer.class);
                    MazeInst inst = MazeManager.INSTANCES.get(idx);
                    //
                    return 0;
                })))
                .then(literal("resume").then(argument("idx", IntegerArgumentType.integer(0, MazeManager.INSTANCES.size())).executes(context -> {
                    int idx = context.getArgument("idx", Integer.class);
                    MazeInst inst = MazeManager.INSTANCES.get(idx);
                    //
                    return 0;
                })))
            );
        }

        private static boolean isOp(CommandSourceStack con){
            if(con.getPlayer() == null) return false;
            if(con.getServer().isSingleplayer()) return true;
            return con.getServer().getPlayerList().isOp(con.getPlayer().getGameProfile());
        }

    }

}
