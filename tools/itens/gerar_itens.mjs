// Gera as texturas e os modelos dos itens e blocos novos (pixel art no estilo do Minecraft).
//
//   node tools/itens/gerar_itens.mjs
//
// - Itens 2D (16x16): materiais, núcleos (animados), Olho de Agamoto e armadura (selado/desperto/supremo).
// - Blocos: Minério do Olho (a pedra é a do próprio Minecraft; o mod só desenha os olhos por cima, brilhando
//   e piscando), Altar do Olho, Altar Abissal e Altar do Vazio (modelos 3D).
// - Camadas da Armadura Ocular (64x32) nas três formas + o brilho.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Tex, mix, rgb, seeded } from '../texturas/png.mjs';
import { PURPLE, Skin, box as faces, stamp, upscale, board } from '../texturas/pintura.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const ASSETS = join(ROOT, 'src', 'main', 'resources', 'assets', 'uda');
const PREVIEWS = join(ROOT, 'previas');

const writeJson = (file, obj) => {
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
};
const itemTex = (name) => join(ASSETS, 'textures', 'item', name + '.png');
const blockTex = (name) => join(ASSETS, 'textures', 'block', name + '.png');

// ---------------------------------------------------------------- cores

const C = {
  out: '#07060a',
  plate: ['#0f0d14', '#17141f', '#211c2b', '#2c2638', '#393147', '#4a4159'],
  stone: ['#1b1822', '#26222e', '#322d3c', '#3f394b', '#4d465b'],
  bone: ['#25212d', '#302b39', '#3e3849', '#4e4759', '#5f586b'],
  metal: ['#332e3c', '#4d4658', '#6c637a', '#91879f', '#bcb2ca'],
  ...PURPLE,
};

// olhos pequenos: R aro, P roxo, N neon, W brilho, K pupila, L costura fechada, s pálpebra, D órbita
const EYE_COLORS = (state) => ({
  R: state === 'sealed' ? '#1e0b30' : C.dark,
  P: state === 'supreme' ? C.bright : C.base,
  N: state === 'supreme' ? C.white : C.neon,
  W: '#ffffff',
  K: '#050208',
  L: state === 'sealed' ? '#4b1a80' : C.bright,
  s: '#1b1624',
  D: '#060309',
});

function put(t, rows, colors, x0 = 0, y0 = 0) {
  rows.forEach((row, j) => [...row].forEach((ch, i) => {
    if (ch === '.' || ch === ' ') return;
    const c = typeof colors === 'function' ? colors(ch, x0 + i, y0 + j) : colors[ch];
    if (c === undefined) throw new Error('cor desconhecida: ' + ch);
    if (c) t.set(x0 + i, y0 + j, c);
  }));
}

/** Contorno escuro em volta da silhueta (como os itens vanilla). */
function outline(t, color = C.out) {
  const solid = (x, y) => x >= 0 && y >= 0 && x < t.w && y < t.h && t.get(x, y)[3] > 0;
  const add = [];
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    if (solid(x, y)) continue;
    if (solid(x + 1, y) || solid(x - 1, y) || solid(x, y + 1) || solid(x, y - 1)) add.push([x, y]);
  }
  for (const [x, y] of add) t.set(x, y, color);
}

/** Preenche onde inside(x,y) com cor sombreada (luz vindo de cima-esquerda). */
function fill(t, inside, pal, seed, opts = {}) {
  const rand = seeded(seed);
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    const n = rand();
    if (!inside(x, y)) continue;
    const light = opts.light ? opts.light(x, y) : 1 - (x + y) / (t.w + t.h);
    let l = Math.round(light * (pal.length - 1));
    if (n < 0.15) l--;
    else if (n > 0.88) l++;
    t.set(x, y, pal[Math.max(0, Math.min(pal.length - 1, l))]);
  }
}

