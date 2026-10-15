// Gera as texturas do Painel de Exploracao (arte do NexusCore, estilo vanilla).
// Uso: node tools/textures/generate-panel-textures.cjs
// Saida: src/main/resources/assets/nexuscore/textures/block/panel/
//
// A tela de varios blocos e um display unico: o fundo e tileavel (sem nada nas bordas, para nao
// aparecer emenda) e a moldura vem em quatro texturas separadas, aplicadas so nos lados que nao
// tem vizinho da mesma tela (blockstate multipart).
//
// As quatro bordas nao se sobrepoem: esquerda e direita ocupam a coluna inteira (com os cantos),
// topo e base so o miolo. Assim dois overlays nunca desenham no mesmo pixel.
const fs = require('fs');
const path = require('path');
const { png, hex } = require('../lib/png.cjs');

const OUT = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'nexuscore', 'textures', 'block', 'panel');

const C = {
  '.': [0, 0, 0, 0],   // transparente
  L: hex(0x6B6B70),    // moldura: luz (PanelColors.BEZEL_LIGHT)
  M: hex(0x45454A),    // moldura: transicao nos cantos (BEZEL_MID)
  D: hex(0x2A2A2E),    // moldura: sombra (BEZEL_DARK)
  S: hex(0x101317),    // tela (PanelColors.SCREEN)
  s: hex(0x0D1014),    // linha de varredura da tela
  a: hex(0x5A5A5F),    // carcaca: luz
  b: hex(0x3C3C41),    // carcaca: miolo
  c: hex(0x37373C),    // carcaca: miolo escuro
  d: hex(0x414146),    // carcaca: miolo claro
  e: hex(0x232326),    // carcaca: sombra
  f: hex(0x4B4B50),    // carcaca: transicao
  g: hex(0x6E6E73),    // rebite
  h: hex(0x2B2B2F),    // sombra do rebite
};

const SIZE = 16;

function blank(fill) {
  return [...Array(SIZE)].map(() => fill.repeat(SIZE).split(''));
}

function rows(grid) {
  return grid.map(row => row.join(''));
}

/** Fundo da tela: tileavel (o padrao se repete a cada 4 px, nada nas bordas). */
function background() {
  const grid = blank('S');
  for (let y = 3; y < SIZE; y += 4) {
    for (let x = 0; x < SIZE; x++) {
      grid[y][x] = 's';
    }
  }
  return grid;
}

function borderTop() {
  const grid = blank('.');
  for (let x = 1; x < SIZE - 1; x++) {
    grid[0][x] = 'L';
  }
  return rows(grid);
}

function borderBottom() {
  const grid = blank('.');
  for (let x = 1; x < SIZE - 1; x++) {
    grid[SIZE - 1][x] = 'D';
  }
  return rows(grid);
}

/** Coluna inteira: os cantos pertencem as bordas laterais, entao nenhum pixel e desenhado duas vezes. */
function borderLeft() {
  const grid = blank('.');
  for (let y = 0; y < SIZE; y++) {
    grid[y][0] = y === SIZE - 1 ? 'M' : 'L';
  }
  return rows(grid);
}

function borderRight() {
  const grid = blank('.');
  for (let y = 0; y < SIZE; y++) {
    grid[y][SIZE - 1] = y === 0 ? 'M' : 'D';
  }
  return rows(grid);
}

/** Painel completo (fundo + as quatro bordas): usado so no item, no inventario e na mao. */
function front() {
  const grid = background();
  for (const border of [borderTop(), borderBottom(), borderLeft(), borderRight()]) {
    border.forEach((row, y) => [...row].forEach((ch, x) => {
      if (ch !== '.') {
        grid[y][x] = ch;
      }
    }));
  }
  return rows(grid);
}

// Laterais, topo e fundo do bloco: chapa de metal escuro com rebites nos cantos.
const SIDE = [
  'aaaaaaaaaaaaaaaf',
  'abbbbbbbbbbbbbbe',
  'abghbbcbbbdbghbe',
  'abhhbbbbbbbbhhbe',
  'abbbbdbbbcbbbbbe',
  'abcbbbbbbbbbbdbe',
  'abbbbbbcbbbbbbbe',
  'abbdbbbbbbbdbbbe',
  'abbbbbbbdbbbbbbe',
  'abbbcbbbbbbbbcbe',
  'abbbbbbbbbcbbbbe',
  'abdbbbbcbbbbbbbe',
  'abbbbbbbbbbbdbbe',
  'abghbbdbbbcbghbe',
  'abhhbbbbbbbbhhbe',
  'feeeeeeeeeeeeeee',
];

const TEXTURES = [
  { name: 'background', rows: rows(background()) },
  { name: 'border_top', rows: borderTop() },
  { name: 'border_bottom', rows: borderBottom() },
  { name: 'border_left', rows: borderLeft() },
  { name: 'border_right', rows: borderRight() },
  { name: 'front', rows: front() },
  { name: 'side', rows: SIDE },
];

fs.mkdirSync(OUT, { recursive: true });
for (const texture of TEXTURES) {
  fs.writeFileSync(path.join(OUT, texture.name + '.png'), png(texture.rows, C));
  console.log('gerado: panel/' + texture.name + '.png');
}
