package com.uda.client.aura.efeito;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Onde está o dono da aura: posição dos pés, tamanho e direção do corpo. */
public final class Corpo {
    public double x, y, z;
    public double xo, yo, zo;
    public float altura = 1.8f;
    public float largura = 0.6f;
    /** Direção do corpo em graus (igual ao yBodyRot do Minecraft). */
    public float yaw, yawO;
    public boolean noChao = true;
    public boolean primeiraPessoa;
    /** Prévia sem mundo (não usada pelos mobs, mantida para o Desenho). */
    public boolean gui;
    private boolean primeiro = true;

    public void copiar(LivingEntity entidade) {
        xo = x;
        yo = y;
        zo = z;
        x = entidade.getX();
        y = entidade.getY();
        z = entidade.getZ();
        yawO = yaw;
        yaw = entidade.yBodyRot;
        if (primeiro) {
            xo = x;
            yo = y;
            zo = z;
            yawO = yaw;
            primeiro = false;
        }
        altura = entidade.getBbHeight();
        largura = entidade.getBbWidth();
        noChao = entidade.onGround();
    }

    public double x(float parcial) {
        return Mth.lerp(parcial, xo, x);
    }

    public double y(float parcial) {
        return Mth.lerp(parcial, yo, y);
    }

    public double z(float parcial) {
        return Mth.lerp(parcial, zo, z);
    }

    public float yaw(float parcial) {
        return Mth.rotLerp(parcial, yawO, yaw);
    }

    /** Quanto andou no último tick (blocos). */
    public double velocidade() {
        double dx = x - xo, dz = z - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
