// Gera a geometria e as texturas dos bosses.
//
//   node tools/bosses/gerar_bosses.mjs
//
// Cada boss é descrito aqui como uma árvore de partes com caixas (como no Blockbench). O gerador:
//   - distribui as caixas na textura (UV automático) e grava a geometria em assets/uda/geo/<boss>.json,
//     que o jogo lê para montar o modelo (com.uda.client.model.GeoModels) — modelo e textura nunca
//     ficam desalinhados;
//   - pinta as 3 variantes (dormant / blink / awakened) e o brilho de cada uma, como nos mobs.
// Coordenadas no padrão dos modelos de entidade: y cresce para BAIXO, a frente é -z, y = 24 é o chão.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Tex, mix, seeded } from '../texturas/png.mjs';
import { PAL, PURPLE, VARIANTS, tone, eyeColors, Skin, box as cubeFaces, crackSet, stamp, upscale, board, night } from '../texturas/pintura.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const ASSETS = join(ROOT, 'src', 'main', 'resources', 'assets', 'uda');

// ---------------------------------------------------------------- paleta dos bosses

const BPAL = {
  scale: ['#07060a', '#0e0c13', '#15121c', '#1d1926', '#262131', '#302a3e'],
  belly: ['#0f0c14', '#18131f', '#211b2a', '#2b2336', '#352b42', '#40344f'],
  horn: ['#100e14', '#1c1922', '#2a2632', '#3a3545', '#4c465a', '#615a72'],
  membrane: ['#0a0610', '#120a1b', '#1a0f27', '#231433', '#2d1a41', '#38204f'],
  sclera: ['#050407', '#0a080e', '#110e16', '#18141f', '#201b29', '#2a2335'],
  lid: ['#0c0a10', '#141019', '#1c1723', '#251e2e', '#2f273a', '#3a3047'],
  ring: ['#0f0d14', '#1a1722', '#262231', '#353042', '#474056', '#5d556e'],
  feather: ['#050407', '#0b0910', '#120f18', '#1a1622', '#231e2d', '#2d2739'],
  chain: ['#121016', '#1e1b24', '#2c2834', '#3c3747', '#4f495d', '#665f78'],
  teeth: ['#4a4456', '#5d566b', '#726a82', '#898099', '#a198b2', '#bbb2cb'],
  mouth: ['#1a0628', '#240a38', '#300e4a', '#3c125c', '#4a1870', '#5a1e88'],
};

// ---------------------------------------------------------------- geometria

/** Caixa: origem e tamanho relativos ao pivô da parte. o: { mat, mirror, inflate, share, faceMat, eyes, cracks, ... } */
const B = (origin, size, mat, o = {}) => ({ origin, size, mat, ...o });

class Geo {
  constructor(name, texW, texH) {
    this.name = name;
    this.texW = texW;
    this.texH = texH;
    this.parts = [];
    this.byId = new Map();
  }

  part(name, parent, pivot, rotation, boxes) {
    for (const b of boxes) if (b.size.some((v) => !Number.isInteger(v))) throw new Error(`${this.name}/${name}: tamanho fracionado ${b.size}`);
    const p = { name, parent, pivot, rotation: rotation || [0, 0, 0], boxes };
    boxes.forEach((b, i) => {
      b.id = b.id || `${name}#${i}`;
      this.byId.set(b.id, b);
    });
    this.parts.push(p);
    return p;
  }

  /** UV automático em prateleiras; caixas com share reaproveitam o UV de outra (espelhadas). */
  pack() {
    const boxes = this.parts.flatMap((p) => p.boxes).filter((b) => !b.share);
    const region = (b) => [2 * (b.size[2] + b.size[0]), b.size[2] + b.size[1]];
    const sorted = [...boxes].sort((a, b) => region(b)[1] - region(a)[1] || region(b)[0] - region(a)[0]);
    let x = 0, y = 0, shelf = 0;
    for (const b of sorted) {
      const [w, h] = region(b);
      if (x + w > this.texW) {
        x = 0;
        y += shelf;
        shelf = 0;
      }
      b.uv = [x, y];
      x += w;
      shelf = Math.max(shelf, Math.max(h, 1));
    }
    if (y + shelf > this.texH) throw new Error(`${this.name}: textura pequena (${y + shelf} > ${this.texH})`);
    for (const p of this.parts) for (const b of p.boxes) if (b.share) b.uv = this.byId.get(b.share).uv;
  }

