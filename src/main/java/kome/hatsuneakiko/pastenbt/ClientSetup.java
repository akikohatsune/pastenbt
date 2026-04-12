package kome.hatsuneakiko.pastenbt;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public class ClientSetup {
    public static final KeyMapping ROTATE_KEY = new KeyMapping("key.pastenbt.rotate", GLFW.GLFW_KEY_R, "key.categories.pastenbt");
    public static final KeyMapping CONFIRM_KEY = new KeyMapping("key.pastenbt.confirm", GLFW.GLFW_KEY_P, "key.categories.pastenbt");
    public static final KeyMapping CANCEL_KEY = new KeyMapping("key.pastenbt.cancel", GLFW.GLFW_KEY_C, "key.categories.pastenbt");

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onRegisterClientCommands);
    }

    private static void registerKeys(final RegisterKeyMappingsEvent event) {
        event.register(ROTATE_KEY);
        event.register(CONFIRM_KEY);
        event.register(CANCEL_KEY);
    }

    private static void onRegisterClientCommands(final RegisterClientCommandsEvent event) {
        PasteCommand.register(event.getDispatcher());
    }
}
