package com.uda.block;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.uda.item.Ocular;
import com.uda.item.OcularItem;
import com.uda.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Os despertares do Altar do Olho. Cada ritual: o que vai em cima do altar, o catalisador (na mão,
 * consumido) e a oferenda (tirada do inventário).
 * <ul>
 *   <li>Selado → Desperto: Núcleo do Olho + 1 Cristal Roxo Ocular</li>
 *   <li>Desperto → Supremo: Essência do Vazio + 2 Escamas Abissais</li>
 *   <li>Núcleo do Olho → Núcleo do Olho Desperto: Fragmento Abissal + 2 Cristais Roxos Oculares</li>
 * </ul>
 */
public final class AltarRituals {
    public record Ritual(Predicate<ItemStack> input, Supplier<? extends Item> catalyst, Supplier<? extends Item> offering, int offeringCount,
            Function<ItemStack, ItemStack> result) {
    }

    private static final List<Ritual> RITUALS = List.of(
            new Ritual(stack -> stack.getItem() instanceof OcularItem && Ocular.tier(stack) == Ocular.SEALED,
                    ModItems.EYE_CORE, ModItems.EYE_CRYSTAL, 1, stack -> Ocular.withTier(stack, Ocular.AWAKENED)),
            new Ritual(stack -> stack.getItem() instanceof OcularItem && Ocular.tier(stack) == Ocular.AWAKENED,
                    ModItems.VOID_ESSENCE, ModItems.ABYSSAL_SCALE, 2, stack -> Ocular.withTier(stack, Ocular.SUPREME)),
            new Ritual(stack -> stack.is(ModItems.EYE_CORE.get()),
                    ModItems.ABYSSAL_FRAGMENT, ModItems.EYE_CRYSTAL, 2, stack -> new ItemStack(ModItems.AWAKENED_EYE_CORE.get())));

    private AltarRituals() {
    }

    /** O item pode ir para o altar (ainda tem algo a despertar). */
    public static boolean accepts(ItemStack stack) {
        return RITUALS.stream().anyMatch(r -> r.input().test(stack));
    }

    @Nullable
    public static Ritual forInput(ItemStack stack) {
        return RITUALS.stream().filter(r -> r.input().test(stack)).findFirst().orElse(null);
    }
}
