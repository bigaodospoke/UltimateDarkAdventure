package com.uda.client;

import com.uda.UltimateDarkAdventure;
import com.uda.client.aura.AurasDosMobs;
import com.uda.client.model.BlindExecutionerModel;
import com.uda.client.model.HollowWatcherModel;
import com.uda.client.renderer.BlindExecutionerRenderer;
import com.uda.client.renderer.EnergySlashRenderer;
import com.uda.client.renderer.EyeAltarRenderer;
import com.uda.client.renderer.EyeArmorGlowLayer;
import com.uda.client.renderer.EyeArrowRenderer;
import com.uda.client.model.GeoModels;
import com.uda.client.renderer.AbyssalDragonRenderer;
import com.uda.client.renderer.BlackEyeRenderer;
import com.uda.client.renderer.GlowingEyeRenderer;
import com.uda.client.renderer.HollowWatcherRenderer;
import com.uda.client.renderer.SummonAltarRenderer;
import com.uda.client.ui.EyeHudLayer;
import com.uda.client.ui.InterfaceEvents;
import com.uda.client.ui.InterfaceTheme;
import com.uda.entity.Awakenable;
import com.uda.item.Ocular;
import com.uda.item.OcularDiggerItems;
import com.uda.network.AwakenItemPayload;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModEntities;
import com.uda.registry.ModItems;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = UltimateDarkAdventure.MODID, dist = Dist.CLIENT)
public class UltimateDarkAdventureClient {
    /** 1 quando o item foi despertado pelo jogador, ou quando quem o segura (um mob do Olho) está desperto. */
    private static final ItemPropertyFunction AWAKENED = (stack, level, entity, seed) ->
            Ocular.isAwakened(stack) || entity instanceof Awakenable awakenable && awakenable.getAwakenProgress(1.0F) >= 0.5F ? 1.0F : 0.0F;

    /** Forma liberada no altar: 0 selada, 1 desperta, 2 suprema. */
    private static final ItemPropertyFunction TIER = (stack, level, entity, seed) -> Ocular.tier(stack);

    public static final KeyMapping AWAKEN_KEY = new KeyMapping("key.uda.awaken", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.uda");

    public UltimateDarkAdventureClient(IEventBus modEventBus) {
        modEventBus.addListener(UltimateDarkAdventureClient::registerLayers);
        modEventBus.addListener(UltimateDarkAdventureClient::registerRenderers);
        modEventBus.addListener(UltimateDarkAdventureClient::clientSetup);
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(AWAKEN_KEY));
        NeoForge.EVENT_BUS.addListener(UltimateDarkAdventureClient::awakenKey);

        // Aura dos mobs despertos (motor adaptado do mod Auras)
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AurasDosMobs.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(UltimateDarkAdventureClient::renderAuras);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            AurasDosMobs.limpar();
            OreHighlights.clear();
        });
        modEventBus.addListener(UltimateDarkAdventureClient::addLayers);
        // Detecção de minérios da Picareta Ocular (o item chama este gancho no cliente)
        OcularDiggerItems.oreScanHook = OreHighlights::scan;

        // Interface temática: pacote de recursos embutido + desenhos por código
        InterfaceTheme.register(modEventBus);
        InterfaceEvents.register();
        modEventBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.HOTBAR, EyeHudLayer.ID, new EyeHudLayer()));
    }

    private static void awakenKey(ClientTickEvent.Post event) {
        while (AWAKEN_KEY.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                PacketDistributor.sendToServer(AwakenItemPayload.INSTANCE);
            }
        }
    }

    private static void renderAuras(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            AurasDosMobs.desenhar(event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(true), event.getFrustum());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            OreHighlights.render(event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        }
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.HOLLOW_WATCHER, HollowWatcherModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.BLIND_EXECUTIONER, BlindExecutionerModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.ABYSSAL_DRAGON, () -> GeoModels.load("abyssal_dragon"));
        event.registerLayerDefinition(ModModelLayers.BLACK_EYE, () -> GeoModels.load("black_eye"));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOW_WATCHER.get(), HollowWatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.BLIND_EXECUTIONER.get(), BlindExecutionerRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_ARROW.get(), EyeArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_ORB.get(), GlowingEyeRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_BOMB.get(), GlowingEyeRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_DRAGON.get(), AbyssalDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_EYE.get(), BlackEyeRenderer::new);
        event.registerEntityRenderer(ModEntities.ENERGY_SLASH.get(), EnergySlashRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.EYE_ALTAR.get(), EyeAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SUMMON_ALTAR.get(), SummonAltarRenderer::new);
    }

    /** Brilho da Armadura Ocular desperta nos jogadores e nos suportes de armadura. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet models = event.getEntityModels();
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                renderer.addLayer(new EyeArmorGlowLayer(renderer,
                        new HumanoidModel<>(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR))));
            }
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer renderer) {
            renderer.addLayer(new EyeArmorGlowLayer(renderer, new ArmorStandArmorModel(models.bakeLayer(ModelLayers.ARMOR_STAND_INNER_ARMOR)),
                    new ArmorStandArmorModel(models.bakeLayer(ModelLayers.ARMOR_STAND_OUTER_ARMOR))));
        }
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation awakened = UltimateDarkAdventure.id("awakened");
            ResourceLocation tier = UltimateDarkAdventure.id("tier");
            for (Item item : List.of(ModItems.EYE_SWORD.get(), ModItems.EYE_AXE.get(), ModItems.EYE_PICKAXE.get(), ModItems.EYE_SHOVEL.get(),
                    ModItems.EYE_BOW.get(), ModItems.EYE_SHIELD.get(), ModItems.EYE_GRIMOIRE.get())) {
                ItemProperties.register(item, awakened, AWAKENED);
                ItemProperties.register(item, tier, TIER);
            }
            // armadura e amuleto mudam de cara pela forma (não abrem/fecham)
            for (Item item : List.of(ModItems.EYE_HELMET.get(), ModItems.EYE_CHESTPLATE.get(), ModItems.EYE_LEGGINGS.get(), ModItems.EYE_BOOTS.get(),
                    ModItems.AGAMOTO_EYE.get())) {
                ItemProperties.register(item, tier, TIER);
            }
            ItemProperties.register(ModItems.EYE_SHIELD.get(), ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            // O arco vanilla registra "pull"/"pulling" só para Items.BOW; o nosso precisa dos mesmos.
            ItemProperties.register(ModItems.EYE_BOW.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                    entity == null || entity.getUseItem() != stack ? 0.0F
                            : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
            ItemProperties.register(ModItems.EYE_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }
}
