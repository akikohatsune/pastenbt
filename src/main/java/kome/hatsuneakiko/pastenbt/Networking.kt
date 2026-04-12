package kome.hatsuneakiko.pastenbt

import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel

object Networking {
    private const val PROTOCOL_VERSION = "1"
    
    val INSTANCE: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(PasteNbtMod.MODID, "main"),
        { PROTOCOL_VERSION },
        { version -> version == PROTOCOL_VERSION || version == NetworkRegistry.ABSENT },
        { version -> version == PROTOCOL_VERSION || version == NetworkRegistry.ABSENT }
    )

    fun register() {
        var id = 0
        INSTANCE.registerMessage(id++, ServerboundPastePacket::class.java, ServerboundPastePacket::encode, ServerboundPastePacket::decode, ServerboundPastePacket::handle)
        INSTANCE.registerMessage(id++, ServerboundRequestPreviewPacket::class.java, ServerboundRequestPreviewPacket::encode, ServerboundRequestPreviewPacket::decode, ServerboundRequestPreviewPacket::handle)
        INSTANCE.registerMessage(id++, ClientboundPreviewDataPacket::class.java, ClientboundPreviewDataPacket::encode, ClientboundPreviewDataPacket::decode, ClientboundPreviewDataPacket::handle)
        INSTANCE.registerMessage(id++, ServerboundGridPastePacket::class.java, ServerboundGridPastePacket::encode, ServerboundGridPastePacket::decode, ServerboundGridPastePacket::handle)
    }
}
