// Gera os dados do jogo: traduções, sons, receitas, drops, tags, minério no mundo, estruturas e conquistas.
//
//   node tools/dados/gerar_dados.mjs
//
// As estruturas (Ruínas Amaldiçoadas, Santuário Abissal e Catedral do Vazio) são montadas bloco a bloco
// aqui e gravadas no formato .nbt de estrutura do Minecraft (o mesmo que a Estrutura de Blocos salva).

import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';
import { seeded } from '../texturas/png.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const RES = join(ROOT, 'src', 'main', 'resources');
const DATA = join(RES, 'data');
const writeJson = (path, obj) => {
  const file = join(RES, path);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
};

// ================================================================ traduções

const PT = {
  'itemGroup.uda': 'Ultimate Dark Adventure',
  // entidades
  'entity.uda.hollow_watcher': 'Hollow Watcher',
  'entity.uda.ancient_watcher': 'Vigia Ancestral',
  'entity.uda.blind_executioner': 'Blind Executioner',
  'entity.uda.abyssal_dragon': 'Dragão Abissal',
  'entity.uda.black_eye': 'O Olho Negro',
  'entity.uda.eye_arrow': 'Flecha Ocular',
  'entity.uda.eye_orb': 'Orbe Ocular',
  'entity.uda.eye_bomb': 'Bomba Ocular',
  'entity.uda.energy_slash': 'Corte de Energia',
  // materiais
  'item.uda.raw_eye_fragment': 'Fragmento Ocular Bruto',
  'item.uda.eye_crystal': 'Cristal Roxo Ocular',
  'item.uda.eye_ingot': 'Lingote Ocular',
  'item.uda.eye_core': 'Núcleo do Olho',
  'item.uda.awakened_eye_core': 'Núcleo do Olho Desperto',
  'item.uda.watcher_eye': 'Olho do Vigia',
  'item.uda.abyssal_fragment': 'Fragmento Abissal',
  'item.uda.abyssal_scale': 'Escama Abissal',
  'item.uda.abyssal_heart': 'Coração Abissal',
  'item.uda.void_essence': 'Essência do Vazio',
  'tooltip.uda.raw_eye_fragment': 'Derreta para fazer Lingotes Oculares',
  'tooltip.uda.eye_crystal': 'Núcleos, o Altar do Olho e os despertares',
  'tooltip.uda.eye_ingot': 'Armas, ferramentas e armadura oculares',
  'tooltip.uda.eye_core': 'Catalisador do primeiro despertar no Altar do Olho',
  'tooltip.uda.awakened_eye_core': 'Invoca o Dragão Abissal no Santuário Abissal',
  'tooltip.uda.watcher_eye': 'Do Vigia Ancestral. Ingrediente do Altar do Olho',
  'tooltip.uda.abyssal_fragment': 'Grimório, Núcleo Desperto e o Altar Abissal (4)',
  'tooltip.uda.abyssal_scale': 'Oferenda do despertar supremo (2)',
  'tooltip.uda.abyssal_heart': 'Desperta O Olho Negro no Altar do Vazio',
  'tooltip.uda.void_essence': 'Catalisador do despertar supremo',
  // armas e equipamento
  'item.uda.eye_sword': 'Espada Ocular',
  'item.uda.eye_axe': 'Machado Ocular',
  'item.uda.eye_pickaxe': 'Picareta Ocular',
  'item.uda.eye_shovel': 'Pá Ocular',
  'item.uda.eye_bow': 'Arco Ocular',
  'item.uda.eye_shield': 'Escudo Ocular',
  'item.uda.eye_grimoire': 'Grimório do Olho',
  'item.uda.eye_helmet': 'Elmo Ocular',
  'item.uda.eye_chestplate': 'Peitoral Ocular',
  'item.uda.eye_leggings': 'Calças Oculares',
  'item.uda.eye_boots': 'Botas Oculares',
  'item.uda.agamoto_eye': 'Olho de Agamoto',
  // blocos
  'block.uda.eye_ore': 'Minério do Olho',
  'block.uda.deepslate_eye_ore': 'Minério do Olho de Ardósia Profunda',
  'block.uda.eye_altar': 'Altar do Olho',
  'block.uda.abyssal_altar': 'Altar Abissal',
  'block.uda.void_altar': 'Altar do Vazio',
  // ovos
  'item.uda.hollow_watcher_spawn_egg': 'Ovo Gerador de Hollow Watcher',
  'item.uda.blind_executioner_spawn_egg': 'Ovo Gerador de Blind Executioner',
  'item.uda.abyssal_dragon_spawn_egg': 'Ovo Gerador de Dragão Abissal',
  'item.uda.black_eye_spawn_egg': 'Ovo Gerador do Olho Negro',
  // dicas do equipamento
  'tooltip.uda.tier.0': '◉ Selado',
  'tooltip.uda.tier.1': '◉ Desperto',
  'tooltip.uda.tier.2': '◉ Supremo',
  'tooltip.uda.state.open': ' — olho aberto',
  'tooltip.uda.state.closed': ' — olho fechado',
  'tooltip.uda.needs_altar': 'Desperte no Altar do Olho com um Núcleo do Olho',
  'tooltip.uda.toggle_hint': '%s abre ou fecha o olho (aberto gasta durabilidade)',
  'tooltip.uda.eye_sword.power': 'Desperta: +3 de dano, definha, Corte de Energia (botão direito)',
  'tooltip.uda.eye_sword.supreme': 'Suprema: +5 de dano e três cortes em leque',
  'tooltip.uda.eye_axe.power': 'Desperto: +3 de dano, corta mais rápido, Giro do Carrasco (botão direito)',
  'tooltip.uda.eye_axe.supreme': 'Supremo: giro maior e mais forte, corta o dobro de rápido',
  'tooltip.uda.eye_pickaxe.power': 'Desperta: minera 60% mais rápido e revela minérios pelas paredes (botão direito)',
  'tooltip.uda.eye_pickaxe.supreme': 'Suprema: minera o dobro de rápido e enxerga bem mais longe',
  'tooltip.uda.eye_shovel.power': 'Desperta: cava 60% mais rápido e em 3x3 (agachado: um bloco)',
  'tooltip.uda.eye_shovel.supreme': 'Suprema: cava em 5x5',
  'tooltip.uda.eye_bow.power': 'Desperto: Flechas Oculares que perseguem o inimigo na mira',
  'tooltip.uda.eye_bow.supreme': 'Supremo: flechas mais fortes que perseguem mais longe e definham',
  'tooltip.uda.eye_shield.power': 'Desperto: Barreira Ocular devolve projéteis da frente e parte dos golpes',
  'tooltip.uda.eye_shield.supreme': 'Supremo: a barreira cobre todos os lados e devolve o dobro',
  'tooltip.uda.eye_grimoire.power': 'Desperto: três Orbes Oculares que perseguem',
  'tooltip.uda.eye_grimoire.supreme': 'Supremo: cinco orbes que explodem ao acertar',
  'tooltip.uda.eye_armor.power': 'Conjunto desperto: Visão do Olho (visão noturna e inimigos marcados)',
  'tooltip.uda.eye_armor.supreme': 'Conjunto supremo: Resistência e imunidade a definhamento e escuridão',
  'tooltip.uda.agamoto_eye.power': 'Desperto: Instante Congelado quase para o tempo dos inimigos (botão direito)',
  'tooltip.uda.agamoto_eye.supreme': 'Supremo: uma vez a cada 5 minutos, o tempo volta quando você morreria',
  // mensagens
  'key.categories.uda': 'Ultimate Dark Adventure',
  'key.uda.awaken': 'Despertar a arma',
  'uda.loading.world': 'Carregando o mundo...',
  'uda.loading': 'Carregando...',
  'message.uda.awakened': 'O olho se abre...',
  'message.uda.dormant': 'O olho se fecha.',
  'message.uda.too_weak': 'A arma está fraca demais para despertar.',
  'message.uda.sealed': 'Esta arma ainda está selada. Desperte-a num Altar do Olho.',
  'message.uda.altar.invalid': 'O altar só aceita equipamento do Olho ou um Núcleo do Olho.',
  'message.uda.altar.catalyst': 'Use %1$s para despertar (oferenda: %2$sx %3$s)',
  'message.uda.altar.offering': 'Falta a oferenda: %1$sx %2$s',
  'message.uda.sanctuary.fragments': 'Fragmentos Abissais no altar: %s/4',
  'message.uda.sanctuary.needs_fragments': 'Faltam %s Fragmentos Abissais no altar.',
  'message.uda.sanctuary.hint': 'Altar Abissal (%s/4): insira 4 Fragmentos Abissais e um Núcleo do Olho Desperto.',
  'message.uda.cathedral.hint': 'Altar do Vazio: ofereça o Coração Abissal.',
  'message.uda.summon.already': 'Algo já despertou por perto...',
  'boss.uda.awakened': '%s — Desperto',
  'boss.uda.enraged': '%s — Enfurecido',
  'filled_map.uda.sanctuary': 'Mapa do Santuário Abissal',
  'filled_map.uda.cathedral': 'Mapa da Catedral do Vazio',
  // legendas dos sons
  'subtitles.uda.eye.open': 'Olho se abre',
  'subtitles.uda.altar.place': 'Altar recebe item',
  'subtitles.uda.altar.ritual': 'Ritual começa',
  'subtitles.uda.altar.heartbeat': 'Altar pulsa',
  'subtitles.uda.altar.awaken': 'Explosão de energia',
  'subtitles.uda.summon.start': 'Invocação começa',
  'subtitles.uda.summon.complete': 'Algo desperta',
  'subtitles.uda.boss.enrage': 'Boss enfurece',
  'subtitles.uda.weapon.slash': 'Corte de energia',
  'subtitles.uda.weapon.scan': 'O olho enxerga',
  'subtitles.uda.weapon.barrier': 'Barreira devolve',
  'subtitles.uda.agamoto.freeze': 'O tempo para',
  'subtitles.uda.agamoto.rewind': 'O tempo volta',
};

