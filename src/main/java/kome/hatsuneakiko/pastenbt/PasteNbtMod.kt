package kome.hatsuneakiko.pastenbt

import net.minecraft.client.KeyMapping
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(PasteNbtMod.MODID)
object PasteNbtMod {
    const val MODID = "pastenbt"
    val LOGGER: Logger = LogManager.getLogger()

    val ROTATE_KEY = KeyMapping("key.pastenbt.rotate", GLFW.GLFW_KEY_R, "key.categories.pastenbt")
    val CONFIRM_KEY = KeyMapping("key.pastenbt.confirm", GLFW.GLFW_KEY_P, "key.categories.pastenbt")
    val CANCEL_KEY = KeyMapping("key.pastenbt.cancel", GLFW.GLFW_KEY_C, "key.categories.pastenbt")

    init {
        MOD_BUS.addListener(::setup)
        MOD_BUS.addListener(::registerKeys)
        FORGE_BUS.addListener(::onRegisterCommands)
        FORGE_BUS.addListener(::onRegisterClientCommands)
        FORGE_BUS.addListener(::onServerAboutToStart)
        FORGE_BUS.addListener(::onServerTick)

        // Làm cho mod trở thành tùy chọn phía Client
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fml.IExtensionPoint.DisplayTest::class.java) {
            net.minecraftforge.fml.IExtensionPoint.DisplayTest(
                { net.minecraftforge.network.NetworkConstants.IGNORESERVERONLY },
                { _, _ -> true }
            )
        }
    }

    private fun setup(event: FMLCommonSetupEvent) {
        event.enqueueWork(Networking::register)
    }

    private fun registerKeys(event: RegisterKeyMappingsEvent) {
        event.register(ROTATE_KEY)
        event.register(CONFIRM_KEY)
        event.register(CANCEL_KEY)
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        NbtPasterCommand.register(event.dispatcher)
        RtpCommand.register(event.dispatcher)
        PasteCommand.register(event.dispatcher)
    }

    private fun onRegisterClientCommands(event: RegisterClientCommandsEvent) {
        PasteCommand.register(event.dispatcher)
    }

    private fun onServerAboutToStart(event: ServerAboutToStartEvent) {
        ModConfig.load()
    }

    private fun onServerTick(_event: TickEvent.ServerTickEvent) {
        if (_event.phase == TickEvent.Phase.END) {
            PlacementManager.processTick()
        }
    }
}
