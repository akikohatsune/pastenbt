package kome.hatsuneakiko.pastenbt;

import net.minecraft.core.BlockPos;

public enum SnapMode {
    CORNER, CENTER;

    public BlockPos snap(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int ox = cx << 4;
        int oz = cz << 4;
        if (this == CENTER) {
            ox += 8;
            oz += 8;
        }
        return new BlockPos(ox, pos.getY(), oz);
    }
}
