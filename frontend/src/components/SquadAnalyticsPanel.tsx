import React from 'react';
import type {UserLineupResponse} from '../types/fpl';
import { PieChart, AlertTriangle, Flame, Layers } from 'lucide-react';

interface SquadAnalyticsPanelProps {
    lineup: UserLineupResponse;
}

export const SquadAnalyticsPanel: React.FC<SquadAnalyticsPanelProps> = ({ lineup }) => {
    const allPlayers = [...lineup.starters, ...lineup.bench];

    const getPosCost = (pos: string) => {
        return allPlayers
            .filter((p) => p.position === pos)
            .reduce((acc, p) => acc + (p.currentPrice > 100 ? p.currentPrice / 10 : p.currentPrice), 0);
    };

    const gkpCost = getPosCost('GKP');
    const defCost = getPosCost('DEF');
    const midCost = getPosCost('MID');
    const fwdCost = getPosCost('FWD');
    const totalSquadCost = gkpCost + defCost + midCost + fwdCost;

    const costBreakdown = [
        { label: 'DEF', cost: defCost, pct: (defCost / totalSquadCost) * 100, color: 'bg-blue-500' },
        { label: 'MID', cost: midCost, pct: (midCost / totalSquadCost) * 100, color: 'bg-emerald-400' },
        { label: 'FWD', cost: fwdCost, pct: (fwdCost / totalSquadCost) * 100, color: 'bg-pl-pink' },
        { label: 'GKP', cost: gkpCost, pct: (gkpCost / totalSquadCost) * 100, color: 'bg-amber-400' },
    ];

    const sortedStarters = [...lineup.starters].sort((a, b) => b.projectedXp - a.projectedXp);
    const top3Xp = sortedStarters.slice(0, 3).reduce((acc, p) => acc + p.projectedXp, 0);
    const concentrationRatio = ((top3Xp / lineup.totalProjectedXp) * 100).toFixed(0);

    const flaggedPlayers = allPlayers.filter((p) => p.status !== 'a');
    const avgStarterXp = (lineup.totalProjectedXp / 11).toFixed(1);

    return (
        <div className="space-y-4">

            {/* Widget 1: Budget Allocation with Subtle Radar Lineart */}
            <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] [mask-image:linear-gradient(to_bottom,black_40%,transparent)]">
                    <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 300 120" preserveAspectRatio="none">
                        <line x1="0" y1="20" x2="300" y2="20" strokeDasharray="3 4" strokeWidth="0.8" />
                        <line x1="0" y1="60" x2="300" y2="60" strokeDasharray="3 4" strokeWidth="0.8" />
                        <line x1="0" y1="100" x2="300" y2="100" strokeDasharray="3 4" strokeWidth="0.8" />
                        <circle cx="260" cy="60" r="45" strokeWidth="1" strokeDasharray="4 6" />
                        <circle cx="260" cy="60" r="25" strokeWidth="1" strokeDasharray="2 4" />
                    </svg>
                </div>

                <div className="relative z-10">
                    <div className="flex items-center justify-between border-b border-panel-border/60 pb-2.5 mb-3">
                        <div className="flex items-center gap-2 text-slate-200">
                            <PieChart className="h-4 w-4 text-pl-green" />
                            <span className="text-xs font-black uppercase tracking-wider font-mono">
                Budget Allocation
              </span>
                        </div>
                        <span className="text-[10px] font-mono text-slate-400">
              Total £{totalSquadCost.toFixed(1)}m
            </span>
                    </div>

                    <div className="flex h-2.5 w-full overflow-hidden rounded-full bg-slate-900 border border-slate-800 p-0.5 gap-0.5 mb-3">
                        {costBreakdown.map((item) => (
                            <div
                                key={item.label}
                                style={{ width: `${item.pct}%` }}
                                className={`h-full rounded-xs transition-all duration-300 ${item.color}`}
                                title={`${item.label}: £${item.cost.toFixed(1)}m (${item.pct.toFixed(0)}%)`}
                            />
                        ))}
                    </div>

                    <div className="grid grid-cols-4 gap-2 text-center font-mono">
                        {costBreakdown.map((item) => (
                            <div key={item.label} className="rounded-xl bg-canvas/70 border border-panel-border p-2">
                <span className="block text-[9px] font-bold text-slate-400 uppercase tracking-tight">
                  {item.label}
                </span>
                                <span className="block text-xs font-black text-white">
                  £{item.cost.toFixed(1)}m
                </span>
                                <span className="block text-[8.5px] text-slate-400">
                  {item.pct.toFixed(0)}%
                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Widget 2: Poisson Reliability with Curve Lineart */}
            <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.06] [mask-image:linear-gradient(to_bottom,black_40%,transparent)]">
                    <svg className="h-full w-full stroke-pl-pink fill-none" viewBox="0 0 300 120" preserveAspectRatio="none">
                        <path d="M20 100 Q100 95 140 40 T220 20 T280 90" strokeWidth="1.2" strokeDasharray="4 3" />
                        <circle cx="140" cy="40" r="3" />
                        <circle cx="220" cy="20" r="3" />
                    </svg>
                </div>

                <div className="relative z-10">
                    <div className="flex items-center justify-between border-b border-panel-border/60 pb-2.5 mb-3">
                        <div className="flex items-center gap-2 text-slate-200">
                            <Flame className="h-4 w-4 text-pl-pink" />
                            <span className="text-xs font-black uppercase tracking-wider font-mono">
                Poisson Reliability
              </span>
                        </div>
                        <span className="text-[10px] font-mono text-pl-green font-bold">
              Avg {avgStarterXp} xP / Player
            </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2.5">
                        <div className="rounded-xl bg-canvas/70 border border-panel-border p-3 flex flex-col justify-between">
                            <div>
                <span className="block text-[10px] font-bold text-slate-400 uppercase font-mono tracking-tight">
                  Top 3 xP Share
                </span>
                                <span className="text-base font-black text-white font-mono">
                  {concentrationRatio}%
                </span>
                            </div>
                            <span className="text-[9px] text-slate-400 mt-1">
                {Number(concentrationRatio) > 40 ? 'Heavy reliance on key stars' : 'Balanced team spread'}
              </span>
                        </div>

                        <div className="rounded-xl bg-canvas/70 border border-panel-border p-3 flex flex-col justify-between">
                            <div>
                <span className="block text-[10px] font-bold text-slate-400 uppercase font-mono tracking-tight">
                  Fitness Risk
                </span>
                                <div className="flex items-center gap-1.5 mt-0.5">
                                    {flaggedPlayers.length > 0 ? (
                                        <>
                                            <AlertTriangle className="h-4 w-4 text-amber-400 shrink-0" />
                                            <span className="text-xs font-bold text-amber-300 font-mono">
                        {flaggedPlayers.length} Flagged
                      </span>
                                        </>
                                    ) : (
                                        <span className="text-xs font-bold text-pl-green font-mono">
                      100% Available
                    </span>
                                    )}
                                </div>
                            </div>
                            <span className="text-[9px] text-slate-400 mt-1">
                {flaggedPlayers.length > 0
                    ? flaggedPlayers.map((p) => p.webName).join(', ')
                    : 'Zero injury/doubt concerns'}
              </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Widget 3: Key Projections with Pitch Grid Lineart */}
            <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] [mask-image:linear-gradient(to_bottom,black_40%,transparent)]">
                    <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 300 160" preserveAspectRatio="none">
                        <rect x="20" y="20" width="260" height="120" rx="6" strokeWidth="1" strokeDasharray="4 4" />
                        <circle cx="150" cy="80" r="30" strokeWidth="1" strokeDasharray="3 4" />
                    </svg>
                </div>

                <div className="relative z-10">
                    <div className="flex items-center justify-between border-b border-panel-border/60 pb-2.5 mb-3">
                        <div className="flex items-center gap-2 text-slate-200">
                            <Layers className="h-4 w-4 text-pl-gold" />
                            <span className="text-xs font-black uppercase tracking-wider font-mono">
                Key Matchup Projections
              </span>
                        </div>
                        <span className="text-[10px] font-mono text-slate-400">
              Top Expected Returns
            </span>
                    </div>

                    <div className="space-y-2">
                        {sortedStarters.slice(0, 4).map((p, idx) => (
                            <div
                                key={p.fplId}
                                className="flex items-center justify-between rounded-xl bg-canvas/60 border border-panel-border p-2.5 transition-colors hover:bg-canvas/90"
                            >
                                <div className="flex items-center gap-2.5 min-w-0">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-slate-900 border border-slate-700 text-[10px] font-black font-mono text-slate-300">
                    {idx + 1}
                  </span>
                                    <div className="min-w-0 truncate">
                    <span className="block truncate text-xs font-bold text-white">
                      {p.webName}
                    </span>
                                        <span className="block text-[9.5px] text-slate-400 font-mono">
                      {p.teamShortName} • {p.position}
                    </span>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2 font-mono shrink-0">
                  <span className="text-xs font-black text-pl-green drop-shadow-[0_0_6px_rgba(0,255,135,0.3)]">
                    {p.projectedXp.toFixed(1)} xP
                  </span>
                                    {p.isCaptain && (
                                        <span className="rounded bg-pl-gold/20 border border-pl-gold/40 px-1 text-[9px] font-extrabold text-pl-gold">
                      C
                    </span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

        </div>
    );
};