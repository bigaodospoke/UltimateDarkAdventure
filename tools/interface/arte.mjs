// Arte da interface: tela com anti-serrilhado, olho, logo em fonte de pixels, céu do panorama e peças de pixel art.
// Tudo é desenhado por código (sem fontes do sistema), então sai igual no Windows e no Linux.

import { Tex, rgb, seeded } from '../texturas/png.mjs';

// ---------------------------------------------------------------- utilidades

export const clamp = (v, a = 0, b = 1) => (v < a ? a : v > b ? b : v);
export const smooth = (a, b, v) => {
  const t = clamp((v - a) / (b - a));
  return t * t * (3 - 2 * t);
};
const lerp = (a, b, t) => a + (b - a) * t;
/** '#rrggbb' → [r,g,b] em 0..1 */
export const col = (hex) => rgb(hex).slice(0, 3).map((v) => v / 255);
const mixc = (a, b, t) => [lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t)];

// Ruído de valor 3D (determinístico) e fbm
function hash3(x, y, z, s = 0) {
  let h = (x * 374761393 + y * 668265263 + z * 1274126177 + s * 982451653) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}
function noise3(x, y, z, s) {
  const xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z);
  const fx = x - xi, fy = y - yi, fz = z - zi;
  const u = fx * fx * (3 - 2 * fx), v = fy * fy * (3 - 2 * fy), w = fz * fz * (3 - 2 * fz);
  const c = (dx, dy, dz) => hash3(xi + dx, yi + dy, zi + dz, s);
  const x00 = lerp(c(0, 0, 0), c(1, 0, 0), u), x10 = lerp(c(0, 1, 0), c(1, 1, 0), u);
  const x01 = lerp(c(0, 0, 1), c(1, 0, 1), u), x11 = lerp(c(0, 1, 1), c(1, 1, 1), u);
  return lerp(lerp(x00, x10, v), lerp(x01, x11, v), w);
}
export function fbm3(x, y, z, s = 0, oct = 5) {
  let sum = 0, amp = 0.5, f = 1;
  for (let i = 0; i < oct; i++) {
    sum += amp * noise3(x * f, y * f, z * f, s + i * 31);
    f *= 2.03;
    amp *= 0.5;
  }
  return sum;
}

// ---------------------------------------------------------------- tela com supersampling

/** Tela RGBA em ponto flutuante (pré-multiplicada). */
export class Canvas {
  constructor(w, h) {
    this.w = w;
    this.h = h;
    this.d = new Float32Array(w * h * 4);
  }

