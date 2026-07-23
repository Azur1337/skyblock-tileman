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
import net.minecraft.world.phys.Vec3;

// Fills the top face of unlocked blocks near the player with green overlay
public final class TilemanRenderer {

    // slightly above block top so it does not z fight with the terrain
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
                    submitTopFaceQuad(buffer, pose, pos);
                }
            }
        );
        poseStack.popPose();
    }

    private static void submitTopFaceQuad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        BlockPos pos
    ) {
        float x0 = pos.getX();
        float y = pos.getY() + TOP_FACE_Y_OFFSET;
        float z0 = pos.getZ();

        buffer.addVertex(pose, x0, y, z0).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0 + 1, y, z0).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0 + 1, y, z0 + 1).setColor(UNLOCKED_COLOR);
        buffer.addVertex(pose, x0, y, z0 + 1).setColor(UNLOCKED_COLOR);
    }
}
