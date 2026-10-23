// Gera a interface temática do Ultimate Dark Adventure.
//
//   node tools/interface/gerar_interface.mjs [caminho do minecraft_1.21.1_client.jar]
//
// 1. Extrai as texturas de textures/gui do jar do Minecraft (o padrão é o jar que o Gradle baixa) e as recolore
//    pela luminância numa escala roxa escura. O desenho de cada tela fica exatamente igual; só as cores mudam.
//    O resultado vai para o pacote de recursos embutido (src/main/resources/resourcepacks/uda_interface).
// 2. Desenha as peças próprias: panorama, logo, ícone do pacote, arte de carregamento, olho ornamental dos
//    containers, olho do HUD e barra de boss (assets/uda/textures/gui/interface, usadas pelo código).
// 3. Monta pranchas de prévia em previas/interface.

import { execFileSync } from 'node:child_process';
import { copyFileSync, existsSync, mkdirSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Tex, mix, rgb, seeded } from '../texturas/png.mjs';
import { readPNG } from '../texturas/lerpng.mjs';
import { board, upscale } from '../texturas/pintura.mjs';
import { Pix, handEye, loadingArt, makeLogo, makePackIcon, panoramaFace, pixelEye } from './arte.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const PACK = join(ROOT, 'src', 'main', 'resources', 'resourcepacks', 'uda_interface');
const PACK_GUI = join(PACK, 'assets', 'minecraft', 'textures', 'gui');
const MOD_GUI = join(ROOT, 'src', 'main', 'resources', 'assets', 'uda', 'textures', 'gui', 'interface');
const VANILLA = join(ROOT, 'build', 'uda-gui-vanilla');
const PREVIEWS = join(ROOT, 'previas', 'interface');
const JAR = process.argv[2] || join(homedir(), '.gradle', 'caches', 'neoformruntime', 'artifacts', 'minecraft_1.21.1_client.jar');

// ================================================================ 1. extrair

function extractVanilla() {
  if (!existsSync(JAR)) {
    throw new Error(`Não achei o jar do Minecraft em ${JAR}. Rode "./gradlew build" uma vez (ele baixa o jar) ou passe o caminho como argumento.`);
  }
  const prefix = 'assets/minecraft/textures/gui/';
  let list;
  try {
    list = execFileSync('unzip', ['-Z1', JAR], { encoding: 'utf8', maxBuffer: 64 << 20 }).split(/\r?\n/);
  } catch {
    list = execFileSync('jar', ['tf', JAR], { encoding: 'utf8', maxBuffer: 64 << 20 }).split(/\r?\n/);
  }
  const files = list.filter((f) => f.startsWith(prefix) && !f.endsWith('/'));
  rmSync(VANILLA, { recursive: true, force: true });
  mkdirSync(VANILLA, { recursive: true });
  // em blocos: a linha de comando do Windows tem limite de tamanho
  for (let i = 0; i < files.length; i += 80) {
    const chunk = files.slice(i, i + 80);
    try {
      execFileSync('unzip', ['-o', '-q', JAR, ...chunk, '-d', VANILLA]);
    } catch {
      execFileSync('jar', ['xf', JAR, ...chunk], { cwd: VANILLA });
    }
  }
  return join(VANILLA, ...prefix.split('/'));
}

// ================================================================ 2. recolorir

