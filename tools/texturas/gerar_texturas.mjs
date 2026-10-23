// Gera as texturas do Ultimate Dark Adventure em pixel art.
//
//   node tools/texturas/gerar_texturas.mjs            -> grava em src/main/resources/assets/uda/textures
//   As prévias ampliadas vão para previas/ (ou para a <pasta> passada como argumento).
//
// Cada mob tem 3 variantes, todas com o mesmo desenho base (as rachaduras ficam no mesmo lugar):
//   dormant  = olhos fechados, energia selada (quase sem brilho)
//   blink    = olhos fechados, mas a energia já vaza (transição e piscadas)
//   awakened = olhos abertos, roxo neon
// Para cada variante existe uma "<nome>_glow.png", desenhada em brilho total por cima do mob.

import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Tex, mix, rgb, seeded } from './png.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'src', 'main', 'resources', 'assets', 'uda', 'textures');
const PREVIEW_DIR = process.argv[2] || null;

import { PAL, PURPLE, VARIANTS, tone, eyeColors, Skin, R, box, SIDES, clear, crackSet, surface, stamp, trim, FONT, text, textWidth, upscale, night, doll, BG, NIGHT_BG, LABEL, TITLE, board } from './pintura.mjs';

// ================================================================ HOLLOW WATCHER (64x64)
// Partes (iguais a HollowWatcherModel.createBodyLayer):
//   cabeça (0,0) 8x8x8 · corpo/costelas (16,16) 8x12x4 · braços (40,16) 2x12x2 · pernas (0,16) 2x12x2
//   núcleo ocular (0,32) 5x5x3 · ombreiras (16,32) 4x3x4 · espinhos (32,32) 1x2x1

