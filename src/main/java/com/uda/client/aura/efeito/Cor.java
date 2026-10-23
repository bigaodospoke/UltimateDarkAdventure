package com.uda.client.aura.efeito;

import net.minecraft.util.Mth;

/** Cores em ARGB (0xAARRGGBB). */
public final class Cor {
    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    /** Cor RGB opaca (0xRRGGBB) com um alfa de 0..1. */
    public static int comAlfa(int rgb, float alfa) {
        return (Mth.clamp((int) (alfa * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int multAlfa(int argb, float f) {
        int a = (int) ((argb >>> 24) * Mth.clamp(f, 0f, 1f));
        return a << 24 | (argb & 0xFFFFFF);
    }

    public static int misturar(int c0, int c1, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) Mth.lerp(t, c0 >>> 24, c1 >>> 24);
        int r = (int) Mth.lerp(t, c0 >> 16 & 255, c1 >> 16 & 255);
        int g = (int) Mth.lerp(t, c0 >> 8 & 255, c1 >> 8 & 255);
        int b = (int) Mth.lerp(t, c0 & 255, c1 & 255);
        return argb(a, r, g, b);
    }

    /** Percorre uma lista de cores igualmente espacadas, t de 0 a 1. */
    public static int gradiente(int[] cores, float t) {
        if (cores.length == 1) return cores[0];
        float p = Mth.clamp(t, 0f, 1f) * (cores.length - 1);
        int i = Math.min((int) p, cores.length - 2);
        return misturar(cores[i], cores[i + 1], p - i);
    }

    /** Matiz 0..1, saturacao 0..1, brilho 0..1 -> RGB. */
    public static int hsv(float h, float s, float v) {
        return Mth.hsvToRgb(h - Mth.floor(h), s, v) & 0xFFFFFF;
    }

    private Cor() {
    }
}
