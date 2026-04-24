import React from 'react';

const COLOR_MAP = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
  'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF',
};

const FORMAT_MAP = {
  'l': 'font-bold',
  'o': 'italic',
  'n': 'underline',
  'm': 'line-through',
  'k': 'obfuscated', // We can simulate with an animation or just ignore
};

export default function MinecraftText({ text, className = "" }) {
  if (!text) return null;

  const parse = (input) => {
    const parts = [];
    let currentColor = null;
    let activeFormats = new Set();
    let i = 0;

    const pushPart = (content) => {
      if (!content) return;
      parts.push({
        content,
        color: currentColor,
        formats: new Set(activeFormats),
      });
    };

    let currentBuffer = "";

    while (i < input.length) {
      // Legacy color or format: &c or §c
      if ((input[i] === '&' || input[i] === '§') && i + 1 < input.length) {
        const code = input[i + 1].toLowerCase();
        
        // Hex check: &#ABCDEF
        if (code === '#' && i + 7 < input.length) {
            const hex = input.substring(i + 2, i + 8);
            if (/^[0-9a-fA-F]{6}$/.test(hex)) {
                pushPart(currentBuffer);
                currentBuffer = "";
                currentColor = '#' + hex;
                i += 8;
                continue;
            }
        }

        if (COLOR_MAP[code] !== undefined) {
          pushPart(currentBuffer);
          currentBuffer = "";
          currentColor = COLOR_MAP[code];
          activeFormats.clear();
          i += 2;
          continue;
        } else if (FORMAT_MAP[code] !== undefined) {
          pushPart(currentBuffer);
          currentBuffer = "";
          activeFormats.add(code);
          i += 2;
          continue;
        } else if (code === 'r') {
          pushPart(currentBuffer);
          currentBuffer = "";
          currentColor = null;
          activeFormats.clear();
          i += 2;
          continue;
        }
      }

      // MiniMessage Hex: <#ABCDEF>
      if (input[i] === '<' && i + 8 < input.length && input[i+1] === '#') {
          const match = input.substring(i).match(/^<#([0-9a-fA-F]{6})>/);
          if (match) {
              pushPart(currentBuffer);
              currentBuffer = "";
              currentColor = '#' + match[1];
              i += match[0].length;
              continue;
          }
      }

      // MiniMessage Formatting: <bold>, <italic>, etc.
      if (input[i] === '<') {
          const match = input.substring(i).match(/^<(bold|italic|underlined|strikethrough|italic|white|red|gold|blue|green|yellow)>/i);
          if (match) {
              pushPart(currentBuffer);
              currentBuffer = "";
              const tag = match[1].toLowerCase();
              if (tag === 'bold') activeFormats.add('l');
              else if (tag === 'italic') activeFormats.add('o');
              else if (tag === 'underlined') activeFormats.add('n');
              else if (tag === 'strikethrough') activeFormats.add('m');
              else {
                  // Basic color tags
                  const colorTags = {
                      white: '#ffffff', red: '#ff5555', gold: '#ffaa00', 
                      blue: '#5555ff', green: '#55ff55', yellow: '#ffff55'
                  };
                  if (colorTags[tag]) {
                      currentColor = colorTags[tag];
                      activeFormats.clear();
                  }
              }
              i += match[0].length;
              continue;
          }
          // Handle closing tags
          if (input[i+1] === '/') {
              const closeMatch = input.substring(i).match(/^<\/[^>]+>/);
              if (closeMatch) {
                  pushPart(currentBuffer);
                  currentBuffer = "";
                  // For simplicity, closing any tag resets formatting in this basic parser
                  activeFormats.clear();
                  i += closeMatch[0].length;
                  continue;
              }
          }
      }

      // Raw Hex check: #ABCDEF
      if (input[i] === '#' && i + 6 < input.length) {
          const rawHex = input.substring(i, i + 7);
          if (/^#[0-9a-fA-F]{6}$/.test(rawHex)) {
              pushPart(currentBuffer);
              currentBuffer = "";
              currentColor = rawHex;
              i += 7;
              continue;
          }
      }

      currentBuffer += input[i];
      i++;
    }
    pushPart(currentBuffer);
    return parts;
  };

  const parts = parse(text);

  return (
    <span className={className}>
      {parts.map((part, index) => {
        const style = {};
        if (part.color) style.color = part.color;
        
        let classNames = [];
        part.formats.forEach(f => classNames.push(FORMAT_MAP[f]));
        
        return (
          <span key={index} style={style} className={classNames.join(' ')}>
            {part.content}
          </span>
        );
      })}
    </span>
  );
}
