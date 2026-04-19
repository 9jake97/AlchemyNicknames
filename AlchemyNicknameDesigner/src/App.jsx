import React, { useEffect, useState } from 'react';
import GradientGenerator from './components/GradientGenerator';

function App() {
  const [playerInfo, setPlayerInfo] = useState({ name: '', token: '', apiBase: '' });

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const player = params.get('player') || '';
    const token = params.get('token') || '';
    const apiBase = params.get('api') || '';
    if (player || token) {
      setPlayerInfo({ name: player, token: token, apiBase: apiBase });
    }
  }, []);

  if (!playerInfo.token || !playerInfo.name) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="birdflop-panel p-8 max-w-md w-full text-center space-y-6">
          <div className="flex justify-center">
            <div className="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center border border-red-500/20">
              <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
          </div>
          <div className="space-y-2">
            <h1 className="text-2xl font-bold text-white">Session Required</h1>
            <p className="text-[var(--text-secondary)]">
              This designer is only accessible via a direct link generated from the server.
            </p>
          </div>
          <div className="bg-[var(--bg-input)] p-4 rounded-md border border-[var(--border-color)]">
            <p className="text-sm font-mono text-[var(--accent-blue)]">/nicknameeditor</p>
            <p className="text-xs text-[var(--text-secondary)] mt-2">Run this command in-game to access your designer.</p>
          </div>
        </div>
      </div>
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