  json() {
    const r = (v) => Math.round(v * 10000) / 10000;
    return {
      texture: [this.texW, this.texH],
      parts: this.parts.map((p) => ({
        name: p.name,
        parent: p.parent,
        pivot: p.pivot.map(r),
        rotation: p.rotation.map(r),
        boxes: p.boxes.map((b) => ({ uv: b.uv, origin: b.origin.map(r), size: b.size, inflate: b.inflate || 0, mirror: !!b.mirror })),
      })),
    };
  }
}

// ---------------------------------------------------------------- pintura

const FACE_KEYS = ['up', 'down', 'right', 'front', 'left', 'back'];

// Olhos pequenos espalhados (aberto / fechado), por tamanho.
const SMALL_EYES = [
  { w: 3, h: 3, open: ['RPR', 'NKN', 'RPR'], closed: ['sss', 'LLL', 'sss'] },
  { w: 3, h: 2, open: ['PNP', 'RKR'], closed: ['sss', 'LLL'] },
  { w: 2, h: 2, open: ['NP', 'KR'], closed: ['ss', 'LL'] },
];

function eyeStamp(S, x, y, shape, V, shell) {
  const c = eyeColors(V, shell);
  stamp(S, x, y, V.open ? shape.open : shape.closed, c);
}

/** Espalha olhos numa face sem sobrepor, sempre nas mesmas posições em todas as variantes. */
function scatterEyes(S, r, V, seed, density, shell) {
  const rand = seeded(seed + ':olhos');
  const count = Math.floor(r.w * r.h * density + rand());
  const used = [];
  for (let n = 0; n < count * 3 && used.length < count; n++) {
    const shape = SMALL_EYES[Math.floor(rand() * SMALL_EYES.length)];
    if (shape.w > r.w || shape.h > r.h) continue;
    const x = Math.floor(rand() * (r.w - shape.w + 1));
    const y = Math.floor(rand() * (r.h - shape.h + 1));
    if (used.some((u) => x < u.x + u.w + 1 && u.x < x + shape.w + 1 && y < u.y + u.h + 1 && u.y < y + shape.h + 1)) continue;
    used.push({ x, y, w: shape.w, h: shape.h });
    eyeStamp(S, r.x + x, r.y + y, shape, V, shell);
  }
}

