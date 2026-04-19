import React from 'react';

const ColorPicker = ({ color, onChange, onRemove, canRemove, index }) => {
  const isValidHex = /^#[0-9A-F]{0,6}$/i.test(color);

  return (
    <div className="flex flex-col gap-2 mb-3">
      <div className="flex items-center gap-2 text-sm text-[var(--text-secondary)] font-medium">
        <span>Color {index + 1}</span>
      </div>
      <div className="flex items-center gap-2">
        <div className="flex-1 flex items-center birdflop-input p-1 relative" style={{ height: '40px' }}>
          {/* Visual color box */}
          <div 
            className="rounded flex-shrink-0 border border-black/20" 
            style={{ backgroundColor: color, width: '32px', height: '32px' }} 
          />
          {/* Invisible color picker overlaid only on the color box */}
          <input
            type="color"
            value={isValidHex && color.length === 7 ? color : '#FFFFFF'}
            onChange={(e) => onChange(e.target.value)}
            className="absolute left-1 top-1 opacity-0 cursor-pointer z-10"
            style={{ width: '32px', height: '32px' }}
          />
          {/* Hex input */}
          <input
            type="text"
            value={color.toUpperCase()}
            onChange={(e) => {
              const val = e.target.value;
              if (/^#[0-9A-F]{0,6}$/i.test(val)) onChange(val);
            }}
            maxLength={7}
            className="bg-transparent text-sm font-medium w-full px-3 h-full outline-none"
          />
        </div>
        {canRemove && (
          <button
            onClick={onRemove}
            className="birdflop-btn-red p-2 rounded-md transition-colors w-10 h-[38px] flex items-center justify-center flex-shrink-0"
            title="Remove Color"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
          </button>
        )}
      </div>
    </div>
  );
};

export default ColorPicker;
