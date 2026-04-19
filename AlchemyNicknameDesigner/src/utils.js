
/**
 * Converts HSL to Hex.
 */
export function hslToHex(h, s, l) {
    l /= 100;
    const a = s * Math.min(l, 1 - l) / 100;
    const f = n => {
        const k = (n + h / 30) % 12;
        const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
        return Math.round(255 * color).toString(16).padStart(2, '0');
    };
    return `#${f(0)}${f(8)}${f(4)}`;
}

/**
 * Generates a vibrant random color.
 */
export function generateRandomColor(baseHue = null) {
    const h = baseHue !== null ? baseHue : Math.floor(Math.random() * 361);
    const s = Math.floor(Math.random() * 21) + 80; // 80-100%
    const l = Math.floor(Math.random() * 21) + 40; // 40-60%
    return hslToHex(h, s, l);
}

/**
 * Generates an array of harmonic colors based on a scheme.
 */
export function generateHarmonicColors(count) {
    const baseHue = Math.floor(Math.random() * 361);
    const schemes = ['analogous', 'triadic', 'monochromatic', 'complementary'];
    const scheme = schemes[Math.floor(Math.random() * schemes.length)];
    
    return Array.from({ length: count }, (_, i) => {
        let h = baseHue;
        if (scheme === 'analogous') h = (baseHue + (i * 30)) % 360;
        else if (scheme === 'triadic') h = (baseHue + (i * 120)) % 360;
        else if (scheme === 'complementary') h = (baseHue + (i % 2 === 0 ? 0 : 180)) % 360;
        // monochromatic uses same hue but varying lightness/saturation handled by generateRandomColor internal randomization
        
        return generateRandomColor(h);
    });
}

/**
 * Converts a hex string to an RGB array.
 * Logic ported from Birdflop's `x` function.
 * @param {string} hex 
 * @returns {number[]} [r, g, b]
 */
export function hexToRgbArray(hex) {
    hex = hex.replace('#', '');
    return [
        parseInt(hex.substring(0, 2), 16),
        parseInt(hex.substring(2, 4), 16),
        parseInt(hex.substring(4, 6), 16)
    ];
}

/**
 * Converts RGB array to hex string.
 * Logic ported from Birdflop's `P` and `E` functions.
 */
function toHexPiece(e) {
    const t = "0123456789ABCDEF";
    let o = e;
    return o == 0 || isNaN(e) ? "00" : (o = Math.round(Math.min(Math.max(0, o), 255)), t.charAt((o - o % 16) / 16) + t.charAt(o % 16));
}

export function rgbToHex(r, g, b) {
    if (Array.isArray(r)) [r, g, b] = r;
    return '#' + toHexPiece(r) + toHexPiece(g) + toHexPiece(b);
}

/**
 * Darkens a hex color.
 * Ported from Birdflop logic where factor is applied to array.
 */
export function darkenHex(hex, factor = 0.25) {
    const rgb = hexToRgbArray(hex);
    const darkened = rgb.map(x => x * factor);
    return rgbToHex(darkened);
}

// Ported 'st' class
class GradientSegment {
    constructor(startColor, endColor, lowerRange, upperRange) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
    }
    colorAt(t) {
        if (this.startColor === this.endColor) return this.startColor;
        return [
            this.calculateHexPiece(t, this.startColor[0], this.endColor[0]),
            this.calculateHexPiece(t, this.startColor[1], this.endColor[1]),
            this.calculateHexPiece(t, this.startColor[2], this.endColor[2])
        ];
    }
    calculateHexPiece(t, start, end) {
        const range = this.upperRange - this.lowerRange;
        const ratio = (end - start) / range;
        return Math.round(ratio * (t - this.lowerRange) + start);
    }
}

