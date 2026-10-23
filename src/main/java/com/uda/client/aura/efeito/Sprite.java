package com.uda.client.aura.efeito;

/** Um pedaço do atlas de efeitos (textures/aura/atlas.png, gerado por tools/aura/GerarAtlasAura.java). */
public record Sprite(float u0, float v0, float u1, float v1) {
    public static final int LARGURA = 512;
    public static final int ALTURA = 768;
    private static final int CEL = 64;
    private static final int COLUNAS = LARGURA / CEL;

    /** Célula pequena (64px) no índice i da grade de 8 colunas (linhas 0 a 3). */
    static Sprite celula(int i) {
        int c = i % COLUNAS, l = i / COLUNAS;
        return area(c * CEL, l * CEL, CEL, CEL);
    }

    /** Sprite grande (256px): dois por faixa, a partir de y = 256. */
    static Sprite grande(int b) {
        return area((b % 2) * CEL * 4, CEL * 4 + (b / 2) * CEL * 4, CEL * 4, CEL * 4);
    }

    private static Sprite area(int x, int y, int w, int h) {
        return new Sprite((x + 0.5f) / LARGURA, (y + 0.5f) / ALTURA, (x + w - 0.5f) / LARGURA, (y + h - 0.5f) / ALTURA);
    }
}