const EN = {
  'itemGroup.uda': 'Ultimate Dark Adventure',
  'entity.uda.hollow_watcher': 'Hollow Watcher',
  'entity.uda.ancient_watcher': 'Ancient Watcher',
  'entity.uda.blind_executioner': 'Blind Executioner',
  'entity.uda.abyssal_dragon': 'Abyssal Dragon',
  'entity.uda.black_eye': 'The Black Eye',
  'entity.uda.eye_arrow': 'Ocular Arrow',
  'entity.uda.eye_orb': 'Ocular Orb',
  'entity.uda.eye_bomb': 'Ocular Bomb',
  'entity.uda.energy_slash': 'Energy Slash',
  'item.uda.raw_eye_fragment': 'Raw Ocular Fragment',
  'item.uda.eye_crystal': 'Purple Ocular Crystal',
  'item.uda.eye_ingot': 'Ocular Ingot',
  'item.uda.eye_core': 'Eye Core',
  'item.uda.awakened_eye_core': 'Awakened Eye Core',
  'item.uda.watcher_eye': "Watcher's Eye",
  'item.uda.abyssal_fragment': 'Abyssal Fragment',
  'item.uda.abyssal_scale': 'Abyssal Scale',
  'item.uda.abyssal_heart': 'Abyssal Heart',
  'item.uda.void_essence': 'Void Essence',
  'tooltip.uda.raw_eye_fragment': 'Smelt into Ocular Ingots',
  'tooltip.uda.eye_crystal': 'Cores, the Eye Altar and awakenings',
  'tooltip.uda.eye_ingot': 'Ocular weapons, tools and armor',
  'tooltip.uda.eye_core': 'Catalyst of the first awakening at the Eye Altar',
  'tooltip.uda.awakened_eye_core': 'Summons the Abyssal Dragon at the Abyssal Sanctuary',
  'tooltip.uda.watcher_eye': 'From the Ancient Watcher. Eye Altar ingredient',
  'tooltip.uda.abyssal_fragment': 'Grimoire, Awakened Core and the Abyssal Altar (4)',
  'tooltip.uda.abyssal_scale': 'Offering of the supreme awakening (2)',
  'tooltip.uda.abyssal_heart': 'Awakens The Black Eye at the Void Altar',
  'tooltip.uda.void_essence': 'Catalyst of the supreme awakening',
  'item.uda.eye_sword': 'Ocular Sword',
  'item.uda.eye_axe': 'Ocular Axe',
  'item.uda.eye_pickaxe': 'Ocular Pickaxe',
  'item.uda.eye_shovel': 'Ocular Shovel',
  'item.uda.eye_bow': 'Ocular Bow',
  'item.uda.eye_shield': 'Ocular Shield',
  'item.uda.eye_grimoire': 'Grimoire of the Eye',
  'item.uda.eye_helmet': 'Ocular Helmet',
  'item.uda.eye_chestplate': 'Ocular Chestplate',
  'item.uda.eye_leggings': 'Ocular Leggings',
  'item.uda.eye_boots': 'Ocular Boots',
  'item.uda.agamoto_eye': 'Eye of Agamoto',
  'block.uda.eye_ore': 'Eye Ore',
  'block.uda.deepslate_eye_ore': 'Deepslate Eye Ore',
  'block.uda.eye_altar': 'Eye Altar',
  'block.uda.abyssal_altar': 'Abyssal Altar',
  'block.uda.void_altar': 'Void Altar',
  'item.uda.hollow_watcher_spawn_egg': 'Hollow Watcher Spawn Egg',
  'item.uda.blind_executioner_spawn_egg': 'Blind Executioner Spawn Egg',
  'item.uda.abyssal_dragon_spawn_egg': 'Abyssal Dragon Spawn Egg',
  'item.uda.black_eye_spawn_egg': 'Black Eye Spawn Egg',
  'tooltip.uda.tier.0': '◉ Sealed',
  'tooltip.uda.tier.1': '◉ Awakened',
  'tooltip.uda.tier.2': '◉ Supreme',
  'tooltip.uda.state.open': ' — eye open',
  'tooltip.uda.state.closed': ' — eye closed',
  'tooltip.uda.needs_altar': 'Awaken it at an Eye Altar with an Eye Core',
  'tooltip.uda.toggle_hint': '%s opens or closes the eye (open drains durability)',
  'tooltip.uda.eye_sword.power': 'Awakened: +3 damage, withers, Energy Slash (right click)',
  'tooltip.uda.eye_sword.supreme': 'Supreme: +5 damage and three fanned slashes',
  'tooltip.uda.eye_axe.power': "Awakened: +3 damage, chops faster, Executioner's Whirl (right click)",
  'tooltip.uda.eye_axe.supreme': 'Supreme: bigger, stronger whirl; chops twice as fast',
  'tooltip.uda.eye_pickaxe.power': 'Awakened: mines 60% faster and reveals ores through walls (right click)',
  'tooltip.uda.eye_pickaxe.supreme': 'Supreme: mines twice as fast and sees much farther',
  'tooltip.uda.eye_shovel.power': 'Awakened: digs 60% faster in 3x3 (sneak: single block)',
  'tooltip.uda.eye_shovel.supreme': 'Supreme: digs in 5x5',
  'tooltip.uda.eye_bow.power': 'Awakened: Ocular Arrows that seek the enemy you aim at',
  'tooltip.uda.eye_bow.supreme': 'Supreme: stronger arrows that seek farther and wither',
  'tooltip.uda.eye_shield.power': 'Awakened: Ocular Barrier reflects frontal projectiles and part of the blows',
  'tooltip.uda.eye_shield.supreme': 'Supreme: the barrier covers every side and reflects twice as much',
  'tooltip.uda.eye_grimoire.power': 'Awakened: three seeking Ocular Orbs',
  'tooltip.uda.eye_grimoire.supreme': 'Supreme: five orbs that explode on hit',
  'tooltip.uda.eye_armor.power': 'Awakened set: Sight of the Eye (night vision, enemies marked)',
  'tooltip.uda.eye_armor.supreme': 'Supreme set: Resistance and immunity to wither and darkness',
  'tooltip.uda.agamoto_eye.power': 'Awakened: Frozen Instant nearly stops time for nearby enemies (right click)',
  'tooltip.uda.agamoto_eye.supreme': 'Supreme: once every 5 minutes, time rewinds when you would die',
  'key.categories.uda': 'Ultimate Dark Adventure',
  'key.uda.awaken': 'Awaken weapon',
  'uda.loading.world': 'Loading the world...',
  'uda.loading': 'Loading...',
  'message.uda.awakened': 'The eye opens...',
  'message.uda.dormant': 'The eye closes.',
  'message.uda.too_weak': 'The weapon is too weak to awaken.',
  'message.uda.sealed': 'This weapon is still sealed. Awaken it at an Eye Altar.',
  'message.uda.altar.invalid': 'The altar only accepts Eye equipment or an Eye Core.',
  'message.uda.altar.catalyst': 'Use %1$s to awaken (offering: %2$sx %3$s)',
  'message.uda.altar.offering': 'Missing offering: %1$sx %2$s',
  'message.uda.sanctuary.fragments': 'Abyssal Fragments on the altar: %s/4',
  'message.uda.sanctuary.needs_fragments': '%s Abyssal Fragments missing on the altar.',
  'message.uda.sanctuary.hint': 'Abyssal Altar (%s/4): insert 4 Abyssal Fragments and an Awakened Eye Core.',
  'message.uda.cathedral.hint': 'Void Altar: offer the Abyssal Heart.',
  'message.uda.summon.already': 'Something has already awakened nearby...',
  'boss.uda.awakened': '%s — Awakened',
  'boss.uda.enraged': '%s — Enraged',
  'filled_map.uda.sanctuary': 'Abyssal Sanctuary Map',
  'filled_map.uda.cathedral': 'Void Cathedral Map',
  'subtitles.uda.eye.open': 'Eye opens',
  'subtitles.uda.altar.place': 'Altar receives item',
  'subtitles.uda.altar.ritual': 'Ritual begins',
  'subtitles.uda.altar.heartbeat': 'Altar throbs',
  'subtitles.uda.altar.awaken': 'Energy bursts',
  'subtitles.uda.summon.start': 'Summoning begins',
  'subtitles.uda.summon.complete': 'Something awakens',
  'subtitles.uda.boss.enrage': 'Boss enrages',
  'subtitles.uda.weapon.slash': 'Energy slash',
  'subtitles.uda.weapon.scan': 'The eye sees',
  'subtitles.uda.weapon.barrier': 'Barrier reflects',
  'subtitles.uda.agamoto.freeze': 'Time stops',
  'subtitles.uda.agamoto.rewind': 'Time rewinds',
};

