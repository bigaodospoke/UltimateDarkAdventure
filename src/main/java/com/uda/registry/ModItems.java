package com.uda.registry;

import com.uda.UltimateDarkAdventure;
import com.uda.item.AgamotoItem;
import com.uda.item.GrimoireItem;
import com.uda.item.LoreItem;
import com.uda.item.ModTiers;
import com.uda.item.OcularArmorItem;
import com.uda.item.OcularAxeItem;
import com.uda.item.OcularBowItem;
import com.uda.item.OcularDiggerItems;
import com.uda.item.OcularShieldItem;
import com.uda.item.OcularSwordItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UltimateDarkAdventure.MODID);

    // ---------------------------------------------------------------- materiais (na ordem da progressão)

    /** Cai do Minério do Olho; derretido vira Lingote Ocular. */
    public static final DeferredItem<LoreItem> RAW_EYE_FRAGMENT = lore("raw_eye_fragment", Rarity.COMMON, false);
    /** Cai às vezes do Minério do Olho: núcleos, altar e despertares. */
    public static final DeferredItem<LoreItem> EYE_CRYSTAL = lore("eye_crystal", Rarity.UNCOMMON, false);
    public static final DeferredItem<LoreItem> EYE_INGOT = lore("eye_ingot", Rarity.COMMON, false);
    /** Item central: catalisador do primeiro despertar. */
    public static final DeferredItem<LoreItem> EYE_CORE = lore("eye_core", Rarity.UNCOMMON, false);
    /** Núcleo despertado no altar com um Fragmento Abissal: invoca o Dragão Abissal. */
    public static final DeferredItem<LoreItem> AWAKENED_EYE_CORE = lore("awakened_eye_core", Rarity.RARE, true);
    /** Do Vigia Ancestral (Hollow Watcher especial): libera o Altar do Olho. */
    public static final DeferredItem<LoreItem> WATCHER_EYE = lore("watcher_eye", Rarity.RARE, false);
    /** Do Blind Executioner: Grimório, Núcleo Desperto e o Santuário Abissal. */
    public static final DeferredItem<LoreItem> ABYSSAL_FRAGMENT = lore("abyssal_fragment", Rarity.UNCOMMON, false);
    /** Do Dragão Abissal: despertar supremo. */
    public static final DeferredItem<LoreItem> ABYSSAL_SCALE = lore("abyssal_scale", Rarity.RARE, false);
    /** Do Dragão Abissal: desperta O Olho Negro na Catedral do Vazio. */
    public static final DeferredItem<LoreItem> ABYSSAL_HEART = lore("abyssal_heart", Rarity.EPIC, true);
    /** Do Olho Negro: catalisador do despertar supremo. */
    public static final DeferredItem<LoreItem> VOID_ESSENCE = lore("void_essence", Rarity.EPIC, true);

    // ---------------------------------------------------------------- armas e ferramentas (modelos 3D em tools/modelos)

    public static final DeferredItem<OcularSwordItem> EYE_SWORD = ITEMS.register("eye_sword",
            () -> new OcularSwordItem(ModTiers.OCULAR, ocular().attributes(SwordItem.createAttributes(ModTiers.OCULAR, 3, -2.4F))));

    public static final DeferredItem<OcularAxeItem> EYE_AXE = ITEMS.register("eye_axe",
            () -> new OcularAxeItem(ModTiers.OCULAR, ocular().attributes(AxeItem.createAttributes(ModTiers.OCULAR, 4.0F, -3.0F))));

    public static final DeferredItem<OcularDiggerItems.Pickaxe> EYE_PICKAXE = ITEMS.register("eye_pickaxe",
            () -> new OcularDiggerItems.Pickaxe(ModTiers.OCULAR, ocular().attributes(PickaxeItem.createAttributes(ModTiers.OCULAR, 1.0F, -2.8F))));

    public static final DeferredItem<OcularDiggerItems.Shovel> EYE_SHOVEL = ITEMS.register("eye_shovel",
            () -> new OcularDiggerItems.Shovel(ModTiers.OCULAR, ocular().attributes(ShovelItem.createAttributes(ModTiers.OCULAR, 1.5F, -3.0F))));

    public static final DeferredItem<OcularBowItem> EYE_BOW = ITEMS.register("eye_bow",
            () -> new OcularBowItem(ocular().durability(512)));

    public static final DeferredItem<OcularShieldItem> EYE_SHIELD = ITEMS.register("eye_shield",
            () -> new OcularShieldItem(ocular().durability(504)));

    public static final DeferredItem<GrimoireItem> EYE_GRIMOIRE = ITEMS.register("eye_grimoire",
            () -> new GrimoireItem(ocular().durability(300)));

    // ---------------------------------------------------------------- armadura e amuleto

    public static final DeferredItem<OcularArmorItem> EYE_HELMET = armor("eye_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<OcularArmorItem> EYE_CHESTPLATE = armor("eye_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<OcularArmorItem> EYE_LEGGINGS = armor("eye_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<OcularArmorItem> EYE_BOOTS = armor("eye_boots", ArmorItem.Type.BOOTS);

    /** O pingente do Blind Executioner. */
    public static final DeferredItem<AgamotoItem> AGAMOTO_EYE = ITEMS.register("agamoto_eye",
            () -> new AgamotoItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---------------------------------------------------------------- blocos

    public static final DeferredItem<BlockItem> EYE_ORE = block(ModBlocks.EYE_ORE, Rarity.COMMON);
    public static final DeferredItem<BlockItem> DEEPSLATE_EYE_ORE = block(ModBlocks.DEEPSLATE_EYE_ORE, Rarity.COMMON);
    public static final DeferredItem<BlockItem> EYE_ALTAR = block(ModBlocks.EYE_ALTAR, Rarity.RARE);
    public static final DeferredItem<BlockItem> ABYSSAL_ALTAR = block(ModBlocks.ABYSSAL_ALTAR, Rarity.EPIC);
    public static final DeferredItem<BlockItem> VOID_ALTAR = block(ModBlocks.VOID_ALTAR, Rarity.EPIC);

    // ---------------------------------------------------------------- ovos

    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_WATCHER_SPAWN_EGG = ITEMS.register("hollow_watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HOLLOW_WATCHER, 0x14121A, 0xA62BFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BLIND_EXECUTIONER_SPAWN_EGG = ITEMS.register("blind_executioner_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLIND_EXECUTIONER, 0x1E1626, 0xD25CFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ABYSSAL_DRAGON_SPAWN_EGG = ITEMS.register("abyssal_dragon_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ABYSSAL_DRAGON, 0x0A0810, 0x9B30FF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<DeferredSpawnEggItem> BLACK_EYE_SPAWN_EGG = ITEMS.register("black_eye_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLACK_EYE, 0x050407, 0xE28BFF, new Item.Properties().rarity(Rarity.EPIC)));

    private static Item.Properties ocular() {
        return new Item.Properties().rarity(Rarity.UNCOMMON);
    }

    private static DeferredItem<LoreItem> lore(String name, Rarity rarity, boolean glint) {
        return ITEMS.register(name, () -> new LoreItem(new Item.Properties().rarity(rarity), glint));
    }

    private static DeferredItem<OcularArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new OcularArmorItem(ModArmorMaterials.OCULAR, type,
                ocular().durability(type.getDurability(35))));
    }

    private static DeferredItem<BlockItem> block(DeferredBlock<? extends Block> block, Rarity rarity) {
        return ITEMS.registerSimpleBlockItem(block, new Item.Properties().rarity(rarity));
    }

    private ModItems() {
    }
}
