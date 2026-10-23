// Gera os modelos 3D (JSON) e as texturas das armas e ferramentas oculares.
//
//   node tools/modelos/gerar_modelos.mjs
//
// Cada item é uma lista de caixas, como no Blockbench. O gerador:
//   - distribui as faces numa textura 64x64 e pinta cada face pelo material;
//   - escreve <item>.json (selado), <item>_awakened.json (desperto, olho aberto) e <item>_supreme.json (supremo);
//   - no aberto, as peças com olho ou rachadura ganham uma "casca" emissiva por cima, que usa
//     <item>_awakened_glow.png: uma textura animada (o olho pisca e o brilho pulsa).
//   - peças marcadas com glow (fio da lâmina, corda, gemas) brilham inteiras quando abertas.
//
// As armas são modeladas em pé (cabo embaixo, eixo Y) e as transformações de exibição giram
// tudo 45° para ficar na diagonal, como os itens vanilla na mão.

import { writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Tex, mix, rgb, seeded } from '../texturas/png.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const ASSETS = join(ROOT, 'src', 'main', 'resources', 'assets', 'uda');
const TEX_DIR = join(ASSETS, 'textures', 'item');
const MODEL_DIR = join(ASSETS, 'models', 'item');
const TAM = 64;

// ---------------------------------------------------------------- paleta

const PAL = {
  metal: ['#0b0a0f', '#13111a', '#1b1823', '#25212e', '#302b3b', '#3d374a'],
  steel: ['#16141d', '#201d29', '#2b2737', '#383345', '#474157', '#59526b'],
  trim: ['#1a0a2a', '#260e3f', '#331257', '#42186f', '#55208c', '#6a2aa8'],
  grip: ['#0f0b10', '#161018', '#1e1620', '#271d29', '#302432', '#3a2c3c'],
  leather: ['#130d13', '#1c131c', '#261a26', '#302130', '#3a293a', '#453145'],
  pages: ['#26202c', '#2f2836', '#393141', '#433a4c', '#4e4458', '#594f64'],
};

const PURPLE = { deep: '#1e0b30', dark: '#3a1060', mid: '#6a1fb0', base: '#9b30ff', bright: '#c45cff', neon: '#e28bff', white: '#fbe6ff' };

// Materiais de cor única. [dormente, desperto, supremo]; nas formas despertas estas peças brilham inteiras.
const FLAT = {
  edge: [['#4f4660', '#5d536f'], [PURPLE.bright, PURPLE.neon], [PURPLE.neon, PURPLE.white]],
  gem: [['#2e1450', '#3a1866'], [PURPLE.base, PURPLE.neon], [PURPLE.white, PURPLE.neon]],
  string: [['#5d4a73', '#54436a'], [PURPLE.bright, PURPLE.neon], [PURPLE.neon, PURPLE.white]],
  shaft: [['#3b3346', '#2f2938'], [PURPLE.mid, '#7a2ad0'], [PURPLE.bright, PURPLE.mid]],
};

const tone = (pal, l) => pal[Math.max(0, Math.min(pal.length - 1, l))];

// ---------------------------------------------------------------- olhos

// Formas de olho por tamanho da face. Letras: R aro, P roxo, B claro, N neon, W brilho,
// K pupila, L costura da pálpebra fechada, s pálpebra.
const EYES = [
  { w: 6, h: 4, open: ['.RPPR.', 'RBKKNR', 'RPKKPR', '.RPPR.'], half: ['......', 'ssssss', 'RPKKPR', '.ssss.'], closed: ['......', '.ssss.', 'LLLLLL', '.ssss.'] },
  { w: 5, h: 5, open: ['.RRR.', 'RPKWR', 'RNKNR', 'RPKPR', '.RRR.'], half: ['.....', 'sssss', 'RNKNR', 'sssss', '.....'], closed: ['.....', '.sss.', 'LLLLL', '.sss.', '.....'] },
  { w: 4, h: 3, open: ['RPPR', 'NKKN', 'RPPR'], half: ['ssss', 'NKKN', 'ssss'], closed: ['.ss.', 'LLLL', '.ss.'] },
  { w: 3, h: 3, open: ['RPR', 'NKN', 'RPR'], half: ['sss', 'NKN', 'sss'], closed: ['sss', 'LLL', 'sss'] },
  { w: 3, h: 2, open: ['PKN', 'RPR'], half: ['sss', 'NKN'], closed: ['sss', 'LLL'] },
  { w: 2, h: 2, open: ['PN', 'NP'], half: ['ss', 'NN'], closed: ['ss', 'LL'] },
];

function eyeFor(w, h) {
  return EYES.find((e) => e.w <= w && e.h <= h) || null;
}

// Cor de cada letra do olho no brilho (aberto) e na textura base.
const EYE_GLOW = { R: '#3a0f60', P: PURPLE.base, B: PURPLE.bright, N: PURPLE.neon, W: '#ffffff', L: '#b84dff' };

// ---------------------------------------------------------------- caixas