// ================================================================ conquistas (a trilha da progressão)

// [id, pai, ícone, critério, moldura, título pt, descrição pt, título en, descrição en]
const has = (item, extra = {}) => ({ trigger: 'minecraft:inventory_changed', conditions: { items: [{ items: item, ...extra }] } });
const kill = (type) => ({ trigger: 'minecraft:player_killed_entity', conditions: { entity: { type } } });
const summon = (type) => ({ trigger: 'minecraft:summoned_entity', conditions: { entity: { type } } });
const ADV = [
  ['root', null, 'uda:eye_core', { trigger: 'minecraft:tick' }, 'task', 'Ultimate Dark Adventure', 'O Olho tudo vê. Comece pelas cavernas profundas.',
    'Ultimate Dark Adventure', 'The Eye sees all. Start in the deep caves.'],
  ['mine_ore', 'root', 'uda:raw_eye_fragment', has('uda:raw_eye_fragment'), 'task', 'Algo me observa', 'Minere o Minério do Olho (abaixo de y 16)',
    'Something Is Watching', 'Mine Eye Ore (below y 16)'],
  ['ingot', 'mine_ore', 'uda:eye_ingot', has('uda:eye_ingot'), 'task', 'Metal que enxerga', 'Derreta um Fragmento Ocular Bruto',
    'Metal That Sees', 'Smelt a Raw Ocular Fragment'],
  ['crystal', 'mine_ore', 'uda:eye_crystal', has('uda:eye_crystal'), 'task', 'Cristal Ocular', 'Encontre um Cristal Roxo Ocular no minério',
    'Ocular Crystal', 'Find a Purple Ocular Crystal in the ore'],
  ['eye_core', 'ingot', 'uda:eye_core', has('uda:eye_core'), 'task', 'O Núcleo', 'Faça um Núcleo do Olho', 'The Core', 'Craft an Eye Core'],
  ['kill_watcher', 'root', 'uda:eye_bow', kill('uda:hollow_watcher'), 'task', 'Vigia das Profundezas', 'Derrote um Hollow Watcher',
    'Watcher of the Deep', 'Defeat a Hollow Watcher'],
  ['watcher_eye', 'kill_watcher', 'uda:watcher_eye', has('uda:watcher_eye'), 'goal', 'O Vigia Ancestral',
    'Derrote um Vigia Ancestral (um Hollow Watcher maior) e pegue o Olho do Vigia', 'The Ancient Watcher',
    "Defeat an Ancient Watcher (a bigger Hollow Watcher) and take its Watcher's Eye"],
  ['altar', 'watcher_eye', 'uda:eye_altar', has('uda:eye_altar'), 'task', 'O Altar do Olho', 'Construa o Altar do Olho', 'The Eye Altar', 'Build the Eye Altar'],
  ['first_awakening', 'altar', 'uda:eye_sword', has('#uda:ocular_equipment', { components: { 'uda:tier': 1 } }), 'goal', 'O Primeiro Despertar',
    'Desperte um equipamento no Altar do Olho', 'The First Awakening', 'Awaken a piece of equipment at the Eye Altar'],
  ['kill_executioner', 'first_awakening', 'uda:eye_axe', kill('uda:blind_executioner'), 'task', 'Cego, não surdo',
    'Derrote o Blind Executioner nas Ruínas Amaldiçoadas', 'Blind, Not Deaf', 'Defeat the Blind Executioner in the Cursed Ruins'],
  ['agamoto', 'kill_executioner', 'uda:agamoto_eye', has('uda:agamoto_eye'), 'task', 'O Pingente', 'Pegue o Olho de Agamoto do Executioner',
    'The Pendant', "Take the Executioner's Eye of Agamoto"],
  ['awakened_core', 'kill_executioner', 'uda:awakened_eye_core', has('uda:awakened_eye_core'), 'goal', 'Núcleo Desperto',
    'Desperte um Núcleo do Olho com um Fragmento Abissal', 'Awakened Core', 'Awaken an Eye Core with an Abyssal Fragment'],
  ['summon_dragon', 'awakened_core', 'uda:abyssal_altar', summon('uda:abyssal_dragon'), 'goal', 'O Santuário Abissal',
    'Invoque o Dragão Abissal no Santuário Abissal', 'The Abyssal Sanctuary', 'Summon the Abyssal Dragon at the Abyssal Sanctuary'],
  ['kill_dragon', 'summon_dragon', 'uda:abyssal_scale', kill('uda:abyssal_dragon'), 'challenge', 'Abismo Silenciado', 'Derrote o Dragão Abissal',
    'Abyss Silenced', 'Defeat the Abyssal Dragon'],
  ['heart', 'kill_dragon', 'uda:abyssal_heart', has('uda:abyssal_heart'), 'task', 'O Coração Abissal', 'Pegue o Coração Abissal do dragão',
    'The Abyssal Heart', "Take the dragon's Abyssal Heart"],
  ['summon_black_eye', 'heart', 'uda:void_altar', summon('uda:black_eye'), 'goal', 'A Catedral do Vazio',
    'Desperte O Olho Negro no altar da Catedral do Vazio', 'The Void Cathedral', 'Awaken The Black Eye at the Void Cathedral altar'],
  ['kill_black_eye', 'summon_black_eye', 'uda:void_essence', kill('uda:black_eye'), 'challenge', 'O Olho se Fecha', 'Derrote O Olho Negro',
    'The Eye Closes', 'Defeat The Black Eye'],
  ['supreme', 'kill_black_eye', 'uda:awakened_eye_core', has('#uda:ocular_equipment', { components: { 'uda:tier': 2 } }), 'challenge',
    'Despertar Supremo', 'Leve um equipamento à forma suprema com a Essência do Vazio', 'Supreme Awakening',
    'Bring a piece of equipment to its supreme form with Void Essence'],
];

