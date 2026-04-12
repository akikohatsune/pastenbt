package kome.hatsuneakiko.pastenbt;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@Mod.EventBusSubscriber(modid = PasteNbtMod.MODID, value = Dist.CLIENT)
public class PreviewHandler {
    private static String currentName;
    private static StructureTemplate template;
    private static Rotation currentRotation = Rotation.NONE;
    private static boolean isPreviewing = false;
    private static SnapMode currentSnapMode = SnapMode.CORNER;

    public static void startPreview(String name, CompoundTag nbt, SnapMode snapMode) {
        currentName = name;
        template = new StructureTemplate();
        if (Minecraft.getInstance().level != null) {
            template.load(Minecraft.getInstance().level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
        }
        isPreviewing = true;
        currentRotation = Rotation.NONE;
        currentSnapMode = snapMode;
    }

    private static void rotate() {
        if (!isPreviewing) return;
        currentRotation = currentRotation.getRotated(Rotation.CLOCKWISE_90);
    }

    private static void confirm() {
        if (!isPreviewing) return;
        if (currentName != null) {
            Networking.INSTANCE.sendToServer(new ServerboundPastePacket(currentName, currentRotation, currentSnapMode));
        }
        isPreviewing = false;
    }

    private static void cancel() {
        isPreviewing = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!isPreviewing || template == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        BlockPos pos = currentSnapMode.snap(mc.player.blockPosition());
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        Vec3 camera = mc.getEntityRenderDispatcher().camera.getPosition();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(currentRotation);
        List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(BlockPos.ZERO, settings, null);

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            poseStack.pushPose();
            poseStack.translate(info.pos().getX(), info.pos().getY(), info.pos().getZ());
            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0f, 1.0f, 1.0f, 0.5f);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isPreviewing) return;
        
        while (ClientSetup.ROTATE_KEY.consumeClick()) rotate();
        while (ClientSetup.CONFIRM_KEY.consumeClick()) confirm();
        while (ClientSetup.CANCEL_KEY.consumeClick()) cancel();
    }
}
