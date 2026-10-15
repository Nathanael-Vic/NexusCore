// Gera as texturas das reliquias: os itens (16x16), a pedra inabalavel e a porta do refugio.
// Uso: node tools/textures/generate-relic-textures.cjs [pasta-da-previa]
//
// Mesma lingua do anel da metamorfose: contorno escuro, um tom de base, luz em cima/esquerda e
// sombra embaixo/direita, e um brilho de "coisa magica" em cada uma (gema, brasa, olho, gota).
// Com uma pasta como argumento, salva tambem uma previa ampliada 8x de tudo, lado a lado.
const fs = require('fs');
const path = require('path');
const { png, hex } = require('../lib/png.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'nexuscore', 'textures');
const ITEM_OUT = path.join(ASSETS, 'item');
const BLOCK_OUT = path.join(ASSETS, 'block');
const T = [0, 0, 0, 0];

// ---- chave do refugio: ouro claro, cabeca em anel com uma ametista ----
const KEY = {
  rows: [
    '................',
    '.....KKKKKK.....',
    '....KLLGGGgK....',
    '...KLGKKKKGgK...',
    '...KLKPPWpKgK...',
    '...KGKPPPpKgK...',
    '...KGKppppKgK...',
    '...KgGKKKKGgK...',
    '....KggGGggK....',
    '.....KKGgKK.....',
    '......KLgK......',
    '......KGgKKK....',
    '......KLgGGgK...',
    '......KGgKKK....',
    '......KLgGgK....',
    '......KKKKK.....',
  ],
  palette: {
    '.': T, K: hex(0x3B2410), L: hex(0xFFE7A0), G: hex(0xE6B648), g: hex(0xA87424),
    P: hex(0xA55CFF), p: hex(0x5B23B0), W: hex(0xF4E8FF),
  },
};

// ---- pena dos ventos: branca, ponta azul-ceu, haste dourada ----
const FEATHER = {
  rows: [
    '............KKK.',
    '..........KKWWcK',
    '.........KWWWCcK',
    '........KWWWCCK.',
    '.......KWWWCCcK.',
    '......KWWWCCcK..',
    '.....KWWWqCcK...',
    '....KWWWqCcK....',
    '....KWWqCcK.....',
    '...KWWqCcK......',
    '...KWqCcK.......',
    '..KWqccK........',
    '..KqcKK.........',
    '.KqK............',
    'KqK.............',
    'KK..............',
  ],
  palette: {
    '.': T, K: hex(0x24364A), W: hex(0xF6FBFF), C: hex(0xBFE6FF), c: hex(0x6FB6E8), q: hex(0xE2B95A),
  },
};

// ---- coracao de brasa (aceso) e em cinzas ----
const HEART_ROWS = [
  '................',
  '....f.....f.....',
  '...KKK...KKK....',
  '..KYOOK.KRRRK...',
  '.KYYORRKRRRRdK..',
  '.KOORRRRRRRRdK..',
  '.KORRRORRRRRdK..',
  '.KRRRROORRRddK..',
  '..KRRRRORRRdK...',
  '...KRRRRRRdK....',
  '....KRRRRdK.....',
  '.....KRRdK......',
  '......KdK.......',
  '.......K........',
  '................',
  '................',
];
const HEART = {
  rows: HEART_ROWS,
  palette: {
    '.': T, K: hex(0x2A0705), R: hex(0xD8301E), d: hex(0x8A140C), O: hex(0xFF8A1E), Y: hex(0xFFE27A),
    f: hex(0xFFB23A),
  },
};
const HEART_DORMANT = {
  rows: HEART_ROWS,
  palette: {
    '.': T, K: hex(0x1E1E1E), R: hex(0x6E6A66), d: hex(0x45413E), O: hex(0x8E8A86), Y: hex(0xB4B0AA),
    f: T,
  },
};

// ---- mascara astral: porcelana, olhos vazios com estrela, lagrimas roxas ----
const MASK = {
  rows: [
    '................',
    '....KKKKKKKK....',
    '...KWWWWWWWWK...',
    '..KWWWWWWWWWWK..',
    '..KWPWWWWWWPWK..',
    '.KWWKKKWWKKKWWK.',
    '.KWKKSKWWKSKKWK.',
    '.KWWKKKWWKKKWWK.',
    '.KWWPWWWWWWPWWK.',
    '.KsWPWWWWWWPWsK.',
    '..KsWWWKKWWWsK..',
    '..KsWWWWWWWWsK..',
    '...KsWWWWWWsK...',
    '....KssWWssK....',
    '.....KKKKKK.....',
    '................',
  ],
  palette: {
    '.': T, K: hex(0x1C1430), W: hex(0xEEEAF6), s: hex(0xB4ADC8), P: hex(0x8C5CFF), S: hex(0xD9C6FF),
  },
};

// ---- bolsa sem fundo: couro, amarrada em cima, com uma estrela roxa costurada ----
const BAG = {
  rows: [
    '................',
    '......KKKK......',
    '.....KhhhhK.....',
    '......KrrK......',
    '.....KkrrkK.....',
    '....KLLLLLLK....',
    '...KLLLLLLLLK...',
    '..KLLLLPPLLLLK..',
    '..KLLLPSSPLLdK..',
    '.KLLLPSSSSPLLdK.',
    '.KLLLLPSSPLLLdK.',
    '.KLLLLLPPLLLLdK.',
    '.KLLLLLLLLLLddK.',
    '..KdLLLLLLLddK..',
    '...KKddddddKK...',
    '.....KKKKKK.....',
  ],
  palette: {
    '.': T, K: hex(0x2B1808), L: hex(0x8B5A2B), d: hex(0x5E3A1A), h: hex(0xA8743C), k: hex(0x4A2C12),
    r: hex(0xD9C38C), P: hex(0x7A3CD6), S: hex(0xD8B8FF),
  },
};

// ---- amuleto da pureza: corrente, moldura de prata e tres gotas (acesas conforme a carga) ----
function amulet(drops) {
  const rows = [
    '.c............c.',
    '..c..........c..',
    '...c........c...',
    '....c......c....',
    '.....c....c.....',
    '......KGGK......',
    '....KKSSSSKK....',
    '...KSSl11SSsK...',
    '..KSlS1111SSsK..',
    '..KSSS1111SSsK..',
    '..KS22S11S33sK..',
    '..KS22SSSS33sK..',
    '..KSS2SSSS3ssK..',
    '...KSSSSSSssK...',
    '....KKssssKK....',
    '......KKKK......',
  ];
  // Gota acesa: azul claro com brilho; gasta: vidro escuro.
  const lit = (n) => n <= drops ? hex(0x52DCFF) : hex(0x34495C);
  return {
    rows,
    palette: {
      '.': T, c: hex(0x4B5560), K: hex(0x1A2028), S: hex(0xD2DAE3), s: hex(0x8692A0), l: hex(0xFFFFFF),
      G: hex(0xE6B648), 1: lit(1), 2: lit(2), 3: lit(3),
    },
  };
}

// ---- pedra inabalavel: basalto escuro com um selo de runas douradas ----
function stone() {
  // Ruido fixo (semente) para a pedra ter textura, e um selo desenhado por cima.
  let seed = 7;
  const rand = () => { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; };
  const base = [hex(0x24222A), hex(0x2C2A33), hex(0x1C1B21), hex(0x33303B)];
  const seal = [
    '................',
    '.......GG.......',
    '.....GG..GG.....',
    '....G......G....',
    '...G...gg...G...',
    '...G..g..g..G...',
    '..G..g.GG.g..G..',
    '.G...g.GG.g...G.',
    '.G...g.GG.g...G.',
    '..G..g.GG.g..G..',
    '...G..g..g..G...',
    '...G...gg...G...',
    '....G......G....',
    '.....GG..GG.....',
    '.......GG.......',
    '................',
  ];
  const palette = { G: hex(0xE6B648), g: hex(0x9E7424) };
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let row = '';
    for (let x = 0; x < 16; x++) {
      const mark = seal[y][x];
      if (mark !== '.') {
        row += mark;
      } else {
        const edge = x === 0 || y === 0 ? 3 : (x === 15 || y === 15 ? 2 : Math.floor(rand() * 3));
        row += String(edge);
      }
    }
    rows.push(row);
  }
  base.forEach((color, i) => { palette[String(i)] = color; });
  return { rows, palette };
}