// ================================================================ NBT (estruturas)

const T = { byte: 1, short: 2, int: 3, long: 4, float: 5, double: 6, string: 8, list: 9, compound: 10 };
const nb = {
  byte: (v) => ({ t: 'byte', v }), int: (v) => ({ t: 'int', v }), float: (v) => ({ t: 'float', v }), double: (v) => ({ t: 'double', v }),
  str: (v) => ({ t: 'string', v }), list: (et, v) => ({ t: 'list', et, v }), comp: (v) => ({ t: 'compound', v }),
};

function writeTag(out, tag) {
  const b = (n) => { const x = Buffer.alloc(n); out.push(x); return x; };
  switch (tag.t) {
    case 'byte': b(1).writeInt8(tag.v); break;
    case 'int': b(4).writeInt32BE(tag.v); break;
    case 'float': b(4).writeFloatBE(tag.v); break;
    case 'double': b(8).writeDoubleBE(tag.v); break;
    case 'string': { const s = Buffer.from(tag.v, 'utf8'); b(2).writeUInt16BE(s.length); out.push(s); break; }
    case 'list': {
      b(1).writeUInt8(tag.v.length ? T[tag.et] : 0);
      b(4).writeInt32BE(tag.v.length);
      for (const item of tag.v) writeTag(out, item);
      break;
    }
    case 'compound': {
      for (const [name, child] of Object.entries(tag.v)) {
        b(1).writeUInt8(T[child.t]);
        const n = Buffer.from(name, 'utf8');
        b(2).writeUInt16BE(n.length);
        out.push(n);
        writeTag(out, child);
      }
      b(1).writeUInt8(0);
      break;
    }
    default: throw new Error('tag ' + tag.t);
  }
}

function nbtFile(root) {
  const out = [Buffer.from([T.compound, 0, 0])];
  writeTag(out, root);
  return gzipSync(Buffer.concat(out));
}

/** Molde de estrutura: grade de blocos + entidades, gravado como .nbt. */
class Template {
  constructor(w, h, d) {
    this.size = [w, h, d];
    this.blocks = new Map();
    this.entities = [];
  }

  set(x, y, z, name, props = {}, nbt = null) {
    if (x < 0 || y < 0 || z < 0 || x >= this.size[0] || y >= this.size[1] || z >= this.size[2]) return;
    this.blocks.set(`${x},${y},${z}`, { name, props, nbt });
  }

  get(x, y, z) {
    return this.blocks.get(`${x},${y},${z}`)?.name;
  }

  entity(x, y, z, nbt) {
    this.entities.push({ pos: [x + 0.5, y, z + 0.5], blockPos: [x, y, z], nbt });
  }

  save(path) {
    const palette = [];
    const index = new Map();
    const blocks = [];
    for (const [key, b] of this.blocks) {
      const id = b.name + JSON.stringify(b.props);
      if (!index.has(id)) {
        index.set(id, palette.length);
        const entry = { Name: nb.str(b.name) };
        if (Object.keys(b.props).length) entry.Properties = nb.comp(Object.fromEntries(Object.entries(b.props).map(([k, v]) => [k, nb.str(String(v))])));
        palette.push(nb.comp(entry));
      }
      const [x, y, z] = key.split(',').map(Number);
      const entry = { pos: nb.list('int', [nb.int(x), nb.int(y), nb.int(z)]), state: nb.int(index.get(id)) };
      if (b.nbt) entry.nbt = b.nbt;
      blocks.push(nb.comp(entry));
    }
    const entities = this.entities.map((e) => nb.comp({
      pos: nb.list('double', e.pos.map(nb.double)),
      blockPos: nb.list('int', e.blockPos.map(nb.int)),
      nbt: e.nbt,
    }));
    const root = nb.comp({
      DataVersion: nb.int(3955),
      size: nb.list('int', this.size.map(nb.int)),
      palette: nb.list('compound', palette),
      blocks: nb.list('compound', blocks),
      entities: nb.list('compound', entities),
    });
    const file = join(DATA, 'uda', 'structure', path + '.nbt');
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, nbtFile(root));
  }
}

const chest = (loot, facing = 'south') => ['minecraft:chest', { facing, type: 'single', waterlogged: 'false' },
  nb.comp({ id: nb.str('minecraft:chest'), LootTable: nb.str('uda:chests/' + loot) })];
const item = (id) => nb.comp({ id: nb.str(id), count: nb.int(1) });
const mob = (id, hands) => nb.comp({
  id: nb.str(id),
  PersistenceRequired: nb.byte(1),
  HandItems: nb.list('compound', hands.map((h) => (h ? item(h) : nb.comp({})))),
  HandDropChances: nb.list('float', [nb.float(0.05), nb.float(0.05)]),
});
const EXECUTIONER = () => mob('uda:blind_executioner', ['uda:eye_axe', 'uda:eye_axe']);
const WATCHER = () => mob('uda:hollow_watcher', ['uda:eye_bow', null]);