/** Material base de uma face, com padrão próprio e as rachaduras roxas. */
function paintFace(S, r, V, seed, mat, opts, face) {
  const rand = seeded(seed);
  const pal = BPAL[mat] || BPAL.scale;
  const cr = opts.cracks ? crackSet(seeded(seed + ':rachaduras'), r.w, r.h, opts.cracks, [3, 8]) : new Set();
  const crackColor = V.energized ? [PURPLE.base, PURPLE.bright] : ['#22102e', '#3a1260'];
  const halo = V.energized ? [PURPLE.dark, '#16052a'] : null;
  for (let j = 0; j < r.h; j++) {
    for (let i = 0; i < r.w; i++) {
      const n = rand();
      let l = 2;
      if (face === 'up') l++;
      if (face === 'down') l--;
      if (n < 0.17) l--;
      else if (n > 0.86) l++;
      let alpha = true;
      switch (mat) {
        case 'scale': // escamas em losango
          if ((i + (j >> 1)) % 4 === 0 || (j % 2 === 0 && (i + 2) % 4 === 0)) l -= 1;
          if ((i + (j >> 1)) % 4 === 1 && j % 2 === 1) l += 1;
          break;
        case 'belly':
          if (j % 3 === 0) l -= 1;
          break;
        case 'horn':
        case 'teeth':
          l += j < r.h / 3 ? 1 : 0;
          break;
        case 'membrane': {
          // nervuras; nas faces de cima/baixo de um plano: j = 0 é a borda de trás, j = h-1 encosta no braço
          if ((i + j) % 7 === 0 || i % 9 === 0) l += 1;
          if (opts.wing && (face === 'up' || face === 'down')) {
            const t = i / Math.max(1, r.w - 1);
            const fromArm = r.h - 1 - j;
            let depth = r.h * (opts.wing === 'base' ? 1 - 0.3 * t : Math.pow(1 - t, 0.85));
            depth -= 3.5 * Math.abs(Math.sin(t * Math.PI * (opts.wing === 'base' ? 2 : 1.5))); // recortes entre os dedos
            if (fromArm > depth) alpha = false;
            else if (fromArm > depth - 2 && rand() < 0.35) alpha = false; // borda rasgada
          }
          if (opts.torn && rand() < 0.01) alpha = false;
          break;
        }
        case 'feather': {
          const fi = face === 'back' ? r.w - 1 - i : i;
          if (opts.wingShape) {
            // asa de anjo em plano: raiz em x mínimo; borda de cima em arco, penas longas embaixo com pontas recortadas
            const t = fi / Math.max(1, r.w - 1);
            const top = Math.round(r.h * 0.3 * (1 - Math.sin(Math.PI * t)));
            const featherW = 4;
            const tip = Math.abs(((fi % featherW) - (featherW - 1) / 2)) / ((featherW - 1) / 2); // 0 no meio da pena, 1 entre penas
            const bottom = r.h * (1 - 0.7 * t) - (t < 0.88 ? 2.5 * tip : 0); // termina em ponta, sem ilhas soltas
            if (j < top || j > bottom) alpha = false;
            else if (j < top + 4) l += (fi + j) % 3 === 0 ? 0 : 1; // penas pequenas (coberteiras)
            else if (fi % featherW === 0) l -= 2; // separação entre as penas longas
            else if (j > bottom - 2) l -= 1;
            break;
          }
          // pena em plano: raiz em x mínimo, ponta em x máximo (a face de trás é espelhada)
          const t = fi / Math.max(1, r.w - 1);
          const mid = (r.h - 1) / 2;
          const half = (r.h / 2) * (t < 0.12 ? 0.55 : t > 0.7 ? Math.max(0.1, (1 - t) / 0.3) : 1);
          const dist = Math.abs(j - mid);
          if (dist > half) alpha = false;
          if (dist < 0.6) l += 2; // haste
          else if ((fi + Math.floor(dist * 2)) % 3 === 0) l -= 1; // barbas
          break;
        }
        case 'ring':
          if (j === 0 || j === r.h - 1) l += 1;
          break;
        case 'chain':
          if (i > 0 && i < r.w - 1 && j > 0 && j < r.h - 1) l -= 2;
          break;
        default:
      }
      if (!alpha) {
        S.px(r.x + i, r.y + j, null);
        continue;
      }
      let base = tone(pal, l), glow = null;
      if (mat === 'mouth' && V.energized) glow = mix(PURPLE.dark, PURPLE.base, n * 0.6);
      if (cr.has(i + ',' + j)) {
        [base, glow] = crackColor;
      } else if (halo && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([a, b]) => cr.has(i + a + ',' + (j + b)))) {
        base = mix(base, halo[0], 0.55);
        glow = halo[1];
      }
      S.px(r.x + i, r.y + j, base, glow);
    }
  }
  // runas gravadas nos anéis
  if (mat === 'ring' && (face === 'front' || face === 'back') && r.w >= 4) {
    const g = seeded(seed + ':runas');
    for (let i = 1; i < r.w - 1; i++) {
      if (g() < 0.45) S.px(r.x + i, r.y + Math.floor(r.h / 2), V.energized ? PURPLE.mid : '#2a1240', V.energized ? PURPLE.base : null);
    }
  }
  // elos e espinhos acesos quando desperto
  if (opts.glowEdges && V.energized) {
    for (let i = 0; i < r.w; i++) S.px(r.x + i, r.y + r.h - 1, PURPLE.bright, PURPLE.neon);
  }
}