  /** Pinta uma camada por cima: fn(x, y) → [r, g, b, a] (0..1, não pré-multiplicado) ou null. ss = amostras por eixo. */
  paint(fn, ss = 3) {
    const { w, h, d } = this;
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        let r = 0, g = 0, b = 0, a = 0;
        for (let j = 0; j < ss; j++) {
          for (let i = 0; i < ss; i++) {
            const c = fn(x + (i + 0.5) / ss, y + (j + 0.5) / ss);
            if (!c || c[3] <= 0) continue;
            r += c[0] * c[3];
            g += c[1] * c[3];
            b += c[2] * c[3];
            a += c[3];
          }
        }
        const n = ss * ss;
        if (a <= 0) continue;
        const o = (y * w + x) * 4;
        const k = 1 - a / n;
        d[o] = d[o] * k + r / n;
        d[o + 1] = d[o + 1] * k + g / n;
        d[o + 2] = d[o + 2] * k + b / n;
        d[o + 3] = d[o + 3] * k + a / n;
      }
    }
    return this;
  }

  /** Desenha outra tela por cima (mesmo tamanho), opcionalmente deslocada. */
  over(other, dx = 0, dy = 0, tint = null, alpha = 1) {
    for (let y = 0; y < this.h; y++) {
      for (let x = 0; x < this.w; x++) {
        const sx = x - dx, sy = y - dy;
        if (sx < 0 || sy < 0 || sx >= other.w || sy >= other.h) continue;
        const so = (sy * other.w + sx) * 4, o = (y * this.w + x) * 4;
        const a = other.d[so + 3] * alpha;
        if (a <= 0) continue;
        const [r, g, b] = tint ? [tint[0] * a, tint[1] * a, tint[2] * a] : [other.d[so] * alpha, other.d[so + 1] * alpha, other.d[so + 2] * alpha];
        const k = 1 - a;
        this.d[o] = this.d[o] * k + r;
        this.d[o + 1] = this.d[o + 1] * k + g;
        this.d[o + 2] = this.d[o + 2] * k + b;
        this.d[o + 3] = this.d[o + 3] * k + a;
      }
    }
    return this;
  }

  /** Cópia só com o canal alfa borrado (para brilhos e contornos macios). */
  blurredAlpha(radius) {
    const { w, h } = this;
    const src = new Float32Array(w * h);
    for (let i = 0; i < w * h; i++) src[i] = this.d[i * 4 + 3];
    const r = Math.ceil(radius * 2.5);
    const k = [];
    let sum = 0;
    for (let i = -r; i <= r; i++) sum += k[i + r] = Math.exp(-(i * i) / (2 * radius * radius));
    for (let i = 0; i < k.length; i++) k[i] /= sum;
    const tmp = new Float32Array(w * h), out = new Float32Array(w * h);
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      let s = 0;
      for (let i = -r; i <= r; i++) s += src[y * w + Math.min(w - 1, Math.max(0, x + i))] * k[i + r];
      tmp[y * w + x] = s;
    }
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      let s = 0;
      for (let i = -r; i <= r; i++) s += tmp[Math.min(h - 1, Math.max(0, y + i)) * w + x] * k[i + r];
      out[y * w + x] = s;
    }
    const c = new Canvas(w, h);
    for (let i = 0; i < w * h; i++) c.d[i * 4 + 3] = out[i];
    return c;
  }

  /** Silhueta engrossada em n pixels (para contornos). */
  dilated(n) {
    const { w, h } = this;
    const c = new Canvas(w, h);
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      let m = 0;
      for (let j = -n; j <= n; j++) for (let i = -n; i <= n; i++) {
        if (i * i + j * j > n * n + n) continue;
        const sx = x + i, sy = y + j;
        if (sx < 0 || sy < 0 || sx >= w || sy >= h) continue;
        m = Math.max(m, this.d[(sy * w + sx) * 4 + 3]);
      }
      c.d[(y * w + x) * 4 + 3] = m;
    }
    return c;
  }

  toTex() {
    const t = new Tex(this.w, this.h);
    for (let i = 0; i < this.w * this.h; i++) {
      const a = this.d[i * 4 + 3];
      if (a <= 0.002) continue;
      const un = (v) => Math.round(clamp(v / a) * 255);
      t.set(i % this.w, Math.floor(i / this.w), [un(this.d[i * 4]), un(this.d[i * 4 + 1]), un(this.d[i * 4 + 2]), Math.round(clamp(a) * 255)]);
    }
    return t;
  }
}

// ---------------------------------------------------------------- o olho (vetorial)

/**
 * Olho amendoado com íris e pupila em fenda. Retorna a cor no ponto (coordenadas já relativas ao centro,
 * em unidades onde a meia-largura é 1) ou null. open: 0 fechado .. 1 aberto.
 */
export function eyeSample(nx, ny, opts = {}) {
  const { open = 1, iris = 0.42, outline = 0.09, lidColor = '#07040c', glowCore = '#fbe6ff' } = opts;
  const ax = Math.abs(nx);
  if (ax >= 1) return null;
  const half = 0.5 * Math.pow(1 - ax * ax, 0.9); // meia-altura da amêndoa (relativa à meia-largura)
  const lid = half * Math.max(open, 0.02);
  const ay = Math.abs(ny);
  if (ay > half + outline) return null;
  if (ay > lid - outline * 0.6 || open < 0.05) {
    // contorno da pálpebra (e linha da pálpebra fechada)
    if (ay <= lid + outline * 0.6 || (open < 0.05 && ay < outline * 0.9)) {
      return open < 0.05 ? [...col('#c45cff'), 1] : [...col(lidColor), 1];
    }
    return null;
  }
  const r = Math.hypot(nx, ny);
  if (r < iris) {
    // pupila em fenda, mais fina nas pontas
    const slitHalf = 0.055 * Math.sqrt(Math.max(0, 1 - (ny / (iris * 0.95)) ** 2));
    if (Math.abs(nx) < slitHalf) return [0.01, 0.0, 0.02, 1];
    const t = r / iris;
    let c = t < 0.35 ? mixc(col(glowCore), col('#e28bff'), t / 0.35) : t < 0.75 ? mixc(col('#e28bff'), col('#9b30ff'), (t - 0.35) / 0.4) : mixc(col('#9b30ff'), col('#3a1060'), (t - 0.75) / 0.25);
    const ang = Math.atan2(ny, nx);
    if (Math.round(ang * 9) % 2 === 0 && t > 0.4) c = mixc(c, [0, 0, 0], 0.18); // raios da íris
    if (Math.hypot(nx + iris * 0.4, ny + iris * 0.4) < iris * 0.1) c = [1, 1, 1]; // brilho
    return [...c, 1];
  }
  // esclera escura com veias roxas irregulares (cristas de ruído, saindo da íris)
  const n = fbm3(nx * 3.2, ny * 3.2, 0.5, 23, 4);
  const vein = Math.abs(n - 0.5) < 0.008 && r > iris * 1.08 ? 1 - smooth(iris * 1.08, 0.9, r) * 0.6 : 0;
  return [...mixc(col('#140a20'), col('#7a2ad0'), vein * 0.85 + (1 - smooth(iris, 0.95, r)) * 0.25), 1];
}

