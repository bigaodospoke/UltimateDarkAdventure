package com.uda.client.aura.visual;

import com.uda.client.aura.efeito.Cor;
import com.uda.client.aura.efeito.Corpo;
import com.uda.client.aura.efeito.EstadoAura;
import com.uda.client.aura.efeito.Particula;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.Desenho;
import net.minecraft.util.Mth;

/**
 * Como uma aura se parece. tick() cria particulas (20x por segundo) e desenhar() pinta o que e
 * continuo (aneis no chao, orbes, raios) a cada frame.
 */
public abstract class VisualAura {
    protected static final float TAU = (float) (Math.PI * 2);
    protected static final float PI = (float) Math.PI;

    public Object criarDados(EstadoAura e) {
        return null;
    }

    public abstract void tick(EstadoAura e);

    /** Efeito de quando o jogador acabou de ligar a aura. */
    public void anunciar(EstadoAura e) {
    }

    public void desenhar(Desenho d, EstadoAura e) {
    }

    // ---------------- Ajudantes ----------------

    /** Anel que se espalha pelo chao. */
    protected static void onda(EstadoAura e, int cor, float raioFinal, int vida, Sprite sprite) {
        e.emitirRel(0, 0.06, 0).forma(Particula.Forma.CHAO).sprite(sprite).tam(0.2f, raioFinal).vida(vida)
                .cores(cor, Cor.multAlfa(cor, 0.6f), Cor.multAlfa(cor, 0f)).fade(0f, 0.5f).fisica(0, 1);
    }

    /** Clarao no centro do corpo. */
    protected static void clarao(EstadoAura e, int cor, float tam, int vida) {
        e.emitirRel(0, e.corpo.altura * 0.5, 0).sprite(Sprites.BRILHO).tam(tam * 0.6f, tam).vida(vida)
                .cores(cor, Cor.multAlfa(cor, 0f)).fade(0f, 0.8f).fisica(0, 1);
    }

    /** Particulas saindo do centro do corpo em todas as direcoes. */
    protected static void explosao(EstadoAura e, int n, float vel, Sprite s, float tam, int vida, float gravidade, int... cores) {
        Corpo c = e.corpo;
        for (int i = 0; i < n; i++) {
            float ang = e.random.nextFloat() * TAU;
            float sobe = e.aleatorio(-0.3f, 1f);
            float h = Mth.sqrt(1 - sobe * sobe * 0.5f);
            float v = vel * e.aleatorio(0.5f, 1f);
            e.emitir(c.x, c.y + c.altura * e.aleatorio(0.2f, 0.7f), c.z)
                    .vel(Mth.cos(ang) * h * v, sobe * v * 0.8f, Mth.sin(ang) * h * v)
                    .sprite(s).tam(tam, tam * 0.2f).vida(vida + e.random.nextInt(Math.max(1, vida / 2)))
                    .cores(cores).fisica(gravidade, 0.9f).giro(e.random.nextFloat() * TAU, (e.random.nextFloat() - 0.5f) * 0.3f);
        }
    }

    /** Ponto aleatorio numa casca cilindrica em volta do corpo, relativo aos pes. */
    protected static double[] noCorpo(EstadoAura e, float rMin, float rMax, float yMin, float yMax) {
        float ang = e.random.nextFloat() * TAU;
        float r = e.aleatorio(rMin, rMax);
        float y = e.corpo.altura * e.aleatorio(yMin, yMax);
        return new double[]{Mth.cos(ang) * r, y, Mth.sin(ang) * r, ang};
    }

    /** Ponto aleatorio dentro de um disco no chao (distribuicao uniforme). */
    protected static double[] noChao(EstadoAura e, float rMin, float rMax) {
        float ang = e.random.nextFloat() * TAU;
        float r = Mth.sqrt(e.aleatorio(rMin * rMin, rMax * rMax));
        return new double[]{Mth.cos(ang) * r, 0, Mth.sin(ang) * r, ang};
    }

    /** Respiracao lenta 0..1. */
    protected static float pulso(float tempo, float velocidade, float fase) {
        return 0.5f + 0.5f * Mth.sin(tempo * velocidade + fase);
    }

    /** Tremor rapido e irregular 0..1 (chama, eletricidade). */
    protected static float tremor(float tempo, float fase) {
        float a = Mth.sin(tempo * 1.9f + fase) * Mth.sin(tempo * 3.7f + fase * 1.3f);
        float b = Mth.sin(tempo * 7.3f + fase * 0.7f);
        return Mth.clamp(0.55f + 0.3f * a + 0.15f * b, 0f, 1f);
    }

    /** Fita circular deitada (anel) em volta dos pes. */
    protected static void anelFita(Desenho d, double y, float raio, int pontos, float largura, int cor, Sprite s) {
        d.fitaInicio();
        for (int i = 0; i <= pontos; i++) {
            float a = TAU * i / pontos;
            d.fitaPonto(d.cx + Mth.cos(a) * raio, y, d.cz + Mth.sin(a) * raio, largura, cor);
        }
        d.fitaFim(true, s, Desenho.LUZ_TOTAL);
    }
}
