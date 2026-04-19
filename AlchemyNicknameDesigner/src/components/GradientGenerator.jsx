import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';

const UI_GRADIENTS_URL = 'https://raw.githubusercontent.com/ghosh/uiGradients/master/gradients.json';
const CACHE_KEY = 'uiGradients_cache';
const CACHE_TTL = 24 * 60 * 60 * 1000;

// --- Birdflop Engine ---

const hexToRgb = (hex) => {
    const r = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return r ? { r: parseInt(r[1],16), g: parseInt(r[2],16), b: parseInt(r[3],16) } : { r:255, g:255, b:255 };
};

const rgbToHex = (r,g,b) =>
    '#' + ((1<<24)+(r<<16)+(g<<8)+b).toString(16).slice(1).toUpperCase();

const interpolateColor = (hex1, hex2, factor) => {
    const c1 = hexToRgb(hex1), c2 = hexToRgb(hex2);
    return rgbToHex(
        Math.round(c1.r + factor*(c2.r-c1.r)),
        Math.round(c1.g + factor*(c2.g-c1.g)),
        Math.round(c1.b + factor*(c2.b-c1.b)),
    );
};

const getColorAtProgress = (colors, progress) => {
    if (!colors.length) return '#FFFFFF';
    if (colors.length === 1) return colors[0].hex;
    const sorted = [...colors].sort((a,b) => a.pos - b.pos);
    const pos = progress * 100;
    if (pos <= sorted[0].pos) return sorted[0].hex;
    if (pos >= sorted[sorted.length-1].pos) return sorted[sorted.length-1].hex;
    for (let i = 0; i < sorted.length-1; i++) {
        const c1 = sorted[i], c2 = sorted[i+1];
        if (pos >= c1.pos && pos <= c2.pos) {
            return interpolateColor(c1.hex, c2.hex, (pos-c1.pos)/(c2.pos-c1.pos));
        }
    }
    return '#FFFFFF';
};

const distributeColors = (src, count) =>
    Array.from({ length: count }, (_, i) => {
        const pos = count === 1 ? 50 : (i/(count-1))*100;
        return { id: Math.random().toString(36).slice(2), hex: getColorAtProgress(src, pos/100), pos };
    });

const makeStop = (hex, pos) => ({ id: Math.random().toString(36).slice(2), hex, pos });

const pickRandom = (data, n=16) => [...data].sort(()=>Math.random()-0.5).slice(0,n);

const gradientCSS = (stops) => {
    const s = [...stops].sort((a,b)=>a.pos-b.pos);
    return `linear-gradient(to right, ${s.map(c=>`${c.hex} ${c.pos}%`).join(', ')})`;
};

// --- Sub-components ---

const FORMAT_BUTTONS = [
    { key:'bold',          label:'B', title:'Bold',          style:{fontWeight:'bold'} },
    { key:'italic',        label:'I', title:'Italic',        style:{fontStyle:'italic',fontFamily:'serif'} },
    { key:'underline',     label:'U', title:'Underline',     style:{textDecoration:'underline'} },
    { key:'strikethrough', label:'S', title:'Strikethrough', style:{textDecoration:'line-through'} },
];

const OUTPUT_FORMATS = [
    { value:'minimessage', label:'MiniMessage (<#HEX>)' },
    { value:'legacy',      label:'Legacy (&#HEX)' },
    { value:'mypet',       label:'Standard (&#RRGGBB)' },
];

const TrashIcon = () => (
    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
    </svg>
);

const Toggle = ({ checked, onChange }) => (
    <label className="relative inline-flex cursor-pointer items-center flex-shrink-0">
        <input type="checkbox" className="peer sr-only" checked={checked} onChange={e=>onChange(e.target.checked)}/>
        <div className="h-6 w-11 rounded-full bg-[var(--bg-input)] peer-checked:bg-[var(--accent-blue)] after:absolute after:top-[2px] after:left-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all peer-checked:after:translate-x-full"/>
    </label>
);

