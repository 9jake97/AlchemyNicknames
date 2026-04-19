import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
    Terminal, Bold, Italic, Underline, Strikethrough, Wand2,
    Plus, Trash, Settings, RefreshCw, Palette, Copy, ChevronDown
} from 'lucide-react';

// --- Core Engine: Color Math & Interpolation ---

const hexToRgb = (hex) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : { r: 255, g: 255, b: 255 };
};

const rgbToHex = (r, g, b) => {
    return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1).toUpperCase();
};

const interpolateColor = (color1, color2, factor) => {
    const rgb1 = hexToRgb(color1);
    const rgb2 = hexToRgb(color2);
    const r = Math.round(rgb1.r + factor * (rgb2.r - rgb1.r));
    const g = Math.round(rgb1.g + factor * (rgb2.g - rgb1.g));
    const b = Math.round(rgb1.b + factor * (rgb2.b - rgb1.b));
    return rgbToHex(r, g, b);
};

const getColorAtProgress = (colors, progress) => {
    if (colors.length === 0) return '#FFFFFF';
    if (colors.length === 1) return colors[0].hex;

    const sortedColors = [...colors].sort((a, b) => a.pos - b.pos);
    const pos = progress * 100;

    if (pos <= sortedColors[0].pos) return sortedColors[0].hex;
    if (pos >= sortedColors[sortedColors.length - 1].pos) return sortedColors[sortedColors.length - 1].hex;

    for (let i = 0; i < sortedColors.length - 1; i++) {
        const c1 = sortedColors[i];
        const c2 = sortedColors[i + 1];
        if (pos >= c1.pos && pos <= c2.pos) {
            const range = c2.pos - c1.pos;
            const factor = (pos - c1.pos) / range;
            return interpolateColor(c1.hex, c2.hex, factor);
        }
    }
    return '#FFFFFF';
};

// --- Reusable UI Components ---

const Toggle = ({ label, checked, onChange, description }) => (
    <div className="flex flex-col gap-1 my-2">
        <div className="flex items-center gap-3">
            <label className="relative inline-flex cursor-pointer items-center">
                <input type="checkbox" className="peer sr-only" checked={checked} onChange={(e) => onChange(e.target.checked)} />
                <div className="h-6 w-11 rounded-full bg-gray-700 peer-checked:bg-blue-500 after:absolute after:top-[2px] after:left-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all peer-checked:after:translate-x-full"></div>
            </label>
            <span className="text-gray-200 select-none font-medium">{label}</span>
        </div>
        {description && <p className="text-xs text-gray-400">{description}</p>}
    </div>
);

const FormatButton = ({ icon: Icon, active, onClick, tooltip }) => (
    <button
        onClick={onClick}
        title={tooltip}
        className={`p-2 rounded-lg flex items-center justify-center transition-all ${active ? 'bg-blue-500 text-white shadow-lg shadow-blue-500/30' : 'bg-gray-800 text-gray-400 hover:bg-gray-700 hover:text-gray-200'
            }`}
    >
        <Icon size={18} />
    </button>
);

// --- Main Application ---

