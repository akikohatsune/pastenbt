package kome.hatsuneakiko.pastenbt;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Networking {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(PasteNbtMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        version -> true,
        version -> true
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, ServerboundPastePacket.class, ServerboundPastePacket::encode, ServerboundPastePacket::decode, ServerboundPastePacket::handle);
        INSTANCE.registerMessage(id++, ClientboundPreviewDataPacket.class, ClientboundPreviewDataPacket::encode, ClientboundPreviewDataPacket::decode, ClientboundPreviewDataPacket::handle);
    }
}
