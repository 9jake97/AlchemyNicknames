import React from 'react';

export default function PersonaSelector({ items, type, onSelect }) {
  if (!items || items.length === 0) {
    return (
      <div className="text-center py-12 text-[var(--text-secondary)]">
        No {type} available.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
      {/* None Option */}
      <div 
        onClick={() => onSelect('')}
        className={`birdflop-panel p-4 cursor-pointer border-2 transition-all hover:scale-[1.02] ${
          items.every(i => !i.selected) ? 'border-[var(--accent-blue)]' : 'border-transparent'
        }`}
      >
        <div className="text-center font-bold text-white">None</div>
        <div className="text-center text-xs text-[var(--text-secondary)]">Clear your active {type}</div>
      </div>

      {items.map((item) => (
        <div 
          key={item.id}
          onClick={() => item.owned && onSelect(item.id)}
          className={`birdflop-panel p-4 cursor-pointer border-2 transition-all hover:scale-[1.02] ${
            item.selected ? 'border-[var(--accent-blue)]' : 'border-transparent'
          } ${!item.owned ? 'opacity-50 grayscale cursor-not-allowed' : ''}`}
        >
          <div className="flex justify-between items-start">
            <div>
              <div className="font-bold text-white text-lg">
                {item.unicode && <span className="mr-2">{item.unicode}</span>}
                {item.displayName || item.text || item.id}
              </div>
              <div className="text-xs text-[var(--text-secondary)]">
                {item.owned ? 'Owned' : 'Locked'}
              </div>
            </div>
            {item.selected && (
              <div className="bg-[var(--accent-blue)] text-white text-[10px] px-2 py-0.5 rounded-full uppercase font-bold">
                Active
              </div>
            )}
          </div>
          
          {type === 'Tag' && item.tag && (
            <div className="mt-3 p-2 bg-black/20 rounded font-mono text-sm overflow-hidden text-ellipsis whitespace-nowrap">
              {item.tag}
            </div>
          )}

          {type === 'Join Message' && item.text && (
            <div className="mt-3 p-2 bg-black/20 rounded text-sm italic text-[var(--text-secondary)]">
              "{item.text}"
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