export default function App() {
    // Personalizing the default text
    const [text, setText] = useState("Keleigh Plunkett");

    // Formatting Options
    const [formats, setFormats] = useState({
        bold: true,
        italic: false,
        underline: false,
        strikethrough: false,
        obfuscate: false
    });

    // Gradient Colors State
    const [colors, setColors] = useState([
        { id: 1, hex: '#BFFCC6', pos: 0 },
        { id: 2, hex: '#FFF5BA', pos: 50 },
        { id: 3, hex: '#FFC9DE', pos: 100 }
    ]);

    // Shadow State
    const [shadowEnabled, setShadowEnabled] = useState(false);
    const [shadowColors, setShadowColors] = useState([
        { id: 1, hex: '#303F32', pos: 0 },
        { id: 2, hex: '#403D2F', pos: 50 },
        { id: 3, hex: '#403238', pos: 100 }
    ]);

    // Advanced Options State
    const [outputFormat, setOutputFormat] = useState('minimessage');
    const [prefixSuffix, setPrefixSuffix] = useState("");
    const [disperse, setDisperse] = useState(false);
    const [trimSpaces, setTrimSpaces] = useState(true);
    const [lowercase, setLowercase] = useState(false);

    // Decode State
    const [decodeText, setDecodeText] = useState("");

    // Refs for Dragging
    const sliderRef = useRef(null);
    const [draggingId, setDraggingId] = useState(null);

    // --- Logic Handlers ---

    const toggleFormat = (key) => setFormats(prev => ({ ...prev, [key]: !prev[key] }));

    const addColor = (isShadow) => {
        const target = isShadow ? shadowColors : colors;
        const setTarget = isShadow ? setShadowColors : setColors;
        if (target.length >= 10) return; // Limit stops
        const newColor = { id: Date.now(), hex: '#FFFFFF', pos: 100 };
        setTarget([...target, newColor]);
    };

    const updateColor = (isShadow, id, field, value) => {
        const setTarget = isShadow ? setShadowColors : setColors;
        setTarget(prev => prev.map(c => c.id === id ? { ...c, [field]: value } : c));
    };

    const removeColor = (isShadow, id) => {
        const target = isShadow ? shadowColors : colors;
        const setTarget = isShadow ? setShadowColors : setColors;
        if (target.length <= 2) return; // Require at least 2 stops
        setTarget(target.filter(c => c.id !== id));
    };

    // --- Output Generator ---

    const generatedOutput = useMemo(() => {
        if (!text) return "";

        let activeChars = 0;
        const charArray = text.split('');

        // Count active characters for interpolation
        charArray.forEach(char => {
            if (char === ' ' && trimSpaces) return;
            activeChars++;
        });

        let currentActiveIndex = 0;
        let output = "";

        // Format Strings helper
        const getFormattingTags = (formatType) => {
            if (formatType === 'minimessage') {
                let tags = "";
                if (formats.bold) tags += "<b>";
                if (formats.italic) tags += "<i>";
                if (formats.underline) tags += "<u>";
                if (formats.strikethrough) tags += "<st>";
                if (formats.obfuscate) tags += "<obf>";
                return tags;
            } else {
                let tags = "";
                if (formats.bold) tags += "&l";
                if (formats.italic) tags += "&o";
                if (formats.underline) tags += "&n";
                if (formats.strikethrough) tags += "&m";
                if (formats.obfuscate) tags += "&k";
                return tags;
            }
        };

        const tags = getFormattingTags(outputFormat);

        charArray.forEach((char) => {
            if (char === ' ' && trimSpaces) {
                output += char;
                return;
            }

            const progress = activeChars > 1 ? currentActiveIndex / (activeChars - 1) : 0;
            // Disperse overwrites positional data by forcing even distribution
            const colorProgress = disperse ? progress : progress;

            let hex = getColorAtProgress(disperse ? colors.map((c, i) => ({ ...c, pos: (i / (colors.length - 1)) * 100 })) : colors, colorProgress);

            if (lowercase) hex = hex.toLowerCase();

            if (outputFormat === 'minimessage') {
                output += `<${hex}>${tags}${char}`;
            } else if (outputFormat === 'legacy') {
                output += `&#${hex.slice(1)}${tags}${char}`;
            } else if (outputFormat === 'hex') {
                output += `<${hex}>${tags}${char}`;
            }

            currentActiveIndex++;
        });

        if (prefixSuffix) {
            output = prefixSuffix.replace('$t', output);
        }

        return output;
    }, [text, colors, formats, outputFormat, prefixSuffix, disperse, trimSpaces, lowercase]);

    // Visual Preview styling generator
    const getVisualPreview = () => {
        return text.split('').map((char, i) => {
            if (char === ' ') return <span key={i}>&nbsp;</span>;
            const progress = i / (text.length - 1 || 1);
            const hex = getColorAtProgress(colors, progress);
            const shadowHex = shadowEnabled ? getColorAtProgress(shadowColors, progress) : 'transparent';

            return (
                <span
                    key={i}
                    style={{
                        color: hex,
                        textShadow: shadowEnabled ? `3px 3px 0px ${shadowHex}` : 'none',
                        fontWeight: formats.bold ? 'bold' : 'normal',
                        fontStyle: formats.italic ? 'italic' : 'normal',
                        textDecoration: `${formats.underline ? 'underline ' : ''}${formats.strikethrough ? 'line-through' : ''}`
                    }}
                >
                    {char}
                </span>
            );
        });
    };

    // --- Rendering ---
    return (
        <div className="min-h-screen bg-gray-950 text-gray-100 font-sans p-4 md:p-8 flex justify-center">
            <div className="max-w-6xl w-full flex flex-col gap-8">

                {/* Header */}
                <header className="border-b border-gray-800 pb-4">
                    <h1 className="text-3xl font-extrabold flex items-center gap-3 bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-emerald-400">
                        <Palette size={32} className="text-blue-400" />
                        RGBirdflop Engine
                    </h1>
                    <p className="text-gray-400 mt-2">Advanced Hex gradient text generator for Minecraft.</p>
                </header>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                    {/* LEFT COLUMN: Input & Format */}
                    <div className="flex flex-col gap-4 lg:col-span-2">

                        {/* Input Area */}
                        <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 shadow-xl relative">
                            <div className="flex justify-between items-center mb-4">
                                <h2 className="text-lg font-semibold flex items-center gap-2">
                                    <Terminal size={20} className="text-blue-400" />
                                    Input Text
                                </h2>

                                {/* Format Toggles */}
                                <div className="flex gap-1 bg-gray-950 p-1 rounded-lg border border-gray-800">
                                    <FormatButton icon={Bold} active={formats.bold} onClick={() => toggleFormat('bold')} tooltip="Bold (&l)" />
                                    <FormatButton icon={Italic} active={formats.italic} onClick={() => toggleFormat('italic')} tooltip="Italic (&o)" />
                                    <FormatButton icon={Underline} active={formats.underline} onClick={() => toggleFormat('underline')} tooltip="Underline (&n)" />
                                    <FormatButton icon={Strikethrough} active={formats.strikethrough} onClick={() => toggleFormat('strikethrough')} tooltip="Strikethrough (&m)" />
                                    <FormatButton icon={Wand2} active={formats.obfuscate} onClick={() => toggleFormat('obfuscate')} tooltip="Obfuscate (&k)" />
                                </div>
                            </div>

                            {/* Visual Preview / Input overlay */}
                            <div className="relative text-3xl md:text-5xl font-bold tracking-tight mb-2 p-2 min-h-[80px] break-all bg-gray-950 rounded-lg border border-gray-800 focus-within:border-blue-500 transition-colors">
                                <div className="pointer-events-none absolute inset-0 p-2 whitespace-pre-wrap opacity-90 z-10">
                                    {getVisualPreview()}
                                </div>
                                <textarea
                                    value={text}
                                    onChange={(e) => setText(e.target.value)}
                                    className="w-full h-full bg-transparent text-transparent caret-white outline-none resize-none relative z-20 whitespace-pre-wrap"
                                    spellCheck="false"
                                    rows={2}
                                />
                            </div>
                        </div>

                        {/* Gradients Builder */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

                            {/* Primary Color Builder */}
                            <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
                                <div className="flex justify-between items-center mb-4">
                                    <h3 className="font-semibold text-gray-200">Primary Colors</h3>
                                    <button onClick={() => addColor(false)} className="text-xs bg-gray-800 hover:bg-gray-700 px-2 py-1 rounded text-gray-300 flex items-center gap-1">
                                        <Plus size={14} /> Add Stop
                                    </button>
                                </div>

                                {/* Visual Gradient Bar */}
                                <div
                                    className="h-4 rounded-full w-full mb-6 relative border border-gray-700"
                                    style={{ background: `linear-gradient(to right, ${[...colors].sort((a, b) => a.pos - b.pos).map(c => `${c.hex} ${c.pos}%`).join(', ')})` }}
                                >
                                    {colors.map(color => (
                                        <div
                                            key={color.id}
                                            className="absolute w-4 h-4 bg-white rounded-full -mt-0.5 border-2 border-gray-900 shadow cursor-ew-resize transform -translate-x-1/2 transition-transform hover:scale-125"
                                            style={{ left: `${color.pos}%`, backgroundColor: color.hex }}
                                        />
                                    ))}
                                </div>

                                {/* Color Stops Settings */}
                                <div className="flex flex-col gap-2 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                                    {[...colors].sort((a, b) => a.pos - b.pos).map((color, index) => (
                                        <div key={color.id} className="flex items-center gap-2 bg-gray-950 p-2 rounded-lg border border-gray-800">
                                            <span className="text-xs text-gray-500 w-4">{index + 1}</span>
                                            <input
                                                type="color"
                                                value={color.hex}
                                                onChange={(e) => updateColor(false, color.id, 'hex', e.target.value)}
                                                className="w-8 h-8 rounded cursor-pointer border-0 bg-transparent p-0"
                                            />
                                            <input
                                                type="number"
                                                value={color.pos}
                                                min="0" max="100"
                                                onChange={(e) => updateColor(false, color.id, 'pos', Number(e.target.value))}
                                                className="bg-gray-800 text-sm text-center w-16 py-1 rounded border border-gray-700 outline-none focus:border-blue-500"
                                            />
                                            <span className="text-gray-500 text-sm">%</span>
                                            <div className="flex-grow"></div>
                                            <button onClick={() => removeColor(false, color.id)} className="text-red-400 hover:bg-red-500/20 p-1.5 rounded transition-colors">
                                                <Trash size={14} />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Shadow Color Builder (Conditional) */}
                            <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
                                <div className="flex justify-between items-center mb-4">
                                    <div className="flex items-center gap-2">
                                        <input
                                            type="checkbox"
                                            id="shadowToggle"
                                            checked={shadowEnabled}
                                            onChange={(e) => setShadowEnabled(e.target.checked)}
                                            className="w-4 h-4 rounded bg-gray-800 border-gray-700 text-blue-500 focus:ring-blue-500"
                                        />
                                        <label htmlFor="shadowToggle" className="font-semibold text-gray-200 cursor-pointer">Text Shadow</label>
                                    </div>
                                    {shadowEnabled && (
                                        <button onClick={() => addColor(true)} className="text-xs bg-gray-800 hover:bg-gray-700 px-2 py-1 rounded text-gray-300 flex items-center gap-1">
                                            <Plus size={14} /> Add Stop
                                        </button>
                                    )}
                                </div>

                                {shadowEnabled ? (
                                    <>
                                        <div
                                            className="h-4 rounded-full w-full mb-6 relative border border-gray-700"
                                            style={{ background: `linear-gradient(to right, ${[...shadowColors].sort((a, b) => a.pos - b.pos).map(c => `${c.hex} ${c.pos}%`).join(', ')})` }}
                                        >
                                            {shadowColors.map(color => (
                                                <div
                                                    key={color.id}
                                                    className="absolute w-4 h-4 bg-white rounded-full -mt-0.5 border-2 border-gray-900 shadow cursor-ew-resize transform -translate-x-1/2 hover:scale-125"
                                                    style={{ left: `${color.pos}%`, backgroundColor: color.hex }}
                                                />
                                            ))}
                                        </div>

                                        <div className="flex flex-col gap-2 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                                            {[...shadowColors].sort((a, b) => a.pos - b.pos).map((color, index) => (
                                                <div key={color.id} className="flex items-center gap-2 bg-gray-950 p-2 rounded-lg border border-gray-800">
                                                    <span className="text-xs text-gray-500 w-4">{index + 1}</span>
                                                    <input
                                                        type="color"
                                                        value={color.hex}
                                                        onChange={(e) => updateColor(true, color.id, 'hex', e.target.value)}
                                                        className="w-8 h-8 rounded cursor-pointer border-0 bg-transparent p-0"
                                                    />
                                                    <input
                                                        type="number"
                                                        value={color.pos}
                                                        min="0" max="100"
                                                        onChange={(e) => updateColor(true, color.id, 'pos', Number(e.target.value))}
                                                        className="bg-gray-800 text-sm text-center w-16 py-1 rounded border border-gray-700 outline-none focus:border-blue-500"
                                                    />
                                                    <span className="text-gray-500 text-sm">%</span>
                                                    <div className="flex-grow"></div>
                                                    <button onClick={() => removeColor(true, color.id)} className="text-red-400 hover:bg-red-500/20 p-1.5 rounded transition-colors">
                                                        <Trash size={14} />
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    </>
                                ) : (
                                    <div className="h-full flex items-center justify-center text-sm text-gray-500 text-center px-4 opacity-50 pb-8">
                                        Enable text shadow to create a secondary gradient map for JSON/MiniMessage formats.
                                    </div>
                                )}
                            </div>

                        </div>
                    </div>

                    {/* RIGHT COLUMN: Output & Settings */}
                    <div className="flex flex-col gap-6">

                        {/* Output Generation */}
                        <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 shadow-xl">
                            <div className="flex justify-between items-center mb-3">
                                <h2 className="text-lg font-semibold flex items-center gap-2">
                                    <Copy size={18} className="text-emerald-400" /> Output
                                </h2>
                                <button
                                    onClick={() => {
                                        navigator.clipboard.writeText(generatedOutput);
                                    }}
                                    className="bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                                >
                                    Copy
                                </button>
                            </div>
                            <textarea
                                readOnly
                                value={generatedOutput}
                                className="w-full h-32 bg-gray-950 border border-gray-800 rounded-lg p-3 text-sm font-mono text-gray-300 resize-none outline-none focus:border-emerald-500 transition-colors"
                            />
                        </div>

                        {/* Advanced Settings */}
                        <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 shadow-xl">
                            <h2 className="text-lg font-semibold flex items-center gap-2 mb-4">
                                <Settings size={18} className="text-purple-400" />
                                Advanced Options
                            </h2>

                            <div className="flex flex-col gap-4">

                                {/* Format Dropdown */}
                                <div className="flex flex-col gap-1">
                                    <label className="text-sm font-medium text-gray-300">Format</label>
                                    <div className="relative">
                                        <select
                                            value={outputFormat}
                                            onChange={(e) => setOutputFormat(e.target.value)}
                                            className="w-full bg-gray-950 border border-gray-800 rounded-lg p-2.5 text-sm appearance-none outline-none focus:border-purple-500 text-gray-200"
                                        >
                                            <option value="minimessage">MiniMessage (&lt;#hex&gt;)</option>
                                            <option value="legacy">Legacy (&#hex)</option>
                                            <option value="hex">Hex (&lt;hex&gt;)</option>
                                        </select>
                                        <ChevronDown size={16} className="absolute right-3 top-3 text-gray-500 pointer-events-none" />
                                    </div>
                                </div>

                                {/* Prefix / Suffix */}
                                <div className="flex flex-col gap-1">
                                    <label className="text-sm font-medium text-gray-300">Prefix/Suffix Command</label>
                                    <input
                                        type="text"
                                        placeholder="e.g. /nick $t"
                                        value={prefixSuffix}
                                        onChange={(e) => setPrefixSuffix(e.target.value)}
                                        className="w-full bg-gray-950 border border-gray-800 rounded-lg p-2.5 text-sm outline-none focus:border-purple-500 text-gray-200 placeholder-gray-600"
                                    />
                                </div>

                                <hr className="border-gray-800 my-1" />

                                {/* Toggles */}
                                <Toggle
                                    label="Always Disperse Colors"
                                    checked={disperse}
                                    onChange={setDisperse}
                                    description="Ignore position values and spread colors evenly."
                                />
                                <Toggle
                                    label="Trim Spaces"
                                    checked={trimSpaces}
                                    onChange={setTrimSpaces}
                                    description="Don't use up color spectrum progression on space characters."
                                />
                                <Toggle
                                    label="Lowercase Hex"
                                    checked={lowercase}
                                    onChange={setLowercase}
                                    description="Force output hex codes to be lowercase."
                                />

                            </div>
                        </div>

                        {/* Decoder / Reverse Engineer */}
                        <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 shadow-xl">
                            <h2 className="text-lg font-semibold flex items-center gap-2 mb-2">
                                <RefreshCw size={18} className="text-orange-400" />
                                Decode RGB
                            </h2>
                            <p className="text-xs text-gray-400 mb-3">Paste existing formatted text to reverse-engineer colors.</p>
                            <textarea
                                placeholder="&#BFFCC6&l&nE..."
                                value={decodeText}
                                onChange={(e) => setDecodeText(e.target.value)}
                                className="w-full h-16 bg-gray-950 border border-gray-800 rounded-lg p-2 text-sm text-gray-300 resize-none outline-none focus:border-orange-500 transition-colors mb-2"
                            />
                            <button
                                onClick={() => alert("Decode logic simulated! (In full prod, this parses regex to extract hex and chars)")}
                                className="w-full bg-gray-800 hover:bg-gray-700 text-gray-200 py-2 rounded-lg text-sm font-medium transition-colors"
                            >
                                Analyze String
                            </button>
                        </div>

                    </div>

                </div>
            </div>

            {/* Scrollbar hide utility */}
            <style dangerouslySetInnerHTML={{
                __html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background-color: #374151; border-radius: 10px; }
      `}} />
        </div>
    );
}