package com.uda.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModItems;
import com.uda.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Altar Abissal (só existe no Santuário Abissal, não quebra): recebe 4 Fragmentos Abissais, um de cada vez,
 * e então o Núcleo do Olho Desperto invoca o Dragão Abissal.
 */
public class AbyssalAltarBlock extends BaseEntityBlock {
    public static final MapCodec<AbyssalAltarBlock> CODEC = simpleCodec(AbyssalAltarBlock::new);
    public static final IntegerProperty FRAGMENTS = IntegerProperty.create("fragments", 0, 4);
    public static final BooleanProperty ACTIVE = EyeAltarBlock.ACTIVE;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 6, 16), Block.box(3, 6, 3, 13, 12, 13));

    public AbyssalAltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FRAGMENTS, 0).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FRAGMENTS, ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SummonAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SUMMON_ALTAR.get(), SummonAltarBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(ACTIVE) || !(level.getBlockEntity(pos) instanceof SummonAltarBlockEntity altar)) {
            return ItemInteractionResult.CONSUME;
        }
        int fragments = state.getValue(FRAGMENTS);
        if (stack.is(ModItems.ABYSSAL_FRAGMENT.get()) && fragments < 4) {
            if (level instanceof ServerLevel server) {
                stack.consume(1, player);
                server.setBlock(pos, state.setValue(FRAGMENTS, fragments + 1), 3);
                server.playSound(null, pos, ModSounds.ALTAR_PLACE.get(), SoundSource.BLOCKS, 1.2F, 0.6F + fragments * 0.15F);
                server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.5F), pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, 20, 0.4, 0.2, 0.4, 0.0);
                message(player, Component.translatable("message.uda.sanctuary.fragments", fragments + 1));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ModItems.AWAKENED_EYE_CORE.get())) {
            if (!level.isClientSide) {
                if (fragments < 4) {
                    message(player, Component.translatable("message.uda.sanctuary.needs_fragments", 4 - fragments));
                } else if (altar.bossNearby()) {
                    message(player, Component.translatable("message.uda.summon.already"));
                } else {
                    stack.consume(1, player);
                    level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                    altar.start();
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            message(player, Component.translatable("message.uda.sanctuary.hint", fragments));
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    static void message(Player player, Component text) {
        player.displayClientMessage(text.copy().withStyle(ChatFormatting.DARK_PURPLE), true);
    }
}