// Escalas: luminância do original → cor nova. edge = cor dos pixels escuros na borda da silhueta (contorno).
const RAMPS = {
  panel: { stops: [[0, '#040306'], [55, '#0a0710'], [85, '#0f0b17'], [139, '#120e1a'], [198, '#1d1629'], [219, '#2b1a45'], [255, '#5e2a9c']], edge: '#8a3be0', protect: 40 },
  button: { stops: [[0, '#050308'], [44, '#0b0812'], [86, '#110c1b'], [111, '#1c1430'], [170, '#3a2560'], [255, '#c45cff']], edge: '#5a2a96', protect: 40 },
  buttonHL: { stops: [[0, '#050308'], [92, '#2a1550'], [117, '#33205c'], [175, '#7a40c0'], [255, '#e8a0ff']], edge: '#c45cff', protect: 40 },
  disabled: { stops: [[0, '#050308'], [44, '#0c0911'], [60, '#100c16'], [255, '#555060']], edge: '#2a1e3a', protect: 40 },
  hud: { stops: [[0, '#030205'], [40, '#120a1e'], [93, '#2a1a40'], [126, '#3a2460'], [147, '#4a2c78'], [255, '#c45cff']], edge: '#6a2aa8', protect: 255 },
  select: { stops: [[0, '#030205'], [100, '#5a1a9e'], [170, '#a040ff'], [225, '#d890ff'], [255, '#f6e0ff']], edge: null, protect: 255 },
  toast: { stops: [[0, '#050308'], [33, '#100b1a'], [85, '#3a1a60'], [255, '#c45cff']], edge: '#8a3be0', protect: 40 },
  light: { stops: [[0, '#2a0d45'], [170, '#b49ad6'], [222, '#e9e0f5'], [255, '#ffffff']], edge: '#5a2a96', protect: 40 },
  // chama das fornalhas: o fogo vira roxo pela luminância (vermelho escuro → amarelo claro)
  flame: { stops: [[0, '#3a1060'], [100, '#7a2ad0'], [150, '#c45cff'], [226, '#f0c8ff'], [255, '#ffffff']], edge: null, protect: 255 },
};

const STOPS = Object.fromEntries(Object.entries(RAMPS).map(([k, r]) => [k, r.stops.map(([l, c]) => [l, rgb(c)])]));

function ramp(name, L) {
  const s = STOPS[name];
  if (L <= s[0][0]) return s[0][1];
  for (let i = 1; i < s.length; i++) {
    if (L <= s[i][0]) {
      const t = (L - s[i - 1][0]) / (s[i][0] - s[i - 1][0]);
      return s[i - 1][1].map((v, k) => Math.round(v + (s[i][1][k] - v) * t));
    }
  }
  return s[s.length - 1][1];
}

const lum = (r, g, b) => Math.round(0.299 * r + 0.587 * g + 0.114 * b);

function toHsl(r, g, b) {
  r /= 255; g /= 255; b /= 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  const l = (max + min) / 2;
  if (max === min) return [0, 0, l];
  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  const h = max === r ? (g - b) / d + (g < b ? 6 : 0) : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return [h / 6, s, l];
}

function fromHsl(h, s, l) {
  if (s === 0) return [l, l, l].map((v) => Math.round(v * 255));
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
  const f = (t) => {
    t = (t + 1) % 1;
    return t < 1 / 6 ? p + (q - p) * 6 * t : t < 1 / 2 ? q : t < 2 / 3 ? p + (q - p) * (2 / 3 - t) * 6 : p;
  };
  return [f(h + 1 / 3), f(h), f(h - 1 / 3)].map((v) => Math.round(v * 255));
}

/**
 * Recolore uma imagem. mode: nome da escala, 'purple' (só troca o matiz), 'purple:<escala>' (troca o matiz das
 * cores e passa os cinzas pela escala), 'gray:<escala>' (dessatura antes) ou
 * 'flame' (fogo pela escala flame, fundo cinza pela escala panel).
 */
