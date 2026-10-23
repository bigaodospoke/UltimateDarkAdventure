package com.uda.client.aura.efeito;

/** Índices aqui precisam bater com tools/aura/GerarAtlasAura.java. */
public final class Sprites {
    public static final Sprite BRILHO = Sprite.celula(0);
    public static final Sprite NUCLEO = Sprite.celula(1);
    public static final Sprite ESTRELA = Sprite.celula(2);
    public static final Sprite[] FUMACAS = {Sprite.celula(3), Sprite.celula(4), Sprite.celula(5)};
    public static final Sprite ANEL = Sprite.celula(6);
    public static final Sprite FAISCA = Sprite.celula(7);
    public static final Sprite FEIXE = Sprite.celula(8);
    public static final Sprite PONTO = Sprite.celula(9);
    public static final Sprite ESTRELA_SUAVE = Sprite.celula(10);
    public static final Sprite ANEL_FINO = Sprite.celula(11);
    /** Olho flutuante: aberto, semicerrado e fechado (piscada). */
    public static final Sprite[] OLHO = {Sprite.celula(12), Sprite.celula(13), Sprite.celula(14)};
    /** Pupila em fenda, desenhada escura por cima do olho aberto. */
    public static final Sprite PUPILA = Sprite.celula(15);

    public static final Sprite VORTICE = Sprite.grande(0);
    public static final Sprite ANEL_SUAVE = Sprite.grande(1);
    /** Selo do chão: olho dentro do triângulo, cercado de olhos menores. */
    public static final Sprite SELO_OLHO = Sprite.grande(2);
    public static final Sprite CIRCULO_RUNAS = Sprite.grande(3);

    private Sprites() {
    }
}
