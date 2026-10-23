package com.uda.item;

import java.util.List;
import java.util.function.IntConsumer;

import com.uda.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Picareta e Pá Oculares. Despertas: cavam mais rápido e cada uma tem um poder próprio. */
public final class OcularDiggerItems {
    private static final float AWAKENED_SPEED = 1.6F;
    private static final float SUPREME_SPEED = 2.2F;

    /**
     * Detecção de minérios: no cliente, quem desenha os minérios através das paredes é a classe
     * client.OreHighlights, ligada aqui na inicialização do cliente (o item não referencia código de cliente).
     */
    public static IntConsumer oreScanHook = radius -> {
    };

    private OcularDiggerItems() {
    }

    private static float speed(ItemStack stack, float base) {
        if (!Ocular.isAwakened(stack) || base <= 1.0F) return base;
        return base * (Ocular.isSupreme(stack) ? SUPREME_SPEED : AWAKENED_SPEED);
    }

    /** Picareta Ocular. Aberta: o botão direito revela os minérios em volta, através das paredes, por 10 s. */
    public static class Pickaxe extends PickaxeItem implements OcularItem {
        public Pickaxe(Tier tier, Properties properties) {
            super(tier, properties);
        }

        @Override
        public String powerKey() {
            return "eye_pickaxe";
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!Ocular.isAwakened(stack)) {
                return InteractionResultHolder.pass(stack);
            }
            boolean supreme = Ocular.isSupreme(stack);
            if (level.isClientSide) {
                oreScanHook.accept(supreme ? 18 : 11);
            } else {
                level.playSound(null, player, ModSounds.WEAPON_SCAN.get(), SoundSource.PLAYERS, 1.0F, supreme ? 0.7F : 0.9F);
            }
            Ocular.spend(player, stack, 2, supreme ? 80 : 140);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return speed(stack, super.getDestroySpeed(stack, state));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            Ocular.inventoryTick(stack, level, entity, selected);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            Ocular.appendTooltip(stack, this, tooltip);
        }
    }

    /** Pá Ocular. Aberta: cava 3x3 (suprema 5x5) no plano para onde você olha; agachado cava um bloco só. */
    public static class Shovel extends ShovelItem implements OcularItem {
        private static final ThreadLocal<Boolean> DIGGING = ThreadLocal.withInitial(() -> false);

        public Shovel(Tier tier, Properties properties) {
            super(tier, properties);
        }

        @Override
        public String powerKey() {
            return "eye_shovel";
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            boolean result = super.mineBlock(stack, level, state, pos, miner);
            if (level.isClientSide || DIGGING.get() || !Ocular.isAwakened(stack) || !(miner instanceof ServerPlayer player)
                    || player.isShiftKeyDown() || !state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                return result;
            }
            Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
            int r = Ocular.isSupreme(stack) ? 2 : 1;
            float hardness = state.getDestroySpeed(level, pos);
            DIGGING.set(true);
            try {
                for (int a = -r; a <= r; a++) {
                    for (int b = -r; b <= r; b++) {
                        if (a == 0 && b == 0 || stack.isEmpty()) continue;
                        BlockPos other = switch (axis) {
                            case X -> pos.offset(0, a, b);
                            case Y -> pos.offset(a, 0, b);
                            case Z -> pos.offset(a, b, 0);
                        };
                        BlockState otherState = level.getBlockState(other);
                        float otherHardness = otherState.getDestroySpeed(level, other);
                        if (otherState.is(BlockTags.MINEABLE_WITH_SHOVEL) && otherHardness >= 0 && otherHardness <= hardness + 1.5F) {
                            player.gameMode.destroyBlock(other);
                        }
                    }
                }
            } finally {
                DIGGING.set(false);
            }
            return result;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return speed(stack, super.getDestroySpeed(stack, state));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            Ocular.inventoryTick(stack, level, entity, selected);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            Ocular.appendTooltip(stack, this, tooltip);
        }
    }
}
