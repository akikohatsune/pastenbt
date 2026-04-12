package kome.hatsuneakiko.pastenbt;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(PasteNbtMod.MODID)
public class PasteNbtMod {
    public static final String MODID = "pastenbt";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final KeyMapping ROTATE_KEY = new KeyMapping("key.pastenbt.rotate", GLFW.GLFW_KEY_R, "key.categories.pastenbt");
    public static final KeyMapping CONFIRM_KEY = new KeyMapping("key.pastenbt.confirm", GLFW.GLFW_KEY_P, "key.categories.pastenbt");
    public static final KeyMapping CANCEL_KEY = new KeyMapping("key.pastenbt.cancel", GLFW.GLFW_KEY_C, "key.categories.pastenbt");

    public PasteNbtMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);

        // Make the mod server-side optional
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fml.IExtensionPoint.DisplayTest.class, () ->
            new net.minecraftforge.fml.IExtensionPoint.DisplayTest(
                () -> net.minecraftforge.network.NetworkConstants.IGNORESERVERONLY,
                (a, b) -> true
            )
        );
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Networking::register);
    }

    private void registerKeys(final RegisterKeyMappingsEvent event) {
        event.register(ROTATE_KEY);
        event.register(CONFIRM_KEY);
        event.register(CANCEL_KEY);
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        NbtPasterCommand.register(event.getDispatcher());
        RtpCommand.register(event.getDispatcher());
        PasteCommand.register(event.getDispatcher());
    }

    private void onRegisterClientCommands(final RegisterClientCommandsEvent event) {
        PasteCommand.register(event.getDispatcher());
    }

    private void onServerAboutToStart(final ServerAboutToStartEvent event) {
        ModConfig.load();
    }

    private void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlacementManager.processTick();
        }
    }
}