const circle = (cx, cy, r) => (x, y) => (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r;
const poly = (pts) => (x, y) => {
  const px = x + 0.5, py = y + 0.5;
  let inside = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [xi, yi] = pts[i], [xj, yj] = pts[j];
    if (yi > py !== yj > py && px < ((xj - xi) * (py - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
};

function strip(frames) {
  const t = new Tex(frames[0].w, frames[0].h * frames.length);
  frames.forEach((f, i) => {
    for (let y = 0; y < f.h; y++) for (let x = 0; x < f.w; x++) t.set(x, y + i * f.h, f.get(x, y));
  });
  return t;
}

function saveAnimated(file, frames, frametime, order) {
  strip(frames).save(file);
  const animation = { frametime };
  if (order) animation.frames = order;
  writeFileSync(file + '.mcmeta', JSON.stringify({ animation }, null, 2) + '\n');
}

// ================================================================ ITENS

function rawFragment() {
  const t = new Tex(16, 16);
  fill(t, poly([[3, 6], [6, 2.5], [10, 3], [13.5, 6], [13, 10.5], [10, 13.5], [5, 13], [2.5, 10]]), C.stone, 'raw');
  put(t, ['NP', 'PR'], { N: C.neon, P: C.base, R: C.dark }, 9, 9);
  put(t, ['P', 'R'], { P: C.base, R: C.dark }, 5, 10);
  put(t, ['RPR', 'PKN', 'RPR'], EYE_COLORS('awakened'), 6, 5);
  outline(t);
  return t;
}

function crystal() {
  const t = new Tex(16, 16);
  const facet = (x0, x1, top, bottom, seed, eye) => {
    const ins = poly([[x0, bottom], [x0, top + 2], [(x0 + x1) / 2, top], [x1, top + 2], [x1, bottom]]);
    fill(t, ins, ['#2a0d45', C.dark, C.mid, C.base, C.bright, C.neon], seed, { light: (x) => 1 - (x - x0) / (x1 - x0) * 0.9 });
    if (eye) put(t, eye, EYE_COLORS('awakened'), Math.round((x0 + x1) / 2) - 1, Math.round((top + bottom) / 2));
  };
  facet(2.5, 6.5, 6, 14, 'c2');
  facet(9.5, 13, 5, 14, 'c3');
  facet(5.5, 10.5, 1, 14.5, 'c1', ['RPR', 'NKN']);
  for (const [x, y] of [[7, 3], [8, 2], [3, 8], [10, 7]]) t.set(x, y, C.white);
  outline(t);
  return t;
}

function ingot() {
  const t = new Tex(16, 16);
  fill(t, poly([[1.5, 8.5], [9, 4.5], [14.5, 7.5], [7, 11.5]]), C.plate.slice(2), 'i1', { light: () => 0.85 });
  fill(t, poly([[1.5, 8.5], [7, 11.5], [7, 13.5], [1.5, 10.5]]), C.plate.slice(0, 3), 'i2', { light: () => 0.6 });
  fill(t, poly([[7, 11.5], [14.5, 7.5], [14.5, 9.5], [7, 13.5]]), C.plate.slice(0, 3), 'i3', { light: () => 0.3 });
  for (let i = 0; i < 6; i++) t.set(2 + i, 8 - Math.floor(i / 2), C.mid); // aresta de cima
  put(t, ['RPR', 'NKN'], EYE_COLORS('awakened'), 7, 7);
  outline(t);
  return t;
}

/** Núcleo do Olho: esfera negra num aro de metal; o olho pisca e o aro pulsa. awakened: aceso e rachado. */
function core(awakened, frame) {
  const t = new Tex(16, 16);
  const pulse = [0, 1, 2, 3, 2, 1, 0, 0][frame];
  const rim = awakened ? [C.mid, C.base, C.bright, C.neon] : ['#2a1240', C.dark, C.mid, C.base];
  fill(t, circle(8, 8, 7), C.metal.slice(0, 4), 'core-rim');
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
    if (d <= 7 && d > 5.6 && (x + y + frame) % 3 === 0) t.set(x, y, rim[Math.min(3, pulse + (awakened ? 1 : 0))]);
    if (d <= 5.6) t.set(x, y, mix('#050407', awakened ? C.deep : '#120a1a', Math.max(0, 1 - d / 5.6)));
  }
  if (awakened) {
    for (const [x, y] of [[3, 5], [4, 4], [12, 11], [11, 12], [4, 11], [12, 4]]) t.set(x, y, C.bright);
  }
  const lid = [0, 0, 0, 0, 1, 2, 1, 0][frame]; // 0 aberto, 1 meio, 2 fechado
  const open = ['.RPPR.', 'RNKKNR', 'RPKKPR', '.RPPR.'];
  const half = ['.ssss.', 'ssssss', 'RPKKPR', '.RPPR.'];
  const shut = ['......', '.ssss.', 'LLLLLL', '.ssss.'];
  put(t, [open, half, shut][lid], EYE_COLORS(awakened ? 'supreme' : 'awakened'), 5, 6);
  outline(t);
  return t;
}

function watcherEye() {
  const t = new Tex(16, 16);
  // moldura de osso irregular
  fill(t, (x, y) => circle(8, 8, 7.2)(x, y) && !circle(8, 8, 4.2)(x, y) && (x + y * 3) % 7 !== 0, C.bone, 'bone');
  fill(t, circle(8, 8, 4.3), ['#120a1a', C.deep, C.dark], 'eyeball', { light: (x, y) => 1 - Math.hypot(x - 8, y - 8) / 4.3 });
  put(t, ['.RPR.', 'RNKNR', 'RPKPR', 'RNKNR', '.RPR.'], EYE_COLORS('awakened'), 6, 6);
  t.set(6, 6, C.white);
  for (const [x, y] of [[1, 8], [8, 1], [14, 8], [8, 14]]) t.set(x, y, C.bone[4]);
  outline(t);
  return t;
}

function abyssalFragment() {
  const t = new Tex(16, 16);
  fill(t, poly([[8.5, 1], [12.5, 6], [11, 14.5], [6, 15], [3.5, 8.5]]), ['#0a0610', '#140a1f', '#1e0b30', '#2d1048', '#3a1060'], 'abf');
  // rachadura acesa de cima a baixo
  for (const [x, y] of [[8, 2], [8, 3], [9, 4], [9, 5], [8, 6], [8, 7], [7, 8], [8, 9], [8, 10], [9, 11], [8, 12], [8, 13]]) t.set(x, y, C.neon);
  for (const [x, y] of [[9, 3], [10, 5], [7, 7], [9, 10], [7, 12]]) t.set(x, y, C.base);
  outline(t);
  return t;
}

function abyssalScale() {
  const t = new Tex(16, 16);
  const ins = (x, y) => ((x + 0.5 - 8) ** 2) / 40 + ((y + 0.5 - 6.5) ** 2) / 26 <= 1 || poly([[2.5, 7], [13.5, 7], [8, 15]])(x, y);
  fill(t, ins, C.plate, 'scale');
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (!ins(x, y)) continue;
    const edge = !ins(x + 1, y) || !ins(x - 1, y) || !ins(x, y + 1) || !ins(x, y - 1);
    if (edge) t.set(x, y, C.mid);
    else if ((y === 5 || y === 9) && x % 2 === 0) t.set(x, y, C.plate[0]);
  }
  put(t, ['RPR', 'NKN', 'RPR'], EYE_COLORS('awakened'), 7, 6);
  outline(t);
  return t;
}

function abyssalHeart(frame) {
  const t = new Tex(16, 16);
  const beat = [0, 1, 0, 0, 2, 0, 0, 0][frame];
  const ins = (x, y) => circle(5.5, 6, 3.6)(x, y) || circle(10.5, 6, 3.6)(x, y) || poly([[2, 7], [14, 7], [8, 14.5]])(x, y);
  fill(t, ins, ['#120614', '#1e0a24', '#2a0d36', '#3a1048', '#4a1460'], 'heart');
  const vein = [C.mid, C.bright, C.neon][beat];
  for (const [x, y] of [[8, 5], [8, 6], [7, 7], [8, 8], [9, 9], [8, 10], [8, 11], [6, 6], [5, 7], [10, 6], [11, 7], [4, 5], [12, 5]]) t.set(x, y, vein);
  put(t, ['PN', 'KP'], EYE_COLORS('awakened'), 4, 8);
  put(t, ['NP', 'PK'], EYE_COLORS('awakened'), 10, 8);
  if (beat === 2) {
    for (const [x, y] of [[3, 3], [12, 3]]) t.set(x, y, C.neon);
  }
  outline(t);
  return t;
}

function voidEssence(frame) {
  const t = new Tex(16, 16);
  // gargalo e rolha
  put(t, ['.cc.', '.cc.'], { c: '#3a2a20' }, 6, 1);
  put(t, ['g..g', 'g..g'], { g: '#6c637a' }, 6, 3);
  const body = circle(8, 10, 5);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (!body(x, y) && !(x >= 7 && x <= 8 && y >= 3 && y <= 5)) continue;
    const a = Math.atan2(y + 0.5 - 10, x + 0.5 - 8) + frame * 0.785;
    const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 10);
    const swirl = Math.sin(a * 2 + d * 1.3);
    t.set(x, y, swirl > 0.55 ? C.mid : swirl > 0.1 ? C.deep : '#050407');
  }
  const stars = [[6, 9], [10, 11], [8, 13], [5, 12], [11, 8]];
  stars.forEach(([x, y], i) => { if ((i + frame) % 3 !== 0) t.set(x, y, C.white); });
  // brilho do vidro
  for (const [x, y] of [[5, 8], [5, 9], [6, 7]]) t.set(x, y, '#bcb2ca');
  outline(t, '#2a2433');
  return t;
}

