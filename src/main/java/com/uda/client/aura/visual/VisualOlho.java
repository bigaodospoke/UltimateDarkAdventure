package com.uda.client.aura.visual;

import java.util.ArrayList;
import java.util.List;

import com.uda.client.aura.efeito.Cor;
import com.uda.client.aura.efeito.Corpo;
import com.uda.client.aura.efeito.EstadoAura;
import com.uda.client.aura.efeito.Particula;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.Desenho;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Aura do Olho: o Selo do Olho girando no chão sobre uma poça de treva, vórtice roxo, olhos
 * flutuantes orbitando o mob (piscam de vez em quando), faíscas sugadas para o corpo, fumaça negra
 * e espíritos subindo. O perfil do Carrasco é maior, tem mais olhos e raios roxos estalando no corpo.
 */
public final class VisualOlho extends VisualAura {
    public record Perfil(int olhos, float escala, boolean raios, float densidade) {
        public static final Perfil VIGIA = new Perfil(3, 1.0f, false, 1.0f);
        public static final Perfil CARRASCO = new Perfil(4, 1.2f, true, 1.35f);
        public static final Perfil DRAGAO = new Perfil(8, 1.5f, true, 2.4f);
        public static final Perfil OLHO_NEGRO = new Perfil(10, 1.7f, true, 2.6f);
    }

    private static final int[] SUGADA = {0x00E070FF, 0xFFE070FF, 0xFFA44BFF, 0xFF5A18C0};
    private static final int[] TREVA = {0x00100418, 0xB0140820, 0x80100418, 0x00000000};
    private static final int[] ESPIRITO = {0xFFD58CFF, 0xFF8A2BE2, 0x003A0A80};
    private static final int[] FAGULHA = {0xFFFFFFFF, 0xFFE070FF, 0x00A44BFF};
    private static final int PISCADA = 6;

