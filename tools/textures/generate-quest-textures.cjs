// Gera os sprites das telas do mural de quests.
// A textura do bloco nao sai daqui: e a do modelo do Blockbench, em block/quest_board/mission_board.png.
// Uso: node tools/textures/generate-quest-textures.cjs
//
// O painel e o "afundado" das telas seguem a mesma anatomia do inventario do jogo (contorno preto,
// luz em cima/esquerda, sombra embaixo/direita), a mesma usada na interface do WaterFrames.
const fs = require('fs');
const path = require('path');
const { png, hex } = require('../lib/png.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'nexuscore', 'textures');
const BLOCK_OUT = path.join(ASSETS, 'block', 'quest_board');
const GUI_OUT = path.join(ASSETS, 'gui', 'sprites', 'quest');

const C = {
  '.': [0, 0, 0, 0],
  // Painel das telas: a anatomia e a do inventario (contorno, luz 2px, sombra 2px), mas em couro e
  // papel em vez do cinza -- e um mural de recados, nao a tela de opcoes.
  K: hex(0x1B1410), // contorno
  W: hex(0xF0E4C8), // luz
  S: hex(0x8A6F49), // sombra do painel
  P: hex(0xC3A87C), // miolo do painel (couro)
  D: hex(0x7E6A47), // sombra de area afundada
  M: hex(0xD9CBA6), // miolo afundado (papel)
  // madeira e cortica do mural
  f: hex(0x6B4E2E), // moldura de madeira
  g: hex(0x82603A), // madeira clara
  h: hex(0x4E3821), // madeira escura
  c: hex(0x9C7B52), // cortica
  d: hex(0x8A6A45), // cortica escura
  p: hex(0xE8E0C8), // papel
  q: hex(0xCFC5A8), // papel na sombra
  t: hex(0x8A8172), // linha escrita no papel
  r: hex(0xB3402F), // alfinete
  // botao no estilo do botao do livro de receitas do jogo
  o: hex(0x000000), // contorno
  l: hex(0xFFFFFF), // luz
  a: hex(0xC6C6C6), // face
  s: hex(0x555555), // sombra
  O: hex(0x00073E), // contorno (com o mouse em cima)
  A: hex(0x8892C9), // face (com o mouse em cima)
  N: hex(0x343E75), // sombra (com o mouse em cima)
};

function grid(width, height, fill) {
  return [...Array(height)].map(() => fill.repeat(width).split(''));
}

function rows(cells) {
  return cells.map(row => row.join(''));
}

/** Painel do inventario: contorno preto arredondado, luz 2px, sombra 2px, miolo cinza. */
function panel() {
  const cells = grid(16, 16, 'P');
  for (let x = 0; x < 16; x++) {
    cells[0][x] = 'K';
    cells[15][x] = 'K';
  }
  for (let y = 0; y < 16; y++) {
    cells[y][0] = 'K';
    cells[y][15] = 'K';
  }
  cells[0][0] = '.';
  cells[0][15] = '.';
  cells[15][0] = '.';
  cells[15][15] = '.';
  for (let i = 1; i < 15; i++) {
    cells[1][i] = 'W';
    cells[2][i] = i >= 13 ? 'S' : 'W';
    cells[i][1] = 'W';
    cells[i][2] = i >= 13 ? 'S' : 'W';
    cells[14][i] = 'S';
    cells[13][i] = i <= 2 ? 'W' : 'S';
    cells[i][14] = 'S';
    cells[i][13] = i <= 2 ? 'W' : 'S';
  }
  cells[1][14] = 'P';
  cells[14][1] = 'P';
  return rows(cells);
}

/** Area afundada (tipo slot): sombra em cima/esquerda, luz embaixo/direita. */
function inset(fill) {
  const cells = grid(16, 16, fill);
  for (let i = 0; i < 16; i++) {
    cells[0][i] = 'D';
    cells[i][0] = 'D';
    cells[15][i] = 'W';
    cells[i][15] = 'W';
  }
  cells[0][15] = 'M';
  cells[15][0] = 'M';
  return rows(cells);
}

/** Frente do mural: moldura de madeira, cortica e tres papeis pregados. */
function boardFront() {
  const cells = grid(16, 16, 'c');
  for (let i = 0; i < 16; i++) {
    cells[0][i] = 'g';
    cells[15][i] = 'h';
    cells[i][0] = 'g';
    cells[i][15] = 'h';
  }
  cells[0][15] = 'f';
  cells[15][0] = 'f';
  for (let i = 1; i < 15; i++) {
    cells[1][i] = 'f';
    cells[14][i] = 'f';
    cells[i][1] = 'f';
    cells[i][14] = 'f';
  }
  // manchas da cortica
  for (const [x, y] of [[4, 5], [9, 4], [6, 10], [11, 9], [5, 12], [10, 12]]) {
    cells[y][x] = 'd';
  }
  // papeis: (x, y, largura, altura)
  for (const [px, py, pw, ph] of [[3, 3, 4, 5], [8, 3, 5, 4], [4, 9, 6, 4]]) {
    for (let y = py; y < py + ph; y++) {
      for (let x = px; x < px + pw; x++) {
        cells[y][x] = (x === px + pw - 1 || y === py + ph - 1) ? 'q' : 'p';
      }
    }
    for (let y = py + 1; y < py + ph - 1; y += 2) {
      for (let x = px + 1; x < px + pw - 1; x++) {
        cells[y][x] = 't';
      }
    }
    cells[py][px + Math.floor(pw / 2)] = 'r';
  }
  return rows(cells);
}

/** Lateral e verso: tabua de madeira. */
function boardSide() {
  const cells = grid(16, 16, 'g');
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      if (y % 8 === 7) {
        cells[y][x] = 'h';
      } else if ((x * 7 + y * 3) % 11 === 0) {
        cells[y][x] = 'f';
      }
    }
  }
  return rows(cells);
}