function agamoto(state) {
  const t = new Tex(16, 16);
  // corrente
  put(t, ['.c.', 'c.c', '.c.', '.c.'], { c: C.metal[2] }, 7, 0);
  const ring = (x, y) => circle(8, 9.5, 5.8)(x, y) && !circle(8, 9.5, 4)(x, y);
  fill(t, ring, state === 'supreme' ? ['#4d4658', '#91879f', '#bcb2ca', '#fbe6ff'] : C.metal.slice(0, 4), 'aga-ring');
  fill(t, circle(8, 9.5, 4.1), ['#050407', '#0c0910', '#141019'], 'aga-in');
  // runas no aro
  for (const [x, y] of [[3, 9], [12, 9], [8, 4], [8, 15], [4, 6], [11, 13]]) t.set(x, y, state === 'sealed' ? '#2a1240' : C.bright);
  const eye = state === 'sealed' ? ['......', '.ssss.', 'LLLLLL', '.ssss.'] : ['.RPPR.', 'RNKKNR', 'RPKKPR', '.RPPR.'];
  put(t, eye, EYE_COLORS(state), 5, 8);
  if (state === 'supreme') {
    for (const [x, y] of [[1, 9], [15, 9], [8, 2], [3, 4], [13, 4]]) t.set(x, y, C.neon);
  }
  outline(t);
  return t;
}