/** Espinhos em leque atrás do olho (coordenadas relativas ao centro, em pixels). */
export function spikesSample(dx, dy, r0, spikes) {
  const r = Math.hypot(dx, dy);
  const ang = Math.atan2(dy, dx);
  for (const s of spikes) {
    let da = Math.abs(ang - s.a);
    if (da > Math.PI) da = 2 * Math.PI - da;
    const len = s.len * (1 - da / s.w);
    if (len > 0 && r > r0 * 0.6 && r < r0 + len) {
      const edge = r > r0 + len - 2.2 || da > s.w * (1 - 2.2 / Math.max(2.2, s.len)) * 0.92;
      const t = (r - r0) / s.len;
      return [...(edge ? col('#c45cff') : mixc(col('#2a1240'), col('#0b0612'), t)), 1];
    }
  }
  return null;
}

export function makeSpikes(count, from, to, longLen, shortLen, width) {
  const out = [];
  for (let i = 0; i < count; i++) {
    const t = count === 1 ? 0.5 : i / (count - 1);
    const a = lerp(from, to, t);
    const long = i % 2 === 0;
    const centerBoost = 1 + 0.35 * Math.cos((t - 0.5) * Math.PI);
    out.push({ a, len: (long ? longLen : shortLen) * centerBoost, w: width * (long ? 1 : 0.7) });
  }
  return out;
}

/** Emblema completo (halo, espinhos, olho) desenhado numa tela. */
export function paintEmblem(canvas, cx, cy, eyeW, opts = {}) {
  const { spikeCount = 13, glow = true, open = 1 } = opts;
  const spikes = makeSpikes(spikeCount, -Math.PI * 0.95, -Math.PI * 0.05, eyeW * 0.62, eyeW * 0.34, 0.16);
  spikes.push({ a: Math.PI * 0.5, len: eyeW * 0.3, w: 0.18 }); // espinho de baixo
  const r0 = eyeW * 0.42;
  if (glow) {
    canvas.paint((x, y) => {
      const r = Math.hypot(x - cx, (y - cy) * 1.4) / (eyeW * 1.2);
      const a = Math.exp(-r * r * 3) * 0.7;
      return a > 0.01 ? [...col('#9b30ff'), a] : null;
    }, 1);
  }
  canvas.paint((x, y) => spikesSample(x - cx, y - cy, r0, spikes));
  // anel escuro atrás do olho
  canvas.paint((x, y) => {
    const nx = (x - cx) / (eyeW * 0.56), ny = (y - cy) / (eyeW * 0.56);
    const r = Math.hypot(nx, ny);
    if (r > 1) return null;
    return r > 0.86 ? [...col('#8a3be0'), 1] : [...col('#0b0612'), 1];
  });
  canvas.paint((x, y) => eyeSample((x - cx) / (eyeW * 0.5), (y - cy) / (eyeW * 0.5), { open }));
}

// ---------------------------------------------------------------- logo (fonte de pixels em blocos)

