package com.uda.block;

import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Guarda o equipamento posto no altar e conduz o ritual de despertar (5 s): a energia é sugada para o item,
 * o coração do altar bate cada vez mais rápido, o item treme e racha, o olho se abre e tudo explode em energia.
 */
public class EyeAltarBlockEntity extends BlockEntity {
    public static final int RITUAL_TICKS = 100;
    public static final int CRACK_START = 60;
    public static final int EYE_OPEN = 88;
    private static final int[] HEARTBEATS = {0, 22, 40, 54, 64, 72, 78, 83, 87};

    private ItemStack item = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private int ritualTicks = -1;

    public EyeAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_ALTAR.get(), pos, state);
    }

    public ItemStack getItem() {
        return this.item;
    }

    /** -1 parado; 0..RITUAL_TICKS durante o ritual. */
    public int getRitualTicks() {
        return this.ritualTicks;
    }

    // ------------------------------------------------------------------ interação

    public ItemInteractionResult use(Player player, InteractionHand hand, ItemStack held) {
        Level level = this.level;
        if (level == null || this.ritualTicks >= 0) {
            return ItemInteractionResult.CONSUME;
        }
        if (this.item.isEmpty()) {
            if (!AltarRituals.accepts(held)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.uda.altar.invalid").withStyle(ChatFormatting.DARK_PURPLE), true);
                }
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                this.item = held.copyWithCount(1);
                held.consume(1, player);
                level.playSound(null, this.worldPosition, ModSounds.ALTAR_PLACE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                this.sync();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        AltarRituals.Ritual ritual = AltarRituals.forInput(this.item);
        if (ritual == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!held.is(ritual.catalyst().get())) {
            if (held.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // mão vazia: devolve o item
            }
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.uda.altar.catalyst", ritual.catalyst().get().getDescription(),
                        ritual.offeringCount(), ritual.offering().get().getDescription()).withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            Item offering = ritual.offering().get();
            if (!player.getAbilities().instabuild && count(player, offering) < ritual.offeringCount()) {
                player.displayClientMessage(Component.translatable("message.uda.altar.offering", ritual.offeringCount(), offering.getDescription())
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!player.getAbilities().instabuild) {
                take(player, offering, ritual.offeringCount());
            }
            held.consume(1, player);
            this.result = ritual.result().apply(this.item);
            this.ritualTicks = 0;
            level.setBlock(this.worldPosition, this.getBlockState().setValue(EyeAltarBlock.ACTIVE, true), 3);
            level.playSound(null, this.worldPosition, ModSounds.ALTAR_RITUAL.get(), SoundSource.BLOCKS, 1.4F, 0.8F);
            this.sync();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult takeItem(Player player) {
        if (this.level == null || this.ritualTicks >= 0 || this.item.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!this.level.isClientSide) {
            if (!player.getInventory().add(this.item.copy())) {
                player.drop(this.item.copy(), false);
            }
            this.item = ItemStack.EMPTY;
            this.level.playSound(null, this.worldPosition, ModSounds.ALTAR_PLACE.get(), SoundSource.BLOCKS, 0.8F, 1.4F);
            this.sync();
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    public void dropContents() {
        if (this.level != null && !this.item.isEmpty()) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                    this.item);
            this.item = ItemStack.EMPTY;
        }
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void take(Player player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) return;
            if (stack.is(item)) {
                int n = Math.min(amount, stack.getCount());
                stack.shrink(n);
                amount -= n;
            }
        }
    }

    // ------------------------------------------------------------------ ritual

    public static void tick(Level level, BlockPos pos, BlockState state, EyeAltarBlockEntity altar) {
        if (altar.ritualTicks < 0) {
            return;
        }
        int t = altar.ritualTicks++;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, 1.5 + Math.min(t, 40) * 0.01, 0.0);
        // energia sugada de um círculo em volta para o item
        if (t < EYE_OPEN && t % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = server.random.nextDouble() * Math.PI * 2;
                double r = 2.2 + server.random.nextDouble();
                double x = pos.getX() + 0.5 + Math.cos(angle) * r, z = pos.getZ() + 0.5 + Math.sin(angle) * r;
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, pos.getY() + 0.3 + server.random.nextDouble() * 2, z, 0,
                        center.x - x, center.y - pos.getY(), center.z - z, 0.08);
            }
        }
        for (int beat : HEARTBEATS) {
            if (t == beat) {
                server.playSound(null, pos, ModSounds.ALTAR_HEARTBEAT.get(), SoundSource.BLOCKS, 1.2F, 0.7F + t * 0.006F);
            }
        }
        // rachaduras: faíscas e estalos cada vez mais fortes
        if (t >= CRACK_START && t < EYE_OPEN && t % 3 == 0) {
            float k = (t - CRACK_START) / (float) (EYE_OPEN - CRACK_START);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 3 + (int) (k * 8), 0.25, 0.25, 0.25, 0.15 + k * 0.3);
            server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 0.8F + k), center.x, center.y, center.z, 4, 0.3, 0.3, 0.3, 0.0);
        }
        if (t == EYE_OPEN) {
            server.playSound(null, pos, ModSounds.EYE_OPEN.get(), SoundSource.BLOCKS, 1.6F, 0.7F);
        }
        if (t >= RITUAL_TICKS) {
            // explosão de energia: só efeito, nada é destruído
            server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 2.2F), center.x, center.y, center.z, 70, 0.9, 0.9, 0.9, 0.12);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 0.2, 0.2, 0.2, 0.6);
            server.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            for (int i = 0; i < 16; i++) {
                float a = i * Mth.TWO_PI / 16;
                server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.6F), pos.getX() + 0.5 + Mth.cos(a) * 2.5,
                        pos.getY() + 0.2, pos.getZ() + 0.5 + Mth.sin(a) * 2.5, 2, 0.1, 0.05, 0.1, 0.0);
            }
            server.playSound(null, pos, ModSounds.ALTAR_AWAKEN.get(), SoundSource.BLOCKS, 2.0F, 0.9F);
            altar.item = altar.result;
            altar.result = ItemStack.EMPTY;
            altar.ritualTicks = -1;
            level.setBlock(pos, state.setValue(EyeAltarBlock.ACTIVE, false), 3);
            altar.sync();
        }
    }

    // ------------------------------------------------------------------ salvar e sincronizar

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.item.isEmpty()) tag.put("Item", this.item.save(registries));
        if (!this.result.isEmpty()) tag.put("Result", this.result.save(registries));
        tag.putInt("Ritual", this.ritualTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.item = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
        this.result = tag.contains("Result") ? ItemStack.parseOptional(registries, tag.getCompound("Result")) : ItemStack.EMPTY;
        this.ritualTicks = tag.contains("Ritual") ? tag.getInt("Ritual") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