const ARMOR_MASKS = {
  helmet: ['................', '................', '................', '.....######.....', '....########....', '...##########...', '...##########...',
    '...###....###...', '...###....###...', '...##......##...', '................', '................', '................', '................',
    '................', '................'],
  chestplate: ['................', '................', '.#####....#####.', '.#####....#####.', '.######..######.', '.##############.', '.##############.',
    '.##############.', '...##########...', '...##########...', '...##########...', '...##########...', '...##########...', '....########....',
    '.....######.....', '................'],
  leggings: ['................', '................', '....########....', '...##########...', '...##########...', '...##########...', '...####..####...',
    '...####..####...', '...####..####...', '...####..####...', '...####..####...', '...####..####...', '...####..####...', '...####..####...',
    '................', '................'],
  boots: ['................', '................', '................', '................', '................', '...###....###...', '...###....###...',
    '...###....###...', '...####..####...', '..#####..#####..', '.######..######.', '.######..######.', '.####......####.', '................',
    '................', '................'],
};
const ARMOR_EYES = { helmet: [[6, 4]], chestplate: [[6, 6]], leggings: [[4, 8], [9, 8]], boots: [[3, 9], [10, 9]] };

function armorIcon(piece, state) {
  const t = new Tex(16, 16);
  const mask = ARMOR_MASKS[piece];
  const ins = (x, y) => mask[y]?.[x] === '#';
  fill(t, ins, C.plate, 'arm-' + piece);
  const trim = state === 'sealed' ? '#2a1240' : state === 'supreme' ? C.neon : C.mid;
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (!ins(x, y)) continue;
    const edge = !ins(x, y - 1) || !ins(x, y + 1) && y > 8;
    if (edge) t.set(x, y, trim);
  }
  if (state === 'supreme') {
    const r = seeded('supreme-' + piece);
    for (let i = 0; i < 6; i++) {
      const x = 2 + Math.floor(r() * 12), y = 3 + Math.floor(r() * 10);
      if (ins(x, y)) t.set(x, y, C.bright);
    }
  }
  for (const [x, y] of ARMOR_EYES[piece]) {
    const w = piece === 'leggings' || piece === 'boots' ? 3 : 4;
    const rows = state === 'sealed' ? [w === 4 ? '.ss.' : 'sss', w === 4 ? 'LLLL' : 'LLL'] : w === 4 ? ['RPPR', 'NKKN'] : ['RPR', 'NKN'];
    put(t, rows, EYE_COLORS(state), x, y);
  }
  outline(t);
  return t;
}

// ================================================================ BLOCOS

/** Olhos do minério (por cima da pedra vanilla). Cada olho pisca num momento diferente. */
function oreEyes(frame) {
  const t = new Tex(16, 16);
  const eyes = [
    { x: 2, y: 3, rows: ['DRPRD', 'RNKNR', 'DRPRD'], closed: ['DsssD', 'DLLLD', 'DsssD'], blink: [3, 4] },
    { x: 10, y: 8, rows: ['RPR', 'NKN', 'RPR'], closed: ['sss', 'LLL', 'sss'], blink: [6, 7] },
    { x: 5, y: 11, rows: ['RPPR', 'PKKN'], closed: ['ssss', 'LLLL'], blink: [1] },
    { x: 11, y: 2, rows: ['PN', 'KP'], closed: ['ss', 'LL'], blink: [5] },
  ];
  for (const e of eyes) put(t, e.blink.includes(frame) ? e.closed : e.rows, EYE_COLORS('awakened'), e.x, e.y);
  // veios roxos
  for (const [x, y] of [[7, 6], [8, 7], [8, 8], [1, 9], [2, 10], [13, 13], [14, 12]]) t.set(x, y, C.mid);
  return t;
}