// Ported 'w' class
class GradientWalker {
    constructor(colors, steps) {
        this.colors = colors;
        this.gradients = [];
        this.steps = steps - 1; // Birdflop subtracts 1 here
        this.step = 0;

        // Ensure first and last positions are 0 and 100
        if (this.colors[0].pos !== 0) {
            this.colors.unshift({ rgb: this.colors[0].rgb, pos: 0 });
        }
        if (this.colors[this.colors.length - 1].pos !== 100) {
            this.colors.push({ rgb: this.colors[this.colors.length - 1].rgb, pos: 100 });
        }

        for (let n = 0; n < this.colors.length - 1; n++) {
            let start = this.colors[n];
            let end = this.colors[n + 1];
            if (start.pos > end.pos) {
                const temp = start; start = end; end = temp;
            }
            const lower = Math.round(start.pos / 100 * this.steps);
            const upper = Math.round(end.pos / 100 * this.steps);

            if (upper >= 1 && (lower !== upper)) {
                this.gradients.push(new GradientSegment(start.rgb, end.rgb, lower, upper));
            }
        }
    }

    next() {
        if (this.steps < 1) return this.colors[0].rgb;
        // The triangular wave function from Birdflop
        const t = Math.round(Math.abs(2 * Math.asin(Math.sin(this.step * (Math.PI / (2 * this.steps)))) / Math.PI * this.steps));

        let color;
        if (this.gradients.length < 2) {
            color = this.gradients[0].colorAt(t);
        } else {
            const segment = this.gradients.find(s => s.lowerRange <= t && s.upperRange >= t);
            if (!segment) return this.colors[0].rgb;
            color = segment.colorAt(t);
        }
        this.step++;
        return color;
    }
}

/**
 * Generates gradient using the ported GradientWalker.
 * @param {string} text 
 * @param {object[]|string[]} colors 
 * @param {number} charsPerColor 
 */
export function generateGradient(text, colors, charsPerColor = 1) {
    if (!text) return [];
    if (colors.length < 1) return text.split('').map(char => ({ char, color: '#ffffff' }));
    if (colors.length === 1) {
        const c = typeof colors[0] === 'string' ? colors[0] : colors[0].hex;
        return text.split('').map(char => ({ char, color: c }));
    }

    // Normalize colors
    const normalizedColors = colors.map((c, i) => {
        if (typeof c === 'string') {
            return { rgb: hexToRgbArray(c), pos: (i / (colors.length - 1)) * 100 };
        }
        return { rgb: hexToRgbArray(c.hex), pos: c.pos };
    }).sort((a, b) => a.pos - b.pos);

    // Setup walker
    // Birdflop logic: l = new w(n, Math.ceil(e.text.length/e.colorlength))
    const totalSteps = Math.ceil(text.length / charsPerColor);
    const walker = new GradientWalker(normalizedColors, totalSteps);

    const gradient = [];
    const chunks = [];

    // Chunk text
    for (let i = 0; i < text.length; i += charsPerColor) {
        chunks.push(text.slice(i, i + charsPerColor));
    }

    // Generate colors for chunks
    chunks.forEach(chunk => {
        const rgb = walker.next();
        const colorHex = rgbToHex(rgb);
        // Distribute color to all chars in chunk
        for (const char of chunk) {
            gradient.push({ char, color: colorHex });
        }
    });

    return gradient;
}

// Small Caps Mapping
export const SMALL_CAPS_MAP = {
    'A': 'ᴀ', 'B': 'ʙ', 'C': 'ᴄ', 'D': 'ᴅ', 'E': 'ᴇ',
    'F': 'ꜰ', 'G': 'ɢ', 'H': 'ʜ', 'I': 'ɪ', 'J': 'ᴊ',
    'K': 'ᴋ', 'L': 'ʟ', 'M': 'ᴍ', 'N': 'ɴ', 'O': 'ᴏ',
    'P': 'ᴘ', 'Q': 'ꞯ', 'R': 'ʀ', 'S': 'ꜱ', 'T': 'ᴛ',
    'U': 'ᴜ', 'V': 'ᴠ', 'W': 'ᴡ', 'X': 'x', 'Y': 'ʏ',
    'Z': 'ᴢ', '0': '0', '1': '1', '2': '2', '3': '3',
    '4': '4', '5': '5', '6': '6', '7': '7', '8': '8', '9': '9'
};