function recolor(img, mode) {
  const { w, h, data } = img;
  const t = new Tex(w, h);
  const alpha = (x, y) => (x < 0 || y < 0 || x >= w || y >= h ? 0 : data[(y * w + x) * 4 + 3]);
  const [kind, gray] = mode.startsWith('gray:') ? [mode.slice(5), true] : [mode.split(':')[0], false];
  const grayRamp = mode.startsWith('purple:') ? mode.slice(7) : null;
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const o = (y * w + x) * 4;
      const [r, g, b, a] = data.subarray(o, o + 4);
      if (a === 0) continue;
      if (kind === 'purple') {
        if (Math.max(r, g, b) - Math.min(r, g, b) < 20) {
          t.set(x, y, grayRamp ? [...ramp(grayRamp, lum(r, g, b)).slice(0, 3), a] : [r, g, b, a]);
          continue;
        }
        const [, s, l] = toHsl(r, g, b);
        t.set(x, y, [...fromHsl(0.78, Math.min(1, s * 1.05), l), a]);
        continue;
      }
      if (kind === 'flame') {
        const saturated = Math.max(r, g, b) - Math.min(r, g, b) > 40;
        const c = ramp(saturated || lum(r, g, b) >= 250 ? 'flame' : 'panel', lum(r, g, b));
        t.set(x, y, [c[0], c[1], c[2], a]);
        continue;
      }
      const R = RAMPS[kind];
      if (!gray && Math.max(r, g, b) - Math.min(r, g, b) > R.protect) {
        t.set(x, y, [r, g, b, a]); // cores fortes (chamas, bolhas, setas coloridas) ficam como estão
        continue;
      }
      const L = lum(r, g, b);
      const edge = alpha(x + 1, y) === 0 || alpha(x - 1, y) === 0 || alpha(x, y + 1) === 0 || alpha(x, y - 1) === 0;
      if (edge && R.edge && L < 60) {
        t.set(x, y, [...rgb(R.edge).slice(0, 3), a]);
        continue;
      }
      let c = ramp(kind, L);
      // painel com uma textura bem sutil de pedra
      if (kind === 'panel' && L === 198 && seeded(`${x},${y}`)() < 0.18) c = rgb('#211a2f');
      t.set(x, y, [c[0], c[1], c[2], a]);
    }
  }
  return t;
}

/** Qual escala cada arquivo de textures/gui recebe (null = não mexe). */
function modeFor(rel) {
  const p = rel.split(sep).join('/');
  const name = p.split('/').pop();
  if (p === 'container/beacon.png') return null;
  if (p.startsWith('container/') || p === 'recipe_book.png' || p === 'advancements/window.png') return 'panel';
  if (p.startsWith('sprites/container/enchanting_table/') || p.startsWith('sprites/container/beacon/')) return null;
  if (p.startsWith('sprites/container/inventory/effect_background')) return 'toast';
  if (/^sprites\/container\/(furnace|blast_furnace|smoker)\/lit_progress\.png$/.test(p)) return 'flame';
  if (/^sprites\/recipe_book\/button(_highlighted)?\.png$/.test(p)) return name.includes('highlighted') ? 'purple:buttonHL' : 'purple:button';
  if (p.startsWith('sprites/container/') || p.startsWith('sprites/recipe_book/') || p === 'sprites/popup/background.png') return 'panel';
  if (p.startsWith('sprites/widget/')) return name.includes('highlighted') ? 'buttonHL' : name.includes('disabled') ? 'disabled' : 'button';
  if (/^sprites\/hud\/(hotbar|hotbar_offhand_left|hotbar_offhand_right|experience_bar_background)\.png$/.test(p)) return 'hud';
  if (p === 'sprites/hud/hotbar_selection.png') return 'select';
  if (p === 'sprites/hud/experience_bar_progress.png') return 'purple';
  if (/^sprites\/hud\/effect_background(_ambient)?\.png$/.test(p)) return 'toast';
  if (/^sprites\/hud\/heart\/(hardcore_)?(full|half)(_blinking)?\.png$/.test(p)) return 'purple';
  if (p.startsWith('sprites/advancements/')) {
    if (name.endsWith('_obtained.png')) return 'purple';
    if (name === 'box_unobtained.png') return 'gray:toast';
    if (name === 'title_box.png') return 'toast';
    return 'panel';
  }
  if (p === 'sprites/toast/advancement.png') return 'toast';
  if (p === 'sprites/toast/system.png') return 'gray:toast'; // o original é azul
  if (/^sprites\/toast\/(recipe|tutorial)\.png$/.test(p)) return 'light';
  return null;
}