function stoneTex(seed, pal, opts = {}) {
  const t = new Tex(16, 16);
  const rand = seeded(seed);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let l = 2;
    const n = rand();
    if (n < 0.2) l--;
    else if (n > 0.85) l++;
    if (opts.bricks) {
      const row = Math.floor(y / 4);
      const off = row % 2 ? 4 : 0;
      if (y % 4 === 3 || (x + off) % 8 === 7) l -= 2;
      else if (y % 4 === 0) l++;
    }
    t.set(x, y, pal[Math.max(0, Math.min(pal.length - 1, l))]);
  }
  for (const [x, y] of opts.glow || []) t.set(x, y, C.mid);
  return t;
}

function runeTop(seed, pal, glow) {
  const t = stoneTex(seed, pal);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
    if (Math.abs(d - 6.5) < 0.6) t.set(x, y, glow);
    else if (Math.abs(d - 4.5) < 0.5 && (x + y) % 2 === 0) t.set(x, y, mix(glow, '#000000', 0.4));
  }
  return t;
}

function altarEye(open, frame = 0) {
  const t = new Tex(16, 16);
  const rows = open
    ? ['....RRRRRRRR....', '..RRPPPPPPPPRR..', '.RPPNNNKKNNNPPR.', 'RPPNNNKKKKNNNPPR', 'RPPNNNKKKKNNNPPR', '.RPPNNNKKNNNPPR.', '..RRPPPPPPPPRR..',
      '....RRRRRRRR....']
    : ['................', '................', '..ssssssssssss..', '.ssssssssssssss.', 'LLLLLLLLLLLLLLLL', '.ssssssssssssss.', '..ssssssssssss..',
      '................'];
  put(t, rows, (ch) => {
    const c = EYE_COLORS(open ? 'awakened' : 'sealed')[ch];
    if (open && frame % 2 && (ch === 'N')) return C.bright;
    return c;
  }, 0, 4);
  return t;
}

function shardTex() {
  const t = new Tex(16, 16);
  fill(t, () => true, ['#2a0d45', C.dark, C.mid, C.base, C.bright], 'shard', { light: (x) => 1 - x / 16 });
  for (let i = 0; i < 16; i++) t.set(i, (i * 5) % 16, C.neon);
  return t;
}

// ---------------------------------------------------------------- modelos de bloco

const EMISSIVE = { block_light: 15, sky_light: 15 };
const allFaces = (texture, extra = {}) => Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map((f) => [f, { texture, ...extra }]));
const cube = (from, to, texture, opts = {}) => ({ from, to, faces: allFaces(texture, opts.faceExtra || {}), ...(opts.emissive ? { shade: false, neoforge_data: EMISSIVE } : {}) });

function oreModel(name, base) {
  writeJson(join(ASSETS, 'models', 'block', name + '.json'), {
    parent: 'minecraft:block/block',
    render_type: 'minecraft:cutout',
    textures: { particle: base, base, eyes: 'uda:block/eye_ore_eyes' },
    elements: [
      { from: [0, 0, 0], to: [16, 16, 16], faces: Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map((f) => [f, { texture: '#base', cullface: f }])) },
      { from: [0, 0, 0], to: [16, 16, 16], shade: false, neoforge_data: EMISSIVE,
        faces: Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map((f) => [f, { texture: '#eyes', cullface: f }])) },
    ],
  });
  writeJson(join(ASSETS, 'blockstates', name + '.json'), { variants: { '': { model: 'uda:block/' + name } } });
  writeJson(join(ASSETS, 'models', 'item', name + '.json'), { parent: 'uda:block/' + name });
}

function eyeAltarModels() {
  for (const active of [false, true]) {
    const name = 'eye_altar' + (active ? '_active' : '');
    writeJson(join(ASSETS, 'models', 'block', name + '.json'), {
      parent: 'minecraft:block/block',
      render_type: 'minecraft:cutout',
      textures: { particle: 'uda:block/eye_altar_side', side: 'uda:block/eye_altar_side', top: 'uda:block/eye_altar_top',
        eye: active ? 'uda:block/eye_altar_eye_open' : 'uda:block/eye_altar_eye' },
      elements: [
        cube([1, 0, 1], [15, 4, 15], '#side'),
        cube([4, 4, 4], [12, 11, 12], '#side'),
        { ...cube([2, 11, 2], [14, 14, 14], '#side'), faces: { ...allFaces('#side'), up: { texture: '#top' } } },
        { from: [4, 14.01, 4], to: [12, 14.01, 12], shade: false, ...(active ? { neoforge_data: EMISSIVE } : {}), faces: { up: { texture: '#eye' } } },
      ],
    });
  }
  writeJson(join(ASSETS, 'blockstates', 'eye_altar.json'), {
    variants: { 'active=false': { model: 'uda:block/eye_altar' }, 'active=true': { model: 'uda:block/eye_altar_active' } },
  });
  writeJson(join(ASSETS, 'models', 'item', 'eye_altar.json'), { parent: 'uda:block/eye_altar' });
}

