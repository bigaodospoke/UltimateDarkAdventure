// Utilitários mínimos de imagem: PNG RGBA, cores e aleatório determinístico.
import { deflateSync } from 'node:zlib';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

const CRC = new Int32Array(256).map((_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c;
});

function crc32(buf) {
  let c = -1;
  for (const b of buf) c = CRC[(c ^ b) & 255] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

export function encodePNG(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0);
  ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; // bits por canal
  ihdr[9] = 6; // RGBA
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

/** '#rrggbb' / '#rrggbbaa' / [r,g,b,a] -> [r,g,b,a] */
export function rgb(c) {
  if (Array.isArray(c)) return c.length === 4 ? c : [...c, 255];
  const h = c.slice(1);
  return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16)).concat(h.length > 6 ? parseInt(h.slice(6, 8), 16) : 255);
}

export function mix(a, b, t) {
  a = rgb(a);
  b = rgb(b);
  return a.map((v, i) => Math.round(v + (b[i] - v) * t));
}

/** Gerador pseudoaleatório com semente em texto (sempre gera o mesmo desenho). */
export function seeded(str) {
  let h = 1779033703 ^ str.length;
  for (const ch of str) {
    h = Math.imul(h ^ ch.charCodeAt(0), 3432918353);
    h = (h << 13) | (h >>> 19);
  }
  let a = h >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export class Tex {
  constructor(w, h) {
    this.w = w;
    this.h = h;
    this.d = Buffer.alloc(w * h * 4);
  }

  set(x, y, c) {
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
    const o = (y * this.w + x) * 4;
    if (!c) {
      this.d.fill(0, o, o + 4);
      return;
    }
    const [r, g, b, a] = rgb(c);
    this.d[o] = r;
    this.d[o + 1] = g;
    this.d[o + 2] = b;
    this.d[o + 3] = a;
  }

  get(x, y) {
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return [0, 0, 0, 0];
    const o = (y * this.w + x) * 4;
    return [this.d[o], this.d[o + 1], this.d[o + 2], this.d[o + 3]];
  }

  save(file) {
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, encodePNG(this.w, this.h, this.d));
  }
}