/** Íris gigante (frente do Olho Negro): círculo com degradê e pupila em fenda. */
function bigIris(S, r, V) {
  const cx = (r.w - 1) / 2, cy = (r.h - 1) / 2, rad = Math.min(r.w, r.h) / 2 - 0.5;
  for (let j = 0; j < r.h; j++) {
    for (let i = 0; i < r.w; i++) {
      const d = Math.hypot(i - cx, j - cy) / rad;
      if (d > 1) continue;
      const slit = Math.abs(i - cx) < 1.1 && Math.abs(j - cy) < rad * 0.82;
      if (slit) {
        S.px(r.x + i, r.y + j, '#030105', null);
        continue;
      }
      let color = d < 0.35 ? PURPLE.neon : d < 0.7 ? PURPLE.bright : d < 0.88 ? PURPLE.base : PURPLE.dark;
      if (!V.energized) color = mix(color, '#120a1a', 0.75);
      // raios da íris
      if (Math.round(Math.atan2(j - cy, i - cx) * 6) % 3 === 0 && d > 0.4) color = mix(color, '#000000', 0.25);
      S.px(r.x + i, r.y + j, color, V.open ? color : null);
    }
  }
  if (V.open) {
    S.px(r.x + Math.round(cx) - 3, r.y + Math.round(cy) - 3, '#ffffff', '#ffffff');
    S.px(r.x + Math.round(cx) - 2, r.y + Math.round(cy) - 3, PURPLE.white, '#ffffff');
  }
}

function paintGeo(geo, V) {
  const S = new Skin(geo.texW, geo.texH);
  const painted = new Set();
  for (const p of geo.parts) {
    for (const b of p.boxes) {
      if (b.share || painted.has(b.id)) continue;
      painted.add(b.id);
      const [w, h, d] = b.size;
      const f = cubeFaces(b.uv[0], b.uv[1], w, h, d);
      for (const key of FACE_KEYS) {
        const r = f[key];
        if (r.w <= 0 || r.h <= 0) continue;
        const mat = b.faceMat?.[key] || b.mat;
        const seed = `${geo.name}:${b.id}:${key}`;
        paintFace(S, r, V, seed, mat, b, key);
        if (b.eyes && mat !== 'mouth' && mat !== 'teeth') scatterEyes(S, r, V, seed, b.eyes, tone(BPAL[mat] || BPAL.scale, 1));
        if (b.bigEyes?.[key]) {
          const [ex, ey] = b.bigEyes[key];
          eyeStamp(S, r.x + ex, r.y + ey, { w: 4, h: 3, open: ['RPPR', 'NKKN', 'RPPR'], closed: ['.ss.', 'LLLL', '.ss.'] }, V, tone(BPAL.scale, 1));
        }
        if (b.iris === key) bigIris(S, r, V);
        if (b.seam && key === 'front') {
          const row = b.seam === 'bottom' ? r.h - 1 : 0;
          for (let i = 0; i < r.w; i++) S.px(r.x + i, r.y + row, V.energized ? PURPLE.bright : '#3d1766', V.energized ? PURPLE.neon : '#4b1a80');
        }
      }
    }
  }
  return S;
}

// ================================================================ DRAGÃO ABISSAL (256x128, escala 1.6)