/** Uma caixa do modelo. o: { rot, eye: [faces], cracks, fuller, glow, hide: [faces] } */
const box = (from, to, mat, o = {}) => ({ from, to, mat, ...o });
const rotZ = (angle, origin) => ({ angle, axis: 'z', origin });

const FACE_SIZE = {
  north: (s) => [s[0], s[1]],
  south: (s) => [s[0], s[1]],
  east: (s) => [s[2], s[1]],
  west: (s) => [s[2], s[1]],
  up: (s) => [s[0], s[2]],
  down: (s) => [s[0], s[2]],
};
const FACES = Object.keys(FACE_SIZE);

/** Distribui as faces de todas as caixas numa textura (prateleiras). */
function layout(boxes) {
  const rects = [];
  boxes.forEach((b, bi) => {
    const s = [0, 1, 2].map((k) => b.to[k] - b.from[k]);
    for (const f of FACES) {
      if (b.hide?.includes(f)) continue;
      const [fw, fh] = FACE_SIZE[f](s);
      rects.push({ bi, f, fw, fh, w: Math.max(1, Math.ceil(fw - 1e-6)), h: Math.max(1, Math.ceil(fh - 1e-6)) });
    }
  });
  const ordered = [...rects].sort((a, b) => b.h - a.h || b.w - a.w);
  let x = 0, y = 0, shelf = 0;
  for (const r of ordered) {
    if (x + r.w > TAM) {
      x = 0;
      y += shelf;
      shelf = 0;
    }
    r.x = x;
    r.y = y;
    x += r.w;
    shelf = Math.max(shelf, r.h);
  }
  if (y + shelf > TAM) throw new Error('textura pequena demais para o modelo');
  const map = new Map();
  for (const r of rects) map.set(r.bi + ':' + r.f, r);
  return map;
}

// ---------------------------------------------------------------- pintura

function crackSet(rand, w, h, count) {
  const set = new Set();
  for (let n = 0; n < count; n++) {
    let x = Math.floor(rand() * w), y = Math.floor(rand() * h);
    const dy = rand() < 0.5 ? 1 : -1;
    let drift = rand() < 0.5 ? 1 : -1;
    const steps = 3 + Math.floor(rand() * 5);
    for (let k = 0; k < steps; k++) {
      if (x >= 0 && y >= 0 && x < w && y < h) set.add(x + ',' + y);
      const r = rand();
      if (r < 0.6) y += dy;
      else if (r < 0.85) x += drift;
      else { x += drift; y += dy; }
      if (rand() < 0.2) drift = -drift;
    }
  }
  return set;
}

/**
 * Pinta todas as faces de um modelo.
 * Devolve { base, glow: [quadros] } — o brilho só existe no desperto (5 quadros da animação).
 */
