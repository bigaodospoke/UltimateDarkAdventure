package com.uda.client.aura.render;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.uda.client.aura.efeito.Cor;
import com.uda.client.aura.efeito.EstadoAura;
import com.uda.client.aura.efeito.Particula;
import com.uda.client.aura.efeito.Sprite;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Desenha as auras dos mobs no mundo (depois das entidades). */
public final class RenderAuras {
    private static MultiBufferSource.BufferSource fonte;
    private static final Desenho DESENHO = new Desenho();
    private static final Quaternionf ROT_BLOCO = new Quaternionf();
    /** Conta os frames do mundo desenhados (o roteiro de prints usa para não gravar quadro repetido). */
    public static int quadros;

    private static MultiBufferSource.BufferSource fonte() {
        if (fonte == null) {
            // Ordem do desenho no fim: translúcido, brilho e o escuro por cima de tudo
            LinkedHashMap<RenderType, ByteBufferBuilder> fixos = new LinkedHashMap<>();
            fixos.put(TiposRender.TRANSLUCIDO, new ByteBufferBuilder(1 << 17));
            fixos.put(TiposRender.BRILHO, new ByteBufferBuilder(1 << 18));
            fixos.put(TiposRender.ESCURO, new ByteBufferBuilder(1 << 15));
            fonte = MultiBufferSource.immediateWithBuffers(fixos, new ByteBufferBuilder(1 << 16));
        }
        return fonte;
    }

    public static void mundo(Map<LivingEntity, EstadoAura> estados, Camera camera, float parcial, @Nullable Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        quadros++;
        if (mc.level == null || estados.isEmpty()) return;
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource f = fonte();
        PoseStack pose = new PoseStack();
        DESENHO.comecar(pose, f, f.getBuffer(TiposRender.BRILHO), f.getBuffer(TiposRender.TRANSLUCIDO), f.getBuffer(TiposRender.ESCURO),
                cam.x, cam.y, cam.z, parcial, false);
        DESENHO.visao.mundo(camera);
        for (Map.Entry<LivingEntity, EstadoAura> entrada : estados.entrySet()) {
            LivingEntity dono = entrada.getKey();
            if (frustum != null && !frustum.isVisible(dono.getBoundingBox().inflate(3.0, 2.0, 3.0))) continue;
            DESENHO.luzMundo = LevelRenderer.getLightColor(mc.level, BlockPos.containing(dono.getX(), dono.getEyeY(), dono.getZ()));
            desenhar(DESENHO, entrada.getValue());
        }
        f.endBatch();
    }

    private static void desenhar(Desenho d, EstadoAura e) {
        d.preparar(e);
        e.visual.desenhar(d, e);
        for (Particula p : e.particulas) particula(d, p);
    }

    private static void particula(Desenho d, Particula p) {
        float parcial = d.parcial;
        float t = p.progresso(parcial);
        double x = Mth.lerp(parcial, p.xo, p.x), y = Mth.lerp(parcial, p.yo, p.y), z = Mth.lerp(parcial, p.zo, p.z);
        if (p.relativo) {
            x += d.cx;
            y += d.cy;
            z += d.cz;
        }
        int cor = Cor.multAlfa(Cor.gradiente(p.cores, t), p.envelope(t, d.tempo));
        if ((cor >>> 24) == 0) return;
        float tam = p.tamanho(t);
        if (p.forma == Particula.Forma.BILLBOARD) {
            // Quadrado grande perto do chão entraria na grama e deixaria uma borda reta: encolhe
            float max = (float) (y - d.cy) * 1.2f + 0.12f;
            if (tam > max) tam = Math.max(max, 0f);
        }
        int luz = p.aditivo || p.luzPropria ? Desenho.LUZ_TOTAL : d.luzMundo;
        Sprite s = p.sprite;
        if (p.animacao != null) {
            int q = (int) ((p.idade + parcial) * p.quadrosPorTick + p.fase * 2f);
            s = p.animacao[Math.floorMod(q, p.animacao.length)];
        }
        d.porCima = p.porCima;
        switch (p.forma) {
            case BILLBOARD -> d.billboard(p.aditivo, x, y, z, tam, Mth.lerp(parcial, p.rotO, p.rot), cor, s, luz);
            case ESTICADA -> d.esticada(p.aditivo, x, y, z, (p.x - p.xo) * p.esticar, (p.y - p.yo) * p.esticar,
                    (p.z - p.zo) * p.esticar, tam, cor, s, luz);
            case CHAO -> d.chao(p.aditivo, x, y, z, tam, Mth.lerp(parcial, p.rotO, p.rot), cor, s, luz);
            case BLOCO -> {
                if (p.bloco != null && tam > 0.001f) {
                    ROT_BLOCO.identity().rotateY(Mth.lerp(parcial, p.rotO, p.rot));
                    d.bloco(p.bloco, x, y + tam * 0.5, z, tam, ROT_BLOCO, p.luzPropria ? Desenho.LUZ_TOTAL : d.luzMundo);
                }
            }
        }
        d.porCima = false;
    }

    private RenderAuras() {
    }
}