function abyssalAltarModels() {
  const corners = [[1, 1], [12, 1], [1, 12], [12, 12]];
  for (let n = 0; n <= 4; n++) {
    const elements = [
      { ...cube([0, 0, 0], [16, 6, 16], '#side'), faces: { ...allFaces('#side'), up: { texture: '#top' } } },
      cube([3, 6, 3], [13, 12, 13], '#side'),
      { from: [4, 12.01, 4], to: [12, 12.01, 12], shade: false, neoforge_data: EMISSIVE, faces: { up: { texture: '#eye' } } },
    ];
    corners.slice(0, n).forEach(([x, z]) => elements.push(cube([x, 6, z], [x + 3, 12, z + 3], '#shard', { emissive: true })));
    writeJson(join(ASSETS, 'models', 'block', 'abyssal_altar_' + n + '.json'), {
      parent: 'minecraft:block/block',
      textures: { particle: 'uda:block/abyssal_altar_side', side: 'uda:block/abyssal_altar_side', top: 'uda:block/abyssal_altar_top',
        shard: 'uda:block/abyssal_shard', eye: n === 4 ? 'uda:block/eye_altar_eye_open' : 'uda:block/eye_altar_eye' },
      elements,
    });
  }
  const variants = {};
  for (let n = 0; n <= 4; n++) for (const a of [false, true]) variants[`active=${a},fragments=${n}`] = { model: 'uda:block/abyssal_altar_' + n };
  writeJson(join(ASSETS, 'blockstates', 'abyssal_altar.json'), { variants });
  writeJson(join(ASSETS, 'models', 'item', 'abyssal_altar.json'), { parent: 'uda:block/abyssal_altar_4' });
}

function voidAltarModels() {
  for (const active of [false, true]) {
    const name = 'void_altar' + (active ? '_active' : '');
    writeJson(join(ASSETS, 'models', 'block', name + '.json'), {
      parent: 'minecraft:block/block',
      render_type: 'minecraft:cutout',
      textures: { particle: 'uda:block/void_altar_side', side: 'uda:block/void_altar_side', top: 'uda:block/void_altar_top',
        eye: active ? 'uda:block/eye_altar_eye_open' : 'uda:block/eye_altar_eye' },
      elements: [
        cube([0, 0, 0], [16, 4, 16], '#side'),
        cube([2, 4, 2], [14, 14, 14], '#side'),
        { ...cube([1, 14, 1], [15, 16, 15], '#side'), faces: { ...allFaces('#side'), up: { texture: '#top' } } },
        { from: [3, 16.01, 3], to: [13, 16.01, 13], shade: false, neoforge_data: EMISSIVE, faces: { up: { texture: '#eye' } } },
        { from: [4, 6, 1.99], to: [12, 12, 1.99], shade: false, ...(active ? { neoforge_data: EMISSIVE } : {}), faces: { north: { texture: '#eye' } } },
      ],
    });
  }
  writeJson(join(ASSETS, 'blockstates', 'void_altar.json'), {
    variants: { 'active=false': { model: 'uda:block/void_altar' }, 'active=true': { model: 'uda:block/void_altar_active' } },
  });
  writeJson(join(ASSETS, 'models', 'item', 'void_altar.json'), { parent: 'uda:block/void_altar' });
}

// ================================================================ ARMADURA (64x32)

const STATES = ['sealed', 'awakened', 'supreme'];

/** Pinta uma face de armadura (rows = linhas a pintar, ex.: [0, 7]) com placas, frisos e rachaduras. */
function armorFace(S, r, state, seed, opts = {}) {
  const rand = seeded(seed);
  const [y0, y1] = opts.rows || [0, r.h - 1];
  const crack = state === 'sealed' ? null : state === 'supreme' ? [C.bright, C.neon] : [C.mid, C.base];
  const trim = state === 'sealed' ? ['#2a1240', null] : state === 'supreme' ? [C.bright, C.neon] : [C.mid, C.base];
  for (let j = y0; j <= y1; j++) for (let i = 0; i < r.w; i++) {
    const n = rand();
    if (opts.hole && opts.hole(i, j)) continue;
    let l = 2 + (j === y0 ? 1 : 0) - (j === y1 ? 1 : 0);
    if (n < 0.15) l--;
    else if (n > 0.86) l++;
    S.px(r.x + i, r.y + j, C.plate[Math.max(0, Math.min(5, l))]);
    if ((j === y0 || j === y1) && opts.trimEdges) S.px(r.x + i, r.y + j, trim[0], trim[1]);
    else if (crack && n > (state === 'supreme' ? 0.93 : 0.965)) S.px(r.x + i, r.y + j, crack[0], crack[1]);
  }
}