const GLYPHS = {
  U: ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
  L: ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
  T: ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
  I: ['###', '.#.', '.#.', '.#.', '.#.', '.#.', '###'],
  M: ['#...#', '##.##', '#.#.#', '#.#.#', '#...#', '#...#', '#...#'],
  A: ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
  E: ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
  D: ['####.', '#...#', '#...#', '#...#', '#...#', '#...#', '####.'],
  R: ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
  K: ['#...#', '#..#.', '#.#..', '##...', '#.#..', '#..#.', '#...#'],
  V: ['#...#', '#...#', '#...#', '#...#', '.#.#.', '.#.#.', '..#..'],
  N: ['#...#', '##..#', '#.#.#', '#.#.#', '#..##', '#...#', '#...#'],
  O: ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
  ' ': ['..', '..', '..', '..', '..', '..', '..'],
};

/** Lista de blocos (x, y em células) de uma linha de texto. */
export function textCells(text) {
  const cells = [];
  let cx = 0;
  for (const ch of text) {
    const g = GLYPHS[ch];
    if (!g) throw new Error('letra sem glifo: ' + ch);
    g.forEach((row, y) => [...row].forEach((c, x) => c === '#' && cells.push({ x: cx + x, y })));
    cx += g[0].length + 1;
  }
  return { cells, width: cx - 1 };
}

/** Desenha uma linha de texto em blocos chanfrados com degradê, extrusão, contorno e brilho. */
export function paintBlockText(canvas, text, x0, y0, s, opts = {}) {
  const { top = '#f6dcff', mid = '#c45cff', bottom = '#4a1580', extrude = [Math.max(2, s / 3), Math.max(2, s / 2.4)] } = opts;
  const { cells } = textCells(text);
  const H = 7 * s;
  const filled = new Set(cells.map((c) => c.x + ',' + c.y));
  const at = (x, y) => filled.has(Math.floor((x - x0) / s) + ',' + Math.floor((y - y0) / s));
  // silhueta (texto + extrusão) para contorno e brilho
  const sil = new Canvas(canvas.w, canvas.h);
  sil.paint((x, y) => (at(x, y) || at(x - extrude[0], y - extrude[1]) ? [0, 0, 0, 1] : null), 2);
  canvas.over(sil.blurredAlpha(s * 0.9), 0, 0, col('#9b30ff'), 0.85);
  canvas.over(sil.dilated(Math.max(2, Math.round(s / 4))), 0, 0, col('#05030a'));
  // extrusão (a "espessura" das letras)
  canvas.paint((x, y) => {
    for (let k = 1; k <= 4; k++) {
      if (at(x - (extrude[0] * k) / 4, y - (extrude[1] * k) / 4)) return [...mixc(col('#2a0d45'), col('#12061e'), k / 4), 1];
    }
    return null;
  }, 2);
  // face dos blocos: degradê vertical + chanfro em cada bloco
  const bevel = Math.max(1, Math.round(s / 5));
  canvas.paint((x, y) => {
    if (!at(x, y)) return null;
    const t = clamp((y - y0) / H);
    let c = t < 0.45 ? mixc(col(top), col(mid), t / 0.45) : mixc(col(mid), col(bottom), (t - 0.45) / 0.55);
    const lx = (x - x0) % s, ly = (y - y0) % s;
    const left = !at(x - s, y), right = !at(x + s, y), up = !at(x, y - s), down = !at(x, y + s);
    if ((ly < bevel && up) || (lx < bevel && left)) c = mixc(c, [1, 1, 1], 0.45);
    else if ((ly >= s - bevel && down) || (lx >= s - bevel && right)) c = mixc(c, [0, 0, 0], 0.4);
    return [...c, 1];
  }, 3);
  // rachaduras acesas em algumas letras
  const rand = seeded('logo-' + text);
  const cracks = [];
  for (let i = 0; i < Math.max(2, Math.floor(cells.length / 12)); i++) {
    const c = cells[Math.floor(rand() * cells.length)];
    cracks.push({ x: x0 + (c.x + 0.2 + rand() * 0.6) * s, y: y0 + c.y * s, len: s * (1.2 + rand() * 1.5) });
  }
  canvas.paint((x, y) => {
    if (!at(x, y)) return null;
    for (const k of cracks) {
      const t = (y - k.y) / k.len;
      if (t < 0 || t > 1) continue;
      const cx = k.x + Math.sin(t * 9 + k.x) * s * 0.18;
      if (Math.abs(x - cx) < Math.max(0.7, s / 10)) return [...col('#fbe6ff'), 0.9 * (1 - t * 0.5)];
    }
    return null;
  }, 3);
}