function hollowWatcher(V) {
  const S = new Skin(64, 64);
  const bone = { pal: PAL.bone, level: 2 };
  const eye = eyeColors(V, tone(PAL.bone, 1));

  // --- crânio
  const head = box(0, 0, 8, 8, 8);
  surface(S, head.up, V, 'hw.head.up', { ...bone, cracks: 2 });
  surface(S, head.down, V, 'hw.head.down', { ...bone, level: 1 });
  surface(S, head.back, V, 'hw.head.back', { ...bone, cracks: 2 });
  for (const f of ['right', 'left']) {
    surface(S, head[f], V, 'hw.head.' + f, {
      ...bone,
      cracks: 1,
      pattern: ['########', '########', '########', '##ss####', '##ss####', '########', '#=#=#=#=', 'ss#s#s#s'],
      levels: { s: 0, '=': 3 },
    });
  }
  const face = head.front;
  surface(S, face, V, 'hw.head.front', {
    ...bone,
    cracks: 1,
    crackLen: [2, 3],
    pattern: ['########', '#==##==#', '########', '########', '########', '########', '=s=s=s=s', 's#s#s#ss'],
    levels: { '=': 4, s: 0 },
  });
  // órbitas fundas: brasa apagada quando dormente, olhos roxos acesos quando desperto
  stamp(S, face.x + 1, face.y + 2, V.open ? ['RP', 'PW', 'RB'] : V.energized ? ['DD', 'De', 'DD'] : ['DD', 'De', 'sD'], eye);
  stamp(S, face.x + 5, face.y + 2, V.open ? ['PR', 'WP', 'BR'] : V.energized ? ['DD', 'eD', 'DD'] : ['DD', 'eD', 'Ds'], eye);
  stamp(S, face.x + 3, face.y + 5, ['DD'], eye); // cavidade nasal

  // --- caixa torácica (com transparência entre as costelas)
  const body = box(16, 16, 8, 12, 4);
  surface(S, body.front, V, 'hw.body.front', {
    ...bone,
    cracks: 2,
    crackLen: [3, 5],
    pattern: ['========', '#......#', '###..###', '........', '###..###', '........', '###..###', '........', '.##..##.', '........', '.######.', '##....##'],
    levels: { '=': 3 },
  });
  const ribSide = ['####', '#..#', '####', '....', '####', '....', '####', '....', '####', '....', '####', '#..#'];
  surface(S, body.right, V, 'hw.body.right', { ...bone, pattern: ribSide, cracks: 1 });
  surface(S, body.left, V, 'hw.body.left', { ...bone, pattern: ribSide, cracks: 1 });
  surface(S, body.back, V, 'hw.body.back', {
    ...bone,
    cracks: 2,
    pattern: ['########', '##.||.##', '########', '##.||.##', '########', '...||...', '########', '...||...', '.######.', '...||...', '.######.', '##.||.##'],
    levels: { '|': 3 },
  });
  surface(S, body.up, V, 'hw.body.up', { ...bone, level: 3, pattern: ['########', '#......#', '#......#', '########'] });
  surface(S, body.down, V, 'hw.body.down', { ...bone, level: 1, pattern: ['##....##', '#......#', '#......#', '##....##'] });

  // --- braços e pernas finos (ossos com juntas)
  const limb = (u, v, name, pattern) => {
    const b = box(u, v, 2, 12, 2);
    for (const f of SIDES) surface(S, b[f], V, `hw.${name}.${f}`, { ...bone, pattern, levels: { '=': 4, s: 0 }, cracks: 1, crackLen: [3, 5] });
    surface(S, b.up, V, `hw.${name}.up`, { ...bone, level: 3 });
    surface(S, b.down, V, `hw.${name}.down`, { ...bone, level: 1 });
  };
  limb(40, 16, 'arm', ['==', '##', '##', '##', '##', '==', 'ss', '##', '##', '##', '=s', 's=']);
  limb(0, 16, 'leg', ['==', '##', '##', '##', '##', '=#', '#=', 'ss', '##', '##', '##', '==']);

  // --- núcleo ocular no peito
  const core = box(0, 32, 5, 5, 3);
  for (const f of ['up', 'down', 'right', 'left', 'back']) surface(S, core[f], V, 'hw.core.' + f, { ...bone, level: 1, cracks: 1, crackLen: [2, 3] });
  stamp(S, core.front.x, core.front.y,
    V.open ? ['SRRRS', 'RPKWR', 'RNKNR', 'RPKPR', 'SRRRS'] : ['SSSSS', 'SsssS', 'LLLLL', 'SsssS', 'SSSSS'], eye);

  // --- ombreiras de osso com um olho na lateral de fora
  const pd = box(16, 32, 4, 3, 4);
  for (const f of ['up', 'front', 'left', 'back']) surface(S, pd[f], V, 'hw.pauldron.' + f, { ...bone, level: 3, cracks: 1, crackLen: [2, 3] });
  surface(S, pd.down, V, 'hw.pauldron.down', { ...bone, level: 1 });
  surface(S, pd.right, V, 'hw.pauldron.right', { ...bone, level: 3 });
  stamp(S, pd.right.x, pd.right.y, V.open ? ['RPPR', 'NKKN', 'RPPR'] : ['.ss.', 'LLLL', '.ss.'], eye);

  // --- espinhos (ponta roxa quando energizado)
  const sp = box(32, 32, 1, 2, 1);
  for (const f of [...SIDES, 'up', 'down']) surface(S, sp[f], V, 'hw.spike.' + f, { ...bone, level: 4, shade: false });
  const tip = V.energized ? [PURPLE.base, PURPLE.bright] : [tone(PAL.bone, 5), null];
  for (const f of SIDES) S.px(sp[f].x, sp[f].y, ...tip);
  S.px(sp.up.x, sp.up.y, ...tip);

  return S;
}

// ================================================================ BLIND EXECUTIONER (128x64)
// Partes (iguais a BlindExecutionerModel.createBodyLayer):
//   cabeça (0,0) 8x10x8 · nariz (24,0) 2x4x2 · capuz (32,0) 8x12x8 · venda (64,0) 8x2x8
//   gola (64,10) 10x3x8 · pingente (100,0) 5x5x1 · corpo (16,20) 8x12x6 · manto (0,38) 8x20x6
//   braços (40,46) 4x12x4 · pernas (0,22) 4x12x4

