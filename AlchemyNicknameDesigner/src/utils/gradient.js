/**
 * Utility for Minecraft RGB Gradients
 */

export function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
}

export function rgbToHex(r, g, b) {
  return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}

export function interpolateColor(color1, color2, factor) {
  const c1 = hexToRgb(color1);
  const c2 = hexToRgb(color2);
  
  const r = Math.round(c1.r + (c2.r - c1.r) * factor);
  const g = Math.round(c1.g + (c2.g - c1.g) * factor);
  const b = Math.round(c1.b + (c2.b - c1.b) * factor);
  
  return rgbToHex(r, g, b);
}

export function getGradientColors(colors, steps) {
  if (steps <= 0) return [];
  if (steps === 1) return [colors[0]];
  if (colors.length === 1) return Array(steps).fill(colors[0]);

  const result = [];
  const segmentSize = (steps - 1) / (colors.length - 1);

  for (let i = 0; i < steps; i++) {
    const segment = Math.min(Math.floor(i / segmentSize), colors.length - 2);
    const factor = (i - segment * segmentSize) / segmentSize;
    result.push(interpolateColor(colors[segment], colors[segment + 1], factor));
  }

  return result;
}

export function formatString(text, colors, formats) {
  if (!text) return "";
  const chars = text.split("");
  const gradient = getGradientColors(colors, chars.length);
  
  let result = "";
  const formatPrefix = (formats.bold ? "&l" : "") + 
                       (formats.italic ? "&o" : "") + 
                       (formats.underline ? "&n" : "") + 
                       (formats.strikethrough ? "&m" : "");

  for (let i = 0; i < chars.length; i++) {
    const hex = gradient[i].replace("#", "");
    // Use Legacy Hex format &#RRGGBB which AlchemyNicknames supports
    result += `&#${hex}${formatPrefix}${chars[i]}`;
  }
  
  return result;
}

export function formatMiniMessage(text, colors, formats) {
  if (!text) return "";
  
  let prefix = "";
  if (formats.bold) prefix += "<bold>";
  if (formats.italic) prefix += "<italic>";
  if (formats.underline) prefix += "<underlined>";
  if (formats.strikethrough) prefix += "<strikethrough>";

  if (colors.length === 1) {
    return `${prefix}<${colors[0]}>${text}`;
  }

  // MiniMessage gradient: <gradient:#color1:#color2>text</gradient>
  const colorString = colors.map(c => c).join(":");
  return `${prefix}<gradient:${colorString}>${text}</gradient>`;
}
