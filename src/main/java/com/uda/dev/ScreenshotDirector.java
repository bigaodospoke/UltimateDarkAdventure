package com.uda.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.mojang.blaze3d.platform.NativeImage;
import com.uda.UltimateDarkAdventure;
import com.uda.client.aura.render.RenderAuras;
import com.uda.entity.AbstractEyeBoss;
import com.uda.entity.AbstractEyeMonster;

import net.minecraft.Util;
import com.uda.item.Ocular;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * SÓ PARA DESENVOLVIMENTO (fica fora do jar: veja build.gradle).
 * Com -Duda.screenshots=true (tarefa runScreenshots) cria um mundo plano, monta as cenas
 * com os mobs, tira prints e fecha o jogo. Os prints vão para run-screenshots/screenshots.
 */
@Mod(value = UltimateDarkAdventure.MODID, dist = Dist.CLIENT)
public class ScreenshotDirector {
    private static final String TAG = "[UDA-SCREENSHOTS] ";
    private static final String BOW = "HandItems:[{id:\"uda:eye_bow\",count:1},{}]";
    private static final String AXES = "HandItems:[{id:\"uda:eye_axe\",count:1},{id:\"uda:eye_axe\",count:1}]";

    private boolean worldRequested;
    private int ticks = -1;
    private int frame;
    private int lastRenderedFrame = -1;

