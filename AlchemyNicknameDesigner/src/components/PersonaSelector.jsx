import React, { useState } from 'react';
import MinecraftText from './MinecraftText';

export default function PersonaSelector({ items, type, onSelect, apiBase }) {
  const [filter, setFilter] = useState('unlocked'); // 'all', 'unlocked', 'locked'

  if (!items || items.length === 0) {
    return (
      <div className="text-center py-12 text-[var(--text-secondary)]">
        No {type} available.
      </div>
    );
  }

  const filteredItems = items.filter(item => {
    if (filter === 'unlocked') return item.owned;
    if (filter === 'locked') return !item.owned;
    return true;
  });

  return (
    <div className="flex flex-col h-full">
      {/* Sub-tabs */}
      <div className="flex gap-2 p-4 bg-black/20 border-b border-[var(--border-color)]">
        {[
          { id: 'unlocked', label: 'Unlocked' },
          { id: 'locked', label: 'Locked' },
          { id: 'all', label: 'All' }
        ].map(sub => (
          <button
            key={sub.id}
            onClick={() => setFilter(sub.id)}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              filter === sub.id 
                ? 'bg-[var(--bg-btn-hover)] text-white shadow-inner' 
                : 'text-[var(--text-secondary)] hover:text-white'
            }`}
          >
            {sub.label} ({sub.id === 'all' ? items.length : items.filter(i => sub.id === 'unlocked' ? i.owned : !i.owned).length})
          </button>
        ))}
      </div>

      {/* Grid / List */}
      <div className={`grid gap-4 p-4 ${type === 'Join Message' ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3'}`}>
        {/* None Option - Only show in 'Unlocked' or 'All' */}
        {filter !== 'locked' && (
          <div 
            onClick={() => onSelect('')}
            className={`birdflop-panel p-4 cursor-pointer border-2 transition-all hover:scale-[1.02] flex flex-col justify-center ${
              items.every(i => !i.selected) ? 'border-[var(--accent-blue)]' : 'border-transparent'
            } ${type === 'Join Message' ? 'max-w-md mx-auto w-full' : ''}`}
          >
            <div className="text-center font-bold text-white">None</div>
            <div className="text-center text-[10px] text-[var(--text-secondary)] uppercase">Clear active {type}</div>
          </div>
        )}

        {filteredItems.map((item) => (
          <div 
            key={item.id}
            onClick={() => item.owned && onSelect(item.id)}
            className={`birdflop-panel p-4 cursor-pointer border-2 transition-all hover:scale-[1.02] ${
              item.selected ? 'border-[var(--accent-blue)]' : 'border-transparent'
            } ${!item.owned ? 'opacity-50 grayscale cursor-not-allowed' : ''} ${type === 'Join Message' ? 'max-w-4xl mx-auto w-full' : ''}`}
          >
            <div className="flex justify-between items-start">
              <div className="min-w-0 flex-1">
                {type !== 'Join Message' && type !== 'Tag' && (
                  <div className="font-bold text-white text-lg flex items-center gap-3">
                    {item.imageUrl ? (
                      <div className="w-10 h-10 bg-black/20 rounded-lg p-1 flex items-center justify-center border border-white/5 shadow-inner">
                        <img 
                          src={`${apiBase || ''}${item.imageUrl}`} 
                          alt={item.id} 
                          className="max-w-full max-h-full object-contain pixelated"
                          onError={(e) => {
                            e.target.style.display = 'none';
                            e.target.nextSibling.style.display = 'block';
                          }}
                        />
                        <span className="hidden mc-text">{item.unicode}</span>
                      </div>
                    ) : (
                      item.unicode && <span className="mr-2 mc-text">{item.unicode}</span>
                    )}
                    <div className="truncate flex-1">
                      <MinecraftText text={item.displayName || item.text || item.id} />
                    </div>
                  </div>
                )}
              </div>
              {item.selected && (
                <div className="bg-[var(--accent-blue)] text-white text-[10px] px-2 py-0.5 rounded-full uppercase font-bold flex-shrink-0">
                  Active
                </div>
              )}
            </div>
            
            {type === 'Tag' && item.tag && (
              <div className="mt-2 p-3 bg-black/40 rounded border border-white/5 mc-text mc-shadow text-lg flex items-center gap-2 overflow-hidden text-ellipsis whitespace-nowrap">
                <div className="w-1.5 h-6 bg-[var(--accent-green)] rounded-full opacity-50"></div>
                <MinecraftText text={item.tag} />
              </div>
            )}

            {type === 'Join Message' && item.text && (
              <div className="mt-2 p-3 bg-black/40 rounded border border-white/5 mc-text mc-shadow text-lg flex items-center gap-2">
                <div className="w-1.5 h-6 bg-[var(--accent-blue)] rounded-full opacity-50"></div>
                <MinecraftText text={item.text} />
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

