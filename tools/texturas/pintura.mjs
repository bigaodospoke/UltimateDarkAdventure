// Pincéis compartilhados pelos geradores de textura (mobs, bosses...).
import { Tex, mix, rgb, seeded } from './png.mjs';

// ---------------------------------------------------------------- paleta

export const PAL = {
  bone: ['#0c0b10', '#14121a', '#1c1923', '#25212d', '#302b39', '#3e3849'],
  cloth: ['#08070b', '#0e0c12', '#14111a', '#1b1722', '#231e2c', '#2d2738'],
  skin: ['#352f3c', '#413a49', '#4e4656', '#5b5264', '#685e72', '#776c82'],
  metal: ['#1d1a24', '#332e3c', '#4d4658', '#6c637a', '#91879f', '#bcb2ca'],
  leather: ['#120d12', '#1a131a', '#241a23', '#2e222c', '#382a35', '#423240'],
};

export const PURPLE = {
  deep: '#1e0b30',
  dark: '#3a1060',
  mid: '#6a1fb0',
  base: '#9b30ff',
  bright: '#c45cff',
  neon: '#e28bff',
  white: '#fbe6ff',
};

export const VARIANTS = {
  dormant: { name: 'dormant', open: false, energized: false },
  blink: { name: 'blink', open: false, energized: true },
  awakened: { name: 'awakened', open: true, energized: true },
};

export const tone = (pal, level) => pal[Math.max(0, Math.min(pal.length - 1, level))];

// Cores dos carimbos de olho: [cor base, cor do brilho] (brilho null = não brilha).
export function eyeColors(V, shell) {
  return {
    _: [null, null],
    R: [PURPLE.dark, '#2a0a48'],
    P: [PURPLE.base, PURPLE.base],
    B: [PURPLE.bright, PURPLE.bright],
    N: [PURPLE.neon, PURPLE.neon],
    W: [PURPLE.white, '#ffffff'],
    K: ['#050208', null],
    D: ['#040306', null],
    S: [shell, null],
    s: ['#1b1624', null],
    // costura da pálpebra fechada: apagada quando dormente, vazando energia quando energizada
    L: V.energized ? [PURPLE.bright, PURPLE.neon] : ['#3d1766', '#4b1a80'],
    l: V.energized ? [PURPLE.dark, '#2a0a48'] : ['#1b1624', null],
    e: V.energized ? [PURPLE.mid, PURPLE.base] : ['#1e0b30', '#26093f'],
  };
}

// ---------------------------------------------------------------- pintura

export class Skin {
  constructor(w, h) {
    this.base = new Tex(w, h);
    this.glow = new Tex(w, h);
  }

  px(x, y, base, glow = null) {
    this.base.set(x, y, base);
    this.glow.set(x, y, glow);
  }
}

export const R = (x, y, w, h) => ({ x, y, w, h });

/** Faces de um cubo de modelo com texOffs (u,v) e tamanho (w,h,d), no layout do Minecraft. */
export function box(u, v, w, h, d) {
  return {
    up: R(u + d, v, w, d),
    down: R(u + d + w, v, w, d),
    right: R(u, v + d, d, h), // lado direito da entidade; a coluna d-1 encosta na frente
    front: R(u + d, v + d, w, h),
    left: R(u + d + w, v + d, d, h), // lado esquerdo; a coluna 0 encosta na frente
    back: R(u + d + w + d, v + d, w, h),
  };
}

export const SIDES = ['right', 'front', 'left', 'back'];

export function clear(S, r) {
  for (let j = 0; j < r.h; j++) for (let i = 0; i < r.w; i++) S.px(r.x + i, r.y + j, null);
}