function dragao() {
  const g = new Geo('abyssal_dragon', 256, 128);
  const scaleEyes = 0.035;
  g.part('body', 'root', [0, 6, 0], null, [
    B([-8, -7, -15], [16, 14, 30], 'scale', { eyes: scaleEyes, cracks: 5, faceMat: { down: 'belly' } }),
    ...[0, 1, 2, 3, 4].map((k) => B([-1, -10, -12 + k * 6], [2, 3, 3], 'horn', { glowEdges: true })),
  ]);
  // pescoço em 3 partes e cabeça
  g.part('neck1', 'body', [0, -2, -15], null, [
    B([-4, -4, -10], [8, 8, 10], 'scale', { eyes: scaleEyes, cracks: 1, faceMat: { down: 'belly' } }),
    B([-1, -6, -8], [2, 2, 4], 'horn'),
  ]);
  g.part('neck2', 'neck1', [0, 0, -10], null, [B([-3.5, -3.5, -10], [7, 7, 10], 'scale', { eyes: scaleEyes, faceMat: { down: 'belly' } })]);
  g.part('neck3', 'neck2', [0, 0, -10], null, [B([-3, -3, -9], [6, 6, 9], 'scale', { eyes: scaleEyes, faceMat: { down: 'belly' } })]);
  g.part('head', 'neck3', [0, 0, -9], null, [
    B([-6, -5, -14], [12, 8, 14], 'scale', { eyes: 0.02, cracks: 1, faceMat: { down: 'mouth' }, bigEyes: { right: [9, 2], left: [1, 2] } }),
    B([-4, -3, -22], [8, 5, 8], 'scale', { faceMat: { down: 'mouth' }, cracks: 1 }),
    B([-6.5, -6, -12], [4, 2, 6], 'horn'),
    B([2.5, -6, -12], [4, 2, 6], 'horn'),
    B([-3.5, 2, -21.5], [7, 1, 7], 'teeth'),
  ]);
  g.part('jaw', 'head', [0, 3, -6], null, [
    B([-5, 0, -16], [10, 3, 16], 'scale', { faceMat: { up: 'mouth', down: 'belly' } }),
    B([-4.5, -1, -15.5], [9, 1, 14], 'teeth'),
  ]);
  g.part('horn_left', 'head', [4, -5, -2], [0.6, 0.35, 0], [B([-1, -1, 0], [2, 2, 11], 'horn', { glowEdges: true })]);
  g.part('horn_right', 'head', [-4, -5, -2], [0.6, -0.35, 0], [B([-1, -1, 0], [2, 2, 11], 'horn', { share: 'horn_left#0', mirror: true })]);
  // cauda em 6 partes, terminando numa lâmina
  const tail = [[10, 8, 12], [8, 7, 12], [7, 6, 12], [6, 5, 11], [4, 4, 10], [3, 3, 9]];
  let parent = 'body';
  tail.forEach(([w, h, l], i) => {
    const name = 'tail' + (i + 1);
    const pivot = i === 0 ? [0, -3, 15] : [0, 0, tail[i - 1][2]];
    const boxes = [B([-w / 2, -h / 2, 0], [w, h, l], 'scale', { eyes: scaleEyes, faceMat: { down: 'belly' } })];
    if (i < 4) boxes.push(B([-1, -h / 2 - 2, 3], [2, 2, 4], 'horn', { glowEdges: true }));
    if (i === tail.length - 1) boxes.push(B([-5, -0.5, 4], [10, 1, 8], 'membrane', { glowEdges: true }));
    g.part(name, parent, pivot, null, boxes);
    parent = name;
  });
  // pernas recolhidas em voo
  for (const [side, sx] of [['left', 1], ['right', -1]]) {
    const mirror = sx < 0;
    for (const [pos, z, s] of [['front', -9, 4], ['hind', 9, 5]]) {
      const base = `${pos}_leg_${side}`;
      const share = (n) => (mirror ? { share: `${pos}_leg_left${n}`, mirror: true } : {});
      g.part(base, 'body', [sx * 6, 5, z], null, [B([-s / 2, 0, -s / 2], [s, s + 4, s], 'scale', { id: mirror ? undefined : `${pos}_leg_left#0`, ...share('#0') })]);
      g.part(base + '_shin', base, [0, s + 4, 0], null, [B([-(s - 1) / 2, 0, -(s - 1) / 2], [s - 1, s + 2, s - 1], 'scale', mirror ? share('_shin#0') : { id: `${pos}_leg_left_shin#0` })]);
      g.part(base + '_foot', base + '_shin', [0, s + 2, 0], null, [
        B([-s / 2, 0, -s], [s, 2, s + 1], 'scale', mirror ? share('_foot#0') : { id: `${pos}_leg_left_foot#0` }),
        B([-s / 2, 1, -s - 2], [s, 1, 2], 'horn', mirror ? share('_foot#1') : { id: `${pos}_leg_left_foot#1`, glowEdges: true }),
      ]);
    }
  }
  // asas rasgadas: braço + membrana (plano de altura 0) e a ponta, igual do outro lado espelhado
  g.part('wing_left', 'body', [7, -5, -8], null, [
    B([0, -2, -2], [24, 4, 4], 'scale', { id: 'wl_arm', eyes: scaleEyes }),
    B([0, 0, 2], [24, 0, 24], 'membrane', { id: 'wl_skin', wing: 'base', torn: true, eyes: 0.012, cracks: 3 }),
  ]);
  g.part('wing_left_tip', 'wing_left', [24, 0, 0], null, [
    B([0, -1.5, -1.5], [22, 3, 3], 'scale', { id: 'wl_tip_arm', eyes: scaleEyes }),
    B([0, 0, 1.5], [22, 0, 22], 'membrane', { id: 'wl_tip_skin', wing: 'tip', torn: true, eyes: 0.012, cracks: 2 }),
    B([20, -1, -4], [2, 2, 3], 'horn', { id: 'wl_claw', glowEdges: true }),
  ]);
  // dedos ósseos que sustentam a membrana
  const fingers = (side, parentName, list) => {
    list.forEach(([x, len, ang], k) => {
      const id = `finger_${parentName.includes('tip') ? 't' : 'b'}${k}`;
      const sx = side === 'left' ? 1 : -1;
      g.part(`${parentName}_finger${k}`, parentName, [sx * x, 0, 1], [0, -sx * ang, 0],
        [B([-0.5, -0.5, 0], [1, 1, len], 'horn', side === 'left' ? { id } : { share: id, mirror: true })]);
    });
  };
  fingers('left', 'wing_left', [[7, 22, 0.15], [16, 19, 0.3]]);
  fingers('left', 'wing_left_tip', [[0.5, 20, 0.2], [7, 15, 0.45], [13, 9, 0.7]]);
  g.part('wing_right', 'body', [-7, -5, -8], null, [
    B([-24, -2, -2], [24, 4, 4], 'scale', { share: 'wl_arm', mirror: true }),
    B([-24, 0, 2], [24, 0, 24], 'membrane', { share: 'wl_skin', mirror: true }),
  ]);
  g.part('wing_right_tip', 'wing_right', [-24, 0, 0], null, [
    B([-22, -1.5, -1.5], [22, 3, 3], 'scale', { share: 'wl_tip_arm', mirror: true }),
    B([-22, 0, 1.5], [22, 0, 22], 'membrane', { share: 'wl_tip_skin', mirror: true }),
    B([-22, -1, -4], [2, 2, 3], 'horn', { share: 'wl_claw', mirror: true }),
  ]);
  fingers('right', 'wing_right', [[7, 22, 0.15], [16, 19, 0.3]]);
  fingers('right', 'wing_right_tip', [[0.5, 20, 0.2], [7, 15, 0.45], [13, 9, 0.7]]);
  return g;
}

