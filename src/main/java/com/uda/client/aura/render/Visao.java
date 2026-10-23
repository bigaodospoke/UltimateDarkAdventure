package com.uda.client.aura.render;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Para onde a camera olha no espaco em que estamos desenhando. */
public final class Visao {
    /** Direita e cima da tela, no espaco de desenho. */
    public final Vector3f direita = new Vector3f(1, 0, 0);
    public final Vector3f cima = new Vector3f(0, 1, 0);
    private final Vector3f paraCamera = new Vector3f(0, 0, 1);
    /** Na GUI a camera esta "no infinito": a direcao ate ela e a mesma para todo ponto. */
    private boolean fixa;

    /** No mundo o espaco de desenho e relativo a camera (camera na origem). */
    public void mundo(Camera camera) {
        Quaternionf q = camera.rotation();
        direita.set(1, 0, 0).rotate(q);
        cima.set(0, 1, 0).rotate(q);
        fixa = false;
    }

    /** Na GUI o modelo foi girado por q e escalado com Y invertido (a tela cresce para baixo). */
    public void gui(Quaternionf q) {
        Quaternionf inv = new Quaternionf(q).conjugate();
        direita.set(1, 0, 0).rotate(inv);
        cima.set(0, -1, 0).rotate(inv);
        paraCamera.set(0, 0, -1).rotate(inv);
        fixa = true;
    }

    /** Vetor unitario do ponto (espaco de desenho) ate a camera. */
    public void dirCamera(float x, float y, float z, Vector3f saida) {
        if (fixa) {
            saida.set(paraCamera);
            return;
        }
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        if (l < 1.0e-4f) saida.set(0, 0, 1);
        else saida.set(-x / l, -y / l, -z / l);
    }
}
