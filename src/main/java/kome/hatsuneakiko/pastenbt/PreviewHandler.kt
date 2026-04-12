package kome.hatsuneakiko.pastenbt

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = PasteNbtMod.MODID, value = [Dist.CLIENT])
object PreviewHandler {
    private var currentName: String? = null
    private var template: StructureTemplate? = null
    private var currentRotation = Rotation.NONE
    private var isPreviewing = false
    private var currentSnapMode = SnapMode.CORNER

    fun startPreview(name: String, nbt: CompoundTag, snapMode: SnapMode) {
        currentName = name
        template = StructureTemplate().apply {
            load(Minecraft.getInstance().level!!.holderLookup(Registries.BLOCK), nbt)
        }
        isPreviewing = true
        currentRotation = Rotation.NONE
        currentSnapMode = snapMode
    }

    private fun rotate() {
        if (!isPreviewing) return
        currentRotation = currentRotation.getRotated(Rotation.CLOCKWISE_90)
    }

    private fun confirm() {
        if (!isPreviewing) return
        currentName?.let { name ->
            Networking.INSTANCE.sendToServer(ServerboundPastePacket(name, currentRotation, currentSnapMode))
        }
        isPreviewing = false
    }

    private fun cancel() {
        isPreviewing = false
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevel(event: RenderLevelStageEvent) {
        if (!isPreviewing || template == null || event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return

        val mc = Minecraft.getInstance()
        val pos = currentSnapMode.snap(mc.player!!.blockPosition())
        val poseStack = event.poseStack
        val buffer = mc.renderBuffers().bufferSource()

        poseStack.pushPose()
        val camera = mc.entityRenderDispatcher.camera.position
        poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)

        val settings = StructurePlaceSettings().setRotation(currentRotation)
        val blocks = template!!.filterBlocks(BlockPos.ZERO, settings, null)

        for (info in blocks) {
            poseStack.pushPose()
            poseStack.translate(info.pos.x.toDouble(), info.pos.y.toDouble(), info.pos.z.toDouble())
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 0.5f)
            poseStack.popPose()
        }

        poseStack.popPose()
    }

    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || !isPreviewing) return
        
        while (PasteNbtMod.ROTATE_KEY.consumeClick()) rotate()
        while (PasteNbtMod.CONFIRM_KEY.consumeClick()) confirm()
        while (PasteNbtMod.CANCEL_KEY.consumeClick()) cancel()
    }
}
