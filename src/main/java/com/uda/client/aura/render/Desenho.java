package com.uda.client.aura.render;

import com.uda.client.aura.efeito.Corpo;
import com.uda.client.aura.efeito.Cor;
import com.uda.client.aura.efeito.EstadoAura;
import com.uda.client.aura.efeito.Sprite;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pincel das auras. Todas as posicoes entram em coordenadas do mundo (na previa do menu o "mundo"
 * e centrado nos pes do jogador) e saem relativas a origem do desenho.
 */
public final class Desenho {
    public static final int LUZ_TOTAL = LightTexture.FULL_BRIGHT;

    public PoseStack pose;
    public MultiBufferSource buffers;
    public VertexConsumer brilho, translucido, escuro;
    /** Quando true, o que for translucido vai para a camada desenhada por cima do brilho. */
    public boolean porCima;
    public final Visao visao = new Visao();
    public double ox, oy, oz;
    public float parcial;
    public boolean gui;
    public int luzMundo = LUZ_TOTAL;

    // Aura sendo desenhada agora
    public EstadoAura estado;
    public Corpo corpo;
    /** Pes do dono (interpolado). */
    public double cx, cy, cz;
    /** Ticks de vida da aura com fracao. */
    public float tempo;
    /** 0..1: quanto a aura está acesa (segue o olho do mob). */
    public float forca;
    /** Direcao do corpo em radianos (para asas). */
    public float yawCorpo;

    private Matrix4f m;
    private float nx, ny, nz;
    private final Vector3f tmp = new Vector3f();
    private final Vector3f tmp2 = new Vector3f();

    private float[] fx = new float[64], fy = new float[64], fz = new float[64], fw = new float[64];
    private int[] fc = new int[64];
    private float[] sx = new float[64], sy = new float[64], sz = new float[64];
    private int nf;

    void comecar(PoseStack pose, MultiBufferSource buffers, VertexConsumer brilho, VertexConsumer translucido,
                 VertexConsumer escuro, double ox, double oy, double oz, float parcial, boolean gui) {
        this.pose = pose;
        this.buffers = buffers;
        this.brilho = brilho;
        this.translucido = translucido;
        this.escuro = escuro;
        this.porCima = false;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.parcial = parcial;
        this.gui = gui;
        this.m = pose.last().pose();
        pose.last().normal().transform(tmp.set(0, 1, 0));
        tmp.normalize();
        nx = tmp.x;
        ny = tmp.y;
        nz = tmp.z;
    }

    void preparar(EstadoAura e) {
        porCima = false;
        estado = e;
        corpo = e.corpo;
        cx = corpo.x(parcial);
        cy = corpo.y(parcial);
        cz = corpo.z(parcial);
        tempo = e.idade + parcial;
        forca = e.forca(parcial);
        yawCorpo = corpo.yaw(parcial) * Mth.DEG_TO_RAD;
    }

