// Gera as texturas das backrooms.
// Uso: node tools/textures/generate-backrooms-textures.cjs
//
// Sao provisorias, para o andar ficar jogavel antes das texturas definitivas: e so trocar os PNG em
// src/main/resources/assets/nexuscore/textures/block/backrooms/ pelos de verdade, com os mesmos
// nomes. O tamanho tambem pode mudar (32x32, 64x64): o jogo nao se importa.
const fs = require('fs');
const path = require('path');
const { png, hex } = require('../lib/png.cjs');

const OUT = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'nexuscore',
    'textures', 'block', 'backrooms');

const C = {
  // parede: o amarelo mono das backrooms
  y: hex(0xC7B35E), // papel de parede
  Y: hex(0xD3C06C), // papel de parede claro
  z: hex(0xB9A452), // papel de parede escuro
  // rodape
  r: hex(0x8A7038),
  R: hex(0x9C8044),
  t: hex(0x6E5829),
  // carpete
  c: hex(0x7A6A34),
  C: hex(0x6B5C2C),
  d: hex(0x89783C),
  // teto
  w: hex(0xCFC9B4),
  W: hex(0xDBD5C0),
  v: hex(0xB6B09C),
  // lampada
  l: hex(0xFFFDF0),
  L: hex(0xF5F0D8),
  m: hex(0xD8D2B8),
};

/** Ruido repetivel: a mesma textura sai igual em qualquer maquina. */
function noise(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

function grid(fill) {
  return [...Array(16)].map(() => fill.repeat(16).split(''));
}

function rows(cells) {
  return cells.map(row => row.join(''));
}

/** Papel de parede: listras verticais de tom em tom, como o padrao do Nivel 0. */
function wallpaper() {
  const cells = grid('y');
  const random = noise(11);
  for (let x = 0; x < 16; x++) {
    const stripe = x % 4 === 0 ? 'Y' : x % 4 === 2 ? 'z' : 'y';
    for (let y = 0; y < 16; y++) {
      cells[y][x] = random() < 0.12 ? (stripe === 'Y' ? 'y' : 'Y') : stripe;
    }
  }
  return rows(cells);
}

/** Rodape: o mesmo papel em cima, madeira escura embaixo. */
function baseboard() {
  const cells = wallpaper().map(row => row.split(''));
  for (let y = 11; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      cells[y][x] = y === 11 ? 't' : y === 15 ? 't' : (x % 5 === 0 ? 'R' : 'r');
    }
  }
  return rows(cells);
}

/** Carpete: manchado e sem direcao, como o carpete velho e umido do lugar. */
function carpet() {
  const cells = grid('c');
  const random = noise(29);
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const value = random();
      cells[y][x] = value < 0.3 ? 'C' : value < 0.45 ? 'd' : 'c';
    }
  }
  return rows(cells);
}

/** Placa de forro: moldura em volta e miolo salpicado. */
function ceiling() {
  const cells = grid('w');
  const random = noise(53);
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const border = x === 0 || y === 0 || x === 15 || y === 15;
      cells[y][x] = border ? 'v' : random() < 0.18 ? 'W' : 'w';
    }
  }
  return rows(cells);
}

/** Painel de luz: branco quase liso, com a moldura de sempre. */
function lamp() {
  const cells = grid('l');
  const random = noise(71);
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const border = x === 0 || y === 0 || x === 15 || y === 15;
      cells[y][x] = border ? 'm' : random() < 0.15 ? 'L' : 'l';
    }
  }
  return rows(cells);
}

const OUTPUTS = [
  { name: 'wallpaper', rows: wallpaper() },
  { name: 'baseboard', rows: baseboard() },
  { name: 'carpet', rows: carpet() },
  { name: 'ceiling', rows: ceiling() },
  { name: 'lamp', rows: lamp() },
];

fs.mkdirSync(OUT, { recursive: true });
for (const output of OUTPUTS) {
  const file = path.join(OUT, output.name + '.png');
  // Nao passa por cima de textura de verdade: este script e so o plano B.
  if (fs.existsSync(file)) {
    console.log('mantido: ' + output.name + '.png (ja existe)');
    continue;
  }
  fs.writeFileSync(file, png(output.rows, C));
  console.log('gerado: ' + path.relative(path.join(__dirname, '..', '..'), file).replace(/\\/g, '/'));
}