function walk(dir, out = []) {
  for (const f of readdirSync(dir)) {
    const full = join(dir, f);
    if (statSync(full).isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

function recolorAll(gui) {
  rmSync(PACK_GUI, { recursive: true, force: true });
  let count = 0;
  for (const file of walk(gui)) {
    if (!file.endsWith('.png')) continue;
    const rel = relative(gui, file);
    const mode = modeFor(rel);
    if (!mode) continue;
    const out = join(PACK_GUI, rel);
    recolor(readPNG(file), mode).save(out);
    if (existsSync(file + '.mcmeta')) copyFileSync(file + '.mcmeta', out + '.mcmeta'); // nine-slice vem do mesmo pacote
    count++;
  }
  return count;
}

// ================================================================ 3. peças próprias

const save = (t, dir, name) => {
  t.save(join(dir, name));
  return t;
};

/** Fundo de menu: azulejo 16x16 roxo-escuro translúcido com granulado leve. */
function menuTile(hex, alpha, seed) {
  const t = new Tex(16, 16);
  const r = seeded(seed);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const n = r();
    const c = rgb(n < 0.12 ? mix(hex, '#000000', 0.35) : n > 0.93 ? mix(hex, '#5a1a9e', 0.35) : hex);
    t.set(x, y, [c[0], c[1], c[2], alpha]);
  }
  return t;
}

/** Separador de cabeçalho/rodapé: linha roxa acesa + linha de sombra. */
function separator(lightFirst) {
  const t = new Tex(32, 2);
  for (let x = 0; x < 32; x++) {
    const glow = 110 + Math.round(40 * Math.sin((x / 32) * Math.PI));
    t.set(x, lightFirst ? 0 : 1, [176, 96, 255, glow]);
    t.set(x, lightFirst ? 1 : 0, [5, 2, 10, 200]);
  }
  return t;
}

/** Olho ornamental que fica em cima do painel dos containers (40x13; a linha 9 encosta na borda do painel). */
function containerEye() {
  const p = new Pix(40, 13);
  const cx = 19.5, cy = 6.5;
  // base: filete roxo sobre a borda do painel, afinando para as pontas
  p.fill((x, y) => y >= 9 && y < 10 && Math.abs(x - cx) < 18, '#8a3be0');
  p.fill((x, y) => y >= 10 && y < 11 && Math.abs(x - cx) < 15, '#2a1240');
  // asas curvas
  p.fill((x, y) => {
    const dx = Math.abs(x - cx);
    return dx > 7 && dx < 17 && Math.abs(y - (9 - (17 - dx) * 0.42)) < 0.75;
  }, '#5a2a96');
  // espinhos
  for (const [sx, top, w] of [[cx, 0, 1.6], [cx - 5, 2, 1.2], [cx + 5, 2, 1.2]]) {
    p.fill((x, y) => y >= top && y < 5 && Math.abs(x - sx) < w * ((y - top) / (5 - top)) + 0.5, '#2a1240');
  }
  p.fill((x, y) => y >= 0 && y < 1 && Math.abs(x - cx) < 0.6, '#c45cff');
  // moldura redonda + olho
  p.fill((x, y) => Math.hypot(x - cx, (y - cy) * 1.25) < 6.2, '#140a20');
  pixelEye(p, cx, cy, 5.2, 'open');
  p.outline('#05030a');
  return p.t;
}

const HUD_STATES = ['sealed', 'closed', 'half', 'open', 'open2', 'supreme', 'supreme2'];

/** Olho do HUD: moldura 22x22 + 7 estados do olho, lado a lado. */
function hudEyeSheet() {
  const sheet = new Tex(22 * HUD_STATES.length, 22);
  HUD_STATES.forEach((state, i) => {
    const p = new Pix(22, 22);
    const supreme = state.startsWith('supreme');
    // moldura octogonal
    const oct = (x, y, r) => Math.abs(x - 11) + Math.abs(y - 11) < r * 1.4 && Math.abs(x - 11) < r && Math.abs(y - 11) < r;
    p.fill((x, y) => oct(x, y, 10.5), '#05030a');
    p.fill((x, y) => oct(x, y, 9.6), supreme ? '#e28bff' : '#6a2aa8');
    p.fill((x, y) => oct(x, y, 8.6), '#0d0a14');
    if (state === 'sealed') {
      handEye(p, 4, 7, 'sealed');
      // corrente atravessando
      p.fill((x, y) => Math.abs(x - y) < 1.3 && Math.round(x) % 3 !== 0 && oct(x, y, 8.4), '#6c637a');
      p.fill((x, y) => Math.abs(x - y) < 0.5 && Math.round(x) % 3 === 0 && oct(x, y, 8.4), '#91879f');
    } else {
      handEye(p, 4, 7, state);
    }
    if (supreme) {
      for (const [x, y] of [[11, 1], [11, 20], [1, 11], [20, 11]]) p.t.set(x, y, [255, 230, 255, 255]);
    }
    for (let y = 0; y < 22; y++) for (let x = 0; x < 22; x++) sheet.set(i * 22 + x, y, p.t.get(x, y));
  });
  return sheet;
}

/** Barra de boss do mod: moldura 222x13 (0,0) + preenchimentos 182x5 roxo (0,16), vermelho (0,22) e vazio (0,28). */
function bossBar() {
  const t = new Tex(256, 32);
  const p = new Pix(222, 13);
  // filete em volta da barra (a barra fica em x 20..201, y 4..8)
  p.fill((x, y) => x >= 17 && x < 205 && y >= 1 && y < 12, '#05030a');
  p.fill((x, y) => x >= 18 && x < 204 && y >= 2 && y < 11, '#6a2aa8');
  p.fill((x, y) => x >= 19 && x < 203 && y >= 3 && y < 10, '#0b0812');
  // olhos nas pontas
  for (const cx of [6.5, 214.5]) {
    p.fill((x, y) => Math.hypot(x - cx, y - 6.5) < 6.4, '#05030a');
    p.fill((x, y) => Math.hypot(x - cx, y - 6.5) < 5.6, '#8a3be0');
    p.fill((x, y) => Math.hypot(x - cx, y - 6.5) < 4.6, '#0d0a14');
    handEye(p, Math.round(cx - 4.5), 4, 'open', 'small');
  }
  for (let y = 0; y < 13; y++) for (let x = 0; x < 222; x++) t.set(x, y, p.t.get(x, y));
  const fill = (y0, rows) => {
    for (let x = 0; x < 182; x++) {
      rows.forEach((c, j) => {
        const glint = x % 14 === 7 && j < 2;
        t.set(x, y0 + j, glint ? mix(c, '#ffffff', 0.45) : c);
      });
    }
  };
  fill(16, ['#e8a0ff', '#c45cff', '#9b30ff', '#6a1fb0', '#3a1060']);
  fill(22, ['#ff9a9a', '#ff4040', '#d01818', '#900c0c', '#500606']);
  fill(28, ['#1a1026', '#120c1c', '#0d0915', '#0a0710', '#07050b']);
  return t;
}

// ================================================================ 4. prévias

function compose(base, parts) {
  for (const [img, dx, dy] of parts) {
    for (let y = 0; y < img.h; y++) for (let x = 0; x < img.w; x++) {
      const [r, g, b, a] = img.get(x, y);
      if (!a) continue;
      const [br, bg, bb, ba] = base.get(dx + x, dy + y);
      const t = a / 255;
      base.set(dx + x, dy + y, [Math.round(br * (1 - t) + r * t), Math.round(bg * (1 - t) + g * t), Math.round(bb * (1 - t) + b * t), Math.max(ba, a)]);
    }
  }
  return base;
}

const crop = (img, x0, y0, w, h) => {
  const t = new Tex(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) t.set(x, y, img.get(x0 + x, y0 + y));
  return t;
};

const asTex = (img) => {
  const t = new Tex(img.w, img.h);
  img.data.copy(t.d);
  return t;
};

const fromPack = (rel) => asTex(readPNG(join(PACK_GUI, ...rel.split('/'))));

function nineSlice(src, w, h, border) {
  const t = new Tex(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const sx = x < border ? x : x >= w - border ? src.w - (w - x) : border + ((x - border) % (src.w - 2 * border));
    const sy = y < border ? y : y >= h - border ? src.h - (h - y) : border + ((y - border) % (src.h - 2 * border));
    t.set(x, y, src.get(sx, sy));
  }
  return t;
}

function previews(art) {
  mkdirSync(PREVIEWS, { recursive: true });
  const BG = ['#2a2a30', '#31313a'];
  const k = 3;
  const panelWithEye = (rel, w, h) => {
    const panel = crop(fromPack(rel), 0, 0, w, h);
    const t = new Tex(w, h + 9);
    compose(t, [[panel, 0, 9], [art.eye, Math.round(w / 2 - 20), 0]]);
    return upscale(t, k, BG);
  };
  board('containers', [
    { label: 'inventario', img: panelWithEye('container/inventory.png', 176, 166) },
    { label: 'bau', img: panelWithEye('container/generic_54.png', 176, 222) },
    { label: 'fornalha', img: panelWithEye('container/furnace.png', 176, 166) },
    { label: 'bancada', img: panelWithEye('container/crafting_table.png', 176, 166) },
    { label: 'receitas', img: upscale(crop(fromPack('recipe_book.png'), 0, 0, 147, 166), k, BG) },
  ]).save(join(PREVIEWS, '1_containers.png'));

  const btn = (rel) => upscale(nineSlice(fromPack(rel), 150, 20, 3), 4, BG);
  board('botoes', [
    { label: 'normal', img: btn('sprites/widget/button.png') },
    { label: 'destacado', img: btn('sprites/widget/button_highlighted.png') },
    { label: 'desativado', img: btn('sprites/widget/button_disabled.png') },
    { label: 'campo de texto', img: btn('sprites/widget/text_field_highlighted.png') },
  ]).save(join(PREVIEWS, '2_botoes.png'));

  // HUD simulado
  const hud = new Tex(300, 70);
  for (let y = 0; y < 70; y++) for (let x = 0; x < 300; x++) hud.set(x, y, mix('#3b5a2a', '#2b4220', y / 70));
  const heart = fromPack('sprites/hud/heart/full.png');
  const hearts = [];
  for (let i = 0; i < 10; i++) hearts.push([heart, 20 + i * 8, 8]);
  compose(hud, [
    [fromPack('sprites/hud/experience_bar_background.png'), 20, 22],
    [crop(fromPack('sprites/hud/experience_bar_progress.png'), 0, 0, 120, 5), 20, 22],
    [fromPack('sprites/hud/hotbar.png'), 20, 30],
    [fromPack('sprites/hud/hotbar_selection.png'), 39, 29],
    [crop(art.hudEye, 22 * 3, 0, 22, 22), 208, 30],
    ...hearts,
  ]);
  const boss = new Tex(260, 30);
  for (let y = 0; y < 30; y++) for (let x = 0; x < 260; x++) boss.set(x, y, '#2a3a50');
  compose(boss, [[crop(art.boss, 0, 28, 182, 5), 39, 12], [crop(art.boss, 0, 16, 120, 5), 39, 12], [crop(art.boss, 0, 0, 222, 13), 19, 8]]);
  board('hud', [
    { label: 'hud', img: upscale(hud, 3, BG) },
    { label: 'olhos do hud', img: upscale(art.hudEye, 4, BG) },
    { label: 'barra de boss', img: upscale(boss, 3, BG) },
  ]).save(join(PREVIEWS, '3_hud.png'));

  board('avisos e conquistas', [
    { label: 'conquista', img: upscale(fromPack('sprites/toast/advancement.png'), 3, BG) },
    { label: 'receita', img: upscale(fromPack('sprites/toast/recipe.png'), 3, BG) },
    { label: 'janela', img: upscale(crop(fromPack('advancements/window.png'), 0, 0, 252, 140), 2, BG) },
    { label: 'molduras', img: upscale(compose(new Tex(60, 26), [[fromPack('sprites/advancements/task_frame_obtained.png'), 0, 0], [fromPack('sprites/advancements/task_frame_unobtained.png'), 30, 0]]), 4, BG) },
  ]).save(join(PREVIEWS, '4_avisos.png'));

  // panorama em cruz
  const s = art.faces[0].w / 4;
  const cross = new Tex(s * 4, s * 3);
  const small = art.faces.map((f) => {
    const t = new Tex(s, s);
    for (let y = 0; y < s; y++) for (let x = 0; x < s; x++) t.set(x, y, f.get(x * 4, y * 4));
    return t;
  });
  // ordem na cruz: cima(4), esquerda(3), frente(0), direita(1), trás(2), baixo(5)
  compose(cross, [[small[4], s, 0], [small[3], 0, s], [small[0], s, s], [small[1], s * 2, s], [small[2], s * 3, s], [small[5], s, s * 2]]);
  board('panorama e carregamento', [
    { label: 'panorama', img: upscale(cross, 1, BG) },
    { label: 'carregamento', img: crop(art.loading, 0, 0, art.loading.w, art.loading.h) },
  ]).save(join(PREVIEWS, '5_panorama.png'));
  board('logo e pacote', [
    { label: 'logo', img: upscale(art.logo, 1, ['#0b0812', '#0d0a14']) },
    { label: 'icone do pacote', img: upscale(art.packIcon, 2, BG) },
    { label: 'olho dos containers', img: upscale(art.eye, 5, BG) },
  ]).save(join(PREVIEWS, '6_logo.png'));
}

// ================================================================ main

const gui = extractVanilla();
const recolored = recolorAll(gui);

// fundos de menu, listas e separadores (gerados do zero)
save(menuTile('#0d0716', 150, 'menu'), PACK_GUI, 'menu_background.png');
save(menuTile('#140a22', 150, 'inworld'), PACK_GUI, 'inworld_menu_background.png');
save(menuTile('#08040e', 175, 'lista'), PACK_GUI, 'menu_list_background.png');
save(menuTile('#0a0612', 140, 'lista-mundo'), PACK_GUI, 'inworld_menu_list_background.png');
for (const n of ['header_separator', 'inworld_header_separator']) save(separator(true), PACK_GUI, n + '.png');
for (const n of ['footer_separator', 'inworld_footer_separator']) save(separator(false), PACK_GUI, n + '.png');

const faces = [0, 1, 2, 3, 4, 5].map((k) => save(panoramaFace(k, 512), join(PACK_GUI, 'title', 'background'), `panorama_${k}.png`));
const packIcon = save(makePackIcon(128), PACK, 'pack.png');
writeFileSync(join(PACK, 'pack.mcmeta'), JSON.stringify({
  pack: { pack_format: 34, description: { text: 'O Olho te observa...', color: 'light_purple' } },
}, null, 2) + '\n');

const art = {
  faces,
  packIcon,
  logo: save(makeLogo(), MOD_GUI, 'logo.png'),
  loading: save(loadingArt(768, 432), MOD_GUI, 'loading.png'),
  eye: save(containerEye(), MOD_GUI, 'container_eye.png'),
  hudEye: save(hudEyeSheet(), MOD_GUI, 'eye_hud.png'),
  boss: save(bossBar(), MOD_GUI, 'boss_bar.png'),
};
previews(art);
console.log(`Interface: ${recolored} texturas recoloridas + peças próprias. Prévias em ${PREVIEWS}`);