    private void v(VertexConsumer vc, float x, float y, float z, float u, float v, int cor, int luz) {
        vc.addVertex(m, x, y, z).setColor(cor).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz)
                .setNormal(nx, ny, nz);
    }

    /** Some suavemente o que fica colado na camera (primeira pessoa, ou dentro da aura de outro). */
    private float pertoDaCamera(float x, float y, float z) {
        if (gui) return 1f;
        float d2 = x * x + y * y + z * z;
        if (d2 > 2.1f) return 1f;
        return Mth.clamp(((float) Math.sqrt(d2) - 0.5f) / 0.95f, 0f, 1f);
    }

    private VertexConsumer vc(boolean aditivo) {
        return aditivo ? brilho : porCima ? escuro : translucido;
    }

    /** Quadrado virado para a camera. */
    public void billboard(boolean aditivo, double wx, double wy, double wz, float tam, float rot, int cor, Sprite s, int luz) {
        float x = (float) (wx - ox), y = (float) (wy - oy), z = (float) (wz - oz);
        cor = Cor.multAlfa(cor, pertoDaCamera(x, y, z));
        if ((cor >>> 24) == 0 || tam <= 0) return;
        float c = Mth.cos(rot) * tam, sn = Mth.sin(rot) * tam;
        Vector3f r = visao.direita, u = visao.cima;
        float rx = r.x * c + u.x * sn, ry = r.y * c + u.y * sn, rz = r.z * c + u.z * sn;
        float ux = -r.x * sn + u.x * c, uy = -r.y * sn + u.y * c, uz = -r.z * sn + u.z * c;
        VertexConsumer vc = vc(aditivo);
        v(vc, x - rx - ux, y - ry - uy, z - rz - uz, s.u0(), s.v1(), cor, luz);
        v(vc, x - rx + ux, y - ry + uy, z - rz + uz, s.u0(), s.v0(), cor, luz);
        v(vc, x + rx + ux, y + ry + uy, z + rz + uz, s.u1(), s.v0(), cor, luz);
        v(vc, x + rx - ux, y + ry - uy, z + rz - uz, s.u1(), s.v1(), cor, luz);
    }

    public void brilho(double wx, double wy, double wz, float tam, int cor, Sprite s) {
        billboard(true, wx, wy, wz, tam, 0f, cor, s, LUZ_TOTAL);
    }

    /** Quadrado com eixos livres: a = meia largura (direcao do u), b = meia altura (direcao do v). */
    public void plano(boolean aditivo, double wx, double wy, double wz, float ax, float ay, float az,
                      float bx, float by, float bz, int cor, Sprite s, int luz, boolean espelhar) {
        float x = (float) (wx - ox), y = (float) (wy - oy), z = (float) (wz - oz);
        cor = Cor.multAlfa(cor, pertoDaCamera(x, y, z));
        if ((cor >>> 24) == 0) return;
        float u0 = espelhar ? s.u1() : s.u0(), u1 = espelhar ? s.u0() : s.u1();
        VertexConsumer vc = vc(aditivo);
        v(vc, x - ax - bx, y - ay - by, z - az - bz, u0, s.v0(), cor, luz);
        v(vc, x - ax + bx, y - ay + by, z - az + bz, u0, s.v1(), cor, luz);
        v(vc, x + ax + bx, y + ay + by, z + az + bz, u1, s.v1(), cor, luz);
        v(vc, x + ax - bx, y + ay - by, z + az - bz, u1, s.v0(), cor, luz);
    }

    /** Disco deitado (circulos magicos, poças, ondas no chao). */
    public void chao(boolean aditivo, double wx, double wy, double wz, float raio, float ang, int cor, Sprite s, int luz) {
        float c = Mth.cos(ang) * raio, sn = Mth.sin(ang) * raio;
        plano(aditivo, wx, wy, wz, c, 0, sn, -sn, 0, c, cor, s, luz, false);
    }

    /** Risco que vai de (p - d) ate (p), virado para a camera. */
    public void esticada(boolean aditivo, double wx, double wy, double wz, double dx, double dy, double dz,
                         float largura, int cor, Sprite s, int luz) {
        fitaInicio();
        fitaPonto(wx - dx, wy - dy, wz - dz, largura, cor);
        fitaPonto(wx + dx * 0.15, wy + dy * 0.15, wz + dz * 0.15, largura, cor);
        fitaFim(aditivo, s, luz);
    }

    // ---- Fitas: linhas grossas que sempre encaram a camera (raios, rastros, aneis) ----

    public void fitaInicio() {
        nf = 0;
    }

    public void fitaPonto(double wx, double wy, double wz, float largura, int cor) {
        if (nf == fx.length) crescer();
        fx[nf] = (float) (wx - ox);
        fy[nf] = (float) (wy - oy);
        fz[nf] = (float) (wz - oz);
        fw[nf] = largura * 0.5f;
        fc[nf] = cor;
        nf++;
    }

    public void fitaFim(boolean aditivo, Sprite s, int luz) {
        int n = nf;
        if (n < 2) return;
        for (int i = 0; i < n; i++) {
            int a = Math.max(i - 1, 0), b = Math.min(i + 1, n - 1);
            float tx = fx[b] - fx[a], ty = fy[b] - fy[a], tz = fz[b] - fz[a];
            visao.dirCamera(fx[i], fy[i], fz[i], tmp2);
            tmp.set(tx, ty, tz).cross(tmp2);
            float l = tmp.length();
            if (l < 1.0e-5f) tmp.set(visao.direita);
            else tmp.div(l);
            sx[i] = tmp.x * fw[i];
            sy[i] = tmp.y * fw[i];
            sz[i] = tmp.z * fw[i];
            fc[i] = Cor.multAlfa(fc[i], pertoDaCamera(fx[i], fy[i], fz[i]));
        }
        VertexConsumer vc = vc(aditivo);
        float du = (s.u1() - s.u0()) / (n - 1);
        for (int i = 0; i < n - 1; i++) {
            int j = i + 1;
            if ((fc[i] >>> 24) == 0 && (fc[j] >>> 24) == 0) continue;
            float ui = s.u0() + du * i, uj = s.u0() + du * j;
            v(vc, fx[i] - sx[i], fy[i] - sy[i], fz[i] - sz[i], ui, s.v0(), fc[i], luz);
            v(vc, fx[i] + sx[i], fy[i] + sy[i], fz[i] + sz[i], ui, s.v1(), fc[i], luz);
            v(vc, fx[j] + sx[j], fy[j] + sy[j], fz[j] + sz[j], uj, s.v1(), fc[j], luz);
            v(vc, fx[j] - sx[j], fy[j] - sy[j], fz[j] - sz[j], uj, s.v0(), fc[j], luz);
        }
    }

    private void crescer() {
        int t = fx.length * 2;
        fx = java.util.Arrays.copyOf(fx, t);
        fy = java.util.Arrays.copyOf(fy, t);
        fz = java.util.Arrays.copyOf(fz, t);
        fw = java.util.Arrays.copyOf(fw, t);
        fc = java.util.Arrays.copyOf(fc, t);
        sx = new float[t];
        sy = new float[t];
        sz = new float[t];
    }

    /** Bloco do Minecraft em miniatura, centrado no ponto. */
    public void bloco(BlockState estado, double wx, double wy, double wz, float escala, Quaternionf rot, int luz) {
        float x = (float) (wx - ox), y = (float) (wy - oy), z = (float) (wz - oz);
        if (pertoDaCamera(x, y, z) < 0.6f) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(rot);
        pose.scale(escala, escala, escala);
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(estado, pose, buffers, luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