/**
 * Converts a string to its custom Unicode Small Caps representation.
 * @param {string} input 
 * @returns {string}
 */
export function toSmallCaps(input) {
    if (typeof input !== 'string') return String(input);

    let result = '';
    let i = 0;

    while (i < input.length) {
        const char = input[i];

        // Skip color codes starting with & or §
        if (char === '&' || char === '§') {
            // Check for hex (&#123456)
            if (input.length >= i + 8 && input[i + 1] === '#') {
                const hexBody = input.substring(i + 2, i + 8);
                if (/^[0-9a-fA-F]{6}$/i.test(hexBody)) {
                    result += input.substring(i, i + 8);
                    i += 8;
                    continue;
                }
            }
            // Check for basic format (&l)
            if (input.length >= i + 2) {
                result += input.substring(i, i + 2);
                i += 2;
                continue;
            }
        }

        // Skip %placeholders%
        if (char === '%') {
            const nextPercent = input.indexOf('%', i + 1);
            if (nextPercent !== -1) {
                result += input.substring(i, nextPercent + 1);
                i = nextPercent + 1;
                continue;
            }
        }

        // Hex #123456
        if (char === '#' && input.length >= i + 7) {
            const hexBody = input.substring(i + 1, i + 7);
            if (/^[0-9a-fA-F]{6}$/i.test(hexBody)) {
                result += input.substring(i, i + 7);
                i += 7;
                continue;
            }
        }

        const upperChar = char.toUpperCase();
        if (SMALL_CAPS_MAP.hasOwnProperty(upperChar)) {
            result += SMALL_CAPS_MAP[upperChar];
        } else {
            result += char;
        }
        i++;
    }
    return result;
}

// Formatting codes map
const FORMAT_CODES = {
    bold: 'l',
    italic: 'o',
    underline: 'n',
    strikethrough: 'm',
    obfuscated: 'k'
};

export function formatOutput(gradientData, format, options = {}) {
    const { formats = [], trimSpaces = false, lowercaseHex = false } = options;
    const activeFormats = formats.map(f => FORMAT_CODES[f]).filter(Boolean);

    const getFormatString = (type) => {
        if (activeFormats.length === 0) return '';
        if (type === 'legacy' || type === 'mypet') {
            return activeFormats.map(c => `&${c}`).join('');
        }
        if (type === 'minimessage') {
            return formats.map(f => `<${f === 'underline' ? 'underlined' : f}>`).join('');
        }
        return '';
    };

    const processColor = (color) => {
        return lowercaseHex ? color.toLowerCase() : color;
    };

    switch (format) {
        case 'mypet':
            const formatStr = getFormatString('mypet');
            return gradientData.map(item => {
                if (trimSpaces && item.char === ' ') return ' ';
                return `&#${processColor(item.color.replace('#', ''))}${formatStr}${item.char}`;
            }).join('');

        case 'legacy':
            const legacyFormats = getFormatString('legacy');
            return gradientData.map(item => {
                if (trimSpaces && item.char === ' ') return ' ';
                const hex = processColor(item.color.replace('#', ''));
                return `&x${hex.split('').map(c => '&' + c).join('')}${legacyFormats}${item.char}`;
            }).join('');

        case 'minimessage':
            const mmFormats = getFormatString('minimessage');
            return gradientData.map(item => {
                if (trimSpaces && item.char === ' ') return ' ';
                return `<${processColor(item.color)}>${mmFormats}${item.char}`;
            }).join('');

        case 'json':
            const jsonOutput = gradientData.map(item => {
                const obj = {
                    text: item.char,
                    color: processColor(item.color)
                };
                formats.forEach(f => obj[f] = true);
                return obj;
            });
            return JSON.stringify(jsonOutput);

        case 'hex':
            return gradientData.map(item => `${processColor(item.color)}${item.char}`).join('');

        default:
            return gradientData.map(item => `${processColor(item.color)}${item.char}`).join('');
    }
}