// ---- porta do refugio: branca, moldura dourada, janela de luz roxa em cima ----
const DOOR_PALETTE = {
  K: hex(0x9C93B0), W: hex(0xF4F2FA), w: hex(0xDCD8E8), G: hex(0xE6B648), g: hex(0xB08A2E),
  P: hex(0xB98CFF), p: hex(0x7A48E0), L: hex(0xF0E4FF),
};
const DOOR_TOP = {
  rows: [
    'KKKKKKKKKKKKKKKK',
    'KGGGGGGGGGGGGGGK',
    'KGwwwwwwwwwwwwgK',
    'KGwKKKKKKKKKKwgK',
    'KGwKpPPLLPPpKwgK',
    'KGwKPPLLLLPPKwgK',
    'KGwKPLLWWLLPKwgK',
    'KGwKPLLWWLLPKwgK',
    'KGwKPPLLLLPPKwgK',
    'KGwKpPPLLPPpKwgK',
    'KGwKKKKKKKKKKwgK',
    'KGwwwwwwwwwwwwgK',
    'KGWWWWWWWWWWWWgK',
    'KGWwwwwwwwwwwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwWWWWWWWWwWgK',
  ],
  palette: DOOR_PALETTE,
};
const DOOR_BOTTOM = {
  rows: [
    'KGWwWWWWWWWWwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwwwwwwwwwwWgK',
    'KGWWWWWWWWWWWWgK',
    'KGWWWWWWWWWWWWgK',
    'KGWWWWWWWWWGGWgK',
    'KGWWWWWWWWWGgWgK',
    'KGWwwwwwwwwwwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwWWWWWWWWwWgK',
    'KGWwwwwwwwwwwWgK',
    'KGWWWWWWWWWWWWgK',
    'KGGGGGGGGGGGGGgK',
    'KKKKKKKKKKKKKKKK',
  ],
  palette: DOOR_PALETTE,
};