export function makeLogo(w = 512, h = 228) {
  const c = new Canvas(w, h);
  paintEmblem(c, w / 2, 62, 112);
  const big = 9, small = 5;
  const l1 = textCells('ULTIMATE').width * big, l2 = textCells('DARK ADVENTURE').width * small;
  paintBlockText(c, 'ULTIMATE', Math.round((w - l1) / 2), 104, big);
  paintBlockText(c, 'DARK ADVENTURE', Math.round((w - l2) / 2), 180, small, { top: '#e8d0ff', mid: '#a040ff', bottom: '#3a1060' });
  return c.toTex();
}

export function makePackIcon(size = 128) {
  const c = new Canvas(size, size);
  c.paint((x, y) => {
    const r = Math.hypot(x - size / 2, y - size / 2) / (size / 2);
    return [...mixc(col('#2a0d45'), col('#05030a'), smooth(0, 1, r)), 1];
  }, 1);
  c.paint((x, y) => {
    const ax = Math.min(x, size - x), ay = Math.min(y, size - y);
    const d = Math.min(ax, ay);
    return d < 3 ? [...col(d < 1.5 ? '#07040c' : '#8a3be0'), 1] : null;
  }, 2);
  paintEmblem(c, size / 2, size * 0.58, size * 0.62, { spikeCount: 9 });
  return c.toTex();
}

// ---------------------------------------------------------------- céu (panorama e tela de carregamento)

const norm = (v) => {
  const l = Math.hypot(v[0], v[1], v[2]);
  return [v[0] / l, v[1] / l, v[2] / l];
};
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];

export const EYE_DIR = norm([0, 0.32, -1]);
const EYE_R = norm(cross(EYE_DIR, [0, 1, 0]));
const EYE_U = cross(EYE_R, EYE_DIR);

// Silhueta de ruínas no horizonte: torres com ameias, pináculos e muros quebrados.
const TOWERS = (() => {
  const r = seeded('ruinas-horizonte');
  const out = [];
  for (let i = 0; i < 34; i++) {
    const az = -Math.PI + (i / 34) * Math.PI * 2 + (r() - 0.5) * 0.12;
    const kind = r() < 0.3 ? 'spire' : r() < 0.6 ? 'battlement' : 'broken';
    const far = r() < 0.45;
    out.push({ az, w: (0.015 + r() * 0.03) * (far ? 0.7 : 1), h: (0.05 + r() * 0.13) * (far ? 0.6 : 1), kind, far, seed: r() });
  }
  return out;
})();

function skyline(az, far) {
  let h = far ? 0.025 + 0.02 * fbm3(az * 2.2, 3.1, 0.5, 7, 3) : 0.012 + 0.012 * fbm3(az * 5, 1.7, 0.2, 9, 3);
  for (const t of TOWERS) {
    if (t.far !== far) continue;
    let d = Math.abs(az - t.az);
    if (d > Math.PI) d = 2 * Math.PI - d;
    if (d > t.w * 1.6) continue;
    let th;
    if (t.kind === 'spire') th = d < t.w ? t.h + t.w * 2.2 * (1 - d / t.w) : 0;
    else if (t.kind === 'battlement') th = d < t.w ? t.h + (Math.floor((d / t.w) * 4) % 2 === 0 ? t.w * 0.35 : 0) : 0;
    else th = d < t.w ? t.h * (0.6 + 0.4 * Math.abs(Math.sin(d * 140 + t.seed * 9))) : 0;
    if (d >= t.w && d < t.w * 1.6 && t.kind !== 'broken') th = t.h * 0.35; // muro baixo ao lado
    h = Math.max(h, th);
  }
  return h;
}

