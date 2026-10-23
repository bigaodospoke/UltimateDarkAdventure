// Leitor de PNG (8 bits e paleta), usado para ler as texturas do próprio Minecraft.
import { readFileSync } from 'node:fs';
import { inflateSync } from 'node:zlib';
export function readPNG(file) {
  const buf = readFileSync(file);
  let pos = 8, w, h, depth, ctype, palette, trns, idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos); const type = buf.toString('ascii', pos + 4, pos + 8);
    const data = buf.subarray(pos + 8, pos + 8 + len); pos += 12 + len;
    if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); depth = data[8]; ctype = data[9]; }
    else if (type === 'PLTE') palette = data; else if (type === 'tRNS') trns = data; else if (type === 'IDAT') idat.push(data);
  }
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ctype];
  const raw = inflateSync(Buffer.concat(idat)); const stride = Math.ceil(w * ch * depth / 8); const bpp = Math.max(1, ch * depth / 8 | 0);
  const out = Buffer.alloc(w * h * 4); let prev = Buffer.alloc(stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)]; const line = Buffer.from(raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)));
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? line[i - bpp] : 0, b = prev[i], c = i >= bpp ? prev[i - bpp] : 0;
      if (f === 1) line[i] = (line[i] + a) & 255; else if (f === 2) line[i] = (line[i] + b) & 255;
      else if (f === 3) line[i] = (line[i] + ((a + b) >> 1)) & 255;
      else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); line[i] = (line[i] + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 255; }
    }
    for (let x = 0; x < w; x++) {
      const o = (y * w + x) * 4; let r, g, bb, al = 255;
      if (ctype === 6) [r, g, bb, al] = line.subarray(x * 4, x * 4 + 4);
      else if (ctype === 2) [r, g, bb] = line.subarray(x * 3, x * 3 + 3);
      else if (ctype === 0) r = g = bb = line[x];
      else if (ctype === 4) { r = g = bb = line[x * 2]; al = line[x * 2 + 1]; }
      else { const idx = depth === 8 ? line[x] : (line[(x * depth) >> 3] >> (8 - depth - ((x * depth) & 7))) & ((1 << depth) - 1); r = palette[idx * 3]; g = palette[idx * 3 + 1]; bb = palette[idx * 3 + 2]; al = trns && idx < trns.length ? trns[idx] : 255; }
      out[o] = r; out[o + 1] = g; out[o + 2] = bb; out[o + 3] = al;
    }
    prev = line;
  }
  return { w, h, data: out };
}