function blindExecutioner(V) {
  const S = new Skin(128, 64);
  const cloth = { pal: PAL.cloth, level: 2, folds: true };
  const skin = { pal: PAL.skin, level: 2 };
  const eye = eyeColors(V, tone(PAL.skin, 1));
  const T = trim(V);
  const vein = V.energized ? [PURPLE.mid, PURPLE.base] : [tone(PAL.skin, 1), null];

  // --- cabeça: cabelo escuro por baixo do capuz, rosto acinzentado
  const head = box(0, 0, 8, 10, 8);
  for (const f of ['up', 'right', 'left', 'back']) surface(S, head[f], V, 'be.head.' + f, { pal: PAL.cloth, level: 1 });
  surface(S, head.down, V, 'be.head.down', { ...skin, level: 1 });
  const face = head.front;
  surface(S, face, V, 'be.face', {
    ...skin,
    pattern: ['hhhhhhhh', '########', '#s####s#', '.bbbbbb.', '#oo##oo#', '#s####s#', '##s##s##', '#s####s#', '#s####s#', '##ssss##'],
    levels: { s: 1, b: 0, o: 1 },
    special: { h: [tone(PAL.cloth, 1), null] },
  });
  if (V.open) {
    // sem a venda: um único olho roxo enorme no rosto, com veias saindo dele
    stamp(S, face.x + 1, face.y + 3, ['.RPPR.', 'RBKKNR', 'RPKKPR', '.RPPR.'], eye);
  } else if (V.energized) {
    stamp(S, face.x + 1, face.y + 3, ['.RRRR.', 'RllllR', 'LLLLLL', '.RRRR.'], eye);
  } else {
    stamp(S, face.x + 1, face.y + 4, ['ss..ss'], eye); // olhos fechados sob a venda
  }
  for (const [i, j] of [[0, 2], [7, 2], [0, 6], [7, 7], [1, 7]]) S.px(face.x + i, face.y + j, ...vein);

  // nariz
  const nose = box(24, 0, 2, 4, 2);
  for (const f of [...SIDES, 'up']) surface(S, nose[f], V, 'be.nose.' + f, { ...skin, level: 3 });
  surface(S, nose.down, V, 'be.nose.down', { ...skin, level: 1 });
  surface(S, nose.front, V, 'be.nose.front', { ...skin, level: 3, pattern: ['##', '##', '##', 'ss'], levels: { s: 1 } });

  // --- capuz de mago: pano preto, borda roxa em volta do rosto, olho bordado nas costas
  const hood = box(32, 0, 8, 12, 8);
  surface(S, hood.up, V, 'be.hood.up', { ...cloth, level: 3, cracks: 1 });
  clear(S, hood.down);
  surface(S, hood.right, V, 'be.hood.right', { ...cloth, cracks: 1, pattern: Array(12).fill('#######t'), special: { t: T } });
  surface(S, hood.left, V, 'be.hood.left', { ...cloth, cracks: 1, pattern: Array(12).fill('t#######'), special: { t: T } });
  surface(S, hood.back, V, 'be.hood.back', { ...cloth });
  stamp(S, hood.back.x + 1, hood.back.y + 4, V.open ? ['.RPPR.', 'RBKKNR', 'RPKKPR', '.RPPR.'] : ['......', '.ssss.', 'LLLLLL', '.ssss.'], eye);
  surface(S, hood.front, V, 'be.hood.front', {
    ...cloth,
    level: 3,
    pattern: ['########', 'tttttttt', ...Array(10).fill('t......t')],
    special: { t: T },
  });

  // --- venda nos olhos com um olho fechado bordado
  const bf = box(64, 0, 8, 2, 8);
  clear(S, bf.up);
  clear(S, bf.down);
  for (const f of ['right', 'left', 'back']) surface(S, bf[f], V, 'be.blindfold.' + f, { pal: PAL.cloth, level: 3 });
  surface(S, bf.front, V, 'be.blindfold.front', { pal: PAL.cloth, level: 3 });
  stamp(S, bf.front.x, bf.front.y, ['..LLLL..', '.L.ll.L.'], eye);

  // --- gola alta do manto, com fecho de metal
  const collar = box(64, 10, 10, 3, 8);
  surface(S, collar.up, V, 'be.collar.up', { ...cloth, level: 3 });
  surface(S, collar.down, V, 'be.collar.down', { ...cloth, level: 1 });
  for (const f of ['right', 'left', 'back']) {
    surface(S, collar[f], V, 'be.collar.' + f, { ...cloth, level: 3, pattern: ['#'.repeat(collar[f].w), '#'.repeat(collar[f].w), 't'.repeat(collar[f].w)], special: { t: T } });
  }
  surface(S, collar.front, V, 'be.collar.front', {
    ...cloth,
    level: 3,
    pattern: ['##########', '####mm####', 'tttmmmmttt'],
    special: { t: T, m: [tone(PAL.metal, 2), null] },
  });

  // --- pingente (Olho de Agamoto)
  const pend = box(100, 0, 5, 5, 1);
  for (const f of ['up', 'down', 'right', 'left', 'back']) surface(S, pend[f], V, 'be.pendant.' + f, { pal: PAL.metal, level: 2, shade: false });
  const metal = { ...eye, F: [tone(PAL.metal, 2), null], f: [tone(PAL.metal, 0), null] };
  stamp(S, pend.front.x, pend.front.y,
    V.open ? ['_FFF_', 'FRBRF', 'FNKNF', 'FRPRF', '_FFF_'] : ['_FFF_', 'FfffF', 'FLLLF', 'FfffF', '_FFF_'], metal);

  // --- corpo (fica por baixo do manto)
  const body = box(16, 20, 8, 12, 6);
  for (const f of [...SIDES, 'up', 'down']) surface(S, body[f], V, 'be.body.' + f, { ...cloth, level: 1 });

  // --- manto longo: faixa de couro, abertura central com forro roxo, barra rasgada
  const robe = box(0, 38, 8, 20, 6);
  surface(S, robe.up, V, 'be.robe.up', { ...cloth, level: 3 });
  clear(S, robe.down);
  const leather = (l) => [tone(PAL.leather, l), null];
  const robeSpecial = {
    t: T,
    i: V.energized ? [PURPLE.deep, '#1a0630'] : ['#170a22', null],
    b: leather(3),
    c: leather(1),
    m: [tone(PAL.metal, 3), null],
  };
  surface(S, robe.front, V, 'be.robe.front', {
    ...cloth,
    cracks: 3,
    crackLen: [4, 7],
    pattern: [
      '########', '########', '########', '########', '########', '########',
      'bbbmmbbb', 'cccmmccc',
      ...Array(10).fill('##tiit##'),
      'ttt..ttt', '#.#..#.#',
    ],
    special: robeSpecial,
  });
  const robeSide = [...Array(6).fill('######'), 'bbbbbb', 'cccccc', ...Array(10).fill('######'), 'tttttt', '#.##.#'];
  for (const f of ['right', 'left']) {
    surface(S, robe[f], V, 'be.robe.' + f, { ...cloth, cracks: 2, crackLen: [4, 7], pattern: robeSide, special: robeSpecial });
    stamp(S, robe[f].x + 1, robe[f].y + 11, V.open ? ['RPPR', 'NKKN', 'RPPR'] : ['.ss.', 'LLLL', '.ss.'], eye);
  }
  surface(S, robe.back, V, 'be.robe.back', {
    ...cloth,
    cracks: 3,
    crackLen: [4, 7],
    pattern: [...Array(6).fill('########'), 'bbbbbbbb', 'cccccccc', ...Array(10).fill('########'), 'tttttttt', '.#.##.#.'],
    special: robeSpecial,
  });
  stamp(S, robe.back.x + 1, robe.back.y + 10, V.open ? ['.RPPR.', 'RBKKNR', 'RPKKPR', '.RPPR.'] : ['......', '.ssss.', 'LLLLLL', '.ssss.'], eye);

  // --- mangas pretas com punho roxo e mãos acinzentadas
  const arm = box(40, 46, 4, 12, 4);
  for (const f of SIDES) {
    surface(S, arm[f], V, 'be.arm.' + f, {
      ...cloth,
      cracks: 1,
      pattern: [...Array(9).fill('####'), 'tttt', 'kkkk', 'kskk'],
      special: { t: T, k: [tone(PAL.skin, 3), null], s: [tone(PAL.skin, 1), null] },
    });
  }
  surface(S, arm.up, V, 'be.arm.up', { ...cloth, level: 3 });
  surface(S, arm.down, V, 'be.arm.down', { ...skin, level: 2 });

  // --- pernas: calça escura e botas
  const leg = box(0, 22, 4, 12, 4);
  for (const f of SIDES) {
    surface(S, leg[f], V, 'be.leg.' + f, {
      ...cloth,
      level: 1,
      pattern: [...Array(8).fill('####'), 'mmmm', 'oooo', 'oooo', 'OOOO'],
      levels: { o: 2 },
      special: { m: [tone(PAL.leather, 3), null], O: [tone(PAL.leather, 0), null] },
    });
  }
  surface(S, leg.up, V, 'be.leg.up', { ...cloth, level: 1 });
  surface(S, leg.down, V, 'be.leg.down', { pal: PAL.leather, level: 0 });

  return S;
}

