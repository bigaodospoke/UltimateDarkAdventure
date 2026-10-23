package com.uda.client.aura.visual;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Um raio: curva base e a versao tremida que muda a cada tick. Pontos relativos aos pes do dono. */
final class Raio {
    final float[] bx, by, bz, x, y, z;
    final int n;
    int idade, vida;
    float largura, tremor, brilho = 1f;

    Raio(int n) {
        this.n = n;
        bx = new float[n];
        by = new float[n];
        bz = new float[n];
        x = new float[n];
        y = new float[n];
        z = new float[n];
    }

    void tremer(RandomSource r) {
        for (int i = 0; i < n; i++) {
            float amp = (i == 0 || i == n - 1) ? 0f : tremor;
            x[i] = bx[i] + (r.nextFloat() - 0.5f) * amp;
            y[i] = by[i] + (r.nextFloat() - 0.5f) * amp;
            z[i] = bz[i] + (r.nextFloat() - 0.5f) * amp;
        }
        brilho = 0.55f + 0.45f * r.nextFloat();
    }

    /** Linha quase reta de (x0,y0,z0) a (x1,y1,z1), com uma barriga para cima. */
    static Raio reto(int n, float x0, float y0, float z0, float x1, float y1, float z1, float barriga) {
        Raio raio = new Raio(n);
        for (int i = 0; i < n; i++) {
            float s = i / (n - 1f);
            raio.bx[i] = Mth.lerp(s, x0, x1);
            raio.by[i] = Mth.lerp(s, y0, y1) + Mth.sin(s * Mth.PI) * barriga;
            raio.bz[i] = Mth.lerp(s, z0, z1);
        }
        return raio;
    }
}
