package kome.hatsuneakiko.pastenbt

import net.minecraft.core.BlockPos

enum class SnapMode {
    CORNER, CENTER;

    fun snap(pos: BlockPos): BlockPos {
        val cx = pos.x shr 4
        val cz = pos.z shr 4
        var ox = cx shl 4
        var oz = cz shl 4
        if (this == CENTER) {
            ox += 8
            oz += 8
        }
        return BlockPos(ox, pos.y, oz)
    }
}
