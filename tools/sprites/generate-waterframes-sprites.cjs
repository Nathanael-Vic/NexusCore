// Gera os sprites da interface do WaterFrames (arte do NexusCore, estilo das telas do Minecraft).
// Uso: node tools/sprites/generate-waterframes-sprites.cjs
// Saida: src/main/resources/assets/nexuscore/textures/gui/sprites/waterframes/
//
// Cada sprite e descrito como um mapa de caracteres -> cor, para a arte ficar legivel aqui no codigo.
const fs = require('fs');
const path = require('path');
const { png } = require('../lib/png.cjs');

const OUT = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'nexuscore', 'textures', 'gui', 'sprites', 'waterframes');

// Paleta (README, "Referencia visual: a tela de inventario")
const C = {
  '.': [0, 0, 0, 0],            // transparente
  K: [0x00, 0x00, 0x00, 255],   // contorno
  W: [0xFF, 0xFF, 0xFF, 255],   // luz
  S: [0x55, 0x55, 0x55, 255],   // sombra do painel
  P: [0xC6, 0xC6, 0xC6, 255],   // miolo do painel (e transicao dos cantos)
  D: [0x37, 0x37, 0x37, 255],   // sombra de area afundada
  M: [0x8B, 0x8B, 0x8B, 255],   // miolo de area afundada
  H: [0x9D, 0x9D, 0x9D, 255],   // miolo afundado realcado
  R: [0xFF, 0x55, 0x55, 255],   // borda de erro (vermelho do jogo)
  B: [0x00, 0x00, 0x00, 255],   // fundo de campo de texto
};

// Painel 16x16, nine-slice com borda 4:
// contorno 1px (cantos arredondados), luz 2px em cima/esquerda, sombra 2px embaixo/direita,
// transicao cinza onde luz e sombra se encontram.
const PANEL = [
  '.KKKKKKKKKKKKKK.',
  'KWWWWWWWWWWWWWPK',
  'KWWWWWWWWWWWWPSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWWPPPPPPPPPPSSK',
  'KWPSSSSSSSSSSSSK',
  'KPSSSSSSSSSSSSSK',
  '.KKKKKKKKKKKKKK.',
];

// Area afundada (slot): sombra em cima/esquerda, luz embaixo/direita. Nine-slice com borda 1.
function inset(fill) {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let row = '';
    for (let x = 0; x < 16; x++) {
      if ((x === 15 && y === 0) || (x === 0 && y === 15)) row += 'M';
      else if (y === 0 || x === 0) row += 'D';
      else if (y === 15 || x === 15) row += 'W';
      else row += fill;
    }
    rows.push(row);
  }
  return rows;
}

// Campo de texto com erro: igual ao widget/text_field do jogo (fundo preto, borda 1px), borda vermelha.
function fieldError() {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let row = '';
    for (let x = 0; x < 16; x++) row += (y === 0 || x === 0 || y === 15 || x === 15) ? 'R' : 'B';
    rows.push(row);
  }
  return rows;
}

// Checkbox 7x7 do CreativeCore (tamanho fixo pelo layout): caixa afundada.
function checkbox(fill) {
  const rows = [];
  for (let y = 0; y < 7; y++) {
    let row = '';
    for (let x = 0; x < 7; x++) {
      if ((x === 6 && y === 0) || (x === 0 && y === 6)) row += 'M';
      else if (y === 0 || x === 0) row += 'D';
      else if (y === 6 || x === 6) row += 'W';
      else row += fill;
    }
    rows.push(row);
  }
  return rows;
}

// Marca de selecao dentro do miolo 5x5 da caixa.
const CHECKMARK = [
  '.......',
  '.....W.',
  '....W..',
  '.W.W...',
  '..W....',
  '.......',
  '.......',
];

const SPRITES = [
  { name: 'panel', rows: PANEL, nineSlice: 4 },
  { name: 'inset', rows: inset('M'), nineSlice: 1 },
  { name: 'inset_highlighted', rows: inset('H'), nineSlice: 1 },
  { name: 'field_error', rows: fieldError(), nineSlice: 1 },
  { name: 'checkbox', rows: checkbox('M') },
  { name: 'checkbox_highlighted', rows: checkbox('H') },
  { name: 'checkmark', rows: CHECKMARK },
];

fs.mkdirSync(OUT, { recursive: true });
for (const sprite of SPRITES) {
  const file = path.join(OUT, sprite.name + '.png');
  fs.writeFileSync(file, png(sprite.rows, C));
  if (sprite.nineSlice) {
    const h = sprite.rows.length, w = sprite.rows[0].length;
    const meta = { gui: { scaling: { type: 'nine_slice', width: w, height: h, border: sprite.nineSlice } } };
    fs.writeFileSync(file + '.mcmeta', JSON.stringify(meta, null, 2) + '\n');
  }
  console.log('gerado: ' + sprite.name + '.png' + (sprite.nineSlice ? ' (nine-slice ' + sprite.nineSlice + ')' : ''));
}