const GradientStopBar = ({ stops, setStops, barRef, draggingId, onDragStart, onBarClick }) => (
    <div>
        <div
            ref={barRef}
            className="relative w-full h-4 rounded-full cursor-crosshair select-none border border-[var(--border-color)] mb-5"
            style={{ background: gradientCSS(stops) }}
            onClick={onBarClick}
        >
            {stops.map(stop => (
                <div
                    key={stop.id}
                    className="absolute w-4 h-4 rounded-full border-2 border-[var(--bg-base)] shadow-lg cursor-ew-resize hover:scale-125 transition-transform"
                    style={{
                        left:`${stop.pos}%`, top:'50%',
                        transform:'translate(-50%,-50%)',
                        backgroundColor: stop.hex,
                        zIndex: draggingId === stop.id ? 10 : 1,
                    }}
                    onMouseDown={e => onDragStart(e, stop.id)}
                    onTouchStart={e => onDragStart(e, stop.id)}
                />
            ))}
        </div>
        <div className="flex flex-col gap-2 max-h-52 overflow-y-auto pr-1">
            {[...stops].sort((a,b)=>a.pos-b.pos).map((stop, i) => (
                <div key={stop.id} className="flex items-center gap-2 bg-[var(--bg-input)] px-2 py-1.5 rounded-lg border border-[var(--border-color)]">
                    <span className="text-xs text-[var(--text-secondary)] w-4 flex-shrink-0">{i+1}</span>
                    <div className="relative flex-shrink-0">
                        <div className="w-7 h-7 rounded border border-black/20 cursor-pointer" style={{backgroundColor:stop.hex}}/>
                        <input type="color" value={stop.hex}
                            onChange={e => setStops(prev => prev.map(s => s.id===stop.id ? {...s,hex:e.target.value} : s))}
                            className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                        />
                    </div>
                    <input type="number" value={stop.pos} min="0" max="100"
                        onChange={e => setStops(prev => prev.map(s => s.id===stop.id ? {...s,pos:Number(e.target.value)} : s))}
                        className="bg-[var(--bg-base)] text-xs text-center w-12 py-1 rounded border border-[var(--border-color)] outline-none focus:border-[var(--accent-blue)]"
                    />
                    <span className="text-[var(--text-secondary)] text-xs">%</span>
                    <div className="flex-grow"/>
                    {stops.length > 2 && (
                        <button onClick={() => setStops(prev => prev.filter(s=>s.id!==stop.id))}
                            className="text-[var(--accent-red)] hover:bg-[var(--accent-red)]/20 p-1 rounded transition-colors">
                            <TrashIcon/>
                        </button>
                    )}
                </div>
            ))}
        </div>
        <button
            onClick={() => stops.length < 10 && setStops(prev=>[...prev, makeStop('#FFFFFF',100)])}
            disabled={stops.length >= 10}
            className="mt-2 w-full birdflop-btn text-sm py-1.5 disabled:opacity-40"
        >+ Add Stop</button>
    </div>
);

// --- Main Component ---

