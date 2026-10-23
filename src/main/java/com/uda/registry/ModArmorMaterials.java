package com.uda.registry;

import java.util.EnumMap;
import java.util.List;

import com.uda.UltimateDarkAdventure;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, UltimateDarkAdventure.MODID);

    /** Entre diamante e netherite. A textura muda com a forma (veja OcularArmorItem#getArmorTexture). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> OCULAR = ARMOR_MATERIALS.register("ocular", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 11);
            }),
            18, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ModItems.EYE_INGOT.get()),
            List.of(new ArmorMaterial.Layer(UltimateDarkAdventure.id("ocular_sealed"))), 2.5F, 0.05F));

    private ModArmorMaterials() {
    }
}