function pick(rand, list) {
  return list[Math.floor(rand() * list.length)];
}

/** Ruínas Amaldiçoadas: muros quebrados de pedra negra com olhos, pilares, um baú e o Blind Executioner. */
function cursedRuins(variant) {
  const rand = seeded('ruinas-' + variant);
  const t = new Template(17, 10, 17);
  const wall = ['minecraft:polished_blackstone_bricks', 'minecraft:cracked_polished_blackstone_bricks', 'minecraft:deepslate_bricks',
    'minecraft:cracked_deepslate_bricks', 'minecraft:blackstone'];
  for (let x = 0; x < 17; x++) for (let z = 0; z < 17; z++) {
    const d = Math.hypot(x - 8, z - 8);
    if (d > 8.4) continue;
    t.set(x, 0, z, d < 7.5 || rand() < 0.5 ? pick(rand, ['minecraft:deepslate_tiles', 'minecraft:cracked_deepslate_tiles', 'minecraft:polished_blackstone']) : 'minecraft:blackstone');
    for (let y = 1; y < 10; y++) t.set(x, y, z, 'minecraft:air');
  }
  // muro quadrado arruinado
  for (let i = 2; i <= 14; i++) {
    for (const [x, z] of [[i, 2], [i, 14], [2, i], [14, i]]) {
      if ((x === 8 && z === 14) || (x === 8 && z === 2 && variant === 1)) continue; // entradas
      const h = 1 + Math.floor(rand() * (rand() < 0.3 ? 5 : 3));
      for (let y = 1; y <= h; y++) t.set(x, y, z, rand() < 0.07 ? 'uda:eye_ore' : pick(rand, wall));
      if (rand() < 0.15) t.set(x, h + 1, z, 'minecraft:cobweb');
    }
  }
  // pilares nos cantos, com cristal e lanterna
  for (const [x, z] of [[2, 2], [14, 2], [2, 14], [14, 14]]) {
    const h = 5 + Math.floor(rand() * 3);
    for (let y = 1; y <= h; y++) t.set(x, y, z, y === h ? 'minecraft:crying_obsidian' : y % 3 === 0 ? 'uda:eye_ore' : 'minecraft:polished_blackstone_bricks');
    t.set(x, h + 1, z, 'minecraft:amethyst_cluster', { facing: 'up', waterlogged: 'false' });
  }
  // altar arruinado no centro
  for (let x = 7; x <= 9; x++) for (let z = 7; z <= 9; z++) t.set(x, 1, z, 'minecraft:chiseled_deepslate');
  t.set(8, 2, 8, ...chest('cursed_ruins'));
  t.set(6, 1, 10, 'minecraft:soul_campfire', { facing: 'north', lit: 'true', signal_fire: 'false', waterlogged: 'false' });
  t.set(10, 1, 6, 'minecraft:soul_campfire', { facing: 'south', lit: 'true', signal_fire: 'false', waterlogged: 'false' });
  t.entity(8, 1, 5, EXECUTIONER());
  return t;
}

/** Santuário Abissal: plataforma de obsidiana, anel de pilares e o Altar Abissal no centro. */
function abyssalSanctuary() {
  const rand = seeded('santuario');
  const t = new Template(23, 15, 23);
  const c = 11;
  for (let x = 0; x < 23; x++) for (let z = 0; z < 23; z++) {
    const d = Math.hypot(x - c, z - c);
    if (d > 11.2) continue;
    t.set(x, 0, z, d > 10 ? 'minecraft:blackstone' : d > 6.5 ? pick(rand, ['minecraft:obsidian', 'minecraft:obsidian', 'minecraft:crying_obsidian'])
      : pick(rand, ['minecraft:polished_blackstone_bricks', 'minecraft:cracked_polished_blackstone_bricks']));
    for (let y = 1; y < 15; y++) t.set(x, y, z, 'minecraft:air');
    if (Math.abs(d - 6) < 0.6) t.set(x, 1, z, 'minecraft:crying_obsidian');
    if (d <= 2.2) {
      t.set(x, 1, z, 'minecraft:polished_blackstone_bricks');
      t.set(x, 2, z, 'minecraft:chiseled_deepslate');
    }
  }
  t.set(c, 3, c, 'uda:abyssal_altar', { active: 'false', fragments: '0' });
  // pilares com cristais e correntes
  for (let k = 0; k < 6; k++) {
    const a = (k / 6) * Math.PI * 2;
    const x = Math.round(c + Math.cos(a) * 8.5), z = Math.round(c + Math.sin(a) * 8.5);
    const h = 8 + Math.floor(rand() * 4);
    for (let y = 1; y <= h; y++) t.set(x, y, z, y % 4 === 0 ? 'uda:deepslate_eye_ore' : y === h ? 'minecraft:crying_obsidian' : 'minecraft:obsidian');
    t.set(x, h + 1, z, 'minecraft:amethyst_block');
    for (let y = h - 1; y > h - 4; y--) {
      const xi = x + Math.sign(c - x), zi = z + Math.sign(c - z);
      if (y === h - 3) t.set(xi, y, zi, 'minecraft:soul_lantern', { hanging: 'true', waterlogged: 'false' });
      else t.set(xi, y, zi, 'minecraft:chain', { axis: 'y', waterlogged: 'false' });
    }
  }
  // pedestais dos fragmentos
  for (const [x, z] of [[c + 4, c], [c - 4, c], [c, c + 4], [c, c - 4]]) {
    t.set(x, 1, z, 'minecraft:chiseled_deepslate');
    t.set(x, 2, z, 'minecraft:amethyst_cluster', { facing: 'up', waterlogged: 'false' });
  }
  t.set(c + 1, 1, c + 9, ...chest('abyssal_sanctuary', 'north'));
  t.entity(c - 3, 1, c + 8, WATCHER());
  t.entity(c + 3, 1, c - 8, WATCHER());
  return t;
}

