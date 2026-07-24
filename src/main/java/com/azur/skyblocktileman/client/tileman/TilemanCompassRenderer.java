package com.azur.skyblocktileman.client.tileman;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public final class TilemanCompassRenderer {

    private static final int BEACON_COLOR = ARGB.color(200, 50, 255, 50);
    private static final int OUTLINE_COLOR = ARGB.color(255, 50, 255, 50);
    private static final float BEACON_WIDTH = 0.3f;
    private static final float BEACON_HEIGHT = 256f;

    private TilemanCompassRenderer() {}

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(TilemanCompassRenderer::onRender);
    }

    private static void onRender(LevelRenderContext context) {
        TilemanConfig config = TilemanConfig.getInstance();
        if (!config.isEnabled() || !config.isTileCompassEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        if (BlockValidation.isPlayerOnUnlockedBlock()) {
            return;
        }

        if (TilemanFirstBlockMode.isActive()) {
            return;
        }

        BlockPos nearest = findNearestUnlockedTile(client);
        if (nearest == null) {
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
                renderBeacon(buffer, pose, nearest);
                renderOutline(buffer, pose, nearest);
            }
        );

        poseStack.popPose();
    }

    private static BlockPos findNearestUnlockedTile(Minecraft client) {
        Set<BlockCoord> unlocked = TilemanState.getInstance().getUnlockedBlocks();
        if (unlocked.isEmpty()) {
            return null;
        }

        BlockPos playerPos = client.player.blockPosition();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockCoord coord : unlocked) {
            BlockPos pos = coord.toBlockPos();
            double dist = playerPos.distSqr(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
        }

        return nearest;
    }

    private static void renderBeacon(VertexConsumer buffer, PoseStack.Pose pose, BlockPos pos) {
        float x = pos.getX() + 0.5f;
        float z = pos.getZ() + 0.5f;
        float y0 = pos.getY() + 1.0f;
        float y1 = y0 + BEACON_HEIGHT;

        float hw = BEACON_WIDTH / 2;

        // north face
        buffer.addVertex(pose, x - hw, y0, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y0, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y1, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y1, z - hw).setColor(BEACON_COLOR);

        // south face
        buffer.addVertex(pose, x + hw, y0, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y0, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y1, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y1, z + hw).setColor(BEACON_COLOR);

        // east face
        buffer.addVertex(pose, x + hw, y0, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y0, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y1, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x + hw, y1, z - hw).setColor(BEACON_COLOR);

        // west face
        buffer.addVertex(pose, x - hw, y0, z + hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y0, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y1, z - hw).setColor(BEACON_COLOR);
        buffer.addVertex(pose, x - hw, y1, z + hw).setColor(BEACON_COLOR);
    }

    private static void renderOutline(VertexConsumer buffer, PoseStack.Pose pose, BlockPos pos) {
        float x0 = pos.getX();
        float y0 = pos.getY();
        float z0 = pos.getZ();
        float x1 = x0 + 1;
        float y1 = y0 + 1;
        float z1 = z0 + 1;

        float t = 0.02f;

        // bottom face outline
        renderLineQuad(buffer, pose, x0, y0 - t, z0, x1, y0 + t, z0 + t, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x0, y0 - t, z1 - t, x1, y0 + t, z1, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x0, y0 - t, z0, x0 + t, y0 + t, z1, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x1 - t, y0 - t, z0, x1, y0 + t, z1, OUTLINE_COLOR);

        // top face outline
        renderLineQuad(buffer, pose, x0, y1 - t, z0, x1, y1 + t, z0 + t, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x0, y1 - t, z1 - t, x1, y1 + t, z1, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x0, y1 - t, z0, x0 + t, y1 + t, z1, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x1 - t, y1 - t, z0, x1, y1 + t, z1, OUTLINE_COLOR);

        // vertical edges
        renderLineQuad(buffer, pose, x0, y0, z0, x0 + t, y1, z0 + t, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x1 - t, y0, z0, x1, y1, z0 + t, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x0, y0, z1 - t, x0 + t, y1, z1, OUTLINE_COLOR);
        renderLineQuad(buffer, pose, x1 - t, y0, z1 - t, x1, y1, z1, OUTLINE_COLOR);
    }

    private static void renderLineQuad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        int color
    ) {
        buffer.addVertex(pose, x0, y0, z0).setColor(color);
        buffer.addVertex(pose, x1, y0, z0).setColor(color);
        buffer.addVertex(pose, x1, y1, z1).setColor(color);
        buffer.addVertex(pose, x0, y1, z1).setColor(color);
    }
}