// ================================================================ ITENS (16x16)

const OUTLINE = '#07060a';

/** Contorno escuro em todos os pixels da borda da silhueta. */
function outline(S, mask) {
  const filled = (x, y) => x >= 0 && y >= 0 && x < 16 && y < 16 && mask[y][x];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      if (!mask[y][x]) continue;
      if (!filled(x + 1, y) || !filled(x - 1, y) || !filled(x, y + 1) || !filled(x, y - 1)) S.px(x, y, OUTLINE, null);
    }
  }
}

// As armas e ferramentas agora são modelos 3D: veja tools/modelos/gerar_modelos.mjs.

// Flecha Ocular (layout de 32x32 igual ao da flecha vanilla).
function eyeArrow() {
  const S = new Skin(32, 32);
  const c = {
    f: [PURPLE.mid, null], F: [PURPLE.base, null],
    s: ['#1d1926', null], S: ['#2c2738', null],
    h: [PURPLE.bright, null], H: [PURPLE.neon, null], W: [PURPLE.white, null],
  };
  stamp(S, 0, 0, [
    'fFf..........hH.',
    '.fFf........HW..',
    'FFsSssSsSsSsHWWH',
    '.fFf........HW..',
    'fFf..........hH.',
  ], c);
  stamp(S, 0, 5, ['..f..', '..F..', 'fFHFf', '..F..', '..f..'], c);
  return S;
}