/** Catedral do Vazio: nave de ardósia e pedra negra, vitrais roxos, pilares e o Altar do Vazio na abside. */
function voidCathedral() {
  const rand = seeded('catedral');
  const W = 19, H = 21, D = 35;
  const t = new Template(W, H, D);
  const wall = ['minecraft:deepslate_bricks', 'minecraft:deepslate_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:polished_blackstone_bricks'];
  for (let z = 0; z < D; z++) {
    for (let x = 0; x < W; x++) {
      t.set(x, 0, z, x >= 8 && x <= 10 ? 'minecraft:polished_blackstone' : pick(rand, ['minecraft:deepslate_tiles', 'minecraft:deepslate_tiles', 'minecraft:cracked_deepslate_tiles']));
      for (let y = 1; y < H; y++) t.set(x, y, z, 'minecraft:air');
    }
    // paredes laterais
    for (const x of [0, W - 1]) {
      for (let y = 1; y <= 12; y++) {
        const window = z % 5 === 2 && y >= 4 && y <= 9 && z > 1 && z < D - 2;
        t.set(x, y, z, window ? 'minecraft:purple_stained_glass' : rand() < 0.05 ? 'uda:deepslate_eye_ore' : pick(rand, wall));
      }
    }
    // telhado em degraus
    for (let k = 0; k <= 8; k++) {
      const y = 13 + k;
      for (const x of [k, W - 1 - k]) t.set(x, y, z, k === 8 ? 'minecraft:crying_obsidian' : 'minecraft:polished_blackstone_bricks');
    }
    t.set(9, 21 - 1, z, 'minecraft:polished_blackstone_bricks');
  }
  // fachada e fundo
  for (const z of [0, D - 1]) {
    for (let x = 1; x < W - 1; x++) {
      const top = 12 + Math.min(x, W - 1 - x);
      for (let y = 1; y <= Math.min(top, 20); y++) {
        const door = z === 0 && x >= 7 && x <= 11 && y <= 7;
        const rose = Math.hypot(x - 9, y - 13) <= 2.5;
        if (door) continue;
        t.set(x, y, z, rose ? (Math.hypot(x - 9, y - 13) < 1 ? 'uda:eye_ore' : 'minecraft:purple_stained_glass') : pick(rand, wall));
      }
    }
  }
  // pilares com correntes e lanternas
  for (let z = 4; z < D - 6; z += 5) {
    for (const x of [4, W - 5]) {
      for (let y = 1; y <= 12; y++) t.set(x, y, z, y === 1 || y === 12 ? 'minecraft:chiseled_deepslate' : y % 4 === 0 ? 'minecraft:crying_obsidian' : 'minecraft:polished_blackstone_bricks');
      for (let y = 11; y >= 8; y--) t.set(x + (x < 9 ? 2 : -2), y, z, y === 8 ? 'minecraft:soul_lantern' : 'minecraft:chain',
        y === 8 ? { hanging: 'true', waterlogged: 'false' } : { axis: 'y', waterlogged: 'false' });
    }
  }
  // abside elevada com o Altar do Vazio
  for (let z = D - 7; z < D - 1; z++) for (let x = 1; x < W - 1; x++) {
    t.set(x, 1, z, 'minecraft:polished_blackstone_bricks');
    if (z >= D - 5 && x >= 4 && x <= W - 5) t.set(x, 2, z, 'minecraft:chiseled_deepslate');
  }
  t.set(9, 3, D - 3, 'uda:void_altar', { active: 'false' });
  for (const x of [6, 12]) {
    for (let y = 3; y <= 6; y++) t.set(x, y, D - 3, y === 6 ? 'minecraft:amethyst_block' : 'minecraft:obsidian');
  }
  t.set(2, 2, D - 3, ...chest('void_cathedral', 'east'));
  t.set(W - 3, 2, D - 3, ...chest('void_cathedral', 'west'));
  t.entity(7, 1, 12, EXECUTIONER());
  t.entity(11, 1, 20, EXECUTIONER());
  t.entity(5, 1, 25, WATCHER());
  t.entity(13, 1, 25, WATCHER());
  return t;
}

// ================================================================ main

writeJson('assets/uda/lang/pt_br.json', PT);
writeJson('assets/uda/lang/en_us.json', EN);

// sons: arquivos do Minecraft, reafinados
const snd = (subtitle, ...sounds) => ({ subtitle: 'subtitles.uda.' + subtitle, sounds: sounds.map(([name, pitch, volume = 1]) => ({ name, pitch, volume })) });
writeJson('assets/uda/sounds.json', {
  'eye.open': snd('eye.open', ['minecraft:block/trial_spawner/ominous_activate', 1.3]),
  'altar.place': snd('altar.place', ...[1, 2, 3, 4].map((i) => [`minecraft:block/amethyst/resonate${i}`, 0.8])),
  'altar.ritual': snd('altar.ritual', ['minecraft:block/beacon/activate', 0.6]),
  'altar.heartbeat': snd('altar.heartbeat', ...[1, 2, 3, 4].map((i) => [`minecraft:mob/warden/heartbeat_${i}`, 1.0])),
  'altar.awaken': snd('altar.awaken', ...[1, 2, 3, 4].map((i) => [`minecraft:mob/warden/sonic_boom${i}`, 1.2, 0.7])),
  'summon.start': snd('summon.start', ['minecraft:portal/trigger', 0.6]),
  'summon.complete': snd('summon.complete', ['minecraft:item/trident/thunder1', 0.8], ['minecraft:item/trident/thunder2', 0.8]),
  'boss.enrage': snd('boss.enrage', ...[1, 2, 3, 4].map((i) => [`minecraft:mob/warden/sonic_charge${i}`, 0.7])),
  'weapon.slash': snd('weapon.slash', ...[1, 2, 3].map((i) => [`minecraft:mob/breeze/wind_burst${i}`, 1.4, 0.8])),
  'weapon.scan': snd('weapon.scan', ['minecraft:block/conduit/activate', 1.5]),
  'weapon.barrier': snd('weapon.barrier', ...[1, 2, 3, 4].map((i) => [`minecraft:enchant/thorns/hit${i}`, 1.6])),
  'agamoto.freeze': snd('agamoto.freeze', ['minecraft:block/beacon/deactivate', 0.5]),
  'agamoto.rewind': snd('agamoto.rewind', ['minecraft:item/totem/use_totem', 0.6]),
});

// ---------------------------------------------------------------- receitas (formato 1.21.1)
const I = (id) => ({ item: id });
const shaped = (name, pattern, key, result, count = 1) => writeJson(`data/uda/recipe/${name}.json`, {
  type: 'minecraft:crafting_shaped', category: 'equipment', pattern,
  key: Object.fromEntries(Object.entries(key).map(([k, v]) => [k, I(v)])), result: { id: result, count },
});
const cook = (type, time) => writeJson(`data/uda/recipe/eye_ingot_from_${type}.json`, {
  type: 'minecraft:' + type, category: 'misc', ingredient: [I('uda:raw_eye_fragment'), I('uda:eye_ore'), I('uda:deepslate_eye_ore')],
  result: { id: 'uda:eye_ingot' }, experience: 1.0, cookingtime: time,
});
cook('smelting', 200);
cook('blasting', 100);
const ING = 'uda:eye_ingot', STICK = 'minecraft:stick', CRY = 'uda:eye_crystal';
shaped('eye_core', [' C ', 'IEI', ' C '], { C: CRY, I: ING, E: 'minecraft:ender_eye' }, 'uda:eye_core');
shaped('eye_altar', ['CWC', 'OIO', 'BBB'], { C: CRY, W: 'uda:watcher_eye', O: 'minecraft:crying_obsidian', I: ING, B: 'minecraft:polished_blackstone' }, 'uda:eye_altar');
shaped('eye_sword', ['I', 'I', 'S'], { I: ING, S: STICK }, 'uda:eye_sword');
shaped('eye_axe', ['II', 'IS', ' S'], { I: ING, S: STICK }, 'uda:eye_axe');
shaped('eye_pickaxe', ['III', ' S ', ' S '], { I: ING, S: STICK }, 'uda:eye_pickaxe');
shaped('eye_shovel', ['I', 'S', 'S'], { I: ING, S: STICK }, 'uda:eye_shovel');
shaped('eye_bow', [' IS', 'C S', ' IS'], { I: ING, C: CRY, S: 'minecraft:string' }, 'uda:eye_bow');
shaped('eye_shield', ['ICI', 'III', ' I '], { I: ING, C: CRY }, 'uda:eye_shield');
shaped('eye_grimoire', ['FBF', ' K '], { F: 'uda:abyssal_fragment', B: 'minecraft:book', K: 'uda:eye_core' }, 'uda:eye_grimoire');
shaped('eye_helmet', ['III', 'I I'], { I: ING }, 'uda:eye_helmet');
shaped('eye_chestplate', ['I I', 'III', 'III'], { I: ING }, 'uda:eye_chestplate');
shaped('eye_leggings', ['III', 'I I', 'I I'], { I: ING }, 'uda:eye_leggings');
shaped('eye_boots', ['I I', 'I I'], { I: ING }, 'uda:eye_boots');

