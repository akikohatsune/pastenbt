package kome.hatsuneakiko.pastenbt;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PasteNbtMod.MODID)
public class PasteNbtMod {
    public static final String MODID = "pastenbt";
    public static final Logger LOGGER = LogManager.getLogger();

    public PasteNbtMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        // Chỉ đăng ký KeyMappings và Client Commands nếu đang chạy trên Client
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.init();
        }

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
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

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        NbtPasterCommand.register(event.getDispatcher());
        RtpCommand.register(event.getDispatcher());
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
