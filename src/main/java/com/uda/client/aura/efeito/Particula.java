package com.uda.client.aura.efeito;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Particula leve das auras. Nao usa o motor de particulas do Minecraft: assim o brilho aditivo
 * funciona igual no Fabric e no NeoForge e a mesma aura aparece na previa do menu.
 */
public final class Particula {
    public enum Forma {
        /** Sempre virada para a camera. */
        BILLBOARD,
        /** Esticada na direcao em que esta indo (faisca, risco). */
        ESTICADA,
        /** Deitada no chao. */
        CHAO,
        /** Um bloco do Minecraft em miniatura (usa o campo bloco). */
        BLOCO
    }

    public interface Comportamento {
        void tick(Particula p, EstadoAura estado);
    }

    public double x, y, z, xo, yo, zo;
    public double vx, vy, vz;
    public int idade, vida = 20;
    public float tamanho = 0.2f;
    /** Tamanho no fim da vida; negativo = igual ao inicial. */
    public float tamanhoFinal = -1f;
    public int[] cores = {0xFFFFFFFF};
    public Sprite sprite = Sprites.BRILHO;
    public float rot, rotO, giro;
    /** Puxa para baixo a cada tick (negativo sobe). */
    public float gravidade;
    public float atrito = 0.96f;
    /** Posicao relativa aos pes do dono (acompanha ele) em vez de solta no mundo. */
    public boolean relativo;
    /** true = brilho somado (luz); false = translucido normal (fumaca, folhas, neve). */
    public boolean aditivo = true;
    /** So para as translucidas: ignora a luz do lugar e fica sempre clara. */
    public boolean luzPropria = true;
    public Forma forma = Forma.BILLBOARD;
    /** ESTICADA: comprimento do risco = velocidade * esticar. */
    public float esticar = 2.5f;
    /** Fracao da vida para aparecer e para sumir. */
    public float entrada = 0.08f, saida = 0.45f;
    /** Em vez de ficar transparente, cresce ao nascer e encolhe ao sumir (blocos nao tem alfa). */
    public boolean crescer;
    /** Quadros de animacao (ex.: asas do morcego). Se nulo, usa sprite. */
    public Sprite[] animacao;
    public float quadrosPorTick = 0.5f;
    /** Translucida desenhada por cima do brilho (estilhaco preto que nao pode ficar avermelhado). */
    public boolean porCima;
    /** 0..1: quanto o brilho tremula. */
    public float piscar;
    public float fase;
    public BlockState bloco;
    public Comportamento comportamento;
    /** Livres para o comportamento guardar o que precisar (angulo, raio...). */
    public double a, b, c;

    void colocar(double x, double y, double z) {
        this.x = this.xo = x;
        this.y = this.yo = y;
        this.z = this.zo = z;
    }

    public Particula vel(double vx, double vy, double vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        return this;
    }

    public Particula vida(int vida) {
        this.vida = Math.max(1, vida);
        return this;
    }

    public Particula tam(float inicio, float fim) {
        this.tamanho = inicio;
        this.tamanhoFinal = fim;
        return this;
    }

    public Particula cores(int... cores) {
        this.cores = cores;
        return this;
    }

    public Particula sprite(Sprite sprite) {
        this.sprite = sprite;
        return this;
    }

    public Particula animada(Sprite[] quadros, float quadrosPorTick) {
        this.animacao = quadros;
        this.quadrosPorTick = quadrosPorTick;
        this.sprite = quadros[0];
        return this;
    }

    /** Translucida preta por cima do brilho. */
    public Particula escura() {
        this.aditivo = false;
        this.luzPropria = true;
        this.porCima = true;
        return this;
    }

    public Particula fisica(float gravidade, float atrito) {
        this.gravidade = gravidade;
        this.atrito = atrito;
        return this;
    }

    public Particula giro(float rot, float giro) {
        this.rot = this.rotO = rot;
        this.giro = giro;
        return this;
    }

    public Particula fade(float entrada, float saida) {
        this.entrada = entrada;
        this.saida = saida;
        return this;
    }

    public Particula translucida(boolean luzPropria) {
        this.aditivo = false;
        this.luzPropria = luzPropria;
        return this;
    }

    public Particula forma(Forma forma) {
        this.forma = forma;
        return this;
    }

    public Particula relativa() {
        this.relativo = true;
        return this;
    }

    public Particula comportamento(Comportamento c) {
        this.comportamento = c;
        return this;
    }

    public void matar() {
        vida = idade;
    }

    /** false quando morreu. */
    boolean tick(EstadoAura estado) {
        xo = x;
        yo = y;
        zo = z;
        rotO = rot;
        if (++idade >= vida) return false;
        if (comportamento != null) comportamento.tick(this, estado);
        vy -= gravidade;
        x += vx;
        y += vy;
        z += vz;
        vx *= atrito;
        vy *= atrito;
        vz *= atrito;
        rot += giro;
        return idade < vida;
    }

    public float progresso(float parcial) {
        return Mth.clamp((idade + parcial) / vida, 0f, 1f);
    }

    public float tamanho(float t) {
        float s = tamanhoFinal < 0 ? tamanho : Mth.lerp(t, tamanho, tamanhoFinal);
        if (crescer) {
            float k = 1f;
            if (entrada > 0 && t < entrada) k = t / entrada;
            if (saida > 0 && t > 1f - saida) k = Math.min(k, (1f - t) / saida);
            // Passa um pouco do tamanho e volta, como se brotasse
            float q = Mth.clamp(k, 0f, 1f) - 1f;
            s *= 1f + 2.70158f * q * q * q + 1.70158f * q * q;
        }
        return s;
    }

    /** Multiplicador de transparencia: entra, fica, some. */
    public float envelope(float t, float tempo) {
        if (crescer) return 1f;
        float a = 1f;
        if (entrada > 0 && t < entrada) a = t / entrada;
        if (saida > 0 && t > 1f - saida) a = Math.min(a, (1f - t) / saida);
        if (piscar > 0) {
            float s = Mth.sin(tempo * 1.7f + fase) * 0.5f + Mth.sin(tempo * 4.3f + fase * 2.1f) * 0.5f;
            a *= 1f - piscar * (0.5f + 0.5f * s);
        }
        return Mth.clamp(a, 0f, 1f);
    }
}