const GradientGenerator = ({ playerInfo }) => {
    const [text, setText] = useState(playerInfo?.name || 'Steve');

    const [colors, setColors] = useState([
        makeStop('#BFFCC6', 0),
        makeStop('#FFF5BA', 50),
        makeStop('#FFC9DE', 100),
    ]);
    const [shadowEnabled, setShadowEnabled] = useState(false);
    const [shadowColors, setShadowColors] = useState([
        makeStop('#303F32', 0),
        makeStop('#403D2F', 50),
        makeStop('#403238', 100),
    ]);

    const [formats, setFormats] = useState({ bold:true, italic:false, underline:false, strikethrough:false });
    const [outputFormat, setOutputFormat] = useState('minimessage');
    const [disperse, setDisperse] = useState(false);
    const [trimSpaces, setTrimSpaces] = useState(true);
    const [lowercase, setLowercase] = useState(false);
    const [prefixSuffix, setPrefixSuffix] = useState('');

    const [uiGradients, setUiGradients] = useState([]);
    const [shownPresets, setShownPresets] = useState([]);

    const [draggingId, setDraggingId] = useState(null);
    const [draggingIsShadow, setDraggingIsShadow] = useState(false);
    const barRef        = useRef(null);
    const shadowBarRef  = useRef(null);
    const dragBarState  = useRef(null);
    const inputRef      = useRef(null);
    const renderRef     = useRef(null);

    const [saving, setSaving]         = useState(false);
    const [saveStatus, setSaveStatus] = useState(null);
    const [copied, setCopied]         = useState(false);

    useEffect(() => { if (playerInfo?.name) setText(playerInfo.name); }, [playerInfo]);

    useEffect(() => {
        const cached = localStorage.getItem(CACHE_KEY);
        if (cached) {
            try {
                const { data, ts } = JSON.parse(cached);
                if (Date.now()-ts < CACHE_TTL) {
                    setUiGradients(data); setShownPresets(pickRandom(data)); return;
                }
            } catch {}
        }
        fetch(UI_GRADIENTS_URL).then(r=>r.json()).then(data => {
            localStorage.setItem(CACHE_KEY, JSON.stringify({ data, ts:Date.now() }));
            setUiGradients(data); setShownPresets(pickRandom(data));
        }).catch(()=>{});
    }, []);

    // Drag
    const startDrag = useCallback((e, id, isShadow) => {
        e.preventDefault(); e.stopPropagation();
        const bar = (isShadow ? shadowBarRef : barRef).current;
        if (!bar) return;
        const rect = bar.getBoundingClientRect();
        dragBarState.current = { barLeft: rect.left, barWidth: rect.width };
        setDraggingId(id); setDraggingIsShadow(isShadow);
    }, []);

    useEffect(() => {
        if (draggingId === null) return;
        const setter = draggingIsShadow ? setShadowColors : setColors;
        const move = (e) => {
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const { barLeft, barWidth } = dragBarState.current;
            const pos = Math.round(Math.min(100,Math.max(0,((clientX-barLeft)/barWidth)*100))*10)/10;
            setter(prev => prev.map(c => c.id===draggingId ? {...c,pos} : c));
        };
        const up = () => setDraggingId(null);
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up);
        window.addEventListener('touchmove', move, {passive:true});
        window.addEventListener('touchend', up);
        return () => {
            window.removeEventListener('mousemove', move);
            window.removeEventListener('mouseup', up);
            window.removeEventListener('touchmove', move);
            window.removeEventListener('touchend', up);
        };
    }, [draggingId, draggingIsShadow]);

    const handleBarClick = useCallback((e, isShadow) => {
        if (draggingId !== null) return;
        const bar = (isShadow ? shadowBarRef : barRef).current;
        if (!bar) return;
        const rect = bar.getBoundingClientRect();
        const pos = Math.round(Math.min(100,Math.max(0,((e.clientX-rect.left)/rect.width)*100))*10)/10;
        const src = isShadow ? shadowColors : colors;
        const setter = isShadow ? setShadowColors : setColors;
        setter(prev => [...prev, makeStop(getColorAtProgress(src, pos/100), pos)]);
    }, [colors, shadowColors, draggingId]);

    // Output (Birdflop direct per-character generation)
    const generatedOutput = useMemo(() => {
        if (!text) return '';
        const chars = text.split('');
        const effectiveColors = disperse
            ? colors.map((c,i) => ({...c, pos:(i/(colors.length-1))*100}))
            : colors;
        const activeCount = trimSpaces ? chars.filter(c=>c!==' ').length : chars.length;

        const tags = outputFormat === 'minimessage'
            ? [formats.bold&&'<b>', formats.italic&&'<i>', formats.underline&&'<u>', formats.strikethrough&&'<st>'].filter(Boolean).join('')
            : [formats.bold&&'&l', formats.italic&&'&o', formats.underline&&'&n', formats.strikethrough&&'&m'].filter(Boolean).join('');

        let activeIdx = 0;
        let out = '';
        chars.forEach(char => {
            if (char===' ' && trimSpaces) { out+=' '; return; }
            const progress = activeCount>1 ? activeIdx/(activeCount-1) : 0;
            let hex = getColorAtProgress(effectiveColors, progress);
            if (lowercase) hex = hex.toLowerCase();
            out += outputFormat==='minimessage' ? `<${hex}>${tags}${char}` : `&#${hex.slice(1)}${tags}${char}`;
            activeIdx++;
        });

        return prefixSuffix ? prefixSuffix.replace('$t', out) : out;
    }, [text, colors, formats, outputFormat, disperse, trimSpaces, lowercase, prefixSuffix]);

    // Visual preview chars
    const previewChars = useMemo(() => {
        const effectiveColors = disperse
            ? colors.map((c,i) => ({...c, pos:(i/(colors.length-1))*100}))
            : colors;
        return text.split('').map((char, i) => {
            const progress = text.length>1 ? i/(text.length-1) : 0;
            return {
                char,
                hex: getColorAtProgress(effectiveColors, progress),
                shadowHex: shadowEnabled ? getColorAtProgress(shadowColors, progress) : null,
            };
        });
    }, [text, colors, shadowColors, shadowEnabled, disperse]);

    const previewStyle = useMemo(() => ({
        fontWeight:     formats.bold          ? 'bold'       : 'normal',
        fontStyle:      formats.italic        ? 'italic'     : 'normal',
        textDecoration: [formats.underline?'underline':'', formats.strikethrough?'line-through':''].filter(Boolean).join(' ') || 'none',
    }), [formats]);

    const applyPreset = (gradient) => {
        const gColors = gradient.colors;
        if (!gColors?.length) return;
        const src = gColors.map((hex,i) => ({hex, pos:(i/(gColors.length-1))*100}));
        setColors(colors.length>src.length
            ? distributeColors(src, colors.length)
            : src.map(s => makeStop(s.hex, s.pos)));
    };

    const handleRandomize = () => {
        if (!uiGradients.length) return;
        const pick = uiGradients[Math.floor(Math.random()*uiGradients.length)];
        if (!pick?.colors?.length) return;
        const src = pick.colors.map((hex,i) => ({hex, pos:(i/(pick.colors.length-1))*100}));
        setColors(distributeColors(src, colors.length));
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(generatedOutput);
        setCopied(true); setTimeout(()=>setCopied(false), 2000);
    };

    const handleSave = async () => {
        if (!playerInfo?.token) {
            setSaveStatus({type:'error', message:'No session. Run /nicknameeditor in-game.'}); return;
        }
        setSaving(true); setSaveStatus(null);
        try {
            const res = await fetch(`${playerInfo.apiBase || ''}/api/nickname/save`, {
                method:'POST', headers:{'Content-Type':'application/json'},
                body: JSON.stringify({token:playerInfo.token, nickname:generatedOutput, plainText:text}),
            });
            if (res.ok) setSaveStatus({type:'success', message:'Nickname saved!'});
            else { const err=await res.text(); setSaveStatus({type:'error', message:`Failed (${res.status}): ${err}`}); }
        } catch { setSaveStatus({type:'error', message:'Connection error.'}); }
        finally { setSaving(false); }
    };

    const handleScroll = (e) => { if (renderRef.current) renderRef.current.scrollLeft=e.target.scrollLeft; };

    return (
        <div className="flex flex-col w-full text-[var(--text-primary)]">

            {/* HEADER */}
            <div className="flex items-baseline gap-2 mb-2">
                <span className="text-xl font-bold font-mono">&gt;_</span>
                <h2 className="text-xl font-bold">Input Text</h2>
                <span className="text-sm text-[var(--text-secondary)] ml-2">Type here to generate a gradient!</span>
            </div>

            {/* LIVE INPUT */}
            <div className="relative w-full overflow-hidden border border-[var(--border-color)] rounded-md bg-[var(--bg-input)] mb-1" style={{height:'84px'}}>
                <div ref={renderRef} className="absolute inset-0 px-4 overflow-hidden whitespace-nowrap pointer-events-none text-5xl mc-text" style={{lineHeight:'82px'}}>
                    {previewChars.map((item,i) => (
                        <span key={i} className="inline-block whitespace-pre" style={{
                            color: item.hex,
                            textShadow: item.shadowHex ? `3px 3px 0px ${item.shadowHex}` : '2px 2px 0 rgba(0,0,0,0.8)',
                            ...previewStyle,
                        }}>{item.char}</span>
                    ))}
                </div>
                <input ref={inputRef} type="text" value={text} onChange={e=>setText(e.target.value)} onScroll={handleScroll}
                    className="absolute inset-0 w-full h-full bg-transparent outline-none text-5xl mc-text px-4 border-none"
                    style={{...previewStyle, color:'transparent', caretColor:'white', textShadow:'none', lineHeight:'82px'}}
                    spellCheck="false"
                />
            </div>

            {/* FONT CONTROLS */}
            <div className="flex gap-1.5 mb-5">
                {FORMAT_BUTTONS.map(({key,label,title,style}) => (
                    <button key={key} onClick={()=>setFormats(p=>({...p,[key]:!p[key]}))}
                        className={`format-btn ${formats[key]?'active':''}`} title={title} style={style}>
                        {label}
                    </button>
                ))}
            </div>

            {/* PRESETS */}
            <div className="flex flex-wrap items-center justify-between gap-4 p-4 bg-[var(--bg-card)] rounded-lg border border-[var(--border-color)] shadow-md mb-6">
                <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
                    <span className="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-widest mr-1 flex-shrink-0">Presets</span>
                    {shownPresets.length>0 ? shownPresets.map((g,i) => (
                        <button key={i} onClick={()=>applyPreset(g)} title={g.name}
                            className="w-7 h-7 rounded-full border-2 border-[var(--border-color)] hover:border-white hover:scale-110 transition-all cursor-pointer flex-shrink-0"
                            style={{background: g.colors?.length>=2 ? `linear-gradient(135deg,${g.colors.join(',')})` : (g.colors?.[0]||'#888')}}/>
                    )) : <span className="text-xs text-[var(--text-secondary)] italic">Loading presets…</span>}
                </div>
                <div className="flex gap-2 flex-shrink-0">
                    <button onClick={()=>setShownPresets(pickRandom(uiGradients))}
                        className="flex items-center gap-2 px-4 py-2 bg-[var(--bg-input)] border border-[var(--border-color)] text-[var(--text-secondary)] hover:text-white rounded-md font-bold text-sm transition-all cursor-pointer">
                        🔀 Shuffle
                    </button>
                    <button onClick={handleRandomize}
                        className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-[var(--accent-blue)] to-[var(--accent-purple)] text-white rounded-md font-bold text-sm shadow-lg hover:brightness-110 active:scale-95 transition-all cursor-pointer">
                        🎲 Randomize
                    </button>
                </div>
            </div>

            {/* MAIN GRID */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* GRADIENT BUILDERS */}
                <div className="lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-6">

                    {/* Primary */}
                    <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                        <h3 className="font-semibold mb-4">🎨 Primary Colors</h3>
                        <GradientStopBar
                            stops={colors} setStops={setColors}
                            barRef={barRef} draggingId={draggingId}
                            onDragStart={(e,id)=>startDrag(e,id,false)}
                            onBarClick={e=>handleBarClick(e,false)}
                        />
                    </div>

                    {/* Shadow */}
                    <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                        <div className="flex items-center gap-3 mb-4">
                            <Toggle checked={shadowEnabled} onChange={setShadowEnabled}/>
                            <h3 className="font-semibold cursor-pointer select-none" onClick={()=>setShadowEnabled(p=>!p)}>
                                🌑 Text Shadow
                            </h3>
                        </div>
                        {shadowEnabled ? (
                            <GradientStopBar
                                stops={shadowColors} setStops={setShadowColors}
                                barRef={shadowBarRef} draggingId={draggingId}
                                onDragStart={(e,id)=>startDrag(e,id,true)}
                                onBarClick={e=>handleBarClick(e,true)}
                            />
                        ) : (
                            <div className="flex items-center justify-center h-32 text-sm text-[var(--text-secondary)] text-center opacity-50 px-4">
                                Enable to add a shadow gradient behind your nickname.
                            </div>
                        )}
                    </div>
                </div>

                {/* OUTPUT + OPTIONS + SYNC */}
                <div className="flex flex-col gap-4">

                    {/* Output */}
                    <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                        <div className="flex justify-between items-center mb-3">
                            <h3 className="font-semibold">📋 Output</h3>
                            <button onClick={handleCopy} className="birdflop-btn px-3 py-1.5 text-sm">
                                {copied ? '✓ Copied' : 'Copy'}
                            </button>
                        </div>
                        <textarea readOnly value={generatedOutput}
                            className="w-full h-28 birdflop-input px-3 py-2 text-xs font-mono resize-none"/>
                    </div>

                    {/* Options */}
                    <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                        <h3 className="font-semibold mb-4">⚙️ Options</h3>
                        <div className="flex flex-col gap-3">
                            <div>
                                <label className="text-xs font-medium text-[var(--text-secondary)] uppercase tracking-wide block mb-1">Format</label>
                                <select value={outputFormat} onChange={e=>setOutputFormat(e.target.value)}
                                    className="birdflop-input w-full px-3 py-2 text-sm">
                                    {OUTPUT_FORMATS.map(f=><option key={f.value} value={f.value}>{f.label}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="text-xs font-medium text-[var(--text-secondary)] uppercase tracking-wide block mb-1">Prefix / Suffix</label>
                                <input type="text" placeholder="/nick $t" value={prefixSuffix}
                                    onChange={e=>setPrefixSuffix(e.target.value)}
                                    className="birdflop-input w-full px-3 py-2 text-sm"/>
                                <p className="text-xs text-[var(--text-secondary)] mt-0.5">Use <code>$t</code> as the output placeholder.</p>
                            </div>
                            <hr className="border-[var(--border-color)]"/>
                            {[
                                {key:'disperse',   label:'Always Disperse',  desc:'Ignore stop positions, spread colors evenly.',        val:disperse,   set:setDisperse},
                                {key:'trimSpaces', label:'Trim Spaces',      desc:"Spaces don't consume gradient spectrum.",             val:trimSpaces, set:setTrimSpaces},
                                {key:'lowercase',  label:'Lowercase Hex',    desc:'Force hex codes to lowercase.',                       val:lowercase,  set:setLowercase},
                            ].map(({key,label,desc,val,set})=>(
                                <label key={key} className="flex items-start gap-3 cursor-pointer select-none">
                                    <input type="checkbox" checked={val} onChange={e=>set(e.target.checked)} className="mt-0.5 w-4 h-4 flex-shrink-0"/>
                                    <div>
                                        <span className="text-sm font-medium">{label}</span>
                                        <p className="text-xs text-[var(--text-secondary)]">{desc}</p>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Server Sync */}
                    <div className="bg-[var(--bg-card)] border border-[var(--accent-blue)]/50 rounded-xl p-4" style={{background:'color-mix(in srgb, var(--accent-blue) 5%, var(--bg-card))'}}>
                        <h3 className="font-semibold mb-2">☁️ Server Sync</h3>
                        <p className="text-sm text-[var(--text-secondary)] mb-3">Apply this nickname directly to the server!</p>
                        {playerInfo?.name ? (
                            <div className="flex items-center gap-3 mb-3">
                                <img src={`https://mc-heads.net/avatar/${playerInfo.name}/32`} alt="Avatar" className="w-8 h-8 rounded"/>
                                <div>
                                    <div className="text-[10px] text-[var(--text-secondary)] uppercase">Logged in as</div>
                                    <div className="font-bold">{playerInfo.name}</div>
                                </div>
                            </div>
                        ) : (
                            <p className="text-sm text-[var(--accent-red)] mb-3">Run <code>/nicknameeditor</code> in-game to get a link.</p>
                        )}
                        <button onClick={handleSave} disabled={saving || !playerInfo?.token}
                            className="birdflop-btn-blue w-full py-3 text-sm flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/>
                            </svg>
                            {saving ? 'Saving…' : 'Save to Server'}
                        </button>
                        {saveStatus && (
                            <p className={`text-sm font-medium mt-2 ${saveStatus.type==='success'?'text-[#34d399]':'text-[#ef4444]'}`}>
                                {saveStatus.message}
                            </p>
                        )}
                    </div>

                </div>
            </div>
        </div>
    );
};

export default GradientGenerator;
