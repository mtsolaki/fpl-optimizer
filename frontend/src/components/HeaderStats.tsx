import React from 'react';
import type { UserLineupResponse } from '../types/fpl';
import { Shield, TrendingUp, Award, Wallet } from 'lucide-react';

/**
 * Properties for the HeaderStats component.
 */
interface HeaderStatsProps {
    /**
     * Lineup response payload containing formation, total xP, captains, and finances.
     */
    lineup: UserLineupResponse;
}

/**
 * Executive dashboard overview tile component summarizing squad lineup metrics.
 *
 * Displays the optimal formation structure, total projected expected points (including captaincy bonus),
 * designated captain / vice-captain names, and financial metrics (team value and remaining bank budget).
 *
 * @param props - The component properties.
 * @returns The rendered executive statistics bar.
 */
export const HeaderStats: React.FC<HeaderStatsProps> = ({ lineup }) => {
    /**
     * Normalizes and formats financial values into standard millions notation (e.g. £100.2m).
     *
     * @param val - Financial value (either in millions or tenths of millions).
     * @returns Formatted currency string.
     */
    const formatPriceValue = (val: number): string => {
        const normalized = val > 1000 ? (val / 10).toFixed(1) : val.toFixed(1);
        return `£${normalized}m`;
    };

    return (
        <div className="w-full rounded-2xl border border-panel-border bg-panel/75 p-3.5 shadow-xl backdrop-blur-md">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">

                {/* Stat 1: Formation Tile */}
                <div className="relative overflow-hidden flex items-center gap-3 rounded-xl bg-canvas/70 border border-panel-border p-3">
                    {/* Subtle Lineart Watermark */}
                    <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] [mask-image:linear-gradient(to_left,black,transparent)]">
                        <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 100 100" preserveAspectRatio="none">
                            <circle cx="50" cy="50" r="38" strokeWidth="1.2" strokeDasharray="3 3" />
                            <line x1="12" y1="50" x2="88" y2="50" strokeWidth="1" strokeDasharray="2 4" />
                            <rect x="25" y="20" width="50" height="60" rx="4" strokeWidth="1.2" />
                        </svg>
                    </div>

                    <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pl-purple border border-pl-pink/30 text-pl-green">
                        <Shield className="h-5 w-5" />
                    </div>
                    <div className="relative z-10">
                        <span className="block text-[10px] font-black tracking-wider text-slate-400 uppercase font-mono">
                            Formation
                        </span>
                        <span className="text-base font-extrabold text-white font-mono">
                            {lineup.formation}
                        </span>
                    </div>
                </div>

                {/* Stat 2: Total Projected xP Tile */}
                <div className="relative overflow-hidden flex items-center gap-3 rounded-xl bg-canvas/70 border border-panel-border p-3">
                    {/* Poisson Distribution Lineart */}
                    <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.06] [mask-image:linear-gradient(to_left,black,transparent)]">
                        <svg className="h-full w-full stroke-pl-green fill-none" viewBox="0 0 120 80" preserveAspectRatio="none">
                            <path d="M5 70 Q30 70 45 35 T70 15 T95 60 T115 70" strokeWidth="1.5" />
                            <line x1="5" y1="70" x2="115" y2="70" strokeWidth="1" strokeDasharray="3 3" />
                        </svg>
                    </div>

                    <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pl-green/10 border border-pl-green/30 text-pl-green shadow-[0_0_12px_rgba(0,255,135,0.15)]">
                        <TrendingUp className="h-5 w-5" />
                    </div>
                    <div className="relative z-10">
                        <span className="block text-[10px] font-black tracking-wider text-slate-400 uppercase font-mono">
                            Projected xP
                        </span>
                        <span className="text-base font-black text-pl-green font-mono drop-shadow-[0_0_8px_rgba(0,255,135,0.3)]">
                            {lineup.totalProjectedXp.toFixed(2)}
                        </span>
                    </div>
                </div>

                {/* Stat 3: Captain & Vice Captain */}
                <div className="relative overflow-hidden flex items-center gap-3 rounded-xl bg-canvas/70 border border-panel-border p-3 min-w-0">
                    {/* Captaincy Armband Lineart */}
                    <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] [mask-image:linear-gradient(to_left,black,transparent)]">
                        <svg className="h-full w-full stroke-amber-300 fill-none" viewBox="0 0 100 100" preserveAspectRatio="none">
                            <circle cx="50" cy="50" r="40" strokeWidth="1.5" />
                            <circle cx="50" cy="50" r="28" strokeWidth="1" strokeDasharray="4 4" />
                            <path d="M40 35 H60 M50 35 V65" strokeWidth="2" strokeLinecap="round" />
                        </svg>
                    </div>

                    <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 border border-amber-500/30 text-pl-gold">
                        <Award className="h-5 w-5" />
                    </div>
                    <div className="relative z-10 min-w-0 flex-1">
                        <span className="block text-[10px] font-black tracking-wider text-slate-400 uppercase font-mono">
                            Captain (C/V)
                        </span>
                        <span className="block truncate text-xs font-bold text-white">
                            <span className="text-pl-gold font-extrabold">{lineup.captain.webName}</span>
                            <span className="text-slate-400 font-normal"> / {lineup.viceCaptain.webName}</span>
                        </span>
                    </div>
                </div>

                {/* Stat 4: Squad Value & Bank */}
                <div className="relative overflow-hidden flex items-center gap-3 rounded-xl bg-canvas/70 border border-panel-border p-3">
                    {/* Financial Volume Lineart */}
                    <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] [mask-image:linear-gradient(to_left,black,transparent)]">
                        <svg className="h-full w-full stroke-pl-pink fill-none" viewBox="0 0 100 80" preserveAspectRatio="none">
                            <rect x="10" y="45" width="14" height="30" rx="1.5" strokeWidth="1.2" />
                            <rect x="32" y="30" width="14" height="45" rx="1.5" strokeWidth="1.2" />
                            <rect x="54" y="15" width="14" height="60" rx="1.5" strokeWidth="1.2" />
                            <rect x="76" y="5" width="14" height="70" rx="1.5" strokeWidth="1.2" />
                        </svg>
                    </div>

                    <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pl-pink/10 border border-pl-pink/30 text-pl-pink">
                        <Wallet className="h-5 w-5" />
                    </div>
                    <div className="relative z-10">
                        <span className="block text-[10px] font-black tracking-wider text-slate-400 uppercase font-mono">
                            Team Value (Bank)
                        </span>
                        <span className="text-xs font-bold text-white font-mono">
                            {formatPriceValue(lineup.teamValue)}{' '}
                            <span className="text-pl-green font-medium">({formatPriceValue(lineup.bank)})</span>
                        </span>
                    </div>
                </div>

            </div>
        </div>
    );
};