// ================================================================ O OLHO NEGRO (256x128, escala 2.0)

function olhoNegro() {
  const g = new Geo('black_eye', 256, 128);
  // o olho (gira junto com o olhar): caixas cruzadas formam uma esfera, a íris fica na frente
  g.part('core', 'root', [0, 12, 0], null, [
    B([-10, -10, -10], [20, 20, 20], 'sclera', { eyes: 0.02, cracks: 5 }),
    B([-11, -8, -8], [22, 16, 16], 'sclera', { eyes: 0.02, cracks: 3 }),
    B([-8, -11, -8], [16, 22, 16], 'sclera', { eyes: 0.02, cracks: 3 }),
    B([-8, -8, -11], [16, 16, 22], 'sclera', { eyes: 0.015, iris: 'front' }),
  ]);
  // pálpebras em domo: fechadas cobrem a íris (a costura brilha); abrem girando para cima e para baixo
  g.part('lid_top', 'core', [0, -12, -9], null, [
    B([-10, 0, -3], [20, 12, 2], 'lid', { seam: 'bottom', id: 'lid_a' }),
    B([-8, 3, -5], [16, 9, 2], 'lid', { seam: 'bottom', id: 'lid_b' }),
    B([-5, 7, -6], [10, 5, 1], 'lid', { seam: 'bottom', id: 'lid_c' }),
  ]);
  g.part('lid_bottom', 'core', [0, 12, -9], null, [
    B([-10, -12, -3], [20, 12, 2], 'lid', { seam: 'top', id: 'lid_d' }),
    B([-8, -12, -5], [16, 9, 2], 'lid', { seam: 'top', id: 'lid_e' }),
    B([-5, -12, -6], [10, 5, 1], 'lid', { seam: 'top', id: 'lid_f' }),
  ]);

  // anéis (rodas cheias de olhos): vertical em volta do olho, um de lado e a auréola acima
  const ring = (name, count, center, radius, place) => {
    g.part(name, 'root', center, null, []);
    for (let i = 0; i < count; i++) {
      const t = (i / count) * Math.PI * 2;
      const { pivot, rotation, box } = place(t, radius);
      g.part(`${name}_${i}`, name, pivot, rotation, [B(box[0], box[1], 'ring', i === 0 ? { eyes: 0.08, id: `${name}_seg` } : { share: `${name}_seg` })]);
    }
  };
  ring('ring_front', 16, [0, 12, 0], 20, (t, r) => ({ pivot: [r * Math.cos(t), r * Math.sin(t), 0], rotation: [0, 0, t + Math.PI / 2], box: [[-4, -1.5, -1.5], [8, 3, 3]] }));
  ring('ring_side', 16, [0, 12, 0], 23, (t, r) => ({ pivot: [0, r * Math.sin(t), r * Math.cos(t)], rotation: [-t - Math.PI / 2, 0, 0], box: [[-1.5, -1.5, -4.5], [3, 3, 9]] }));
  ring('ring_halo', 14, [0, -12, 2], 13, (t, r) => ({ pivot: [r * Math.cos(t), 0, r * Math.sin(t)], rotation: [0, -t - Math.PI / 2, 0], box: [[-3, -1, -1], [6, 2, 2]] }));

  // seis asas (três pares) atrás do olho: a de cima e a de baixo menores e inclinadas
  [[-10, -0.75, 'wing_small'], [0, 0, 'wing_big'], [10, 0.75, 'wing_small']].forEach(([dy, tilt, id], k) => {
    const [w, h] = id === 'wing_big' ? [38, 24] : [30, 19];
    const first = !g.byId.has(id);
    g.part(`wing_left_${k}`, 'root', [9, 12 + dy, 9], [0, 0, tilt], [B([0, -6, 0], [w, h, 0], 'feather', first ? { id, wingShape: true, eyes: 0.01 } : { share: id })]);
    g.part(`wing_right_${k}`, 'root', [-9, 12 + dy, 9], [0, 0, -tilt], [B([-w, -6, 0], [w, h, 0], 'feather', { share: id, mirror: true })]);
  });

  // quatro correntes que selam o olho (presas no alto, enroladas em volta); despertas se soltam e flutuam
  [Math.PI / 3, -Math.PI / 3, (2 * Math.PI) / 3, (-2 * Math.PI) / 3].forEach((phi, c) => {
    let parent = 'root';
    for (let k = 0; k < 8; k++) {
      const name = `chain_${c}_${k}`;
      const pivot = k === 0 ? [0, -1, 0] : [0, 4, 0];
      const rotation = k === 0 ? [-Math.PI / 2, phi, 0] : [0.321, 0, 0];
      const even = k % 2 === 0;
      const id = even ? 'link_a' : 'link_b';
      const first = c === 0 && k < 2;
      const boxes = [B(even ? [-1, 0, -0.5] : [-0.5, 0, -1], even ? [2, 4, 1] : [1, 4, 2], 'chain', first ? { id, glowEdges: true } : { share: id })];
      if (k === 7) boxes.push(B([-1.5, 4, -1.5], [3, 3, 3], 'ring', c === 0 ? { id: 'weight', eyes: 0.3 } : { share: 'weight' }));
      g.part(name, parent, pivot, rotation, boxes);
      parent = name;
    }
  });
  return g;
}

// ================================================================ main

const PREVIEWS = join(ROOT, 'previas');
const columns = [];
for (const geo of [dragao(), olhoNegro()]) {
  geo.pack();
  const file = join(ASSETS, 'geo', geo.name + '.json');
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(geo.json()) + '\n');
  for (const V of Object.values(VARIANTS)) {
    const S = paintGeo(geo, V);
    const dir = join(ASSETS, 'textures', 'entity', geo.name);
    S.base.save(join(dir, `${geo.name}_${V.name}.png`));
    S.glow.save(join(dir, `${geo.name}_${V.name}_glow.png`));
    if (V.name !== 'blink') {
      columns.push({ label: `${geo.name.replace('_', ' ')} ${V.name}`, img: upscale(V.open ? night(S, 0.95) : S.base, 3, ['#202024', '#26262c']) });
    }
  }
  console.log(`${geo.name}: ${geo.parts.length} partes`);
}
board('texturas dos bosses', columns).save(join(PREVIEWS, 'texturas', 'bosses.png'));
