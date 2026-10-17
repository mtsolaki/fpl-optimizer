import React from 'react';
import type {LineupPlayer} from '../types/fpl';
import { PlayerCard } from './PlayerCard';

interface BenchProps {
    bench: LineupPlayer[];
}

export const Bench: React.FC<BenchProps> = ({ bench }) => {
    const sortedBench = [...bench].sort((a, b) => {
        if (a.position === 'GKP') return -1;
        if (b.position === 'GKP') return 1;
        return a.benchOrder - b.benchOrder;
    });

    return (
        // relative z-30: Renders cleanly above pitch starters
        <div className="relative z-30 mx-auto w-full rounded-3xl border border-panel-border bg-gradient-to-b from-[#16072b] via-panel/85 to-[#0b0314] p-4 sm:p-5 shadow-2xl backdrop-blur-md select-none">

            {/* Background Dugout Shelter Architecture Lineart */}
            <div
                className="pointer-events-none absolute inset-0 z-0 opacity-[0.06] overflow-hidden rounded-3xl [mask-image:linear-gradient(to_bottom,black_30%,transparent_100%)]"
                aria-hidden="true"
            >
                <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 600 120" preserveAspectRatio="none">
                    <path d="M20 110 V30 Q20 15 35 15 H565 Q580 15 580 30 V110" strokeWidth="1.2" strokeDasharray="6 4" />
                    <line x1="20" y1="40" x2="580" y2="40" strokeWidth="0.8" opacity="0.6" />
                    <g strokeWidth="0.9" opacity="0.7">
                        <rect x="40" y="45" width="115" height="65" rx="6" />
                        <rect x="175" y="45" width="115" height="65" rx="6" />
                        <rect x="310" y="45" width="115" height="65" rx="6" />
                        <rect x="445" y="45" width="115" height="65" rx="6" />
                    </g>
                </svg>
            </div>

            {/* Dugout Bar Header */}
            <div className="relative z-10 mb-3.5 flex items-center justify-between border-b border-panel-border/70 pb-2.5 px-1">
                <div className="flex items-center gap-2">
                    <span className="h-2 w-2 rounded-full bg-pl-green shadow-[0_0_8px_#00ff87]" />
                    <span className="text-[11px] font-black uppercase tracking-widest text-slate-100 font-mono">
            Substitutes Dugout
          </span>
                </div>
                <span className="text-[10px] font-mono text-slate-400">
          Auto-Sub Priority (1 → 3)
        </span>
            </div>

            {/* 4 Dugout Pods */}
            <div className="relative z-20 grid grid-cols-4 gap-2 sm:gap-3.5">
                {sortedBench.map((player, index) => {
                    const isGkp = player.position === 'GKP';
                    const subBadge = isGkp ? 'GK' : `SUB ${index}`;

                    return (
                        <div
                            key={player.fplId}
                            className="group/pod relative flex flex-col items-center rounded-2xl border border-panel-border/70 bg-canvas/50 p-2 sm:p-2.5 transition-all duration-200 hover:border-slate-600 hover:bg-canvas/80"
                        >
                            {/* Dugout Pod Indicator Header */}
                            <div className="mb-2 flex w-full items-center justify-center">
                <span
                    className={`px-2 py-0.5 rounded text-[9px] font-mono font-black tracking-wider uppercase shadow-xs ${
                        isGkp
                            ? 'bg-pl-green/15 text-pl-green border border-pl-green/30'
                            : 'bg-slate-900 text-slate-300 border border-panel-border'
                    }`}
                >
                  {subBadge}
                </span>
                            </div>

                            {/* Player Card */}
                            <PlayerCard player={player} isBench={true} />
                        </div>
                    );
                })}
            </div>

            {/* Subtle bottom edge ombre glow */}
            <div className="absolute bottom-0 left-8 right-8 h-[1px] bg-gradient-to-r from-transparent via-pl-green/20 to-transparent" />
        </div>
    );
};