function hwParts(V, side) {
  const head = box(0, 0, 8, 8, 8), body = box(16, 16, 8, 12, 4), arm = box(40, 16, 2, 12, 2), leg = box(0, 16, 2, 12, 2);
  const core = box(0, 32, 5, 5, 3), pd = box(16, 32, 4, 3, 4);
  const f = side;
  return [[head[f], 4, 3], [body[f], 4, 11], ...(f === 'front' ? [[core.front, 5, 13]] : []), [arm[f], 1, 11], [arm[f], 13, 11],
    [pd[f], 0, 8], [pd[f], 12, 8], [leg[f], 5, 23], [leg[f], 9, 23]];
}

function beParts(V, side) {
  const head = box(0, 0, 8, 10, 8), hood = box(32, 0, 8, 12, 8), bf = box(64, 0, 8, 2, 8), collar = box(64, 10, 10, 3, 8);
  const pend = box(100, 0, 5, 5, 1), robe = box(0, 38, 8, 20, 6), arm = box(40, 46, 4, 12, 4), leg = box(0, 22, 4, 12, 4);
  const f = side;
  const blindfold = V.name === 'dormant' ? [[bf[f], 4, 4]] : [];
  return [[leg[f], 4, 22], [leg[f], 8, 22], [head[f], 4, 0], ...blindfold, [hood[f], 4, 0], [robe[f], 4, 10],
    [arm[f], 0, 10], [arm[f], 12, 10], [collar[f], 3, 9], ...(f === 'front' ? [[pend.front, 6, 12]] : [])];
}