/** Cor do céu numa direção (y para cima, -z é a frente). */
export function skyColor(d) {
  const e = Math.asin(clamp(d[1], -1, 1));
  const az = Math.atan2(d[0], -d[2]);
  let c;
  if (e >= 0) {
    c = mixc(col('#3c1266'), col('#05030a'), smooth(0, 0.95, e));
    c = mixc(c, col('#8a3be0'), Math.exp(-((e / 0.07) ** 2)) * 0.55);
    // nuvens / nebulosa
    const n = fbm3(d[0] * 2.6, d[1] * 2.6, d[2] * 2.6, 3);
    const cloud = smooth(0.48, 0.78, n) * smooth(-0.05, 0.25, e);
    c = mixc(c, col('#5a1a9e'), cloud * 0.55);
    c = mixc(c, col('#c45cff'), smooth(0.7, 0.85, n) * 0.25 * smooth(0, 0.3, e));
    // estrelas
    const sx = Math.floor(d[0] * 260), sy = Math.floor(d[1] * 260), sz = Math.floor(d[2] * 260);
    const hs = hash3(sx, sy, sz, 5);
    if (hs < 0.004 && e > 0.08) c = mixc(c, [1, 0.92, 1], (0.4 + hs * 150) * smooth(0.08, 0.3, e) * (1 - cloud));
  } else {
    c = mixc(col('#2a0d45'), col('#040208'), smooth(0, 0.22, -e));
  }
  // o olho no céu
  const f = dot(d, EYE_DIR);
  if (f > 0.5) {
    const u = dot(d, EYE_R) / f, v = dot(d, EYE_U) / f;
    const r = Math.hypot(u, v * 1.6);
    c = mixc(c, col('#b050ff'), Math.exp(-r * r * 9) * 0.75); // halo
    // raios de luz saindo do olho
    const ang = Math.atan2(v, u);
    const ray = Math.pow(Math.abs(Math.sin(ang * 7 + 0.4)), 18) * Math.exp(-r * 2.5) * 0.35;
    c = mixc(c, col('#e28bff'), ray);
    const s = eyeSample(u / 0.42, -v / 0.42, { glowCore: '#ffffff' });
    if (s) c = mixc(c, [s[0], s[1], s[2]], s[3]);
  }
  // relâmpagos roxos
  for (const [baz, seed] of [[1.15, 1], [-2.1, 2], [2.6, 3]]) {
    const t = clamp(e / 0.5);
    if (e < 0 || e > 0.5) continue;
    const jag = (fbm3(e * 18, seed, 0, 40 + seed, 3) - 0.5) * 0.09;
    const dist = Math.abs(az - (baz + jag + t * 0.05));
    if (dist < 0.004) c = mixc(c, [0.95, 0.85, 1], 0.9 * (1 - t));
    else if (dist < 0.03) c = mixc(c, col('#9b30ff'), (1 - dist / 0.03) * 0.35 * (1 - t));
  }
  // ruínas no horizonte (distantes mais claras pela névoa)
  const hFar = skyline(az, true), hNear = skyline(az, false);
  if (e < hFar && e > -0.03) c = mixc(c, col('#1a0a2a'), 0.85);
  if (e < hNear && e > -0.05) {
    c = col('#06030a');
    if (e > hNear - 0.003) c = col('#6a2aa8'); // contorno aceso
    const win = hash3(Math.floor(az * 900), Math.floor(e * 900), 1, 11);
    if (win < 0.012 && e > 0.005) c = col('#c45cff'); // janelas
  }
  // névoa roxa rente ao chão
  if (e < 0.04) c = mixc(c, col('#3a1060'), Math.exp(-(((e - 0.0) / 0.03) ** 2)) * 0.45);
  return c;
}

/** Uma face do panorama (vértices de CubeMap.render; o mundo é (x, -y, -z) do vértice). */
export function panoramaFace(k, size) {
  const c = new Canvas(size, size);
  c.paint((px, py) => {
    const u = px / size, v = py / size;
    const a = -1 + 2 * u, b = -1 + 2 * v;
    const vert = [[a, b, 1], [1, b, 1 - 2 * u], [1 - 2 * u, b, -1], [-1, b, -1 + 2 * u], [a, -1, b], [a, 1, 1 - 2 * v]][k];
    const d = norm([vert[0], -vert[1], -vert[2]]);
    return [...skyColor(d), 1];
  }, 2);
  return c.toTex();
}