function paint(name, boxes, rects, form) {
  const awakened = form >= 1;
  const supreme = form >= 2;
  const eyeGlow = supreme ? { ...EYE_GLOW, P: PURPLE.bright, N: PURPLE.white, B: PURPLE.neon } : EYE_GLOW;
  const base = new Tex(TAM, TAM);
  // quadros do brilho: 0-2 aberto (pulsando), 3 semicerrado, 4 fechado
  const frames = [0, 1, 2, 3, 4].map(() => new Tex(TAM, TAM));
  const pulse = [1.0, 0.82, 0.64, 0.85, 0.75];
  const glowPx = (x, y, color, kind) => {
    frames.forEach((t, i) => {
      if (kind === 'eye-open' && i >= 3) return;
      if (kind === 'eye-half' && i !== 3) return;
      if (kind === 'eye-closed' && i !== 4) return;
      const [r, g, b] = rgb(color);
      const k = pulse[i];
      t.set(x, y, [Math.round(r * k), Math.round(g * k), Math.round(b * k), 255]);
    });
  };
  const hasGlow = new Set(); // "bi:face" com pixels de brilho (ganham casca emissiva)

  boxes.forEach((b, bi) => {
    for (const f of FACES) {
      const r = rects.get(bi + ':' + f);
      if (!r) continue;
      const rand = seeded(`${name}:${bi}:${f}`);
      const flat = FLAT[b.mat];
      // material base
      for (let j = 0; j < r.h; j++) {
        for (let i = 0; i < r.w; i++) {
          const n = rand();
          let c;
          if (flat) {
            const pair = flat[form];
            c = pair[(i + j) % 2 === 0 ? 0 : 1];
          } else {
            const pal = PAL[b.mat];
            let l = 2;
            if (f === 'up') l++;
            if (f === 'down') l--;
            if (f !== 'up' && f !== 'down') {
              if (j === 0) l++;
              if (j === r.h - 1 && r.h > 2) l--;
            }
            if (n < 0.17) l--;
            else if (n > 0.86) l++;
            if (b.mat === 'grip') {
              if ((i + j) % 3 === 0) l--;
              if (j % 4 === 1 && f !== 'up' && f !== 'down') c = awakened ? PURPLE.mid : '#2a1240';
            }
            if (b.mat === 'pages' && f !== 'up' && f !== 'down' && j % 2 === 1) l--;
            if (b.mat === 'steel' && b.fuller === undefined && (f === 'north' || f === 'south') && i === 0) l++;
            c = c || tone(pal, l);
          }
          base.set(r.x + i, r.y + j, c);
        }
      }
      if (flat) continue;
      const big = f === 'north' || f === 'south' || (r.w >= 3 && r.h >= 3 && f !== 'up' && f !== 'down');
      // sulco central da lâmina (vira veio de energia)
      if (b.fuller && (f === 'north' || f === 'south')) {
        const col = Math.floor(r.w / 2);
        for (let j = 1; j < r.h - 1; j++) {
          base.set(r.x + col, r.y + j, awakened ? '#5a1a8a' : '#24102f');
          if (awakened) {
            glowPx(r.x + col, r.y + j, j % 3 === 0 ? PURPLE.neon : PURPLE.base);
            hasGlow.add(bi + ':' + f);
          }
        }
      }
      // rachaduras (a forma suprema racha mais e em mais peças)
      const cracks = (b.cracks || 0) + (supreme && ['steel', 'metal', 'trim', 'leather'].includes(b.mat) ? 2 : 0);
      if (cracks && big) {
        const cr = crackSet(seeded(`${name}:${bi}:${f}:rachaduras`), r.w, r.h, cracks);
        for (const key of cr) {
          const [i, j] = key.split(',').map(Number);
          base.set(r.x + i, r.y + j, supreme ? '#a24bf0' : awakened ? '#7a2ad0' : '#2a1240');
          if (awakened) {
            glowPx(r.x + i, r.y + j, supreme ? PURPLE.neon : PURPLE.bright);
            hasGlow.add(bi + ':' + f);
          }
        }
      }
      // supremo: frisos roxos acesos nas peças de acabamento
      if (supreme && b.mat === 'trim' && f !== 'up' && f !== 'down') {
        for (let j = 0; j < r.h; j++) for (let i = 0; i < r.w; i++) {
          if ((i + j) % 3 !== 0) continue;
          base.set(r.x + i, r.y + j, PURPLE.mid);
          glowPx(r.x + i, r.y + j, PURPLE.bright);
        }
        hasGlow.add(bi + ':' + f);
      }
      // olho
      if (b.eye?.includes(f)) {
        const shape = eyeFor(r.w, r.h);
        if (!shape) continue;
        const ox = r.x + Math.floor((r.w - shape.w) / 2);
        const oy = r.y + Math.floor((r.h - shape.h) / 2);
        for (let j = 0; j < shape.h; j++) {
          for (let i = 0; i < shape.w; i++) {
            const open = shape.open[j][i], closed = shape.closed[j][i];
            if (!awakened) {
              // dormente: pálpebra fechada com a costura apagada
              if (closed === 'L') base.set(ox + i, oy + j, '#3d1766');
              else if (closed === 's') base.set(ox + i, oy + j, '#1b1624');
              continue;
            }
            // desperto: a base vira órbita escura; o brilho desenha o olho (aberto/meio/fechado)
            if (open !== '.' || closed !== '.') base.set(ox + i, oy + j, open === 'R' ? '#1c0830' : '#060309');
            if (eyeGlow[open]) glowPx(ox + i, oy + j, eyeGlow[open], 'eye-open');
            const half = shape.half[j][i];
            if (eyeGlow[half]) glowPx(ox + i, oy + j, eyeGlow[half], 'eye-half');
            if (eyeGlow[closed]) glowPx(ox + i, oy + j, eyeGlow[closed], 'eye-closed');
          }
        }
        if (awakened) hasGlow.add(bi + ':' + f);
      }
    }
  });
  return { base, frames, hasGlow };
}

// ---------------------------------------------------------------- JSON

const r4 = (v) => Math.round(v * 10000) / 10000;

function uvOf(rect) {
  const k = 16 / TAM;
  return [r4(rect.x * k), r4(rect.y * k), r4((rect.x + rect.fw) * k), r4((rect.y + rect.fh) * k)];
}

const EMISSIVE = { block_light: 15, sky_light: 15 };

function elements(boxes, rects, awakened, hasGlow) {
  const out = [];
  boxes.forEach((b, bi) => {
    const faces = {};
    for (const f of FACES) {
      const r = rects.get(bi + ':' + f);
      if (r) faces[f] = { uv: uvOf(r), texture: '#0' };
    }
    const el = { from: b.from, to: b.to, faces };
    if (b.rot) el.rotation = b.rot;
    if (awakened && b.glow) {
      el.shade = false;
      el.neoforge_data = EMISSIVE;
    }
    out.push(el);
  });
  if (awakened) {
    // casca emissiva por cima das faces que têm olho/rachadura/veio
    boxes.forEach((b, bi) => {
      const faces = {};
      for (const f of FACES) {
        if (!hasGlow.has(bi + ':' + f)) continue;
        faces[f] = { uv: uvOf(rects.get(bi + ':' + f)), texture: '#glow' };
      }
      if (!Object.keys(faces).length) return;
      const e = 0.02;
      const el = {
        from: b.from.map((v) => r4(v - e)),
        to: b.to.map((v) => r4(v + e)),
        shade: false,
        faces,
        neoforge_data: EMISSIVE,
      };
      if (b.rot) el.rotation = b.rot;
      out.push(el);
    });
  }
  return out;
}