function mobBoard(title, paint, parts, scale) {
  const skins = Object.fromEntries(Object.values(VARIANTS).map((V) => [V.name, paint(V)]));
  const d = (V, side, nightMode) => {
    const S = skins[V.name];
    const tex = nightMode ? night(S, V.energized ? 0.95 : 0.35) : S.base;
    return upscale(doll(tex, parts(V, side), 16, 36), scale, nightMode ? NIGHT_BG : BG);
  };
  const { dormant, blink, awakened } = VARIANTS;
  return board(title, [
    { label: 'dormente', img: d(dormant, 'front') },
    { label: 'costas', img: d(dormant, 'back') },
    { label: 'no escuro', img: d(dormant, 'front', true) },
    { label: 'despertando', img: d(blink, 'front') },
    { label: 'desperto', img: d(awakened, 'front') },
    { label: 'costas', img: d(awakened, 'back') },
    { label: 'no escuro', img: d(awakened, 'front', true) },
  ]);
}

// ================================================================ main

const entity = (p) => join(OUT, 'entity', p);

for (const V of Object.values(VARIANTS)) {
  const hw = hollowWatcher(V);
  hw.base.save(entity(`hollow_watcher/hollow_watcher_${V.name}.png`));
  hw.glow.save(entity(`hollow_watcher/hollow_watcher_${V.name}_glow.png`));
  const be = blindExecutioner(V);
  be.base.save(entity(`blind_executioner/blind_executioner_${V.name}.png`));
  be.glow.save(entity(`blind_executioner/blind_executioner_${V.name}_glow.png`));
}

const arrow = eyeArrow();
arrow.base.save(entity('projectiles/eye_arrow.png'));

// Prévias para acompanhar o andamento (não vão para dentro do mod).
const PREVIEWS = PREVIEW_DIR || join(ROOT, 'previas');
mobBoard('hollow watcher', hollowWatcher, hwParts, 10).save(join(PREVIEWS, '1_hollow_watcher.png'));
mobBoard('blind executioner', blindExecutioner, beParts, 10).save(join(PREVIEWS, '2_blind_executioner.png'));
board('flecha ocular', [
  { label: 'lateral', img: upscale(doll(arrow.base, [[R(0, 0, 16, 5), 0, 0]], 16, 5), 14, BG) },
]).save(join(PREVIEWS, '4_flecha_ocular.png'));
for (const V of Object.values(VARIANTS)) {
  const hw = hollowWatcher(V);
  const be = blindExecutioner(V);
  board(`texturas ${V.name}`, [
    { label: 'hollow watcher', img: upscale(hw.base, 5, BG) },
    { label: 'brilho', img: upscale(hw.glow, 5, ['#000000', '#000000']) },
    { label: 'blind executioner', img: upscale(be.base, 5, BG) },
    { label: 'brilho', img: upscale(be.glow, 5, ['#000000', '#000000']) },
  ]).save(join(PREVIEWS, 'texturas', `${V.name}.png`));
}
console.log('Texturas geradas em', OUT);
console.log('Prévias em', PREVIEWS);