    /** Acelera até o eixo do corpo e some quando chega. */
    private static final Particula.Comportamento SUGAR = (p, e) -> {
        double dx = -p.x, dy = p.c - p.y, dz = -p.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.22) {
            p.matar();
            return;
        }
        double k = 0.014 / dist;
        // um pouco de giro para fazer espiral
        p.vx += dx * k - dz * 0.004 / dist;
        p.vy += dy * k;
        p.vz += dz * k + dx * 0.004 / dist;
    };

    /** Sobe girando em volta do corpo (partícula relativa). */
    private static final Particula.Comportamento GIRAR = (p, e) -> {
        double ang = 0.035;
        double x = p.x * Math.cos(ang) - p.z * Math.sin(ang);
        double z = p.x * Math.sin(ang) + p.z * Math.cos(ang);
        p.x = x;
        p.z = z;
    };

    private final Perfil perfil;

    public VisualOlho(Perfil perfil) {
        this.perfil = perfil;
    }

    /** Por aura: piscadas de cada olho flutuante e os raios ativos. */
    private static final class Dados {
        final int[] piscar;
        final int[] proxima;
        final List<Raio> raios = new ArrayList<>();

        Dados(int olhos, RandomSource r) {
            piscar = new int[olhos];
            proxima = new int[olhos];
            for (int i = 0; i < olhos; i++) proxima[i] = 20 + r.nextInt(80);
        }
    }

    @Override
    public Object criarDados(EstadoAura e) {
        return new Dados(perfil.olhos(), e.random);
    }

    /** Tamanho da aura: cresce com a largura do mob (o Carrasco ascendido fica maior). */
    private float escala(Corpo c) {
        return perfil.escala() * Mth.clamp(c.largura / 0.6f, 1f, 2.5f);
    }

    private static void sugada(EstadoAura e, float rMin, float rMax) {
        float h = e.corpo.altura;
        double[] p = noCorpo(e, rMin, rMax, -0.05f, 1.15f);
        Particula pt = e.emitirRel(p[0], p[1], p[2]).vida(45).tam(0.055f, 0.03f).cores(SUGADA)
                .sprite(Sprites.FAISCA).forma(Particula.Forma.ESTICADA).fisica(0f, 0.95f).fade(0.25f, 0.1f)
                .comportamento(SUGAR);
        pt.c = h * (0.35f + e.random.nextFloat() * 0.3f);
        pt.esticar = 3.5f;
    }

    @Override
    public void tick(EstadoAura e) {
        RandomSource r = e.random;
        Dados dados = (Dados) e.dados;
        float k = escala(e.corpo);
        float dens = perfil.densidade();

        for (int i = 0, n = e.quantos(2.2f * dens); i < n; i++) sugada(e, 1.1f * k, 1.7f * k);
        // Tentáculos de fumaça negra
        for (int i = 0, n = e.quantos(0.8f * dens); i < n; i++) {
            double[] p = noChao(e, 0.3f * k, 0.85f * k);
            e.emitirRel(p[0], r.nextFloat() * 0.3f, p[2]).vel(0, 0.02f + r.nextFloat() * 0.015f, 0)
                    .vida(30 + r.nextInt(20)).tam(0.35f * k, 0.85f * k).cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(true).fisica(0f, 0.99f).giro(r.nextFloat() * TAU, (r.nextFloat() - 0.5f) * 0.05f)
                    .fade(0f, 0f).comportamento(GIRAR);
        }
        // Espíritos roxos subindo em espiral
        for (int i = 0, n = e.quantos(0.5f * dens); i < n; i++) {
            double[] p = noChao(e, 0.4f * k, 0.9f * k);
            e.emitirRel(p[0], 0.05, p[2]).vel(0, 0.03f + r.nextFloat() * 0.02f, 0).vida(28 + r.nextInt(12))
                    .tam(0.16f, 0.04f).cores(ESPIRITO).sprite(Sprites.BRILHO).fisica(0f, 0.99f).fade(0.15f, 0.5f)
                    .comportamento(GIRAR);
        }
        // Brilhos piscando em volta
        for (int i = 0, n = e.quantos(0.3f * dens); i < n; i++) {
            double[] p = noCorpo(e, 0.4f * k, 0.9f * k, 0.1f, 1f);
            e.emitirRel(p[0], p[1], p[2]).vida(8 + r.nextInt(6)).tam(0.2f, 0.05f).cores(0xFFFFFFFF, 0xFFE070FF)
                    .sprite(Sprites.ESTRELA).giro(r.nextFloat() * TAU, 0f).fade(0.2f, 0.6f).fisica(0, 1);
        }

        // Piscadas dos olhos flutuantes
        for (int i = 0; i < dados.piscar.length; i++) {
            if (dados.piscar[i] > 0) {
                dados.piscar[i]--;
            } else if (--dados.proxima[i] <= 0) {
                dados.piscar[i] = PISCADA;
                dados.proxima[i] = 50 + r.nextInt(110);
            }
        }

        // Raios roxos estalando pelo corpo (só no perfil forte)
        if (perfil.raios()) {
            dados.raios.removeIf(raio -> ++raio.idade >= raio.vida);
            if (e.chance(0.2f)) dados.raios.add(novoRaio(e));
            for (Raio raio : dados.raios) raio.tremer(r);
        }
    }

    private static Raio novoRaio(EstadoAura e) {
        Corpo c = e.corpo;
        float a0 = e.random.nextFloat() * TAU;
        float a1 = a0 + e.aleatorio(0.7f, 1.8f) * (e.random.nextBoolean() ? 1 : -1);
        float r = c.largura * 0.55f + 0.15f;
        Raio raio = Raio.reto(8,
                Mth.cos(a0) * r, c.altura * e.aleatorio(0.1f, 0.95f), Mth.sin(a0) * r,
                Mth.cos(a1) * r, c.altura * e.aleatorio(0.1f, 0.95f), Mth.sin(a1) * r, e.aleatorio(0.05f, 0.25f));
        raio.vida = 3 + e.random.nextInt(3);
        raio.largura = 0.09f;
        raio.tremor = 0.14f;
        raio.tremer(e.random);
        return raio;
    }

    @Override
    public void anunciar(EstadoAura e) {
        Corpo c = e.corpo;
        RandomSource r = e.random;
        float k = escala(c);
        // Implosão: energia sugada de longe
        for (int i = 0; i < 45; i++) sugada(e, 2.0f * k, 3.2f * k);
        // Nuvem de treva que explode para fora
        for (int i = 0; i < 22; i++) {
            float ang = r.nextFloat() * TAU, v = 0.08f + r.nextFloat() * 0.08f;
            e.emitir(c.x, c.y + 0.2 + r.nextFloat() * c.altura * 0.7, c.z).vel(Mth.cos(ang) * v, 0.01f, Mth.sin(ang) * v)
                    .vida(25 + r.nextInt(10)).tam(0.5f * k, 1.2f * k).cores(TREVA).sprite(Sprites.FUMACAS[r.nextInt(3)])
                    .translucida(true).fisica(0f, 0.9f).giro(r.nextFloat() * TAU, 0.03f).fade(0f, 0f);
        }
        // O selo do Olho se abre no chão, crescendo e girando
        e.emitirRel(0, 0.05, 0).forma(Particula.Forma.CHAO).sprite(Sprites.SELO_OLHO).tam(0.6f * k, 3.4f * k).vida(24)
                .cores(0xFFE090FF, 0xD0A44BFF, 0x007A20D0).fade(0f, 0.6f).fisica(0, 1).giro(0f, 0.05f);
        onda(e, 0xFF8A2BE2, 3.6f * k, 18, Sprites.ANEL_SUAVE);
        clarao(e, 0xFFC45CFF, 2.2f * k, 10);
        explosao(e, 26, 0.22f, Sprites.ESTRELA, 0.14f, 14, -0.002f, FAGULHA);
    }

    @Override
    public void desenhar(Desenho d, EstadoAura e) {
        float f = d.forca;
        if (f <= 0.001f) return;
        Dados dados = (Dados) e.dados;
        float h = d.corpo.altura, t = d.tempo;
        float k = escala(d.corpo);
        double cx = d.cx, cy = d.cy, cz = d.cz;
        int luz = Desenho.LUZ_TOTAL;
        float p = pulso(t, 0.07f, 0f);

        // Poça de treva, runas, Selo do Olho e vórtice girando ao contrário
        d.chao(false, cx, cy + 0.015, cz, 1.7f * k, 0f, Cor.comAlfa(0x05000A, 0.8f * f), Sprites.BRILHO, luz);
        d.chao(true, cx, cy + 0.018, cz, 1.75f * k, -t * 0.006f, Cor.comAlfa(0x7A20D0, (0.3f + 0.15f * p) * f), Sprites.CIRCULO_RUNAS, luz);
        d.chao(true, cx, cy + 0.021, cz, 1.45f * k, t * 0.012f, Cor.comAlfa(0xB060FF, (0.55f + 0.3f * p) * f), Sprites.SELO_OLHO, luz);
        d.chao(true, cx, cy + 0.024, cz, 0.85f * k, -t * 0.04f, Cor.comAlfa(0xE090FF, 0.28f * f), Sprites.VORTICE, luz);
        d.chao(true, cx, cy + 0.02, cz, 1.65f * k, 0f, Cor.comAlfa(0x7A20D0, (0.2f + 0.15f * p) * f), Sprites.ANEL_SUAVE, luz);
        // Halo em volta do corpo
        d.brilho(cx, cy + h * 0.55, cz, 0.9f * k + h * 0.35f, Cor.comAlfa(0x7A20D0, 0.1f * f), Sprites.BRILHO);

        // Olhos flutuantes orbitando, com rastro
        int olhos = perfil.olhos();
        float tamOlho = 0.24f * perfil.escala();
        float[] o = new float[3];
        for (int i = 0; i < olhos; i++) {
            int n = 12;
            d.fitaInicio();
            for (int j = 0; j < n; j++) {
                float s = j / (n - 1f);
                orbita(i, olhos, t - j * 0.7f, h, d.corpo, o);
                d.fitaPonto(cx + o[0], cy + o[1], cz + o[2], 0.12f * perfil.escala() * (1f - s * 0.9f),
                        Cor.comAlfa(0xA44BFF, (1f - s) * (1f - s) * 0.6f * f));
            }
            d.fitaFim(true, Sprites.FEIXE, luz);

            orbita(i, olhos, t, h, d.corpo, o);
            double x = cx + o[0], y = cy + o[1], z = cz + o[2];
            int piscar = dados.piscar[i];
            Sprite olho = piscar == 0 ? Sprites.OLHO[0] : (piscar >= 5 || piscar <= 2) ? Sprites.OLHO[1] : Sprites.OLHO[2];
            d.brilho(x, y, z, tamOlho * 2.4f, Cor.comAlfa(0x8A2BE2, 0.35f * f), Sprites.BRILHO);
            d.billboard(true, x, y, z, tamOlho, 0f, Cor.comAlfa(0xE88CFF, f), olho, luz);
            if (piscar == 0) {
                d.porCima = true;
                d.billboard(false, x, y, z, tamOlho, 0f, Cor.comAlfa(0x000000, 0.92f * f), Sprites.PUPILA, luz);
                d.porCima = false;
            }
        }

        // Raios: contorno roxo e miolo branco
        for (Raio raio : dados.raios) {
            float a = raio.brilho * f;
            d.fitaInicio();
            for (int i = 0; i < raio.n; i++) d.fitaPonto(cx + raio.x[i], cy + raio.y[i], cz + raio.z[i], raio.largura, Cor.comAlfa(0xB050FF, a));
            d.fitaFim(true, Sprites.FEIXE, luz);
            d.fitaInicio();
            for (int i = 0; i < raio.n; i++) d.fitaPonto(cx + raio.x[i], cy + raio.y[i], cz + raio.z[i], raio.largura * 0.35f, Cor.comAlfa(0xFFFFFF, a));
            d.fitaFim(true, Sprites.FEIXE, luz);
        }
    }

    /** Posição de um olho flutuante (relativa aos pés): órbitas inclinadas, alturas diferentes. */
    private void orbita(int i, int total, float t, float h, Corpo c, float[] out) {
        float ang = t * 0.05f + i * TAU / total;
        float raio = (0.55f + c.largura * 0.6f) * perfil.escala();
        out[0] = Mth.cos(ang) * raio;
        out[1] = h * (0.5f + 0.22f * Mth.sin(t * 0.06f + i * 2.3f)) + Mth.sin(ang) * 0.18f;
        out[2] = Mth.sin(ang) * raio;
    }
}
