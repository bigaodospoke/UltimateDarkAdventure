package com.uda.client.aura.efeito;

import java.util.ArrayList;
import java.util.List;

import com.uda.client.aura.visual.VisualAura;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Tudo o que uma aura ativa precisa entre um tick e outro: partículas, idade e os dados do visual. */
public final class EstadoAura {
    private static final int LIMITE = 400;
    /** Devolvida quando a aura já tem partículas demais: o visual mexe nela e ela some. */
    private static final Particula DESCARTE = new Particula();

    public final VisualAura visual;
    public final Corpo corpo = new Corpo();
    public final RandomSource random = RandomSource.create();
    public final List<Particula> particulas = new ArrayList<>();
    public final Object dados;
    public int idade;
    /** Toca o efeito de ignição no próximo tick (o mob acabou de despertar). */
    public boolean anunciar;
    /** Multiplica quantas partículas nascem (distância da câmera). */
    public float densidade = 1f;
    /** 0..1: quanto a aura está acesa. Segue o olho do mob abrindo e fechando. */
    public float forca, forcaO;

    public EstadoAura(VisualAura visual) {
        this.visual = visual;
        this.dados = visual.criarDados(this);
    }

    public void tick() {
        for (int i = 0; i < particulas.size(); ) {
            if (particulas.get(i).tick(this)) {
                i++;
            } else {
                int fim = particulas.size() - 1;
                particulas.set(i, particulas.get(fim));
                particulas.remove(fim);
            }
        }
        if (anunciar) {
            visual.anunciar(this);
            anunciar = false;
        }
        visual.tick(this);
        idade++;
    }

    /** Nova partícula na posição do mundo dada (ou relativa aos pés, se o visual marcar relativa()). */
    public Particula emitir(double x, double y, double z) {
        Particula p;
        if (particulas.size() >= LIMITE) {
            p = DESCARTE;
            p.comportamento = null;
        } else {
            p = new Particula();
            particulas.add(p);
        }
        p.colocar(x, y, z);
        p.fase = random.nextFloat() * 6.28f;
        return p;
    }

    /** Emite relativa aos pés do dono. */
    public Particula emitirRel(double dx, double dy, double dz) {
        return emitir(dx, dy, dz).relativa();
    }

    /** Média de partículas por tick ajustada pela densidade, com arredondamento aleatório. */
    public int quantos(float media) {
        float n = media * densidade * forca();
        int base = (int) n;
        return base + (random.nextFloat() < n - base ? 1 : 0);
    }

    /** Sorteio de nascimento que respeita a densidade. */
    public boolean chance(float p) {
        return random.nextFloat() < p * densidade * forca();
    }

    public float forca() {
        return forca;
    }

    public float forca(float parcial) {
        return Mth.lerp(parcial, forcaO, forca);
    }

    public float aleatorio(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public void som(SoundEvent som, float volume, float tom) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.playLocalSound(corpo.x, corpo.y + corpo.altura * 0.5, corpo.z, som, SoundSource.HOSTILE, volume, tom, false);
    }
}