/** Arte da tela de carregamento: o céu visto de frente para o olho, com correntes penduradas. */
export function loadingArt(w, h) {
  const c = new Canvas(w, h);
  const fwd = norm([0, 0.2, -1]);
  const right = norm(cross(fwd, [0, 1, 0]));
  const up = cross(right, fwd);
  const tanH = Math.tan((84 * Math.PI) / 360);
  c.paint((px, py) => {
    const sx = ((px / w) * 2 - 1) * tanH, sy = (1 - (py / h) * 2) * tanH * (h / w);
    const d = norm([fwd[0] + right[0] * sx + up[0] * sy, fwd[1] + right[1] * sx + up[1] * sy, fwd[2] + right[2] * sx + up[2] * sy]);
    return [...skyColor(d), 1];
  }, 2);
  // correntes: elos alternados ao longo de curvas dos cantos de cima até perto do olho
  const chains = [
    [[-0.04 * w, 0.02 * h], [0.18 * w, 0.42 * h], [0.4 * w, 0.27 * h]],
    [[1.04 * w, 0.02 * h], [0.82 * w, 0.42 * h], [0.6 * w, 0.27 * h]],
    [[0.16 * w, -0.05 * h], [0.24 * w, 0.2 * h], [0.43 * w, 0.2 * h]],
    [[0.84 * w, -0.05 * h], [0.76 * w, 0.2 * h], [0.57 * w, 0.2 * h]],
  ];
  const linkLen = h * 0.045;
  for (const [p0, p1, p2] of chains) {
    const pts = [];
    for (let i = 0; i <= 400; i++) {
      const t = i / 400;
      pts.push([(1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]]);
    }
    // elos a cada linkLen de comprimento de arco
    const links = [];
    let acc = 0;
    for (let i = 1; i < pts.length; i++) {
      acc += Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]);
      if (acc >= linkLen * 0.8) {
        acc = 0;
        const dx = pts[i][0] - pts[i - 1][0], dy = pts[i][1] - pts[i - 1][1];
        links.push({ x: pts[i][0], y: pts[i][1], a: Math.atan2(dy, dx), flat: links.length % 2 === 1 });
      }
    }
    c.paint((x, y) => {
      for (const l of links) {
        const dx = x - l.x, dy = y - l.y;
        if (Math.abs(dx) > linkLen && Math.abs(dy) > linkLen) continue;
        const ca = Math.cos(l.a), sa = Math.sin(l.a);
        const lx = dx * ca + dy * sa, ly = -dx * sa + dy * ca;
        const rx = linkLen * 0.62, ry = l.flat ? linkLen * 0.14 : linkLen * 0.34;
        const r = Math.hypot(lx / rx, ly / ry);
        const ring = l.flat ? r < 1 : r < 1 && r > 0.55;
        if (ring) {
          const rim = ly < 0 ? 0.6 : 0.15;
          return [...mixc(col('#1a1622'), col('#8a3be0'), rim * (l.flat ? 0.6 : 1)), 1];
        }
      }
      return null;
    }, 2);
  }
  // vinheta
  c.paint((x, y) => {
    const r = Math.hypot((x / w - 0.5) * 1.6, (y / h - 0.45) * 1.4);
    return [0, 0, 0, clamp(smooth(0.45, 1.05, r) * 0.85)];
  }, 1);
  return c.toTex();
}

// ---------------------------------------------------------------- pixel art (sem anti-serrilhado)

/** Grade de pixels com camadas por cor, para peças pequenas da interface. */
export class Pix {
  constructor(w, h) {
    this.t = new Tex(w, h);
    this.w = w;
    this.h = h;
  }

  fill(test, color) {
    for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) if (test(x + 0.5, y + 0.5)) this.t.set(x, y, color);
    return this;
  }

  rows(x0, y0, rows, colors) {
    rows.forEach((r, j) => [...r].forEach((ch, i) => {
      if (ch !== '.' && ch !== ' ') this.t.set(x0 + i, y0 + j, colors[ch]);
    }));
    return this;
  }

  /** Contorno de 1 px em volta do que já foi pintado. */
  outline(color) {
    const solid = (x, y) => x >= 0 && y >= 0 && x < this.w && y < this.h && this.t.get(x, y)[3] > 0;
    const add = [];
    for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) {
      if (!solid(x, y) && (solid(x + 1, y) || solid(x - 1, y) || solid(x, y + 1) || solid(x, y - 1))) add.push([x, y]);
    }
    for (const [x, y] of add) this.t.set(x, y, color);
    return this;
  }
}

