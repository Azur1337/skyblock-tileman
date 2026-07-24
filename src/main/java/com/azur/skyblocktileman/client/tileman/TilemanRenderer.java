package com.azur.skyblocktileman.client.tileman;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

// green overlay on unlocked blocks
public final class TilemanRenderer {

    // above block top to avoid z fighting
    private static final float TOP_FACE_Y_OFFSET = 1.002F;

    private static final int UNLOCKED_COLOR = ARGB.color(100, 0, 255, 60);

    private TilemanRenderer() {}

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(
            TilemanRenderer::onBeforeGizmos
        );
    }

    private static void onBeforeGizmos(LevelRenderContext context) {
        TilemanConfig config = TilemanConfig.getInstance();
        if (!config.isEnabled() || !config.isShowUnlockedOverlay()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Set<BlockCoord> unlocked =
            TilemanState.getInstance().getUnlockedBlocks();
        if (unlocked.isEmpty()) {
            return;
        }

        int radius = config.getHighlightRadius();
        double radiusSq = (double) radius * radius;

        BlockPos playerPos = client.player.blockPosition();
        List<BlockPos> nearby = new ArrayList<>();
        for (BlockCoord coord : unlocked) {
            BlockPos pos = coord.toBlockPos();
            if (playerPos.distSqr(pos) <= radiusSq) {
                nearby.add(pos);
            }
        }
        if (nearby.isEmpty()) {
            return;
        }

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.debugFilledBox(),
            (pose, buffer) -> {
                for (BlockPos pos : nearby) {
                    submitTopFaceQuad(buffer, pose, pos, client.level);
                }
            }
        );
        poseStack.popPose();
    }

    private static void submitTopFaceQuad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        BlockPos pos,
        Level level
    ) {
        float x0 = pos.getX();

        BlockPos above = pos.above();
        BlockState stateAbove = level.getBlockState(above);
        float yOffset = getOverlayOffset(stateAbove, level, above);

        float y = pos.getY() + yOffset;
        float z0 = pos.getZ();

        buffer.addVertex(pose, x0, y, z0).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0 + 1, y, z0).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0 + 1, y, z0 + 1).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0, y, z0 + 1).setColor(UNLOCKED_COLOR);
    }

    private static float getOverlayOffset(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) {
            return TOP_FACE_Y_OFFSET;
        }

        var shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            return TOP_FACE_Y_OFFSET;
        }

        double maxY = shape.max(net.minecraft.core.Direction.Axis.Y);
        return 1.0F + (float) maxY + 0.002F;
    }
}
