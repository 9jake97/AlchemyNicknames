import React, { useEffect, useState } from 'react';
import GradientGenerator from './components/GradientGenerator';
import PersonaSelector from './components/PersonaSelector';

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

  const entries = []; // { color: '#RRGGBB' | null, char }
  let i = 0;

  while (i < nick.length) {
    // Color tag: <#RRGGBB>, <color:#RRGGBB>, <colour:#RRGGBB>
    const colorTag = nick.slice(i).match(/^<(?:color:|colour:)?(#[A-Fa-f0-9]{6})>/i);
    if (colorTag) {
      const color = colorTag[1].toUpperCase();
      i += colorTag[0].length;
      // Skip any modifier tags following the color tag
      let modMatch;
      while ((modMatch = nick.slice(i).match(/^<[^>]+>/))) i += modMatch[0].length;
      // Capture the next non-tag character as the colored char
      if (i < nick.length && nick[i] !== '<') {
        entries.push({ color, char: nick[i++] });
      }
      continue;
    }

    // Space (uncolored — trimSpaces strips color from spaces)
    if (nick[i] === ' ') {
      entries.push({ color: null, char: ' ' });
      i++;
      continue;
    }

    // Any other tag (modifier, closing, etc.) — skip it
    if (nick[i] === '<') {
      const end = nick.indexOf('>', i);
      i = end !== -1 ? end + 1 : i + 1;
      continue;
    }

    i++;
  }

  // Fallback: legacy &#RRGGBB format
  if (entries.filter(e => e.color).length < 2) {
    entries.length = 0;
    const leg = /&#([A-Fa-f0-9]{6})((?:&[lnmok])*)([^&<])/g;
    let m;
    while ((m = leg.exec(nick)) !== null) {
      entries.push({ color: '#' + m[1].toUpperCase(), char: m[3] });
    }
  }

  const colored = entries.filter(e => e.color);
  if (colored.length < 2) return null;

  const text = entries.map(e => e.char).join('');
  const n = Math.min(colored.length, 7);
  const stops = Array.from({ length: n }, (_, i) => {
    const idx = Math.round(i / (n - 1) * (colored.length - 1));
    return makeStop(colored[idx].color, Math.round(i / (n - 1) * 100));
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
  const [activeTab, setActiveTab] = useState('nickname');
  const [personaData, setPersonaData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const fetchPersonaData = async (info) => {
    if (!info?.token || !info?.apiBase) return;
    setIsLoading(true);
    try {
      const res = await fetch(`${info.apiBase}/api/nickname/data?token=${info.token}`);
      if (res.ok) {
        const data = await res.json();
        setPersonaData(data);
      }
    } catch (e) {
      console.error("Failed to fetch persona data:", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const player = params.get('player') || '';
    const token = params.get('token') || '';
    const apiBase = params.get('api') || '';
    if (player && token) {
      const info = { name: player, token, apiBase };
      window.playerInfo = info;
      setPlayerInfo(info);
      fetchPersonaData(info);
    }
  }, []);

  const handleCodeEntry = (name, code) => {
    const info = { name, token: code, apiBase: '' }; // apiBase would need to be known or provided
    setPlayerInfo(info);
    fetchPersonaData(info);
  };

  const handleSaveSelection = async (type, id) => {
    if (!playerInfo) return;
    
    const body = {
      token: playerInfo.token,
      [type]: id
    };

    try {
      const res = await fetch(`${playerInfo.apiBase}/api/nickname/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      
      if (res.ok) {
        // Refresh data to show new selection
        fetchPersonaData(playerInfo);
      }
    } catch (e) {
      console.error("Failed to save selection:", e);
    }
  };

  if (!playerInfo) {
    return <CodeEntryForm onSubmit={handleCodeEntry} />;
  }

  const tabs = [
    { id: 'nickname', label: 'Nickname', icon: '✦' },
    { id: 'pins', label: 'Pins', icon: '⚐' },
    { id: 'tags', label: 'Tags', icon: '🏷' },
    { id: 'joinMessages', label: 'Join Messages', icon: '✉' }
  ];

  return (
    <div className="min-h-screen p-4 md:p-8 flex justify-center">
      <div className="w-full max-w-6xl space-y-6">
        
        {/* Header & Tabs */}
        <div className="birdflop-panel p-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-full bg-gradient-to-br from-[#FF0080] to-[#8000FF] flex items-center justify-center text-xl shadow-lg">
              {playerInfo.name[0].toUpperCase()}
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{playerInfo.name}</h2>
              <p className="text-xs text-[var(--text-secondary)] uppercase tracking-tighter">Persona Identity Designer</p>
            </div>
          </div>

          <div className="flex bg-black/40 rounded-xl p-1 p-1">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
                  activeTab === tab.id 
                    ? 'bg-[var(--accent-blue)] text-white shadow-lg' 
                    : 'text-[var(--text-secondary)] hover:text-white'
                }`}
              >
                <span className="text-lg leading-none">{tab.icon}</span>
                <span className="hidden sm:inline">{tab.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Content Area */}
        <div className="min-h-[600px]">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[var(--accent-blue)]"></div>
            </div>
          ) : (
            <>
              {activeTab === 'nickname' && (
                <GradientGenerator
                  playerInfo={playerInfo}
                  currentNickname={personaData?.nickname}
                  initialText={personaData?.nickname ? stripTags(personaData.nickname) : playerInfo.name}
                />
              )}

              {activeTab === 'pins' && (
                <div className="birdflop-panel">
                   <div className="p-6 border-b border-[var(--border-color)]">
                    <h3 className="text-xl font-bold text-white">Select Your Pin</h3>
                    <p className="text-sm text-[var(--text-secondary)]">Pins appear next to your name in chat.</p>
                  </div>
                  <PersonaSelector 
                    type="Pin" 
                    items={personaData?.pins || []} 
                    onSelect={(id) => handleSaveSelection('selectedPin', id)} 
                    apiBase={playerInfo.apiBase}
                  />
                </div>
              )}

              {activeTab === 'tags' && (
                <div className="birdflop-panel">
                  <div className="p-6 border-b border-[var(--border-color)]">
                    <h3 className="text-xl font-bold text-white">Select Your Tag</h3>
                    <p className="text-sm text-[var(--text-secondary)]">Tags appear as a prefix or suffix in chat.</p>
                  </div>
                  <PersonaSelector 
                    type="Tag" 
                    items={personaData?.tags || []} 
                    onSelect={(id) => handleSaveSelection('selectedTag', id)} 
                  />
                </div>
              )}

              {activeTab === 'joinMessages' && (
                <div className="birdflop-panel">
                  <div className="p-6 border-b border-[var(--border-color)]">
                    <h3 className="text-xl font-bold text-white">Select Join Message</h3>
                    <p className="text-sm text-[var(--text-secondary)]">Choose the message shown when you join the server.</p>
                  </div>
                  <PersonaSelector 
                    type="Join Message" 
                    items={personaData?.joinMessages || []} 
                    onSelect={(id) => handleSaveSelection('selectedJoinMessage', id)} 
                  />
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}


export default App;
