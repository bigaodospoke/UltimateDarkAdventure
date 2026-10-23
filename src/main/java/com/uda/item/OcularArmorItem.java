package com.uda.item;

import java.util.List;

import com.uda.UltimateDarkAdventure;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Armadura Ocular. A textura acompanha a forma (selada / desperta / suprema). Com as quatro peças
 * despertas: Visão do Olho (visão noturna e inimigos próximos marcados). Supremas: também resistência
 * e imunidade a definhamento e escuridão. O bônus é aplicado em ModEvents.
 */
public class OcularArmorItem extends ArmorItem implements OcularItem {
    private static final String[] FORMS = {"sealed", "awakened", "supreme"};

    public OcularArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String powerKey() {
        return "eye_armor";
    }

    @Override
    public boolean opensWithKey() {
        return false;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return UltimateDarkAdventure.id("textures/models/armor/ocular_" + FORMS[Ocular.tier(stack)] + "_layer_" + (innerModel ? 2 : 1) + ".png");
    }

    /** Quantas peças despertas (ou supremas, se supreme) o jogador está vestindo. */
    public static int countWorn(Player player, int minTier) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof OcularArmorItem && Ocular.tier(stack) >= minTier) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Ocular.appendTooltip(stack, this, tooltip);
    }
}
