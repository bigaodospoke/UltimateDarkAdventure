import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Gera o atlas de efeitos da Aura do Olho (512x768). Adaptado do GerarTexturas do mod Auras.
 * Rodar da raiz do projeto:  java tools/aura/GerarAtlasAura.java
 *
 * Os índices precisam bater com com.uda.client.aura.efeito.Sprites.
 * Células pequenas: 64px numa grade de 8 colunas (linhas 0 a 3). Grandes: 256px, duas por faixa, a partir de y = 256.
 * As texturas são brancas: a cor vem do vértice.
 */
public final class GerarAtlasAura {
    static final int TAM = 512, ALT = 768, CEL = 64, COLUNAS = TAM / CEL;
    static final float[] R = new float[TAM * ALT], G = new float[TAM * ALT], B = new float[TAM * ALT], A = new float[TAM * ALT];

    interface Fn {
        /** x, y de -1 a 1 (y para cima). Escreve rgba (alfa reto) em out. */
        void f(float x, float y, float[] out);
    }

    public static void main(String[] args) throws Exception {
        String raiz = args.length > 0 ? args[0] : ".";
        File atlas = new File(raiz, "src/main/resources/assets/uda/textures/aura/atlas.png");

        celula(0, (x, y, o) -> branco(o, brilho(len(x, y))));
        celula(1, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * Math.max(gauss(r, 0.16f), 0.55f * gauss(r, 0.45f)));
        });
        celula(2, (x, y, o) -> branco(o, estrela4(x, y)));
        celula(3, (x, y, o) -> branco(o, fumaca(x, y, 1.3f)));
        celula(4, (x, y, o) -> branco(o, fumaca(x, y, 7.9f)));
        celula(5, (x, y, o) -> branco(o, fumaca(x, y, 15.1f)));
        celula(6, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(gauss(r - 0.72f, 0.07f) + 0.35f * gauss(r - 0.72f, 0.2f)));
        });
        // faísca (risco esticado)
        celula(7, (x, y, o) -> {
            float k = Math.max(0, 1 - x * x);
            branco(o, clamp(gauss(y, 0.09f) * (float) Math.pow(k, 1.5) + 0.3f * gauss(y, 0.3f) * k * k) * borda(Math.abs(y) * 0.95f));
        });
        // feixe (fitas, rastros, raios)
        celula(8, (x, y, o) -> {
            float ay = Math.abs(y);
            branco(o, clamp(gauss(y, 0.13f) + 0.5f * gauss(y, 0.42f)) * (1 - suave(0.82f, 0.98f, ay)));
        });
        celula(9, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(1 - suave(0.3f, 0.42f, r) + 0.3f * gauss(r, 0.5f)));
        });
        celula(10, (x, y, o) -> {
            float a = estrela4(x, y);
            float xr = (x + y) * 0.7071f, yr = (y - x) * 0.7071f;
            branco(o, clamp(a + 0.5f * estrela4(xr * 1.4f, yr * 1.4f)));
        });
        celula(11, (x, y, o) -> {
            float r = len(x, y);
            branco(o, borda(r) * clamp(gauss(r - 0.8f, 0.035f) + 0.4f * gauss(r - 0.8f, 0.12f)));
        });
        // olhos: aberto, semicerrado, fechado, e a pupila (desenhada escura por cima)
        celula(12, (x, y, o) -> branco(o, olho(x, y, 1.0f)));
        celula(13, (x, y, o) -> branco(o, olho(x, y, 0.45f)));
        celula(14, (x, y, o) -> branco(o, olho(x, y, 0.0f)));
        celula(15, (x, y, o) -> branco(o, pupila(x, y)));

        grandeFn(0, GerarAtlasAura::vortice);
        grandeFn(1, (x, y, o) -> {
            float r = len(x, y);
            branco(o, (1 - suave(0.85f, 0.99f, r)) * clamp(0.6f * gauss(r - 0.72f, 0.15f) + 0.3f * (1 - suave(0f, 0.9f, r))));
        });
        grande(2, seloOlho());
        grande(3, circuloRunas());

        atlas.getParentFile().mkdirs();
        ImageIO.write(paraImagem(R, G, B, A, TAM, ALT), "png", atlas);
        System.out.println("Atlas salvo em " + atlas.getAbsolutePath());
    }

    // ------------------------------------------------------------ utilidades

    static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static float len(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    static float gauss(float d, float s) {
        return (float) Math.exp(-(d * d) / (2 * s * s));
    }

    static float suave(float a, float b, float v) {
        float t = clamp((v - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    /** Zera perto da borda da célula para o filtro linear não vazar no vizinho. */
    static float borda(float r) {
        return 1 - suave(0.86f, 0.98f, r);
    }

    static float brilho(float r) {
        return borda(r) * (float) Math.exp(-r * r * 5.0);
    }

    static void branco(float[] o, float a) {
        o[0] = o[1] = o[2] = 1;
        o[3] = clamp(a);
    }

    static float hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    static float ruido(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        float u = fx * fx * (3 - 2 * fx), v = fy * fy * (3 - 2 * fy);
        float a = hash(xi, yi, s), b = hash(xi + 1, yi, s), c = hash(xi, yi + 1, s), d = hash(xi + 1, yi + 1, s);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    static float fbm(float x, float y, int s) {
        float soma = 0, amp = 0.5f, f = 1;
        for (int i = 0; i < 5; i++) {
            soma += amp * ruido(x * f, y * f, s + i * 17);
            f *= 2;
            amp *= 0.5f;
        }
        return soma;
    }

    // ------------------------------------------------------------ formas pequenas

    static float estrela4(float x, float y) {
        float ax = Math.abs(x), ay = Math.abs(y);
        float raioH = (float) Math.exp(-ay * 28) * (float) Math.pow(Math.max(0, 1 - ax), 2.2);
        float raioV = (float) Math.exp(-ax * 28) * (float) Math.pow(Math.max(0, 1 - ay), 2.2);
        float r = len(x, y);
        return borda(r * 0.97f) * clamp(raioH + raioV + gauss(r, 0.12f) + 0.25f * gauss(r, 0.35f));
    }

    static float fumaca(float x, float y, float seed) {
        float r = len(x, y);
        float n = fbm(x * 2.2f + seed, y * 2.2f - seed, (int) (seed * 10));
        float forma = 1 - suave(0.35f + 0.35f * n, 0.95f, r);
        return borda(r) * clamp(forma * (0.45f + 0.55f * n)) * 0.9f;
    }

    /**
     * Olho amendoado visto de frente. abertura: 1 = aberto, 0 = fechado (só a linha da pálpebra).
     * A íris brilha; o furo da pupila fica vazio (a pupila escura é desenhada por cima).
     */
    static float olho(float x, float y, float abertura) {
        float ax = Math.abs(x) / 0.9f;
        if (ax >= 1) return 0.25f * gauss(len(x, y) - 0.9f, 0.08f) * borda(len(x, y));
        float meiaAltura = 0.48f * abertura * (float) Math.pow(1 - ax * ax, 0.85);
        // contorno das pálpebras (de cima e de baixo)
        float linha = Math.min(Math.abs(y - meiaAltura), Math.abs(y + meiaAltura));
        float contorno = gauss(linha, 0.035f) + 0.35f * gauss(linha, 0.11f);
        float dentro = Math.abs(y) < meiaAltura ? 1 : 0;
        // íris com brilho mais forte perto do centro, recortada pelas pálpebras
        float r = len(x, y);
        float iris = dentro * (1 - suave(0.3f, 0.36f, r)) * (0.55f + 0.45f * gauss(r, 0.18f));
        float esclera = dentro * 0.18f;
        // furo da pupila em fenda
        float fenda = len(x / 0.075f, y / 0.3f);
        float furo = suave(0.85f, 1.05f, fenda);
        // cílios quando fechado
        float cilios = 0;
        if (abertura < 0.05f) {
            for (int i = -2; i <= 2; i++) {
                float cx = i * 0.22f;
                cilios += gauss(Math.abs(x - cx) + Math.max(0, y) * 0.0f, 0.025f) * (y < 0 && y > -0.18f ? 1 : 0);
            }
        }
        float a = Math.max(contorno, Math.max(iris, esclera) * furo) + 0.6f * cilios;
        float halo = 0.22f * gauss(r, 0.45f);
        return clamp(a + halo) * borda(r);
    }

    static float pupila(float x, float y) {
        float fenda = len(x / 0.085f, y / 0.32f);
        return (1 - suave(0.8f, 1.0f, fenda)) * borda(len(x, y));
    }

    // ------------------------------------------------------------ formas grandes

    static void vortice(float x, float y, float[] o) {
        float r = len(x, y);
        float ang = (float) Math.atan2(y, x);
        float espiral = (float) Math.cos(4 * (ang + 2.6f * Math.log(r + 0.06f)));
        float braco = (float) Math.pow(0.5f + 0.5f * espiral, 3);
        float n = fbm(x * 4, y * 4, 21);
        float radial = suave(0.08f, 0.35f, r) * (1 - suave(0.7f, 0.97f, r));
        float a = braco * radial * (0.6f + 0.6f * n) + 0.5f * gauss(r - 0.9f, 0.03f) + 0.25f * gauss(r - 0.9f, 0.09f);
        branco(o, a * (1 - suave(0.95f, 0.995f, r)));
    }

    static BufferedImage nova(int tam) {
        return new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D pincel(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.translate(128, 128);
        return g;
    }

    static void anel(Graphics2D g, float r, float esp, float alfa) {
        g.setStroke(new BasicStroke(esp));
        g.setColor(new Color(1f, 1f, 1f, alfa));
        g.draw(new Ellipse2D.Float(-r, -r, 2 * r, 2 * r));
    }

    static void linha(Graphics2D g, double x0, double y0, double x1, double y1, float esp, float alfa) {
        g.setStroke(new BasicStroke(esp, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(1f, 1f, 1f, alfa));
        g.draw(new java.awt.geom.Line2D.Double(x0, y0, x1, y1));
    }

    static void ponto(Graphics2D g, double x, double y, float r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    /** Amêndoa de olho (contorno) centrada na origem, com largura w e altura h. */
    static Path2D.Double amendoa(double w, double h) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(-w / 2, 0);
        p.quadTo(0, -h, w / 2, 0);
        p.quadTo(0, h, -w / 2, 0);
        p.closePath();
        return p;
    }

    /** Selo do Olho: anéis, glifos, um círculo de olhos pequenos e um olho grande no centro. */
    static BufferedImage seloOlho() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(31);
        anel(g, 119, 2.4f, 1f);
        anel(g, 110, 1.2f, 0.85f);
        // glifos angulosos entre os dois anéis de fora
        int glifos = 36;
        for (int i = 0; i < glifos; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / glifos);
            g.translate(0, -114.5);
            for (int k = 0; k < 2; k++) {
                double x0 = rnd.nextInt(3) * 2.5 - 2.5, y0 = rnd.nextInt(2) * 3 - 1.5;
                double x1 = rnd.nextInt(3) * 2.5 - 2.5, y1 = rnd.nextInt(2) * 3 - 1.5;
                linha(g, x0, y0, x1, y1, 1.1f, 0.95f);
            }
            g.setTransform(antes);
        }
        // anel de olhos menores, deitados ao longo do círculo
        int olhos = 10;
        for (int i = 0; i < olhos; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / olhos);
            g.translate(0, -90);
            g.setStroke(new BasicStroke(1.6f));
            g.setColor(Color.WHITE);
            g.draw(amendoa(26, 11));
            ponto(g, 0, 0, 3.6f, Color.WHITE);
            g.setTransform(antes);
        }
        anel(g, 74, 2.6f, 0.95f);
        anel(g, 69, 1f, 0.6f);
        // triângulo (o olho que tudo vê) e raios saindo do olho central
        Path2D.Double tri = new Path2D.Double();
        for (int k = 0; k < 3; k++) {
            double a = -Math.PI / 2 + k * Math.PI * 2 / 3;
            double px = Math.cos(a) * 66, py = Math.sin(a) * 66;
            if (k == 0) tri.moveTo(px, py);
            else tri.lineTo(px, py);
        }
        tri.closePath();
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(1f, 1f, 1f, 0.85f));
        g.draw(tri);
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI * 2 / 16;
            linha(g, Math.cos(a) * 36, Math.sin(a) * 22, Math.cos(a) * (k % 2 == 0 ? 52 : 45), Math.sin(a) * (k % 2 == 0 ? 40 : 34), 1.2f, 0.7f);
        }
        // olho central: contorno grosso, íris em anel e pupila em fenda vazada
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(amendoa(78, 36));
        anel(g, 15, 3.4f, 1f);
        anel(g, 9, 1.4f, 0.7f);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(-2.6, -12, 5.2, 24));
        g.dispose();
        return comBrilho(img, 5f, 0.75f);
    }

    static BufferedImage circuloRunas() {
        BufferedImage img = nova(256);
        Graphics2D g = pincel(img);
        Random rnd = new Random(7);
        anel(g, 118, 2.2f, 1f);
        anel(g, 106, 1.4f, 0.9f);
        int runas = 28;
        for (int i = 0; i < runas; i++) {
            AffineTransform antes = g.getTransform();
            g.rotate(i * Math.PI * 2 / runas);
            g.translate(0, -112);
            for (int k = 0; k < 3; k++) {
                double x0 = rnd.nextInt(3) * 3 - 3, y0 = rnd.nextInt(3) * 2.5 - 2.5;
                double x1 = rnd.nextInt(3) * 3 - 3, y1 = rnd.nextInt(3) * 2.5 - 2.5;
                linha(g, x0, y0, x1, y1, 1.3f, 0.95f);
            }
            g.setTransform(antes);
        }
        g.dispose();
        return comBrilho(img, 4f, 0.6f);
    }

    /** Soma uma cópia borrada por cima: dá o brilho em volta das linhas. */
    static BufferedImage comBrilho(BufferedImage img, float raio, float forca) {
        int w = img.getWidth(), h = img.getHeight();
        float[] r = new float[w * h], g = new float[w * h], b = new float[w * h], a = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            int c = img.getRGB(i % w, i / w);
            float al = (c >>> 24) / 255f;
            a[i] = al;
            r[i] = (c >> 16 & 255) / 255f * al;
            g[i] = (c >> 8 & 255) / 255f * al;
            b[i] = (c & 255) / 255f * al;
        }
        float[][] canais = {r, g, b, a};
        float[][] borrados = new float[4][];
        for (int k = 0; k < 4; k++) borrados[k] = borrar(canais[k], w, h, raio);
        for (int i = 0; i < w * h; i++) {
            for (int k = 0; k < 4; k++) canais[k][i] = Math.min(1, canais[k][i] + borrados[k][i] * forca);
        }
        for (int i = 0; i < w * h; i++) {
            if (a[i] > 1e-4f) {
                r[i] = Math.min(1, r[i] / a[i]);
                g[i] = Math.min(1, g[i] / a[i]);
                b[i] = Math.min(1, b[i] / a[i]);
            } else {
                r[i] = g[i] = b[i] = 1;
            }
        }
        return paraImagem(r, g, b, a, w, h);
    }

    static float[] borrar(float[] src, int w, int h, float sigma) {
        int raio = (int) Math.ceil(sigma * 3);
        float[] k = new float[raio * 2 + 1];
        float soma = 0;
        for (int i = -raio; i <= raio; i++) soma += k[i + raio] = (float) Math.exp(-i * i / (2 * sigma * sigma));
        for (int i = 0; i < k.length; i++) k[i] /= soma;
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float s = 0;
                for (int i = -raio; i <= raio; i++) s += src[y * w + Math.min(w - 1, Math.max(0, x + i))] * k[i + raio];
                tmp[y * w + x] = s;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float s = 0;
                for (int i = -raio; i <= raio; i++) s += tmp[Math.min(h - 1, Math.max(0, y + i)) * w + x] * k[i + raio];
                out[y * w + x] = s;
            }
        }
        return out;
    }

    // ------------------------------------------------------------ montagem do atlas

    static void celula(int indice, Fn f) {
        preencher((indice % COLUNAS) * CEL, (indice / COLUNAS) * CEL, CEL, f);
    }

    static void grandeFn(int b, Fn f) {
        preencher((b % 2) * 4 * CEL, 4 * CEL + (b / 2) * 4 * CEL, CEL * 4, f);
    }

    /** Avalia a função com 3x3 amostras por pixel (borda lisa). */
    static void preencher(int ox, int oy, int tam, Fn f) {
        float[] o = new float[4];
        int ss = 3;
        for (int py = 0; py < tam; py++) {
            for (int px = 0; px < tam; px++) {
                float sr = 0, sg = 0, sb = 0, sa = 0;
                for (int j = 0; j < ss; j++) {
                    for (int i = 0; i < ss; i++) {
                        float x = ((px + (i + 0.5f) / ss) / tam) * 2 - 1;
                        float y = 1 - ((py + (j + 0.5f) / ss) / tam) * 2;
                        f.f(x, y, o);
                        sa += o[3];
                        sr += o[0] * o[3];
                        sg += o[1] * o[3];
                        sb += o[2] * o[3];
                    }
                }
                int idx = (oy + py) * TAM + ox + px;
                A[idx] = sa / (ss * ss);
                if (sa > 1e-5f) {
                    R[idx] = sr / sa;
                    G[idx] = sg / sa;
                    B[idx] = sb / sa;
                } else {
                    R[idx] = G[idx] = B[idx] = 1;
                }
            }
        }
    }

    static void grande(int b, BufferedImage img) {
        int ox = (b % 2) * 4 * CEL, oy = 4 * CEL + (b / 2) * 4 * CEL;
        int tam = CEL * 4;
        for (int y = 0; y < tam; y++) {
            for (int x = 0; x < tam; x++) {
                int c = img.getRGB(x * img.getWidth() / tam, y * img.getHeight() / tam);
                int idx = (oy + y) * TAM + ox + x;
                boolean margem = x < 2 || y < 2 || x >= tam - 2 || y >= tam - 2;
                A[idx] = margem ? 0 : (c >>> 24) / 255f;
                R[idx] = (c >> 16 & 255) / 255f;
                G[idx] = (c >> 8 & 255) / 255f;
                B[idx] = (c & 255) / 255f;
            }
        }
    }

    static BufferedImage paraImagem(float[] r, float[] g, float[] b, float[] a, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < w * h; i++) {
            int ai = Math.round(clamp(a[i]) * 255), ri = Math.round(clamp(r[i]) * 255);
            int gi = Math.round(clamp(g[i]) * 255), bi = Math.round(clamp(b[i]) * 255);
            img.setRGB(i % w, i / w, ai << 24 | ri << 16 | gi << 8 | bi);
        }
        return img;
    }
}
