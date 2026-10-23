package com.uda.item;

/**
 * Equipamento do Olho: tem as formas Selada, Desperta e Suprema, liberadas no Altar do Olho (veja {@link Ocular}).
 */
public interface OcularItem {
    /** Chave dos textos do poder (tooltip.uda.&lt;chave&gt;.power / .supreme). */
    String powerKey();

    /**
     * true: armas, que abrem e fecham o olho com a tecla (aberto gasta durabilidade).
     * false: armadura e amuletos, cujo poder fica sempre ativo depois de despertos no altar.
     */
    default boolean opensWithKey() {
        return true;
    }
}