// ---------------------------------------------------------------- drops
const setCount = (min, max) => ({ function: 'minecraft:set_count', count: min === max ? min : { type: 'minecraft:uniform', min, max } });
const looting = (max) => ({ function: 'minecraft:enchanted_count_increase', enchantment: 'minecraft:looting', count: { type: 'minecraft:uniform', min: 0, max } });
const entry = (name, functions = []) => ({ type: 'minecraft:item', name, functions });
const pool = (entries, conditions) => ({ rolls: 1, bonus_rolls: 0, entries, ...(conditions ? { conditions } : {}) });
const chance = (p) => ({ condition: 'minecraft:random_chance', chance: p });
const byPlayer = { condition: 'minecraft:killed_by_player' };
const silk = { condition: 'minecraft:match_tool', predicate: { predicates: { 'minecraft:enchantments': [{ enchantments: 'minecraft:silk_touch', levels: { min: 1 } }] } } };
const map = (destination, nameKey) => entry('minecraft:map', [
  { function: 'minecraft:exploration_map', destination: 'uda:' + destination, decoration: 'minecraft:red_x', zoom: 1, search_radius: 100, skip_existing_chunks: false },
  { function: 'minecraft:set_name', name: { translate: nameKey }, target: 'item_name' },
]);
const loot = (path, type, pools) => writeJson(`data/uda/loot_table/${path}.json`, { type, pools, random_sequence: 'uda:' + path });

for (const ore of ['eye_ore', 'deepslate_eye_ore']) {
  loot('blocks/' + ore, 'minecraft:block', [
    pool([{ type: 'minecraft:alternatives', children: [
      { type: 'minecraft:item', name: 'uda:' + ore, conditions: [silk] },
      entry('uda:raw_eye_fragment', [setCount(1, 2), { function: 'minecraft:apply_bonus', enchantment: 'minecraft:fortune', formula: 'minecraft:ore_drops' },
        { function: 'minecraft:explosion_decay' }]),
    ] }]),
    pool([entry('uda:eye_crystal')], [{ condition: 'minecraft:inverted', term: silk },
      { condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: [0.25, 0.33, 0.42, 0.55] }]),
  ]);
}
loot('blocks/eye_altar', 'minecraft:block', [pool([entry('uda:eye_altar')], [{ condition: 'minecraft:survives_explosion' }])]);

loot('entities/hollow_watcher', 'minecraft:entity', [
  pool([entry('minecraft:bone', [setCount(0, 2), looting(1)])]),
  pool([entry('minecraft:arrow', [setCount(0, 2), looting(1)])]),
  pool([entry('uda:raw_eye_fragment', [setCount(1, 2), looting(1)])], [byPlayer, chance(0.35)]),
]);
loot('entities/blind_executioner', 'minecraft:entity', [
  pool([entry('uda:agamoto_eye')], [byPlayer]),
  pool([entry('uda:abyssal_fragment', [setCount(2, 3), looting(1)])], [byPlayer]),
  pool([entry('minecraft:emerald', [setCount(1, 3), looting(1)])]),
  pool([map('on_sanctuary_maps', 'filled_map.uda.sanctuary')], [byPlayer]),
]);
loot('entities/abyssal_dragon', 'minecraft:entity', [
  pool([entry('uda:abyssal_heart')]),
  pool([entry('uda:abyssal_scale', [setCount(6, 9)])]),
  pool([map('on_cathedral_maps', 'filled_map.uda.cathedral')]),
  pool([entry('minecraft:diamond', [setCount(3, 6)])]),
]);
loot('entities/black_eye', 'minecraft:entity', [
  pool([entry('uda:void_essence', [setCount(4, 6)])]),
  pool([entry('minecraft:diamond', [setCount(4, 8)])]),
  pool([entry('minecraft:nether_star')]),
]);
const chestPool = (rolls, items) => ({ rolls, bonus_rolls: 0, entries: items.map(([name, w, min, max]) => ({ ...entry(name, [setCount(min, max)]), weight: w })) });
loot('chests/cursed_ruins', 'minecraft:chest', [
  chestPool({ type: 'minecraft:uniform', min: 3, max: 6 }, [['uda:raw_eye_fragment', 20, 1, 4], ['uda:eye_crystal', 10, 1, 2], ['minecraft:emerald', 10, 1, 4],
    ['minecraft:iron_ingot', 12, 2, 5], ['minecraft:ender_pearl', 8, 1, 2], ['minecraft:bone', 15, 2, 6], ['minecraft:cobweb', 6, 1, 3]]),
  { rolls: 1, bonus_rolls: 0, entries: [map('on_sanctuary_maps', 'filled_map.uda.sanctuary')], conditions: [chance(0.4)] },
]);
loot('chests/abyssal_sanctuary', 'minecraft:chest', [
  chestPool({ type: 'minecraft:uniform', min: 3, max: 5 }, [['uda:abyssal_fragment', 8, 1, 2], ['uda:eye_crystal', 14, 1, 3], ['minecraft:obsidian', 10, 2, 5],
    ['minecraft:gold_ingot', 12, 2, 6], ['minecraft:diamond', 5, 1, 2], ['minecraft:ender_pearl', 10, 1, 3]]),
]);
loot('chests/void_cathedral', 'minecraft:chest', [
  chestPool({ type: 'minecraft:uniform', min: 4, max: 7 }, [['uda:eye_crystal', 14, 2, 4], ['uda:eye_core', 6, 1, 1], ['uda:abyssal_scale', 5, 1, 2],
    ['minecraft:diamond', 10, 1, 3], ['minecraft:netherite_scrap', 3, 1, 1], ['minecraft:golden_apple', 8, 1, 2], ['minecraft:experience_bottle', 12, 3, 8]]),
]);

// ---------------------------------------------------------------- tags
const tag = (path, values) => writeJson(`data/${path}.json`, { replace: false, values });
tag('minecraft/tags/block/mineable/pickaxe', ['uda:eye_ore', 'uda:deepslate_eye_ore', 'uda:eye_altar']);
tag('minecraft/tags/block/needs_iron_tool', ['uda:eye_ore', 'uda:deepslate_eye_ore', 'uda:eye_altar']);
tag('c/tags/block/ores', ['uda:eye_ore', 'uda:deepslate_eye_ore']);
tag('c/tags/item/ores', ['uda:eye_ore', 'uda:deepslate_eye_ore']);
tag('minecraft/tags/item/swords', ['uda:eye_sword']);
tag('minecraft/tags/item/axes', ['uda:eye_axe']);
tag('minecraft/tags/item/pickaxes', ['uda:eye_pickaxe']);
tag('minecraft/tags/item/shovels', ['uda:eye_shovel']);
tag('minecraft/tags/item/head_armor', ['uda:eye_helmet']);
tag('minecraft/tags/item/chest_armor', ['uda:eye_chestplate']);
tag('minecraft/tags/item/leg_armor', ['uda:eye_leggings']);
tag('minecraft/tags/item/foot_armor', ['uda:eye_boots']);
tag('minecraft/tags/item/enchantable/bow', ['uda:eye_bow']);
tag('minecraft/tags/item/enchantable/durability', ['uda:eye_bow', 'uda:eye_shield', 'uda:eye_grimoire']);
tag('uda/tags/item/ocular_equipment', ['uda:eye_sword', 'uda:eye_axe', 'uda:eye_pickaxe', 'uda:eye_shovel', 'uda:eye_bow', 'uda:eye_shield',
  'uda:eye_grimoire', 'uda:eye_helmet', 'uda:eye_chestplate', 'uda:eye_leggings', 'uda:eye_boots', 'uda:agamoto_eye']);