/** Olho pequeno em pixel art. state: 'sealed' | 'closed' | 'half' | 'open' | 'open2' | 'supreme' | 'supreme2'. */
export function pixelEye(p, cx, cy, hw, state) {
  const sealed = state === 'sealed';
  const open = state.startsWith('open') || state.startsWith('supreme');
  const half = state === 'half';
  const supreme = state.startsWith('supreme');
  const pulse = state.endsWith('2');
  const hh = hw * 0.55;
  const almond = (x, y) => Math.abs(y - cy) <= hh * Math.pow(Math.max(0, 1 - ((x - cx) / hw) ** 2), 0.9);
  p.fill((x, y) => almond(x, y), '#07040c');
  if (!open && !half) {
    // pálpebra fechada: costura
    p.fill((x, y) => almond(x, y) && Math.abs(y - cy) < 0.9 && Math.abs(x - cx) < hw - 0.6, sealed ? '#3d1766' : '#c45cff');
    p.fill((x, y) => almond(x, y) && y > cy + 0.9 && y < cy + 2 && Math.abs(x - cx) < hw - 1.5 && Math.round(x) % 2 === 0, sealed ? '#2a1240' : '#7a2ad0');
    return;
  }
  const lidTop = half ? cy - 0.6 : cy - hh;
  p.fill((x, y) => almond(x, y) && y > lidTop + 0.8 && Math.abs(x - cx) < hw - 0.8, '#1c0c2c');
  const ir = hw * 0.48;
  p.fill((x, y) => almond(x, y) && y > lidTop + 0.8 && Math.hypot(x - cx, y - cy) < ir, supreme ? (pulse ? '#ffffff' : '#f0c8ff') : pulse ? '#e28bff' : '#c45cff');
  p.fill((x, y) => almond(x, y) && y > lidTop + 0.8 && Math.hypot(x - cx, y - cy) < ir && Math.hypot(x - cx, y - cy) > ir - 1.1, supreme ? '#c45cff' : '#7a2ad0');
  p.fill((x, y) => y > lidTop + 0.8 && Math.abs(x - cx) < (pulse ? 0.5 : 0.8) && Math.abs(y - cy) < ir - 0.4, '#05020a');
  if (!half) p.t.set(Math.round(cx - ir * 0.5), Math.round(cy - ir * 0.45), [255, 255, 255, 255]);
}

/** Cores do olho desenhado à mão por estado (L contorno, w esclera, d borda da íris, p íris, s pupila, h brilho). */
function handEyeColors(state) {
  const base = { w: '#1c0c2c', s: '#05020a', h: '#ffffff', l: '#4a1a78' };
  switch (state) {
    case 'sealed': return { ...base, L: '#3d1766', l: '#2a1240' };
    case 'closed': return { ...base, L: '#b04cff', l: '#7a2ad0' };
    case 'half': return { ...base, L: '#b04cff', d: '#7a2ad0', p: '#c45cff' };
    case 'open': return { ...base, L: '#b04cff', d: '#7a2ad0', p: '#c45cff' };
    case 'open2': return { ...base, L: '#c87aff', d: '#9b40e8', p: '#e28bff' };
    case 'supreme': return { ...base, L: '#f0c8ff', d: '#c45cff', p: '#f0c8ff' };
    case 'supreme2': return { ...base, L: '#ffffff', d: '#e28bff', p: '#ffffff' };
    default: throw new Error('estado do olho: ' + state);
  }
}

const HAND_EYE = {
  open: [
    '....LLLLLLL....',
    '..LLwdddddwLL..',
    '.LwwdhpsppdwwL.',
    'LwwwdppsppdwwwL',
    'LwwwdppsppdwwwL',
    'LwwwdppsppdwwwL',
    '.LwwddpspddwwL.',
    '..LLwdddddwLL..',
    '....LLLLLLL....',
  ],
  half: [
    '...............',
    '...............',
    '...............',
    '..LLLLLLLLLLL..',
    '.LwwdppsppdwwL.',
    'LwwwddpspddwwwL',
    '.LLwwdddddwwLL.',
    '...LLLLLLLLL...',
    '...............',
  ],
  closed: [
    '...............',
    '...............',
    '...............',
    '.LL.........LL.',
    '...LLLLLLLLL...',
    '...l.l.l.l.l...',
    '...............',
    '...............',
    '...............',
  ],
  small: [
    '..LLLLL..',
    '.LwdpdwL.',
    'LwwpspwwL',
    '.LwdpdwL.',
    '..LLLLL..',
  ],
};

/** Olho em pixel art desenhado à mão, 15x9 (ou 9x5 com size 'small'), com o canto de cima à esquerda em (x0, y0). */
export function handEye(p, x0, y0, state, size = 'big') {
  const colors = handEyeColors(state);
  const shape = size === 'small' ? 'small' : state.startsWith('open') || state.startsWith('supreme') ? 'open' : state === 'sealed' ? 'closed' : state;
  p.rows(x0, y0, HAND_EYE[shape], colors);
}