/** Rachaduras em zigue-zague (sempre as mesmas para a mesma semente). */
export function crackSet(rand, w, h, count, len) {
  const set = new Set();
  for (let n = 0; n < count; n++) {
    let x = Math.floor(rand() * w);
    let y = Math.floor(rand() * h);
    const dy = rand() < 0.5 ? 1 : -1;
    let drift = rand() < 0.5 ? 1 : -1;
    const steps = len[0] + Math.floor(rand() * (len[1] - len[0] + 1));
    for (let k = 0; k < steps; k++) {
      if (x >= 0 && y >= 0 && x < w && y < h) set.add(x + ',' + y);
      const r = rand();
      if (r < 0.55) y += dy;
      else if (r < 0.85) x += drift;
      else {
        x += drift;
        y += dy;
      }
      if (rand() < 0.2) drift = -drift;
    }
  }
  return set;
}

/**
 * Pinta uma face com material + ruído + rachaduras.
 * pattern: linhas de texto; '.' = transparente; outros caracteres escolhem o nível em `levels`
 * ou uma cor fixa em `special` ([base, brilho]).
 */
export function surface(S, r, V, seed, opts) {
  const { pal, level = 2, pattern = null, cracks = 0, crackLen = [3, 6], levels = {}, special = {}, folds = false, shade = true } = opts;
  const rand = seeded(seed);
  const cr = cracks > 0 ? crackSet(seeded(seed + ':rachaduras'), r.w, r.h, cracks, crackLen) : new Set();
  const core = V.energized ? [PURPLE.base, PURPLE.bright] : ['#22102e', '#3a1260'];
  const halo = V.energized ? [PURPLE.dark, '#16052a'] : null;
  const charAt = (i, j) => (pattern ? (pattern[j] ?? '')[i] ?? '.' : '#');
  for (let j = 0; j < r.h; j++) {
    for (let i = 0; i < r.w; i++) {
      const n = rand(); // consumido sempre, para o desenho não mudar entre variantes
      const ch = charAt(i, j);
      if (ch === '.') {
        S.px(r.x + i, r.y + j, null);
        continue;
      }
      if (special[ch]) {
        const [b, g] = special[ch];
        S.px(r.x + i, r.y + j, b, g);
        continue;
      }
      let l = levels[ch] ?? level;
      if (shade && j === 0) l++;
      if (shade && j === r.h - 1) l--;
      if (n < 0.18) l--;
      else if (n > 0.84) l++;
      if (folds && i % 3 === 0) l--;
      let base = tone(pal, l);
      let glow = null;
      if (cr.has(i + ',' + j)) {
        [base, glow] = core;
      } else if (halo && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([a, b]) => cr.has(i + a + ',' + (j + b)) && charAt(i + a, j + b) !== '.')) {
        base = mix(base, halo[0], 0.55);
        glow = halo[1];
      }
      S.px(r.x + i, r.y + j, base, glow);
    }
  }
}

export function stamp(S, x, y, rows, colors) {
  rows.forEach((row, j) =>
    [...row].forEach((ch, i) => {
      if (ch === '.') return;
      const c = colors[ch];
      if (!c) throw new Error('cor desconhecida no carimbo: ' + ch);
      S.px(x + i, y + j, c[0], c[1]);
    }),
  );
}

// Acabamento roxo das roupas: apagado quando dormente, aceso quando energizado.
export const trim = (V) => (V.energized ? [PURPLE.mid, PURPLE.base] : ['#2a1240', '#2a0d45']);

// ================================================================ prévias (pasta previas/)

// Fonte de pixel 3x5 para as legendas.
export const FONT = {
  A: '010101111101101', B: '110101110101110', C: '011100100100011', D: '110101101101110', E: '111100110100111',
  F: '111100110100100', G: '011100101101011', H: '101101111101101', I: '111010010010111', J: '001001001101010',
  K: '101101110101101', L: '100100100100111', M: '101111111101101', N: '110101101101101', O: '010101101101010',
  P: '110101110100100', Q: '010101101110011', R: '110101110101101', S: '011100010001110', T: '111010010010010',
  U: '101101101101111', V: '101101101101010', W: '101101111111101', X: '101101010101101', Y: '101101010010010',
  Z: '111001010100111', 0: '111101101101111', 1: '010110010010111', 2: '110001010100111', 3: '110001010001110',
  ' ': '000000000000000', '-': '000000111000000', '/': '001001010100100', '.': '000000000000010',
};

