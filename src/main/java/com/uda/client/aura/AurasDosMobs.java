package com.uda.client.aura;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.uda.client.aura.efeito.EstadoAura;
import com.uda.client.aura.render.RenderAuras;
import com.uda.client.aura.visual.VisualOlho;
import com.uda.entity.AbstractEyeMonster;
import com.uda.entity.AbyssalDragon;
import com.uda.entity.BlackEye;
import com.uda.entity.BlindExecutioner;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Quais mobs do Olho estão despertos perto da câmera, e o estado animado da aura de cada um.
 * A aura acende enquanto o olho se abre, faz a ignição no instante do despertar e apaga junto com o olho.
 */
public final class AurasDosMobs {
    private static final double DISTANCIA = 96.0;
    private static final VisualOlho VIGIA = new VisualOlho(VisualOlho.Perfil.VIGIA);
    private static final VisualOlho CARRASCO = new VisualOlho(VisualOlho.Perfil.CARRASCO);
    private static final VisualOlho DRAGAO = new VisualOlho(VisualOlho.Perfil.DRAGAO);
    private static final VisualOlho OLHO_NEGRO = new VisualOlho(VisualOlho.Perfil.OLHO_NEGRO);

    private static final Map<LivingEntity, EstadoAura> ESTADOS = new IdentityHashMap<>();
    private static final Set<LivingEntity> VISTOS = new HashSet<>();

    private static VisualOlho visualDe(AbstractEyeMonster mob) {
        if (mob instanceof AbyssalDragon) return DRAGAO;
        if (mob instanceof BlackEye) return OLHO_NEGRO;
        return mob instanceof BlindExecutioner ? CARRASCO : VIGIA;
    }

    /** Fim de cada tick do cliente. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            ESTADOS.clear();
            return;
        }
        VISTOS.clear();
        for (Entity entidade : mc.level.entitiesForRendering()) {
            if (!(entidade instanceof AbstractEyeMonster mob) || mob.isRemoved()) continue;
            double dist = mob.distanceTo(mc.player);
            if (dist > DISTANCIA) continue;

            // Morrendo: apaga em meio segundo. Invisível: sem aura.
            float vivo = mob.isDeadOrDying() ? Math.max(0f, 1f - mob.deathTime / 10f) : 1f;
            float forca = mob.isInvisible() ? 0f : mob.getAwakenProgress(1f) * vivo;

            EstadoAura e = ESTADOS.get(mob);
            if (e == null) {
                if (forca <= 0f) continue;
                e = new EstadoAura(visualDe(mob));
                // Só faz a ignição se a gente viu o olho abrir (não para quem já chega desperto).
                e.anunciar = mob.tickCount > 2 && forca < 0.5f && mob.isAwakened();
                ESTADOS.put(mob, e);
            }
            e.corpo.copiar(mob);
            e.forcaO = e.forca;
            e.forca = forca;
            e.densidade = (dist < 16 ? 1f : dist < 32 ? 0.6f : 0.35f) * (mob.isEnraged() ? 1.6f : 1f);
            e.tick();
            if (forca > 0f || !e.particulas.isEmpty()) VISTOS.add(mob);
        }
        ESTADOS.keySet().retainAll(VISTOS);
    }

    public static void desenhar(Camera camera, float parcial, Frustum frustum) {
        RenderAuras.mundo(ESTADOS, camera, parcial, frustum);
    }

    public static void limpar() {
        ESTADOS.clear();
    }

    private AurasDosMobs() {
    }
}
