package com.uda.network;

import com.uda.UltimateDarkAdventure;
import com.uda.item.Ocular;
import com.uda.item.OcularItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> servidor: o jogador apertou "Despertar a arma". */
public record AwakenItemPayload() implements CustomPacketPayload {
    public static final AwakenItemPayload INSTANCE = new AwakenItemPayload();
    public static final Type<AwakenItemPayload> TYPE = new Type<>(UltimateDarkAdventure.id("awaken_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AwakenItemPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, AwakenItemPayload::handle);
    }

    private static void handle(AwakenItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // Mão principal primeiro; senão, a secundária (escudo, grimório...)
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof OcularItem)) {
                stack = player.getOffhandItem();
            }
            if (stack.getItem() instanceof OcularItem) {
                Ocular.toggle(player, stack);
            }
        });
    }
}
