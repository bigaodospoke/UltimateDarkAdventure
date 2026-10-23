package com.uda.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uda.client.aura.render.TiposRender;
import com.uda.registry.ModBlocks;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Detecção de minérios da Picareta Ocular: uma onda sai do jogador e os minérios encontrados
 * ficam contornados através das paredes por 10 segundos, cada tipo com sua cor.
 * A busca é feita no cliente, que já tem os blocos carregados (o servidor só cuida do custo e do som).
 */
public final class OreHighlights {
    private static final int DURATION = 200;
    private static final int MAX_FOUND = 600;
    private static final Map<BlockPos, Integer> FOUND = new HashMap<>();
    private static long startedAt;

    private OreHighlights() {
    }

    public static void scan(int radius) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        FOUND.clear();
        BlockPos center = mc.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius && FOUND.size() < MAX_FOUND; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES)) {
                        FOUND.put(pos.immutable(), color(state));
                    }
                }
            }
        }
        startedAt = level.getGameTime();
    }

    private static int color(BlockState state) {
        if (state.is(ModBlocks.EYE_ORE.get()) || state.is(ModBlocks.DEEPSLATE_EYE_ORE.get())) return 0xD05CFF;
        if (state.is(Tags.Blocks.ORES_DIAMOND)) return 0x5CF2FF;
        if (state.is(Tags.Blocks.ORES_EMERALD)) return 0x3CFF6E;
        if (state.is(Tags.Blocks.ORES_GOLD)) return 0xFFD23C;
        if (state.is(Tags.Blocks.ORES_IRON)) return 0xE8C4A8;
        if (state.is(Tags.Blocks.ORES_REDSTONE)) return 0xFF3C3C;
        if (state.is(Tags.Blocks.ORES_LAPIS)) return 0x3C6EFF;
        if (state.is(Tags.Blocks.ORES_COPPER)) return 0xFF8A4A;
        if (state.is(Tags.Blocks.ORES_NETHERITE_SCRAP)) return 0xA0643C;
        if (state.is(Tags.Blocks.ORES_COAL)) return 0x6E6E6E;
        return 0xFFFFFF;
    }

    public static void render(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (FOUND.isEmpty() || mc.level == null || mc.player == null) {
            return;
        }
        float age = mc.level.getGameTime() - startedAt + partialTick;
        if (age > DURATION) {
            FOUND.clear();
            return;
        }
        float fade = Mth.clamp((DURATION - age) / 40.0F, 0.0F, 1.0F);
        float wave = age * 1.5F; // a onda anda 1,5 bloco por tick
        Vec3 cam = camera.getPosition();
        Vec3 origin = mc.player.position();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(TiposRender.LINHAS_ATRAVES);
        PoseStack pose = new PoseStack();
        for (Map.Entry<BlockPos, Integer> entry : FOUND.entrySet()) {
            BlockPos pos = entry.getKey();
            double dist = Math.sqrt(pos.distToCenterSqr(origin));
            if (dist > wave) continue;
            float appear = Mth.clamp((float) (wave - dist) / 4.0F, 0.0F, 1.0F);
            int rgb = entry.getValue();
            float pulse = 0.75F + 0.25F * Mth.sin(age * 0.25F + pos.getY());
            AABB box = new AABB(pos).deflate(0.02).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(pose, lines, box, (rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F,
                    appear * fade * pulse);
        }
        // o estado "sem teste de profundidade" do Minecraft só funciona se o teste já estiver desligado
        RenderSystem.disableDepthTest();
        buffers.endBatch(TiposRender.LINHAS_ATRAVES);
        RenderSystem.enableDepthTest();
    }

    public static void clear() {
        FOUND.clear();
    }
}
