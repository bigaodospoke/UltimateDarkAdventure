package com.uda.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Altar do Vazio (só existe na Catedral do Vazio, não quebra): o Coração Abissal, tirado do Dragão Abissal,
 * desperta O Olho Negro.
 */
public class VoidAltarBlock extends BaseEntityBlock {
    public static final MapCodec<VoidAltarBlock> CODEC = simpleCodec(VoidAltarBlock::new);
    public static final BooleanProperty ACTIVE = EyeAltarBlock.ACTIVE;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 14, 14), Block.box(1, 14, 1, 15, 16, 15));

    public VoidAltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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
        if (stack.is(ModItems.ABYSSAL_HEART.get())) {
            if (!level.isClientSide) {
                if (altar.bossNearby()) {
                    AbyssalAltarBlock.message(player, Component.translatable("message.uda.summon.already"));
                } else {
                    stack.consume(1, player);
                    level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                    altar.start();
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            AbyssalAltarBlock.message(player, Component.translatable("message.uda.cathedral.hint"));
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
}