function armorEye(S, x, y, state, wide = true) {
  const colors = { R: [EYE_COLORS(state).R, null], P: [EYE_COLORS(state).P, EYE_COLORS(state).P], N: [EYE_COLORS(state).N, EYE_COLORS(state).N],
    K: ['#050208', null], L: state === 'sealed' ? ['#4b1a80', '#4b1a80'] : [C.bright, C.neon], s: ['#1b1624', null] };
  if (state === 'sealed') colors.P[1] = colors.N[1] = null;
  const rows = state === 'sealed' ? (wide ? ['.ss.', 'LLLL', '.ss.'] : ['sss', 'LLL']) : wide ? ['RPPR', 'NKKN', 'RPPR'] : ['RPR', 'NKN'];
  stamp(S, x, y, rows, colors);
}

function armorLayers(state) {
  // ---- layer 1: capacete, peitoral (+ ombros) e botas
  const L1 = new Skin(64, 32);
  const head = faces(0, 0, 8, 8, 8);
  for (const f of ['up', 'right', 'left', 'back']) armorFace(L1, head[f], state, 'h-' + f, { trimEdges: f !== 'up' });
  armorFace(L1, head.front, state, 'h-front', { trimEdges: true, hole: (i, j) => j >= 4 && i >= 2 && i <= 5 });
  armorEye(L1, head.front.x + 2, head.front.y + 1, state);
  const body = faces(16, 16, 8, 12, 4);
  for (const f of ['up', 'down', 'right', 'left', 'back']) armorFace(L1, body[f], state, 'b-' + f, { trimEdges: true });
  armorFace(L1, body.front, state, 'b-front', { trimEdges: true });
  armorEye(L1, body.front.x + 2, body.front.y + 3, state);
  if (state !== 'sealed') armorEye(L1, body.back.x + 2, body.back.y + 5, state);
  const arm = faces(40, 16, 4, 12, 4);
  for (const f of ['right', 'front', 'left', 'back']) armorFace(L1, arm[f], state, 'a-' + f, { rows: [0, 6], trimEdges: true });
  armorFace(L1, arm.up, state, 'a-up');
  armorEye(L1, arm.left.x + 0, arm.left.y + 2, state, false);
  armorEye(L1, arm.right.x + 1, arm.right.y + 2, state, false);
  const leg = faces(0, 16, 4, 12, 4);
  for (const f of ['right', 'front', 'left', 'back']) armorFace(L1, leg[f], state, 'f-' + f, { rows: [6, 11], trimEdges: true });
  armorFace(L1, leg.down, state, 'f-down');
  armorEye(L1, leg.front.x + 0, leg.front.y + 8, state, false);
  // ---- layer 2: calças
  const L2 = new Skin(64, 32);
  const legs = faces(0, 16, 4, 12, 4);
  for (const f of ['right', 'front', 'left', 'back']) armorFace(L2, legs[f], state, 'l-' + f, { rows: [0, 8], trimEdges: true });
  armorFace(L2, legs.up, state, 'l-up');
  armorEye(L2, legs.front.x + 0, legs.front.y + 4, state, false);
  const waist = faces(16, 16, 8, 12, 4);
  for (const f of ['right', 'front', 'left', 'back']) armorFace(L2, waist[f], state, 'w-' + f, { rows: [7, 11], trimEdges: true });
  armorEye(L2, waist.front.x + 2, waist.front.y + 8, state);
  return [L1, L2];
}

// ================================================================ main

const sheet = [];
const save = (name, tex) => {
  tex.save(itemTex(name));
  sheet.push({ label: name.replace(/_/g, ' '), img: upscale(tex, 6, ['#2a2a30', '#31313a']) });
};
const flat = (name, extra = {}) => writeJson(join(ASSETS, 'models', 'item', name + '.json'), {
  parent: 'minecraft:item/generated', textures: { layer0: 'uda:item/' + name }, ...extra,
});

save('raw_eye_fragment', rawFragment());
save('eye_crystal', crystal());
save('eye_ingot', ingot());
save('watcher_eye', watcherEye());
save('abyssal_fragment', abyssalFragment());
save('abyssal_scale', abyssalScale());
for (const name of ['raw_eye_fragment', 'eye_crystal', 'eye_ingot', 'watcher_eye', 'abyssal_fragment', 'abyssal_scale']) flat(name);

