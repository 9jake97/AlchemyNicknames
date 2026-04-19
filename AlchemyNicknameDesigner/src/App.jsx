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

function App() {
  const [playerInfo, setPlayerInfo] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const player = params.get('player') || '';
    const token = params.get('token') || '';
    const apiBase = params.get('api') || '';
    if (player && token) {
      setPlayerInfo({ name: player, token, apiBase });
    }
  }, []);

  // No URL params — show code entry form
  if (!playerInfo) {
    return (
      <CodeEntryForm
        onSubmit={(name, code) => setPlayerInfo({ name, token: code, apiBase: '' })}
      />
    );
  }

  return (
    <div className="min-h-screen p-4 md:p-8 flex justify-center">
      <div className="w-full max-w-6xl">
        <GradientGenerator playerInfo={playerInfo} />
      </div>
    </div>
  );
}

export default App;
