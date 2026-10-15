// PNG RGBA8 minimo, sem dependencias. Arte descrita como linhas de caracteres + paleta.
const zlib = require('zlib');

const CRC = (() => { const t = new Int32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; } return t; })();
function crc32(buf) { let c = 0xFFFFFFFF; for (const b of buf) c = CRC[(c ^ b) & 0xFF] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

/** rows: array de strings; palette: { char: [r, g, b, a] } */
function png(rows, palette) {
  const h = rows.length, w = rows[0].length;
  const raw = Buffer.alloc(h * (w * 4 + 1));
  rows.forEach((row, y) => {
    if (row.length !== w) throw new Error('linha com largura errada: ' + row);
    const base = y * (w * 4 + 1);
    raw[base] = 0;
    [...row].forEach((ch, x) => {
      const color = palette[ch];
      if (!color) throw new Error('cor desconhecida: ' + ch);
      color.forEach((v, i) => { raw[base + 1 + x * 4 + i] = v; });
    });
  });
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
    chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

function hex(value) {
  return [(value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, 255];
}

module.exports = { png, hex };