export function text(t, x, y, str, color, k = 3) {
  let cx = x;
  for (const ch of str.toUpperCase()) {
    const g = FONT[ch] ?? FONT[' '];
    for (let j = 0; j < 5; j++) for (let i = 0; i < 3; i++) {
      if (g[j * 3 + i] !== '1') continue;
      for (let a = 0; a < k; a++) for (let b = 0; b < k; b++) t.set(cx + i * k + a, y + j * k + b, color);
    }
    cx += 4 * k;
  }
}

export const textWidth = (str, k = 3) => str.length * 4 * k - k;

export function upscale(tex, k, bg) {
  const out = new Tex(tex.w * k, tex.h * k);
  for (let y = 0; y < out.h; y++) for (let x = 0; x < out.w; x++) {
    const [r, g, b, a] = tex.get(Math.floor(x / k), Math.floor(y / k));
    const back = rgb(((Math.floor(x / k) + Math.floor(y / k)) & 1) ? bg[0] : bg[1]);
    const t = a / 255;
    out.set(x, y, [0, 1, 2].map((i) => Math.round(back[i] * (1 - t) + [r, g, b][i] * t)).concat(255));
  }
  return out;
}

/** Simula o mob no escuro: textura base escurecida + brilho somado. */
export function night(S, intensity) {
  const out = new Tex(S.base.w, S.base.h);
  for (let y = 0; y < out.h; y++) for (let x = 0; x < out.w; x++) {
    const [r, g, b, a] = S.base.get(x, y);
    const [gr, gg, gb] = S.glow.get(x, y);
    if (a === 0) continue;
    out.set(x, y, [r, g, b].map((v, i) => Math.min(255, Math.round(v * 0.35 + [gr, gg, gb][i] * intensity))).concat(255));
  }
  return out;
}

/** "Boneco" montado com as faces de frente (ou de costas) de cada parte do modelo. */
export function doll(tex, parts, w, h) {
  const out = new Tex(w, h);
  for (const [r, dx, dy] of parts) {
    for (let j = 0; j < r.h; j++) for (let i = 0; i < r.w; i++) {
      const c = tex.get(r.x + i, r.y + j);
      if (c[3] > 0) out.set(dx + i, dy + j, c);
    }
  }
  return out;
}

export const BG = ['#2a2a30', '#31313a'];
export const NIGHT_BG = ['#050507', '#08080b'];
export const LABEL = '#d9c7ef';
export const TITLE = '#c45cff';

/** Prancha com título e colunas legendadas. */
export function board(title, columns, gap = 24) {
  const labelH = 30, titleH = 50;
  const w = Math.max(columns.reduce((s, c) => s + Math.max(c.img.w, textWidth(c.label)) + gap, gap), textWidth(title, 4) + gap * 2);
  const h = titleH + labelH + Math.max(...columns.map((c) => c.img.h)) + gap;
  const out = new Tex(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) out.set(x, y, '#18171c');
  text(out, gap, 16, title, TITLE, 4);
  let x0 = gap;
  for (const c of columns) {
    const cw = Math.max(c.img.w, textWidth(c.label));
    text(out, x0 + Math.floor((cw - textWidth(c.label)) / 2), titleH, c.label, LABEL);
    const ix = x0 + Math.floor((cw - c.img.w) / 2);
    for (let y = 0; y < c.img.h; y++) for (let x = 0; x < c.img.w; x++) out.set(ix + x, titleH + labelH + y, c.img.get(x, y));
    x0 += cw + gap;
  }
  return out;
}