    public ScreenshotDirector() {
        if (Boolean.getBoolean("uda.screenshots")) {
            NeoForge.EVENT_BUS.addListener(this::onClientTick);
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!this.worldRequested && mc.screen instanceof TitleScreen) {
            this.worldRequested = true;
            createWorld(mc);
            return;
        }
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        this.ticks++;
        if (this.ticks >= PROGRESSION_START) {
            this.progression(mc);
            return;
        }
        switch (this.ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                run(mc, "gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "gamerule doMobSpawning false", "gamerule doWeatherCycle false",
                        "time set noon", "weather clear", "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]");
                lineup(mc);
                run(mc, "tp @s 0.5 -60 -5.5 0 12");
                // -PcenaInicial=N (gradle) pula direto para a cena que começa no tick N
                int from = Integer.getInteger("uda.screenshots.from", 0);
                if (from > this.ticks) {
                    this.ticks = from - 1;
                }
            }
            case 90 -> shot(mc, "1_dia_lado_a_lado");
            case 95 -> run(mc, "time set midnight");
            case 140 -> shot(mc, "2_noite_lado_a_lado");
            case 145 -> run(mc, "time set noon", "tp @s -0.5 -59.4 -1.6 0 6");
            case 185 -> shot(mc, "3_hollow_watcher_perto");
            case 190 -> run(mc, "tp @s 3.0 -59.4 -1.4 0 6");
            case 230 -> shot(mc, "4_blind_executioner_perto");
            case 235 -> run(mc, "time set midnight", "tp @s 1.0 -59.4 -2.6 0 6");
            case 275 -> shot(mc, "5_despertos_no_escuro");
            case 280 -> {
                // Cena de ação: IA ligada, jogador em sobrevivência (imune), para testar o despertar de verdade.
                run(mc, "time set noon", "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "gamemode survival",
                        "effect give @s minecraft:resistance infinite 4 true", "effect give @s minecraft:regeneration infinite 4 true",
                        "kill @e[type=item]", "kill @e[type=experience_orb]",
                        "tp @s 0.5 -60 -3.5 0 8",
                        "summon uda:hollow_watcher -2.5 -60 9.5 {PersistenceRequired:1b," + BOW + "}",
                        "summon uda:blind_executioner 3.5 -60 7.5 {PersistenceRequired:1b," + AXES + "}");
            }
            case 300 -> run(mc, "damage @e[type=uda:hollow_watcher,limit=1] 16 minecraft:player_attack by @s");
            case 340 -> {
                shot(mc, "6_acao_watcher_desperta");
                logStates(mc, "depois de ver o jogador");
            }
            case 345 -> {
                // Afasta a câmera e segura o Executioner no lugar para fotografar a ascensão.
                run(mc, "tp @s 0.5 -59.5 -4.5 0 4",
                        "tp @e[type=uda:blind_executioner,limit=1] 0.5 -60 3.5 180 0",
                        "effect give @e[type=uda:blind_executioner] minecraft:slowness infinite 255 true",
                        "damage @e[type=uda:blind_executioner,limit=1] 34 minecraft:player_attack by @s");
            }
            case 360 -> shot(mc, "7_executioner_ascendendo");
            case 420 -> {
                shot(mc, "8_executioner_ascendido");
                logStates(mc, "depois do dano");
            }
            // --- Aura: cada mob desperta de noite, com a câmera parada (quadros para GIF)
            case 440 -> {
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "gamemode creative", "effect clear @s", "time set midnight");
                run(mc, "kill @e[type=item]", "kill @e[type=experience_orb]", "tp @s 0.5 -59.3 -4.2 0 8");
                solo(mc, "uda:hollow_watcher", BOW);
            }
            case 490 -> run(mc, "data merge entity @e[type=uda:hollow_watcher,limit=1] {Awakened:1b}");
            case 565 -> shot(mc, "9_aura_watcher_noite");
            case 570 -> {
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "kill @e[type=item]", "tp @s 0.5 -59.1 -5.2 0 8");
                solo(mc, "uda:blind_executioner", AXES);
            }
            case 620 -> run(mc, "data merge entity @e[type=uda:blind_executioner,limit=1] {Awakened:1b}");
            case 705 -> shot(mc, "10_aura_executioner_noite");
            case 710 -> run(mc, "time set noon");
            case 740 -> shot(mc, "11_aura_executioner_dia");
            // --- Armas e ferramentas: vitrine, primeira/terceira pessoa, inventário e o orbe do grimório
            case 760 -> {
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "kill @e[type=item]", "time set noon", "gamemode survival", "clear @s",
                        "effect give @s minecraft:resistance infinite 4 true", "effect give @s minecraft:saturation infinite 4 true",
                        "fill -5 -60 6 5 -55 6 minecraft:polished_blackstone_bricks");
                for (int i = 0; i < ITEMS.length; i++) {
                    run(mc, "item replace entity @s hotbar." + i + " with " + ITEMS[i],
                            "item replace entity @s inventory." + i + " with " + ITEMS[i] + "[uda:tier=1,uda:awakened=true]",
                            frame(i - 3, -57, ITEMS[i], false), frame(i - 3, -58, ITEMS[i], true));
                }
                run(mc, "tp @s 0.5 -60 1.5 0 -4");
                mc.player.getInventory().selected = 0;
            }
            case 800 -> shot(mc, "12_vitrine_dia");
            case 805 -> run(mc, "time set midnight");
            case 845 -> shot(mc, "13_vitrine_noite");
            case 850 -> {
                mc.options.hideGui = false;
                run(mc, "time set noon", "tp @s 0.5 -60 -2.5 180 10",
                        "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=1,uda:awakened=true]",
                        "item replace entity @s weapon.offhand with uda:eye_shield[uda:tier=1,uda:awakened=true]");
            }
            case 875 -> shot(mc, "14_primeira_pessoa_espada_escudo");
            case 880 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_pickaxe",
                    "item replace entity @s weapon.offhand with uda:eye_grimoire");
            case 900 -> shot(mc, "15_primeira_pessoa_picareta_grimorio");
            case 905 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_bow[uda:tier=1,uda:awakened=true]",
                    "item replace entity @s weapon.offhand with uda:eye_axe[uda:tier=1,uda:awakened=true]");
            case 925 -> shot(mc, "16_primeira_pessoa_arco_machado");
            case 930 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                run(mc, "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=1,uda:awakened=true]",
                        "item replace entity @s weapon.offhand with uda:eye_shield[uda:tier=1,uda:awakened=true]");
            }
            case 955 -> shot(mc, "17_terceira_pessoa");
            case 960 -> run(mc, "time set midnight");
            case 995 -> shot(mc, "18_terceira_pessoa_noite");
            case 1000 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                mc.options.guiScale().set(4);
                mc.resizeDisplay();
                run(mc, "time set noon");
                mc.setScreen(new InventoryScreen(mc.player));
            }
            case 1020 -> shot(mc, "19_inventario");
            case 1025 -> {
                mc.setScreen(null);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.options.hideGui = true;
                run(mc, "time set midnight", "tp @s 0.5 -60 -2.5 0 0",
                        "summon uda:eye_orb 0.5 -58.6 0.8 {Motion:[0.0,0.0,-0.01],Awakened:1b}");
            }
            case 1045 -> shot(mc, "20_orbe_ocular");
            case 1046 -> run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "fill -5 -60 6 5 -55 6 minecraft:air");
            case 1050 -> {
                // Comparação com os itens vanilla (orientação nas duas mãos), em suportes de armadura
                run(mc, "time set noon", "kill @e[type=item]", "kill @e[type=experience_orb]", "tp @s 0.5 -59.4 -3.5 0 0");
                run(mc, stand(-3.5, "minecraft:diamond_sword"), stand(-1.5, "uda:eye_sword"),
                        stand(1.5, "minecraft:bow"), stand(3.5, "uda:eye_bow"),
                        stand(-5.5, "uda:eye_grimoire", AWAKENED_NBT), stand(5.5, "uda:eye_shield", AWAKENED_NBT));
            }
            case 1085 -> shot(mc, "21_comparacao_frente");
            case 1090 -> run(mc, "tp @s 7.5 -59.4 1.5 90 0");
            case 1120 -> shot(mc, "22_comparacao_lado");
            case 1121 -> {
                // ícones: vanilla e os nossos lado a lado no inventário
                String[] icons = {"minecraft:diamond_sword", "uda:eye_sword", "minecraft:iron_axe", "uda:eye_axe", "minecraft:bow", "uda:eye_bow",
                        "minecraft:iron_pickaxe", "uda:eye_pickaxe", "minecraft:shield"};
                for (int i = 0; i < icons.length; i++) {
                    run(mc, "item replace entity @s hotbar." + i + " with " + icons[i]);
                }
                run(mc, "item replace entity @s inventory.0 with uda:eye_shovel", "item replace entity @s inventory.1 with uda:eye_shield",
                        "item replace entity @s inventory.2 with uda:eye_grimoire", "item replace entity @s inventory.3 with minecraft:iron_shovel");
                run(mc, "gamemode survival");
            }
            case 1124 -> {
                mc.options.hideGui = false;
                mc.options.guiScale().set(4);
                mc.resizeDisplay();
                mc.setScreen(new InventoryScreen(mc.player));
            }
            case 1130 -> shot(mc, "23_comparacao_icones");
            case 1132 -> {
                mc.setScreen(null);
                mc.options.hideGui = false;
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                run(mc, "tp @s 0.5 -60 -3.5 180 0", "item replace entity @s weapon.mainhand with minecraft:bow",
                        "item replace entity @s weapon.offhand with uda:eye_bow");
            }
            case 1150 -> shot(mc, "24_pp_arco_vanilla_e_nosso");
            case 1152 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_bow", "item replace entity @s weapon.offhand with minecraft:bow");
            case 1170 -> shot(mc, "25_pp_nosso_e_arco_vanilla");
            case 1172 -> run(mc, "item replace entity @s weapon.mainhand with minecraft:diamond_sword", "item replace entity @s weapon.offhand with uda:eye_sword");
            case 1190 -> shot(mc, "26_pp_espada_vanilla_e_nossa");
            case 1192 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_sword", "item replace entity @s weapon.offhand with minecraft:diamond_sword");
            case 1210 -> shot(mc, "27_pp_nossa_e_espada_vanilla");
            case 1212 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_grimoire[uda:tier=1,uda:awakened=true]",
                    "item replace entity @s weapon.offhand with uda:eye_shield[uda:tier=1,uda:awakened=true]");
            case 1230 -> shot(mc, "28_pp_grimorio_escudo");
            case 1232 -> {
                // segura o botão direito: a espada não tem uso, então o escudo da outra mão bloqueia
                run(mc, "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=1,uda:awakened=true]");
                KeyMapping.set(mc.options.keyUse.getKey(), true);
            }
            case 1250 -> shot(mc, "29_pp_bloqueando");
            case 1252 -> {
                KeyMapping.set(mc.options.keyUse.getKey(), false);
                run(mc, "item replace entity @s weapon.mainhand with uda:eye_bow[uda:tier=1,uda:awakened=true]",
                        "item replace entity @s weapon.offhand with minecraft:arrow");
            }
            case 1255 -> KeyMapping.set(mc.options.keyUse.getKey(), true);
            case 1285 -> shot(mc, "30_pp_puxando_arco");
            case 1287 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.hideGui = true;
            }
            case 1300 -> shot(mc, "31_tp_puxando_arco");
            case 1302 -> {
                KeyMapping.set(mc.options.keyUse.getKey(), false);
                run(mc, "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=1,uda:awakened=true]",
                        "item replace entity @s weapon.offhand with uda:eye_shield[uda:tier=1,uda:awakened=true]");
            }
            case 1305 -> KeyMapping.set(mc.options.keyUse.getKey(), true);
            case 1325 -> shot(mc, "32_tp_bloqueando");
            case 1327 -> KeyMapping.set(mc.options.keyUse.getKey(), false);

            // --- Bosses: dormente de dia, despertando (GIF), desperto de noite e combate (GIF)
            case 1400 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = true;
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "kill @e[type=item]", "time set noon", "gamemode spectator", "clear @s",
                        "fill -5 -60 6 5 -55 6 minecraft:air", "tp @s -12 -54 6 -90 0",
                        "summon uda:abyssal_dragon 0.5 -56 14 {NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]}",
                        "execute as @e[type=uda:abyssal_dragon] at @s run tp @s ~ ~ ~ 90 0");
            }
            case 1440 -> shot(mc, "33_dragao_dormente");
            case 1445 -> run(mc, "data merge entity @e[type=uda:abyssal_dragon,limit=1] {Awakened:1b}");
            case 1530 -> run(mc, "time set midnight", "tp @s -14 -50 2 -50 18");
            case 1565 -> shot(mc, "34_dragao_desperto_noite");
            case 1570 -> run(mc, "time set noon", "tp @s -14 -50 2 -50 18");
            case 1600 -> shot(mc, "35_dragao_frente");
            case 1605 -> run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "tp @s 0.5 -54.5 -2 0 0",
                    "summon uda:black_eye 0.5 -56 10 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}",
                    "execute as @e[type=uda:black_eye] at @s run tp @s ~ ~ ~ 180 0");
            case 1640 -> shot(mc, "36_olho_negro_dormente");
            case 1645 -> run(mc, "data merge entity @e[type=uda:black_eye,limit=1] {Awakened:1b}");
            case 1730 -> run(mc, "time set midnight");
            case 1765 -> shot(mc, "37_olho_negro_desperto_noite");
            case 1770 -> {
                // combate de verdade: IA ligada, jogador em sobrevivência (imune), a câmera acompanha o boss
                // o alvo é um golem (o jogador fica de espectador ao lado, sem levar o raio na cara)
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "time set noon", "gamemode spectator", "tp @s -24 -53 -20 -55 -5",
                        "summon iron_golem 0.5 -60 -2 {NoAI:1b,PersistenceRequired:1b,active_effects:[{id:\"minecraft:resistance\",amplifier:4b,duration:-1},{id:\"minecraft:regeneration\",amplifier:4b,duration:-1}]}", "summon uda:black_eye 0.5 -52 14 {PersistenceRequired:1b}");
            }
            case 1980 -> shot(mc, "38_olho_negro_combate");
            case 1985 -> run(mc, "data merge entity @e[type=uda:black_eye,limit=1] {Health:150f}");
            case 2200 -> shot(mc, "39_olho_negro_desperto_combate");
            case 2205 -> run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "kill @e[type=item]", "tp @s -30 -50 -28 -45 -5",
                    "summon iron_golem 0.5 -60 -2 {NoAI:1b,PersistenceRequired:1b,active_effects:[{id:\"minecraft:resistance\",amplifier:4b,duration:-1},{id:\"minecraft:regeneration\",amplifier:4b,duration:-1}]}", "summon uda:abyssal_dragon 0.5 -48 20 {PersistenceRequired:1b}");
            case 2420 -> shot(mc, "40_dragao_combate");
            case 2425 -> run(mc, "data merge entity @e[type=uda:abyssal_dragon,limit=1] {Health:120f}");
            case 2640 -> shot(mc, "41_dragao_desperto_combate");
            case 2660 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                UltimateDarkAdventure.LOGGER.info(TAG + "fim");
                mc.stop();
            }

            // --- Inspeção (gradlew runScreenshots -PcenaInicial=3000): bosses parados, de perto e de vários ângulos
            case 3000 -> {
                mc.options.hideGui = true;
                run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "kill @e[type=item]", "time set noon", "gamemode spectator",
                        "summon uda:black_eye 0.5 -56 0.5 {NoAI:1b,PersistenceRequired:1b}",
                        "execute as @e[type=uda:black_eye] at @s run tp @s ~ ~ ~ 180 0", "tp @s 0.5 -56 -9 0 0");
            }
            case 3040 -> shot(mc, "insp_olho_frente");
            case 3042 -> run(mc, "tp @s 9.5 -56 0.5 90 0");
            case 3060 -> shot(mc, "insp_olho_lado");
            case 3062 -> run(mc, "tp @s 0.5 -56 9.5 180 0");
            case 3080 -> shot(mc, "insp_olho_tras");
            case 3082 -> run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "summon uda:abyssal_dragon 0.5 -55 0.5 {NoAI:1b,PersistenceRequired:1b}",
                    "execute as @e[type=uda:abyssal_dragon] at @s run tp @s ~ ~ ~ 180 0", "tp @s 18.5 -54 0.5 90 0");
            case 3120 -> shot(mc, "insp_dragao_lado");
            case 3122 -> run(mc, "tp @s 0.5 -54 -17 0 0");
            case 3140 -> shot(mc, "insp_dragao_frente");
            case 3142 -> run(mc, "tp @s 13 -42 13 135 40");
            case 3160 -> shot(mc, "insp_dragao_cima");
            case 3162 -> run(mc, "tp @s -12 -59 -12 -45 -15");
            case 3180 -> shot(mc, "insp_dragao_baixo_tras");
            case 3200 -> mc.stop();
            default -> {
            }
        }
        if (this.ticks >= 480 && this.ticks < 560) {
            this.record(mc, "watcher_desperta", 480);
        } else if (this.ticks >= 610 && this.ticks < 700) {
            this.record(mc, "executioner_ascende", 610);
        } else if (this.ticks >= 1442 && this.ticks < 1525) {
            this.record(mc, "dragao_desperta", 1442);
        } else if (this.ticks >= 1642 && this.ticks < 1725) {
            this.record(mc, "olho_negro_desperta", 1642);
        } else if (this.ticks >= 1985 && this.ticks < 2185) {
            this.record(mc, "olho_negro_combate", 1985);
        } else if (this.ticks >= 2425 && this.ticks < 2625) {
            this.record(mc, "dragao_combate", 2425);
        }
        if (this.ticks > 1770 && this.ticks < 2660) {
            this.lookAtBoss(mc);
            if (this.ticks % 10 == 0) {
                run(mc, "effect clear @s minecraft:darkness");
            }
            if (this.ticks % 40 == 0) {
                logBosses(mc);
            }
        }
    }

    private int lastAttack = -1;

    /** No combate a câmera (o jogador) acompanha o boss suavemente; cada ataque escolhido vai para o log. */
    private void lookAtBoss(Minecraft mc) {
        AbstractEyeBoss boss = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractEyeBoss found) {
                boss = found;
            }
        }
        if (boss == null) {
            return;
        }
        if (boss.getAttack() != this.lastAttack) {
            this.lastAttack = boss.getAttack();
            UltimateDarkAdventure.LOGGER.info(TAG + "{} ataque={} desperto={} vida={}", boss.getType().toShortString(), this.lastAttack,
                    boss.isAwakened(), (int) boss.getHealth());
        }
        Vec3 to = boss.position().add(0.0, boss.getBbHeight() * 0.5, 0.0).subtract(mc.player.getEyePosition());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        mc.player.setYRot(Mth.approachDegrees(mc.player.getYRot(), yaw, 6.0F));
        mc.player.setXRot(Mth.approachDegrees(mc.player.getXRot(), Mth.clamp(pitch, -60.0F, 40.0F), 4.0F));
    }

    private static final String[] ITEMS = {"uda:eye_sword", "uda:eye_axe", "uda:eye_pickaxe", "uda:eye_shovel", "uda:eye_bow",
            "uda:eye_shield", "uda:eye_grimoire"};

    /** Moldura invisível presa na parede (z = 6), virada para o jogador. */
    private static String frame(int x, int y, String item, boolean awakened) {
        String components = awakened ? AWAKENED_NBT : "";
        return "summon item_frame " + x + " " + y + " 5 {Facing:2b,Invisible:1b,Fixed:1b,Item:{id:\"" + item + "\",count:1" + components + "}}";
    }

    /** Suporte de armadura com braços segurando o mesmo item nas duas mãos, virado para o norte. */
    /** Componentes (formato NBT) de uma arma desperta no altar e com o olho aberto. */
    private static final String AWAKENED_NBT = ",components:{\"uda:tier\":1,\"uda:awakened\":true}";

    private static String stand(double x, String item) {
        return stand(x, item, "");
    }

    private static String stand(double x, String item, String components) {
        String hand = "{id:\"" + item + "\",count:1" + components + "}";
        return "summon armor_stand " + x + " -60 1.5 {ShowArms:1b,NoBasePlate:1b,Rotation:[180f,0f],HandItems:[" + hand + "," + hand + "]}";
    }

    /** Um mob parado e dormente no centro da cena, de frente para a câmera. */
    private static void solo(Minecraft mc, String type, String hands) {
        run(mc, "summon " + type + " 0.5 -60 0.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]," + hands + "}",
                "execute as @e[type=!player] at @s run tp @s ~ ~ ~ 180 0");
    }

    /** Grava um quadro por frame desenhado em run-screenshots/quadros/<nome>/ (vira GIF depois). */
    // ================================================================ progressão (gradlew runScreenshots -PcenaInicial=4000)

    private static final int PROGRESSION_START = 4000;
    private static final BlockPos ALTAR = new BlockPos(0, -60, 2);
    private static final BlockPos SUMMON = new BlockPos(0, -60, 40);

    /** Testa o caminho real do jogador: altar, poderes, armadura, estruturas, invocações e a fase enfurecida. */
    private void progression(Minecraft mc) {
        int t = this.ticks;
        switch (t) {
            // --- Altar do Olho: põe a espada selada, usa o Núcleo (com o cristal no inventário) e pega de volta
            case 4000 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(mc, "gamerule sendCommandFeedback false", "gamerule doMobSpawning false", "gamerule doDaylightCycle false",
                        "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "time set noon", "gamemode survival", "clear @s",
                        "effect give @s minecraft:resistance infinite 4 true", "effect give @s minecraft:saturation infinite 4 true",
                        "fill -8 -60 -8 8 -50 8 air", "setblock 0 -60 2 uda:eye_altar", "tp @s 0.5 -59.6 -2 0 12",
                        "item replace entity @s weapon.mainhand with uda:eye_sword");
            }
            case 4010 -> useOn(mc, ALTAR);
            case 4014 -> run(mc, "item replace entity @s weapon.mainhand with uda:eye_core", "give @s uda:eye_crystal 1");
            case 4020 -> useOn(mc, ALTAR);
            case 4024 -> logPlayer(mc, "ritual começou (cristal deve ter sido gasto)");
            case 4060 -> shot(mc, "42_altar_ritual");
            case 4100 -> shot(mc, "43_altar_rachando");
            case 4111 -> shot(mc, "44_altar_olho_abrindo");
            case 4135 -> run(mc, "item replace entity @s weapon.mainhand with air");
            case 4138 -> useOn(mc, ALTAR);
            case 4142 -> logPlayer(mc, "espada tirada do altar");

            // --- poderes (terceira pessoa, contra zumbis parados)
            case 4150 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                run(mc, "setblock 0 -60 2 air", "tp @s 0.5 -60 -1.5 0 0", "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=2,uda:awakened=true]",
                        "summon zombie -2.5 -60 8.5 {NoAI:1b}", "summon zombie 0.5 -60 9.5 {NoAI:1b}", "summon zombie 3.5 -60 8.5 {NoAI:1b}");
            }
            case 4165 -> useItem(mc);
            case 4169 -> shot(mc, "45_corte_de_energia_supremo");
            case 4185 -> run(mc, "tp @e[type=zombie] 0.5 -500 0.5", "item replace entity @s weapon.mainhand with uda:eye_axe[uda:tier=1,uda:awakened=true]",
                    "summon zombie 2.5 -60 -1.5 {NoAI:1b}", "summon zombie -1.5 -60 -1.5 {NoAI:1b}", "summon zombie 0.5 -60 1.5 {NoAI:1b}");
            case 4195 -> useItem(mc);
            case 4197 -> shot(mc, "46_giro_do_carrasco");
            case 4205 -> run(mc, "tp @e[type=zombie] 0.5 -500 0.5", "time set midnight", "fill -6 -61 6 6 -55 14 stone", "fill -5 -60 6 5 -56 6 deepslate",
                    "setblock -3 -58 9 uda:eye_ore", "setblock 2 -57 11 uda:deepslate_eye_ore", "setblock 4 -59 8 diamond_ore", "setblock -1 -56 12 gold_ore",
                    "setblock 0 -59 10 iron_ore", "setblock -4 -57 13 emerald_ore", "setblock 3 -56 13 redstone_ore",
                    "item replace entity @s weapon.mainhand with uda:eye_pickaxe[uda:tier=2,uda:awakened=true]", "tp @s 0.5 -60 3.5 0 5");
            case 4215 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                useItem(mc);
            }
            case 4232 -> shot(mc, "47_deteccao_de_minerios");
            case 4240 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                run(mc, "fill -6 -61 6 6 -55 14 air", "fill -6 -61 6 6 -61 14 grass_block", "time set noon",
                        "item replace entity @s weapon.mainhand with uda:eye_grimoire[uda:tier=2,uda:awakened=true]", "tp @s 0.5 -60 -1.5 0 -5");
            }
            case 4250 -> useItem(mc);
            case 4253 -> shot(mc, "48_grimorio_supremo");
            case 4254 -> logPlayer(mc, "grimório usado");

            // --- armadura nas três formas (de dia e de noite) e o jogador com o conjunto supremo
            case 4270 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(mc, "tp @s 0.5 -59 -3.5 0 10", stand(-2.5, 0), stand(0.5, 1), stand(3.5, 2));
            }
            case 4300 -> shot(mc, "49_armadura_dia");
            case 4302 -> run(mc, "time set midnight");
            case 4330 -> shot(mc, "50_armadura_noite");
            case 4332 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                run(mc, "kill @e[type=armor_stand]", "tp @s 0.5 -60 0.5 0 0", armor("head", "eye_helmet", 2), armor("chest", "eye_chestplate", 2),
                        armor("legs", "eye_leggings", 2), armor("feet", "eye_boots", 2), "item replace entity @s weapon.mainhand with uda:eye_sword[uda:tier=2,uda:awakened=true]",
                        "item replace entity @s weapon.offhand with uda:agamoto_eye[uda:tier=2]");
            }
            case 4360 -> shot(mc, "51_jogador_conjunto_supremo");
            case 4362 -> logPlayer(mc, "conjunto supremo vestido");

            // --- estruturas (/place structure), vistas de cima
            case 4370 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(mc, "clear @s", "time set noon", "gamemode spectator", "tp @s 300 -40 -20 0 40");
            }
            case 4410 -> run(mc, "place structure uda:cursed_ruins 300 -60 0");
            case 4440 -> shot(mc, "52_ruinas_amaldicoadas");
            case 4442 -> run(mc, "tp @s 500 -36 -26 0 40");
            case 4480 -> run(mc, "place structure uda:abyssal_sanctuary 500 -60 0");
            case 4510 -> shot(mc, "53_santuario_abissal");
            case 4512 -> run(mc, "tp @s 700 -30 -40 0 25");
            case 4550 -> run(mc, "place structure uda:void_cathedral 700 -60 0");
            case 4580 -> shot(mc, "54_catedral_do_vazio");
            case 4582 -> run(mc, "tp @s 676 -36 -22 -40 28");
            case 4610 -> shot(mc, "55_catedral_de_cima");

            // --- invocação no Altar Abissal (4 fragmentos + núcleo desperto) e no Altar do Vazio
            case 4620 -> {
                mc.options.hideGui = true;
                run(mc, "fill -10 -60 30 10 -40 60 air", "tp @s 0.5 -60 36.5 0 20", "gamemode survival", "setblock 0 -60 40 uda:abyssal_altar",
                        "item replace entity @s weapon.mainhand with uda:abyssal_fragment 4", "give @s uda:awakened_eye_core 1");
            }
            case 4630, 4632, 4634, 4636 -> useOn(mc, SUMMON);
            case 4640 -> run(mc, "item replace entity @s weapon.mainhand with uda:awakened_eye_core");
            case 4644 -> useOn(mc, SUMMON);
            case 4700 -> shot(mc, "56_invocacao_do_dragao");
            case 4766 -> run(mc, "tp @s 0.5 -60 30.5 0 -25");
            case 4772 -> {
                shot(mc, "57_dragao_invocado");
                logBosses(mc);
            }
            case 4795 -> run(mc, "tp @e[type=!player] 0.5 -500 0.5", "kill @e[type=!player]", "setblock 0 -60 40 uda:void_altar",
                    "item replace entity @s weapon.mainhand with uda:abyssal_heart");
            case 4805 -> useOn(mc, SUMMON);
            case 4870 -> shot(mc, "58_invocacao_do_olho_negro");
            case 4935 -> logBosses(mc);

            // --- fase enfurecida do Olho Negro (contra um golem, o jogador só filma)
            case 4940 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(mc, "gamemode spectator", "tp @s -22 -52 20 -50 -5", "summon iron_golem 0.5 -60 30 {NoAI:1b,PersistenceRequired:1b,active_effects:"
                        + "[{id:\"minecraft:resistance\",amplifier:4b,duration:-1},{id:\"minecraft:regeneration\",amplifier:4b,duration:-1}]}",
                        "data merge entity @e[type=uda:black_eye,limit=1] {Health:100f}");
            }
            case 5010 -> run(mc, "data merge entity @e[type=uda:black_eye,limit=1] {Health:60f}");
            case 5020 -> logBosses(mc);
            case 5300 -> {
                shot(mc, "59_olho_negro_enfurecido");
                logBosses(mc);
            }
            case 5320 -> {
                UltimateDarkAdventure.LOGGER.info(TAG + "fim da progressão");
                mc.stop();
            }
            default -> {
            }
        }
        if (t >= 4020 && t < 4125) {
            this.record(mc, "altar_ritual", 4020);
        } else if (t >= 4644 && t < 4790) {
            this.record(mc, "invocacao_dragao", 4644);
        } else if (t >= 5020 && t < 5300) {
            this.record(mc, "olho_negro_enfurecido", 5020);
        }
        if (t > 4940 && t < 5310) {
            this.lookAtBoss(mc);
            if (t % 40 == 0) {
                logBosses(mc);
            }
        }
    }

    private static String stand(double x, int tier) {
        String c = tier == 0 ? "" : ",components:{\"uda:tier\":" + tier + "}";
        return "summon armor_stand " + x + " -60 1.5 {ShowArms:1b,NoBasePlate:1b,Rotation:[180f,0f],ArmorItems:[{id:\"uda:eye_boots\",count:1" + c
                + "},{id:\"uda:eye_leggings\",count:1" + c + "},{id:\"uda:eye_chestplate\",count:1" + c + "},{id:\"uda:eye_helmet\",count:1" + c + "}]}";
    }

    private static String armor(String slot, String item, int tier) {
        return "item replace entity @s armor." + slot + " with uda:" + item + "[uda:tier=" + tier + "]";
    }

    /** Clica com o botão direito no bloco, como o jogador faria. */
    private static void useOn(Minecraft mc, BlockPos pos) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
    }

    /** Usa o item da mão (botão direito no ar). */
    private static void useItem(Minecraft mc) {
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private static void logPlayer(Minecraft mc, String moment) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            int crystals = player.getInventory().countItem(com.uda.registry.ModItems.EYE_CRYSTAL.get());
            StringBuilder hands = new StringBuilder();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty()) hands.append(stack.getItem()).append("(forma ").append(Ocular.tier(stack)).append(") ");
            }
            int orbs = player.serverLevel().getEntities(com.uda.registry.ModEntities.EYE_ORB.get(), e -> true).size();
            UltimateDarkAdventure.LOGGER.info(TAG + "{}: inventário = {}cristais={} | visão noturna={} resistência={} orbes={}", moment, hands, crystals,
                    player.hasEffect(MobEffects.NIGHT_VISION), player.hasEffect(MobEffects.DAMAGE_RESISTANCE), orbs);
        });
    }

    private void record(Minecraft mc, String name, int start) {
        if (this.ticks == start) {
            this.frame = 0;
        }
        if (RenderAuras.quadros == this.lastRenderedFrame) {
            return;
        }
        this.lastRenderedFrame = RenderAuras.quadros;
        NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        Path file = mc.gameDirectory.toPath().resolve("quadros").resolve(name).resolve(String.format(Locale.ROOT, "%03d.png", this.frame++));
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                image.writeToFile(file);
            } catch (IOException e) {
                UltimateDarkAdventure.LOGGER.error(TAG + "quadro falhou", e);
            } finally {
                image.close();
            }
        });
    }

    private static void lineup(Minecraft mc) {
        String still = "NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]";
        run(mc,
                "summon uda:hollow_watcher -3.5 -60 1.5 {" + still + "," + BOW + "}",
                "summon uda:hollow_watcher -1.5 -60 1.5 {" + still + ",Awakened:1b," + BOW + "}",
                "summon uda:blind_executioner 1.5 -60 1.5 {" + still + "," + AXES + "}",
                "summon uda:blind_executioner 4.0 -60 1.5 {" + still + ",Awakened:1b," + AXES + "}",
                // sem IA a cabeça não acompanha a rotação do summon; o tp acerta as duas
                "execute as @e[type=!player] at @s run tp @s ~ ~ ~ 180 0");
    }

    private static void createWorld(Minecraft mc) {
        String name = "uda_" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1234L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
    }

    private static void run(Minecraft mc, String... commands) {
        for (String command : commands) {
            mc.player.connection.sendCommand(command);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> UltimateDarkAdventure.LOGGER.info(TAG + message.getString()));
    }

    private static void logBosses(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (Entity entity : server.overworld().getAllEntities()) {
                if (entity instanceof AbstractEyeBoss boss) {
                    UltimateDarkAdventure.LOGGER.info(TAG + String.format(Locale.ROOT, "[servidor] %s ataque=%d alvo=%s vida=%.0f pos=%.1f,%.1f,%.1f noAi=%s",
                            boss.getType().toShortString(), boss.getAttack(), boss.getTarget() == null ? "nenhum" : boss.getTarget().getType().toShortString(),
                            boss.getHealth(), boss.getX(), boss.getY(), boss.getZ(), boss.isNoAi()));
                }
            }
        });
    }

    private static void logStates(Minecraft mc, String moment) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (Entity entity : server.overworld().getAllEntities()) {
                if (entity instanceof AbstractEyeMonster monster) {
                    UltimateDarkAdventure.LOGGER.info(TAG + String.format(Locale.ROOT, "%s (%s): desperto=%s vida=%.1f/%.1f escala=%.2f alvo=%s",
                            entity.getType().toShortString(), moment, monster.isAwakened(), monster.getHealth(), monster.getMaxHealth(),
                            monster.getScale(), monster.getTarget() == null ? "nenhum" : monster.getTarget().getName().getString()));
                }
            }
        });
    }
}