// animados: núcleos, coração, essência
const coreFrames = [0, 1, 2, 3, 4, 5, 6, 7].map((f) => core(false, f));
saveAnimated(itemTex('eye_core'), coreFrames, 3);
sheet.push({ label: 'eye core', img: upscale(coreFrames[0], 6, ['#2a2a30', '#31313a']) });
const awakenedCore = [0, 1, 2, 3, 4, 5, 6, 7].map((f) => core(true, f));
saveAnimated(itemTex('awakened_eye_core'), awakenedCore, 2);
sheet.push({ label: 'awakened eye core', img: upscale(awakenedCore[2], 6, ['#2a2a30', '#31313a']) });
const heart = [0, 1, 2, 3, 4, 5, 6, 7].map((f) => abyssalHeart(f));
saveAnimated(itemTex('abyssal_heart'), heart, 3);
sheet.push({ label: 'abyssal heart', img: upscale(heart[4], 6, ['#2a2a30', '#31313a']) });
const essence = [0, 1, 2, 3, 4, 5, 6, 7].map((f) => voidEssence(f));
saveAnimated(itemTex('void_essence'), essence, 3);
sheet.push({ label: 'void essence', img: upscale(essence[0], 6, ['#2a2a30', '#31313a']) });
for (const name of ['eye_core', 'awakened_eye_core', 'abyssal_heart', 'void_essence']) flat(name);

// Olho de Agamoto e armadura: uma textura por forma; o modelo escolhe pela propriedade uda:tier
const tiered = (name, paint) => {
  STATES.forEach((state, tier) => save(tier ? `${name}_${state}` : name, paint(state)));
  flat(name, { overrides: [{ predicate: { 'uda:tier': 1 }, model: `uda:item/${name}_awakened` }, { predicate: { 'uda:tier': 2 }, model: `uda:item/${name}_supreme` }] });
  flat(`${name}_awakened`);
  flat(`${name}_supreme`);
};
tiered('agamoto_eye', agamoto);
for (const piece of ['helmet', 'chestplate', 'leggings', 'boots']) tiered('eye_' + piece, (state) => armorIcon(piece, state));

STATES.forEach((state) => {
  const [l1, l2] = armorLayers(state);
  const dir = join(ASSETS, 'textures', 'models', 'armor');
  l1.base.save(join(dir, `ocular_${state}_layer_1.png`));
  l2.base.save(join(dir, `ocular_${state}_layer_2.png`));
  if (state !== 'sealed') {
    l1.glow.save(join(dir, `ocular_${state}_layer_1_glow.png`));
    l2.glow.save(join(dir, `ocular_${state}_layer_2_glow.png`));
  }
  sheet.push({ label: `armadura ${state}`, img: upscale(l1.base, 4, ['#2a2a30', '#31313a']) });
});

// blocos
saveAnimated(blockTex('eye_ore_eyes'), [0, 1, 2, 3, 4, 5, 6, 7].map((f) => oreEyes(f)), 6);
oreModel('eye_ore', 'minecraft:block/stone');
oreModel('deepslate_eye_ore', 'minecraft:block/deepslate');
stoneTex('altar-side', C.plate, { bricks: true, glow: [[3, 1], [11, 5], [6, 9], [13, 13]] }).save(blockTex('eye_altar_side'));
runeTop('altar-top', C.plate, C.mid).save(blockTex('eye_altar_top'));
altarEye(false).save(blockTex('eye_altar_eye'));
saveAnimated(blockTex('eye_altar_eye_open'), [0, 1].map((f) => altarEye(true, f)), 8);
stoneTex('abyssal-side', ['#07050b', '#0e0915', '#160d22', '#20112f', '#2c163f'], { glow: [[2, 3], [9, 7], [5, 12], [13, 2], [12, 11]] }).save(blockTex('abyssal_altar_side'));
runeTop('abyssal-top', ['#07050b', '#0e0915', '#160d22', '#20112f', '#2c163f'], C.base).save(blockTex('abyssal_altar_top'));
shardTex().save(blockTex('abyssal_shard'));
stoneTex('void-side', ['#020203', '#060508', '#0b090e', '#121017', '#1a1720'], { bricks: true, glow: [[4, 2], [12, 6], [2, 10], [9, 13]] }).save(blockTex('void_altar_side'));
runeTop('void-top', ['#020203', '#060508', '#0b090e', '#121017', '#1a1720'], C.bright).save(blockTex('void_altar_top'));
eyeAltarModels();
abyssalAltarModels();
voidAltarModels();

board('itens novos', sheet.slice(0, 13)).save(join(PREVIEWS, '5_itens_novos.png'));
board('armadura ocular', sheet.slice(13)).save(join(PREVIEWS, '6_armadura.png'));
console.log('Itens, blocos e armadura gerados.');
