package com.uda.entity;

/**
 * Algo que tem uma forma Fechada (dormente) e uma forma Aberta (desperta).
 * Usado pelos renderizadores e pelos itens para trocar o visual.
 */
public interface Awakenable {
    boolean isAwakened();

    /** 0 = olho fechado, 1 = olho aberto. Interpolado para a animação de despertar. */
    float getAwakenProgress(float partialTick);

    /** Verdadeiro durante uma piscada (só tem efeito no cliente). */
    boolean isBlinking();

    /** Fase enfurecida dos bosses (o brilho pulsa mais rápido e forte). */
    default boolean isEnraged() {
        return false;
    }
}