// ---------------------------------------------------------------- mundo: minério e mobs
writeJson('data/uda/worldgen/configured_feature/eye_ore.json', {
  type: 'minecraft:ore',
  config: {
    size: 5, discard_chance_on_air_exposure: 0.3,
    targets: [
      { target: { predicate_type: 'minecraft:tag_match', tag: 'minecraft:stone_ore_replaceables' }, state: { Name: 'uda:eye_ore' } },
      { target: { predicate_type: 'minecraft:tag_match', tag: 'minecraft:deepslate_ore_replaceables' }, state: { Name: 'uda:deepslate_eye_ore' } },
    ],
  },
});
writeJson('data/uda/worldgen/placed_feature/eye_ore.json', {
  feature: 'uda:eye_ore',
  placement: [
    { type: 'minecraft:count', count: 7 },
    { type: 'minecraft:in_square' },
    { type: 'minecraft:height_range', height: { type: 'minecraft:trapezoid', min_inclusive: { absolute: -64 }, max_inclusive: { absolute: 16 } } },
    { type: 'minecraft:biome' },
  ],
});
writeJson('data/uda/neoforge/biome_modifier/add_eye_ore.json', { type: 'neoforge:add_features', biomes: '#minecraft:is_overworld', features: 'uda:eye_ore', step: 'underground_ores' });
// Hollow Watcher: nasce em todo o Overworld, mas a regra de spawn só deixa abaixo de y 0 (cavernas profundas)
writeJson('data/uda/neoforge/biome_modifier/spawn_hollow_watcher.json', {
  type: 'neoforge:add_spawns',
  biomes: { type: 'neoforge:and', values: ['#minecraft:is_overworld', { type: 'neoforge:not', value: '#c:is_mushroom' }] },
  spawners: { type: 'uda:hollow_watcher', weight: 40, minCount: 1, maxCount: 2 },
});
// o Blind Executioner agora só aparece nas Ruínas Amaldiçoadas
rmSync(join(DATA, 'uda', 'neoforge', 'biome_modifier', 'spawn_blind_executioner.json'), { force: true });

// ---------------------------------------------------------------- estruturas
const STRUCTURES = {
  cursed_ruins: { templates: [cursedRuins(0), cursedRuins(1)], spacing: 30, separation: 12, salt: 736251,
    biomes: ['#minecraft:is_forest', '#minecraft:is_taiga', '#minecraft:is_savanna', 'minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:swamp',
      'minecraft:snowy_plains', 'minecraft:meadow'],
    spawns: [{ type: 'uda:blind_executioner', weight: 1, minCount: 1, maxCount: 1 }] },
  abyssal_sanctuary: { templates: [abyssalSanctuary()], spacing: 56, separation: 20, salt: 736252,
    biomes: ['#minecraft:is_badlands', 'minecraft:desert', '#minecraft:is_savanna', 'minecraft:plains', 'minecraft:snowy_plains', 'minecraft:swamp',
      'minecraft:mangrove_swamp', 'minecraft:dark_forest'] },
  void_cathedral: { templates: [voidCathedral()], spacing: 72, separation: 28, salt: 736253,
    biomes: ['minecraft:dark_forest', '#minecraft:is_taiga', 'minecraft:snowy_plains', 'minecraft:plains', 'minecraft:forest', 'minecraft:birch_forest',
      'minecraft:meadow'] },
};
for (const [name, s] of Object.entries(STRUCTURES)) {
  s.templates.forEach((t, i) => t.save(s.templates.length > 1 ? `${name}_${i + 1}` : name));
  writeJson(`data/uda/worldgen/template_pool/${name}/start.json`, {
    fallback: 'minecraft:empty',
    elements: s.templates.map((t, i) => ({ weight: 1, element: { element_type: 'minecraft:single_pool_element',
      location: 'uda:' + (s.templates.length > 1 ? `${name}_${i + 1}` : name), projection: 'rigid', processors: 'minecraft:empty' } })),
  });
  writeJson(`data/uda/tags/worldgen/biome/has_structure/${name}.json`, { values: s.biomes });
  writeJson(`data/uda/worldgen/structure/${name}.json`, {
    type: 'minecraft:jigsaw',
    biomes: `#uda:has_structure/${name}`,
    step: 'surface_structures',
    spawn_overrides: s.spawns ? { monster: { bounding_box: 'piece', spawns: s.spawns } } : {},
    terrain_adaptation: 'beard_thin',
    start_pool: `uda:${name}/start`,
    size: 1,
    start_height: { absolute: 0 },
    project_start_to_heightmap: 'WORLD_SURFACE_WG',
    max_distance_from_center: 80,
    use_expansion_hack: false,
  });
  writeJson(`data/uda/worldgen/structure_set/${name}.json`, {
    structures: [{ structure: `uda:${name}`, weight: 1 }],
    placement: { type: 'minecraft:random_spread', spacing: s.spacing, separation: s.separation, salt: s.salt },
  });
}
writeJson('data/uda/tags/worldgen/structure/on_sanctuary_maps.json', { values: ['uda:abyssal_sanctuary'] });
writeJson('data/uda/tags/worldgen/structure/on_cathedral_maps.json', { values: ['uda:void_cathedral'] });

// ---------------------------------------------------------------- conquistas
for (const [id, parent, icon, criterion, frame, ptTitle, ptDesc, enTitle, enDesc] of ADV) {
  const display = {
    icon: { id: icon },
    title: { translate: `advancements.uda.${id}.title` },
    description: { translate: `advancements.uda.${id}.description` },
    frame,
    show_toast: id !== 'root',
    announce_to_chat: frame !== 'task',
  };
  if (!parent) display.background = 'minecraft:textures/block/polished_blackstone_bricks.png';
  const adv = { display, criteria: { [id]: criterion } };
  if (parent) adv.parent = 'uda:progressao/' + parent;
  if (id === 'kill_black_eye') adv.rewards = { experience: 1000 };
  if (id === 'kill_dragon') adv.rewards = { experience: 500 };
  writeJson(`data/uda/advancement/progressao/${id}.json`, adv);
  PT[`advancements.uda.${id}.title`] = ptTitle;
  PT[`advancements.uda.${id}.description`] = ptDesc;
  EN[`advancements.uda.${id}.title`] = enTitle;
  EN[`advancements.uda.${id}.description`] = enDesc;
}
writeJson('assets/uda/lang/pt_br.json', PT);
writeJson('assets/uda/lang/en_us.json', EN);
console.log('Dados gerados: traduções, sons, receitas, drops, tags, minério, estruturas e conquistas.');
