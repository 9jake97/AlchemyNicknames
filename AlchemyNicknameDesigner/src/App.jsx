import React, { useEffect, useState } from 'react';
import GradientGenerator from './components/GradientGenerator';

function CodeEntryForm({ onSubmit }) {
  const [name, setName] = useState('');
  const [code, setCode] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (name.trim() && code.trim()) {
      onSubmit(name.trim(), code.trim());
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="birdflop-panel p-8 max-w-md w-full space-y-6">

        <div className="text-center space-y-2">
          <h1 className="text-2xl font-bold text-white">✦ Nickname Designer</h1>
          <p className="text-[var(--text-secondary)] text-sm">
            Click the link in chat to open automatically, or enter your details below.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--text-secondary)] uppercase tracking-wide">
              Minecraft Username
            </label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Steve"
              className="birdflop-input w-full px-3 py-2 text-sm"
              autoComplete="off"
              spellCheck="false"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-[var(--text-secondary)] uppercase tracking-wide">
              Session Code
            </label>
            <input
              type="text"
              value={code}
              onChange={e => setCode(e.target.value.replace(/\D/g, '').slice(0, 5))}
              placeholder="12345"
              className="birdflop-input w-full px-3 py-2 text-sm font-mono tracking-widest text-center text-lg"
              maxLength={5}
              inputMode="numeric"
            />
            <p className="text-xs text-[var(--text-secondary)]">
              The 5-digit code shown in chat after running <span className="font-mono text-[var(--accent-blue)]">/nicknameeditor</span>
            </p>
          </div>

          <button
            type="submit"
            disabled={name.trim().length < 1 || code.trim().length !== 5}
            className="birdflop-btn-blue w-full py-3 text-sm font-bold disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Open Designer
          </button>
        </form>

        <div className="border-t border-[var(--border-color)] pt-4 text-center">
          <p className="text-xs text-[var(--text-secondary)]">
            Session codes expire after <span className="text-white">10 minutes</span>
          </p>
        </div>

      </div>
    </div>
  );
}

function stripTags(nick) {
  if (!nick) return '';
  return nick
    .replace(/<[^>]+>/g, '')
    .replace(/&#[0-9A-Fa-f]{6}/g, '')
    .replace(/&[0-9a-fk-or]/gi, '')
    .trim();
}

const makeStop = (hex, pos) => ({ id: Math.random().toString(36).slice(2), hex, pos });

function parseNickname(nick) {
  if (!nick) return null;

  const chars = [];
  // MiniMessage per-character: <#RRGGBB>(modifiers)char
  const mm = /<(#[A-Fa-f0-9]{6})>((?:<[^>]+>)*)([^<])/g;
  let m;
  while ((m = mm.exec(nick)) !== null) {
    chars.push({ color: m[1].toUpperCase(), char: m[3] });
  }

  // Legacy &#RRGGBB fallback
  if (chars.length < 2) {
    const leg = /&#([A-Fa-f0-9]{6})((?:&[lnmok])*)([^&<])/g;
    while ((m = leg.exec(nick)) !== null) {
      chars.push({ color: '#' + m[1].toUpperCase(), char: m[3] });
    }
  }

  if (chars.length < 2) return null;

  const text = chars.map(c => c.char).join('');
  const numStops = Math.min(chars.length, 7);
  const stops = Array.from({ length: numStops }, (_, i) => {
    const idx = Math.round(i / (numStops - 1) * (chars.length - 1));
    return makeStop(chars[idx].color, Math.round(i / (numStops - 1) * 100));
  });

  const formats = {
    bold:          /<b>|<bold>|&l/i.test(nick),
    italic:        /<i>|<italic>|&o/i.test(nick),
    underline:     /<u>|<underlined>|<underline>|&n/i.test(nick),
    strikethrough: /<st>|<strikethrough>|&m/i.test(nick),
  };

  return { text, stops, formats };
}

function App() {
  const [playerInfo, setPlayerInfo] = useState(null);
  const [currentNickname, setCurrentNickname] = useState(null);
  const parsedNickname = React.useMemo(() => parseNickname(currentNickname), [currentNickname]);

  const fetchCurrentNickname = async (info) => {
    if (!info?.token || !info?.apiBase) return;
    try {
      const res = await fetch(`${info.apiBase}/api/nickname/current?token=${info.token}`);
      if (res.ok) {
        const data = await res.json();
        if (data.nickname) setCurrentNickname(data.nickname);
      }
    } catch {}
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const player = params.get('player') || '';
    const token = params.get('token') || '';
    const apiBase = params.get('api') || '';
    if (player && token) {
      const info = { name: player, token, apiBase };
      setPlayerInfo(info);
      fetchCurrentNickname(info);
    }
  }, []);

  const handleCodeEntry = (name, code) => {
    const info = { name, token: code, apiBase: '' };
    setPlayerInfo(info);
    fetchCurrentNickname(info);
  };

  if (!playerInfo) {
    return <CodeEntryForm onSubmit={handleCodeEntry} />;
  }

  return (
    <div className="min-h-screen p-4 md:p-8 flex justify-center">
      <div className="w-full max-w-6xl">
        <GradientGenerator
          playerInfo={playerInfo}
          currentNickname={currentNickname}
          parsedNickname={parsedNickname}
          initialText={parsedNickname ? parsedNickname.text : (currentNickname ? stripTags(currentNickname) : playerInfo.name)}
        />
      </div>
    </div>
  );
}

export default App;