const OUTPUTS = [
  [ITEM_OUT, 'refuge_key', KEY],
  [ITEM_OUT, 'wind_feather', FEATHER],
  [ITEM_OUT, 'ember_heart', HEART],
  [ITEM_OUT, 'ember_heart_dormant', HEART_DORMANT],
  [ITEM_OUT, 'astral_mask', MASK],
  [ITEM_OUT, 'bottomless_bag', BAG],
  [ITEM_OUT, 'purity_amulet_0', amulet(0)],
  [ITEM_OUT, 'purity_amulet_1', amulet(1)],
  [ITEM_OUT, 'purity_amulet_2', amulet(2)],
  [ITEM_OUT, 'purity_amulet_3', amulet(3)],
  [BLOCK_OUT, 'unyielding_stone', stone()],
  [BLOCK_OUT, 'refuge_door_top', DOOR_TOP],
  [BLOCK_OUT, 'refuge_door_bottom', DOOR_BOTTOM],
];

for (const [dir, name, art] of OUTPUTS) {
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name + '.png'), png(art.rows, art.palette));
  console.log('ok', name);
}

// Previa: tudo ampliado 8x numa fileira, sobre um cinza de inventario.
const previewDir = process.argv[2];
if (previewDir) {
  const scale = 8;
  const gap = 2;
  const cell = 16 + gap;
  const width = OUTPUTS.length * cell * scale;
  const height = 16 * scale;
  const rows = [];
  const palette = { '#': hex(0x8B8B8B) };
  let next = 0;
  const key = new Map();
  const charFor = (rgba) => {
    if (rgba[3] === 0) return '#';
    const id = rgba.join(',');
    if (!key.has(id)) {
      const ch = String.fromCharCode(0x100 + next++);
      key.set(id, ch);
      palette[ch] = rgba;
    }
    return key.get(id);
  };
  for (let y = 0; y < height; y++) {
    let row = '';
    for (let x = 0; x < width; x++) {
      const index = Math.floor(x / (cell * scale));
      const local = Math.floor((x % (cell * scale)) / scale);
      if (local >= 16) { row += '#'; continue; }
      const art = OUTPUTS[index][2];
      row += charFor(art.palette[art.rows[Math.floor(y / scale)][local]]);
    }
    rows.push(row);
  }
  fs.mkdirSync(previewDir, { recursive: true });
  fs.writeFileSync(path.join(previewDir, 'relics-preview.png'), png(rows, palette));
  console.log('previa', path.join(previewDir, 'relics-preview.png'));
}
