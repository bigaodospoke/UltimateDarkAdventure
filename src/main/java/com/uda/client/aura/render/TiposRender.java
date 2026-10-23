package com.uda.client.aura.render;

import com.uda.UltimateDarkAdventure;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Os dois jeitos de desenhar as auras, usando o atlas de efeitos com filtro suave.
 * Estende RenderType so para enxergar os estados protegidos do vanilla.
 */
public final class TiposRender extends RenderType {
    public static final ResourceLocation ATLAS = UltimateDarkAdventure.id("textures/aura/atlas.png");
    private static final TextureStateShard TEXTURA = new TextureStateShard(ATLAS, true, false);

    /** Luz somada: cor * alfa vai para cima do que ja esta na tela. Sem descarte de alfa, com neblina. */
    public static final RenderType BRILHO = new RenderType("uda_aura_brilho", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, false,
            () -> {
                RENDERTYPE_EYES_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                LIGHTNING_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                LIGHTNING_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_EYES_SHADER.clearRenderState();
            }) {
    };

    /** Translucido comum (fumaca, folhas, neve), com luz do mundo e ordenado por distancia. */
    public static final RenderType TRANSLUCIDO = new RenderType("uda_aura_translucido", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true,
            () -> {
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                TRANSLUCENT_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                LIGHTMAP.setupRenderState();
                OVERLAY.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                OVERLAY.clearRenderState();
                LIGHTMAP.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                TRANSLUCENT_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            }) {
    };

    /**
     * Igual ao translucido, mas desenhado depois do brilho: um nucleo preto (raio negro, estilhacos)
     * fica preto mesmo com o brilho vermelho somado em volta.
     */
    public static final RenderType ESCURO = new RenderType("uda_aura_escuro", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true,
            () -> {
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
                TEXTURA.setupRenderState();
                TRANSLUCENT_TRANSPARENCY.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                LIGHTMAP.setupRenderState();
                OVERLAY.setupRenderState();
                COLOR_WRITE.setupRenderState();
            },
            () -> {
                COLOR_WRITE.clearRenderState();
                OVERLAY.clearRenderState();
                LIGHTMAP.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                TRANSLUCENT_TRANSPARENCY.clearRenderState();
                TEXTURA.clearRenderState();
                RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            }) {
    };

    /** Linhas desenhadas por cima de tudo (sem teste de profundidade): minérios vistos pela Picareta Ocular. */
    public static final RenderType LINHAS_ATRAVES = RenderType.create("uda_linhas_atraves", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(java.util.OptionalDouble.of(2.5)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));

    private TiposRender() {
        super("uda_aura_nunca", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> {
        }, () -> {
        });
    }
}