function writeJson(file, obj) {
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(obj, null, 1).replace(/\[\s+([^\[\]{}]*?)\s+\]/g, (m, inner) => '[' + inner.replace(/\s+/g, ' ') + ']') + '\n');
}

function saveTextures(texName, painted, form) {
  painted.base.save(join(TEX_DIR, texName + '.png'));
  if (form === 0) return;
  // brilho animado: quadros empilhados na vertical
  const strip = new Tex(TAM, TAM * painted.frames.length);
  painted.frames.forEach((t, i) => {
    for (let y = 0; y < TAM; y++) for (let x = 0; x < TAM; x++) strip.set(x, y + i * TAM, t.get(x, y));
  });
  strip.save(join(TEX_DIR, texName + '_glow.png'));
  const pulseLoop = [0, 1, 2, 1].map((index) => ({ index, time: 5 }));
  const frames = [...pulseLoop, ...pulseLoop, ...pulseLoop, { index: 3, time: 2 }, { index: 4, time: 3 }, { index: 3, time: 2 }, ...pulseLoop];
  // o supremo pulsa mais rápido
  const timed = form === 2 ? frames.map((fr) => ({ ...fr, time: Math.max(1, Math.round(fr.time * 0.6)) })) : frames;
  writeFileSync(join(TEX_DIR, texName + '_glow.png.mcmeta'), JSON.stringify({ animation: { frametime: 5, interpolate: false, frames: timed } }, null, 2) + '\n');
}

// ---------------------------------------------------------------- exibição

// Os modelos são feitos em pé e girados 45° para ficar na diagonal, igual aos itens planos vanilla.
// O sinal do giro depende do lado do item que aparece em cada contexto: o item plano vanilla mostra o
// desenho sem espelhar dos dois lados, um modelo 3D não. Valores conferidos no jogo, comparando com a
// espada e o arco vanilla (gradlew runScreenshots -PcenaInicial=1046):
//   -45 em tudo, menos no arco em terceira pessoa: +45 (a transformação do arco mostra o outro lado)
// Na mão esquerda o jogo inverte o sinal do z do JSON, por isso o giro entra com o sinal oposto lá.
const TOOL_DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 10], translation: [0, 4, 0.5], scale: [0.7, 0.7, 0.7] },
  thirdperson_lefthand: { rotation: [0, 90, -10], translation: [0, 4, 0.5], scale: [0.7, 0.7, 0.7] },
  firstperson_righthand: { rotation: [0, -90, -20], translation: [1.13, 3.2, 1.13], scale: [0.52, 0.52, 0.52] },
  firstperson_lefthand: { rotation: [0, 90, 20], translation: [1.13, 3.2, 1.13], scale: [0.52, 0.52, 0.52] },
  ground: { rotation: [0, 0, -45], translation: [0, 2, 0], scale: [0.5, 0.5, 0.5] },
  gui: { rotation: [-10, 30, -45], translation: [0, 0, 0], scale: [0.8, 0.8, 0.8] },
  head: { rotation: [0, 180, -45], translation: [0, 13, 7], scale: [1, 1, 1] },
  fixed: { rotation: [0, 180, -45], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
};