/**
 * Botao 20x18 com a mesma anatomia do botao do livro de receitas: contorno de 1 px com os cantos
 * cortados, luz em cima e a esquerda, sombra embaixo e a direita. O icone (o item livro) e desenhado
 * por cima em tempo de execucao, entao aqui sai so o botao.
 */
function bookButton(hovered) {
  const outline = hovered ? 'O' : 'o';
  const face = hovered ? 'A' : 'a';
  const shadow = hovered ? 'N' : 's';
  const cells = grid(20, 18, face);
  for (let y = 0; y < 18; y++) {
    for (let x = 0; x < 20; x++) {
      const topRow = y === 0, bottomRow = y === 17;
      const innerTop = y === 1, innerBottom = y === 16;
      if (topRow || bottomRow) {
        cells[y][x] = (x <= 1 || x >= 18) ? '.' : outline;
      } else if (innerTop || innerBottom) {
        if (x === 0 || x === 19) cells[y][x] = '.';
        else if (x === 1 || x === 18) cells[y][x] = outline;
        else cells[y][x] = innerTop ? 'l' : shadow;
      } else {
        if (x === 0 || x === 19) cells[y][x] = outline;
        else if (x === 1) cells[y][x] = 'l';
        else if (x === 18) cells[y][x] = shadow;
      }
    }
  }
  return rows(cells);
}

const OUTPUTS = [
  { dir: GUI_OUT, name: 'panel', rows: panel(), nineSlice: 4 },
  { dir: GUI_OUT, name: 'inset', rows: inset('M'), nineSlice: 1 },
  { dir: GUI_OUT, name: 'book_button', rows: bookButton(false) },
  { dir: GUI_OUT, name: 'book_button_highlighted', rows: bookButton(true) },
];

for (const output of OUTPUTS) {
  fs.mkdirSync(output.dir, { recursive: true });
  const file = path.join(output.dir, output.name + '.png');
  fs.writeFileSync(file, png(output.rows, C));
  if (output.nineSlice) {
    const meta = {
      gui: { scaling: { type: 'nine_slice', width: output.rows[0].length, height: output.rows.length, border: output.nineSlice } },
    };
    fs.writeFileSync(file + '.mcmeta', JSON.stringify(meta, null, 2) + '\n');
  }
  console.log('gerado: ' + path.relative(ASSETS, file).replace(/\\/g, '/'));
}