const BOW_DISPLAY = {
  thirdperson_righthand: { rotation: [-80, 260, 5], translation: [-1, -2, 2.5], scale: [0.8, 0.8, 0.8] },
  thirdperson_lefthand: { rotation: [-80, -280, -5], translation: [-1, -2, 2.5], scale: [0.8, 0.8, 0.8] },
  firstperson_righthand: { rotation: [0, -90, -20], translation: [1.13, 3.2, 1.13], scale: [0.62, 0.62, 0.62] },
  firstperson_lefthand: { rotation: [0, 90, 20], translation: [1.13, 3.2, 1.13], scale: [0.62, 0.62, 0.62] },
  ground: { rotation: [0, 0, -45], translation: [0, 2, 0], scale: [0.5, 0.5, 0.5] },
  gui: { rotation: [-10, 30, -45], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
  head: { rotation: [0, 180, -45], translation: [0, 13, 7], scale: [1, 1, 1] },
  fixed: { rotation: [0, 180, -45], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
};

// Escudo: mesma geometria do escudo vanilla (placa 12x22 centrada na origem, frente em +Z),
// então as transformações do shield.json / shield_blocking.json servem direto.
const SHIELD_DISPLAY = {
  thirdperson_righthand: { rotation: [0, 90, 0], translation: [10, 6, -4], scale: [1, 1, 1] },
  thirdperson_lefthand: { rotation: [0, 90, 0], translation: [10, 6, 12], scale: [1, 1, 1] },
  firstperson_righthand: { rotation: [0, 180, 5], translation: [-10, 2, -10], scale: [1.25, 1.25, 1.25] },
  firstperson_lefthand: { rotation: [0, 180, 5], translation: [10, 0, -10], scale: [1.25, 1.25, 1.25] },
  gui: { rotation: [15, -25, -5], translation: [2, 3, 0], scale: [0.65, 0.65, 0.65] },
  fixed: { rotation: [0, 180, 0], translation: [-4.5, 4.5, -5], scale: [0.55, 0.55, 0.55] },
  ground: { rotation: [0, 0, 0], translation: [2, 4, 2], scale: [0.25, 0.25, 0.25] },
};
const SHIELD_BLOCKING_DISPLAY = {
  ...SHIELD_DISPLAY,
  thirdperson_righthand: { rotation: [45, 155, 0], translation: [-3.49, 11, -2], scale: [1, 1, 1] },
  thirdperson_lefthand: { rotation: [45, 155, 0], translation: [11.51, 7, 2.5], scale: [1, 1, 1] },
  firstperson_righthand: { rotation: [0, 180, -5], translation: [-15, 5, -11], scale: [1.25, 1.25, 1.25] },
  firstperson_lefthand: { rotation: [0, 180, -5], translation: [5, 5, -11], scale: [1.25, 1.25, 1.25] },
};

// Grimório: livro em pé, capa para fora.
const BOOK_DISPLAY = {
  thirdperson_righthand: { rotation: [0, 90, 0], translation: [0, 2.5, 1.5], scale: [0.55, 0.55, 0.55] },
  thirdperson_lefthand: { rotation: [0, 90, 0], translation: [0, 2.5, 1.5], scale: [0.55, 0.55, 0.55] },
  // a capa (face +Z) virada para o centro da tela nas duas mãos
  firstperson_righthand: { rotation: [0, -60, 0], translation: [1, 2.5, 0], scale: [0.45, 0.45, 0.45] },
  firstperson_lefthand: { rotation: [0, -60, 0], translation: [1, 2.5, 0], scale: [0.45, 0.45, 0.45] },
  ground: { rotation: [0, 0, 0], translation: [0, 2, 0], scale: [0.45, 0.45, 0.45] },
  gui: { rotation: [15, -30, 0], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
  fixed: { rotation: [0, 180, 0], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
};

// ---------------------------------------------------------------- os itens

const POMMEL = [box([7, -5, 7], [9, -3, 9], 'trim'), box([7.5, -6, 7.5], [8.5, -5, 8.5], 'gem', { glow: true })];

function espada() {
  return [
    box([7.5, -4.5, 7.5], [8.5, -3.5, 8.5], 'gem', { glow: true }),
    box([6.5, -3.5, 6.5], [9.5, -0.5, 9.5], 'trim', { eye: ['north', 'south'] }),
    box([7.25, -0.5, 7.25], [8.75, 4.5, 8.75], 'grip'),
    box([6, 4.5, 6.5], [10, 7.5, 9.5], 'trim', { eye: ['north', 'south'] }),
    box([2.5, 5, 7], [6, 6.5, 9], 'metal'),
    box([10, 5, 7], [13.5, 6.5, 9], 'metal'),
    box([1.5, 5.5, 7.25], [2.5, 8.5, 8.75], 'metal'),
    box([13.5, 5.5, 7.25], [14.5, 8.5, 8.75], 'metal'),
    box([1.6, 8.5, 7.5], [2.4, 9.5, 8.5], 'gem', { glow: true }),
    box([13.6, 8.5, 7.5], [14.4, 9.5, 8.5], 'gem', { glow: true }),
    box([6.75, 7.5, 7.25], [9.25, 9.5, 8.75], 'steel'),
    box([6.5, 9.5, 7.5], [9.5, 19, 8.5], 'steel', { cracks: 2, fuller: true }),
    box([6, 9.5, 7.75], [6.5, 19, 8.25], 'edge', { glow: true }),
    box([9.5, 9.5, 7.75], [10, 19, 8.25], 'edge', { glow: true }),
    box([7, 19, 7.5], [9, 21, 8.5], 'steel'),
    box([6.5, 19, 7.75], [7, 20.5, 8.25], 'edge', { glow: true }),
    box([9, 19, 7.75], [9.5, 20.5, 8.25], 'edge', { glow: true }),
    box([7.5, 21, 7.75], [8.5, 22.5, 8.25], 'edge', { glow: true }),
  ];
}

function machado() {
  return [
    ...POMMEL,
    box([7.25, -3, 7.25], [8.75, 16, 8.75], 'grip'),
    box([6.5, 11, 6.5], [9.5, 16.5, 9.5], 'metal'),
    box([7.25, 16.5, 7.25], [8.75, 18.5, 8.75], 'metal'),
    box([7.6, 18.5, 7.6], [8.4, 19.5, 8.4], 'gem', { glow: true }),
    box([2.5, 10, 7.25], [6.5, 17, 8.75], 'steel', { eye: ['north', 'south'], cracks: 1 }),
    box([1.5, 9, 7.5], [2.5, 18, 8.5], 'steel'),
    box([1, 8.5, 7.75], [1.5, 18.5, 8.25], 'edge', { glow: true }),
    box([1.5, 18, 7.5], [3, 19, 8.5], 'steel'),
    box([1.5, 8, 7.5], [3, 9, 8.5], 'steel'),
    box([9.5, 12.5, 7.5], [11.5, 15, 8.5], 'metal'),
    box([11.5, 13.25, 7.75], [13, 14.25, 8.25], 'edge', { glow: true }),
  ];
}

function picareta() {
  const left = rotZ(22.5, [6, 13.75, 8]);
  const right = rotZ(-22.5, [10, 13.75, 8]);
  return [
    ...POMMEL,
    box([7.25, -3, 7.25], [8.75, 13, 8.75], 'grip'),
    box([6, 12, 6.5], [10, 15.5, 9.5], 'metal', { eye: ['north', 'south'] }),
    box([1.5, 12.75, 7], [6, 14.75, 9], 'steel', { rot: left, cracks: 1 }),
    box([10, 12.75, 7], [14.5, 14.75, 9], 'steel', { rot: right, cracks: 1 }),
    box([-0.5, 13, 7.5], [1.5, 14.5, 8.5], 'edge', { rot: left, glow: true }),
    box([14.5, 13, 7.5], [16.5, 14.5, 8.5], 'edge', { rot: right, glow: true }),
    box([7.25, 15.5, 7.25], [8.75, 16.5, 8.75], 'metal'),
    box([7.6, 16.5, 7.6], [8.4, 17.3, 8.4], 'gem', { glow: true }),
  ];
}

function pa() {
  return [
    ...POMMEL,
    box([7.25, -3, 7.25], [8.75, 10, 8.75], 'grip'),
    box([6.75, 9, 6.75], [9.25, 11, 9.25], 'trim'),
    box([4.5, 11, 7.25], [11.5, 18, 8.75], 'steel', { eye: ['north', 'south'], cracks: 1 }),
    box([5.5, 18, 7.25], [10.5, 19.5, 8.75], 'steel'),
    box([6.5, 19.5, 7.5], [9.5, 20.5, 8.5], 'steel'),
    box([4, 11.5, 7.75], [4.5, 18, 8.25], 'edge', { glow: true }),
    box([11.5, 11.5, 7.75], [12, 18, 8.25], 'edge', { glow: true }),
    box([5, 18, 7.75], [5.5, 19.5, 8.25], 'edge', { glow: true }),
    box([10.5, 18, 7.75], [11, 19.5, 8.25], 'edge', { glow: true }),
    box([7, 20.5, 7.75], [9, 21, 8.25], 'edge', { glow: true }),
  ];
}

/** Arco em pé (barriga para -X, corda em +X). stage: -1 parado, 0..2 puxando. */
function arco(stage) {
  const DEG = Math.PI / 180;
  const angles = [[0, -22.5, -45], [0, -22.5, -45], [-22.5, -22.5, -45], [-22.5, -45, -45]][stage + 1];
  const lens = [4, 4, 3.5];
  const parts = [
    box([2.75, 6.25, 7], [4.75, 9.75, 9], 'grip'),
    box([1.75, 6.5, 6.5], [4.75, 9.5, 9.5], 'trim', { eye: ['north', 'south', 'west'] }),
  ];
  // braço de cima (sobe e curva para a corda) e o de baixo, espelhado
  let top = [3.75, 9.5];
  let bottom = [3.75, 6.5];
  for (let k = 0; k < 3; k++) {
    const a = angles[k], len = lens[k];
    const mat = k === 2 ? 'trim' : 'metal';
    parts.push(box([top[0] - 0.75, top[1], 7.25], [top[0] + 0.75, top[1] + len, 8.75], mat, a ? { rot: rotZ(a, [top[0], top[1], 8]) } : {}));
    parts.push(box([bottom[0] - 0.75, bottom[1] - len, 7.25], [bottom[0] + 0.75, bottom[1], 8.75], mat, a ? { rot: rotZ(-a, [bottom[0], bottom[1], 8]) } : {}));
    top = [top[0] - Math.sin(a * DEG) * len, top[1] + Math.cos(a * DEG) * len];
    bottom = [bottom[0] - Math.sin(a * DEG) * len, bottom[1] - Math.cos(a * DEG) * len];
  }
  const [tx, ty] = top;
  // pontas com gema
  parts.push(box([tx - 0.6, ty - 0.6, 7.4], [tx + 0.6, ty + 0.6, 8.6], 'gem', { glow: true }));
  parts.push(box([tx - 0.6, bottom[1] - 0.6, 7.4], [tx + 0.6, bottom[1] + 0.6, 8.6], 'gem', { glow: true }));
  if (stage < 0) {
    parts.push(box([tx + 0.1, bottom[1], 7.8], [tx + 0.5, ty, 8.2], 'string', { glow: true }));
    return parts;
  }
  // corda puxada: dois segmentos até o encaixe da flecha (ângulos permitidos: 22.5 ou 45)
  const sa = stage === 2 ? 45 : 22.5;
  const h = ty - 8;
  const nx = tx + 0.3 + h * Math.tan(sa * DEG);
  const L = h / Math.cos(sa * DEG);
  parts.push(box([nx - 0.2, 8, 7.8], [nx + 0.2, 8 + L, 8.2], 'string', { glow: true, rot: rotZ(sa, [nx, 8, 8]) }));
  parts.push(box([nx - 0.2, 8 - L, 7.8], [nx + 0.2, 8, 8.2], 'string', { glow: true, rot: rotZ(-sa, [nx, 8, 8]) }));
  // flecha: haste, ponta e penas
  const tip = nx - 17;
  parts.push(box([tip + 2, 7.75, 7.75], [nx + 0.5, 8.25, 8.25], 'shaft', { glow: true }));
  parts.push(box([tip, 7.4, 7.6], [tip + 2, 8.6, 8.4], 'edge', { glow: true }));
  parts.push(box([nx - 2.5, 7.1, 7.9], [nx, 8.9, 8.1], 'trim'));
  return parts;
}

function escudo() {
  return [
    // placa em forma de escudo (pipa), igual em tamanho à vanilla
    box([-6, -8, 1], [6, 10.5, 2], 'metal', { cracks: 3 }),
    box([-5, -10, 1], [5, -8, 2], 'metal'),
    box([-3.5, -11.5, 1], [3.5, -10, 2], 'metal'),
    box([-1.5, -12.5, 1], [1.5, -11.5, 2], 'metal'),
    // aro roxo
    box([-6.5, 10.5, 0.5], [6.5, 12, 2.5], 'trim'),
    box([-7, -8, 0.5], [-6, 10.5, 2.5], 'trim'),
    box([6, -8, 0.5], [7, 10.5, 2.5], 'trim'),
    box([-6, -10, 0.5], [-5, -8, 2.5], 'trim'),
    box([5, -10, 0.5], [6, -8, 2.5], 'trim'),
    box([-5, -11.5, 0.5], [-3.5, -10, 2.5], 'trim'),
    box([3.5, -11.5, 0.5], [5, -10, 2.5], 'trim'),
    box([-3.5, -12.5, 0.5], [-1.5, -11.5, 2.5], 'trim'),
    box([1.5, -12.5, 0.5], [3.5, -11.5, 2.5], 'trim'),
    box([-1.5, -13.5, 0.5], [1.5, -12.5, 2.5], 'trim'),
    // olho no centro da frente, com sobrancelha e espinhos
    box([-3, -1.5, 2], [3, 4.5, 3.5], 'trim', { eye: ['south'] }),
    box([-3.5, 4.5, 2], [3.5, 5.5, 3], 'metal'),
    box([-0.5, 5.5, 2.2], [0.5, 8, 2.8], 'metal'),
    box([-0.4, 8, 2.3], [0.4, 8.8, 2.7], 'gem', { glow: true }),
    box([-0.5, -4, 2.2], [0.5, -1.5, 2.8], 'metal'),
    box([-0.4, -4.8, 2.3], [0.4, -4, 2.7], 'gem', { glow: true }),
    box([-4.5, 7.5, 2], [-3.5, 8.5, 2.8], 'gem', { glow: true }),
    box([3.5, 7.5, 2], [4.5, 8.5, 2.8], 'gem', { glow: true }),
    box([-4.5, -6.5, 2], [-3.5, -5.5, 2.8], 'gem', { glow: true }),
    box([3.5, -6.5, 2], [4.5, -5.5, 2.8], 'gem', { glow: true }),
    // alça atrás (onde fica a mão)
    box([-1, -3, -5], [1, 3, 1], 'grip'),
  ];
}

function grimorio() {
  return [
    box([2, 0, 6], [14, 16, 6.75], 'leather', { cracks: 2 }),
    box([2.5, 0.5, 6.75], [13.5, 15.5, 9.25], 'pages'),
    box([2, 0, 9.25], [14, 16, 10], 'leather', { cracks: 3 }),
    box([1.25, -0.25, 5.75], [2.25, 16.25, 10.25], 'leather'),
    box([1, 2.5, 5.5], [2.5, 3.5, 10.5], 'trim'),
    box([1, 12.5, 5.5], [2.5, 13.5, 10.5], 'trim'),
    // cantos de metal
    box([12, 0, 10], [14, 2, 10.5], 'metal'),
    box([12, 14, 10], [14, 16, 10.5], 'metal'),
    box([2.25, 0, 10], [4, 2, 10.5], 'metal'),
    box([2.25, 14, 10], [4, 16, 10.5], 'metal'),
    // o olho da capa, saltado, numa moldura
    box([4.5, 4.5, 10], [11.5, 11.5, 10.5], 'trim'),
    box([5.5, 5.5, 10.5], [10.5, 10.5, 11.5], 'trim', { eye: ['south'] }),
    box([7.5, 11.5, 10], [8.5, 13, 10.4], 'gem', { glow: true }),
    box([7.5, 3, 10], [8.5, 4.5, 10.4], 'gem', { glow: true }),
    // fecho
    box([13.5, 6.5, 5.75], [14.75, 9.5, 10.25], 'metal'),
    box([14.75, 7.5, 7.5], [15.25, 8.5, 8.5], 'gem', { glow: true }),
  ];
}

// ---------------------------------------------------------------- geração

const FORM_SUFFIX = ['', '_awakened', '_supreme'];

function generate(name, boxes, display, extra = {}) {
  const rects = layout(boxes);
  for (const form of [0, 1, 2]) {
    const modelName = name + FORM_SUFFIX[form];
    // extra.textureOf: reaproveita as texturas de outro modelo com as mesmas caixas
    const texName = (extra.textureOf || name) + FORM_SUFFIX[form];
    const painted = paint(extra.textureOf || name, boxes, rects, form);
    if (!extra.textureOf) saveTextures(texName, painted, form);
    const textures = { 0: 'uda:item/' + texName, particle: 'uda:item/' + texName };
    if (form > 0) textures.glow = 'uda:item/' + texName + '_glow';
    const model = {
      credit: 'Gerado por tools/modelos/gerar_modelos.mjs',
      texture_size: [TAM, TAM],
      gui_light: 'front',
      textures,
      elements: elements(boxes, rects, form > 0, painted.hasGlow),
      display,
    };
    if (form === 0 && extra.overrides) model.overrides = extra.overrides;
    writeJson(join(MODEL_DIR, modelName + '.json'), model);
  }
}

// a última regra que bate vale: aberto → desperto; aberto e supremo → supremo
const awake = (name) => [
  { predicate: { 'uda:awakened': 1 }, model: `uda:item/${name}_awakened` },
  { predicate: { 'uda:awakened': 1, 'uda:tier': 2 }, model: `uda:item/${name}_supreme` },
];

generate('eye_sword', espada(), TOOL_DISPLAY, { overrides: awake('eye_sword') });
generate('eye_axe', machado(), TOOL_DISPLAY, { overrides: awake('eye_axe') });
generate('eye_pickaxe', picareta(), TOOL_DISPLAY, { overrides: awake('eye_pickaxe') });
generate('eye_shovel', pa(), TOOL_DISPLAY, { overrides: awake('eye_shovel') });
generate('eye_grimoire', grimorio(), BOOK_DISPLAY, { overrides: awake('eye_grimoire') });

// Arco: um modelo por estágio. O raiz decide pelo predicado (puxando / desperto).
const pulls = [[0, null], [1, 0.65], [2, 0.9]];
const bowOverrides = [
  ...pulls.map(([i, pull]) => ({ predicate: { pulling: 1, ...(pull ? { pull } : {}) }, model: `uda:item/eye_bow_pulling_${i}` })),
  { predicate: { 'uda:awakened': 1 }, model: 'uda:item/eye_bow_awakened' },
  ...pulls.map(([i, pull]) => ({ predicate: { 'uda:awakened': 1, pulling: 1, ...(pull ? { pull } : {}) }, model: `uda:item/eye_bow_pulling_${i}_awakened` })),
  { predicate: { 'uda:awakened': 1, 'uda:tier': 2 }, model: 'uda:item/eye_bow_supreme' },
  ...pulls.map(([i, pull]) => ({ predicate: { 'uda:awakened': 1, 'uda:tier': 2, pulling: 1, ...(pull ? { pull } : {}) }, model: `uda:item/eye_bow_pulling_${i}_supreme` })),
];
generate('eye_bow', arco(-1), BOW_DISPLAY, { overrides: bowOverrides });
for (const [i] of pulls) generate(`eye_bow_pulling_${i}`, arco(i), BOW_DISPLAY);

// Escudo: modelo normal e de bloqueio, fechado e aberto.
const shieldOverrides = [
  { predicate: { blocking: 1 }, model: 'uda:item/eye_shield_blocking' },
  { predicate: { 'uda:awakened': 1 }, model: 'uda:item/eye_shield_awakened' },
  { predicate: { 'uda:awakened': 1, blocking: 1 }, model: 'uda:item/eye_shield_blocking_awakened' },
  { predicate: { 'uda:awakened': 1, 'uda:tier': 2 }, model: 'uda:item/eye_shield_supreme' },
  { predicate: { 'uda:awakened': 1, 'uda:tier': 2, blocking: 1 }, model: 'uda:item/eye_shield_blocking_supreme' },
];
generate('eye_shield', escudo(), SHIELD_DISPLAY, { overrides: shieldOverrides });
generate('eye_shield_blocking', escudo(), SHIELD_BLOCKING_DISPLAY, { textureOf: 'eye_shield' });

console.log('Modelos gerados em', MODEL_DIR);
