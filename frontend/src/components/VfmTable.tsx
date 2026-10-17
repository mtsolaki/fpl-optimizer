import React, { useState, useEffect } from 'react';
import type {VfmPlayer} from '../types/fpl';
import { fetchVfmPlayers } from '../services/fplApi';
import {
    Search,
    Filter,
    ArrowUpDown,
    Loader2,
    Sparkles,
    DollarSign,
    AlertCircle,
    RotateCcw
} from 'lucide-react';

const POSITION_FILTERS = ['ALL', 'GKP', 'DEF', 'MID', 'FWD'] as const;
type PositionFilter = (typeof POSITION_FILTERS)[number];

export const VfmTable: React.FC = () => {
    const [players, setPlayers] = useState<VfmPlayer[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Filters State (No Gameweek manual inputs)
    const [searchQuery, setSearchQuery] = useState<string>('');
    const [selectedPosition, setSelectedPosition] = useState<PositionFilter>('ALL');
    const [maxPrice, setMaxPrice] = useState<number>(15.0);
    const [sortField, setSortField] = useState<'vfmScore' | 'projectedXp' | 'currentPrice'>('vfmScore');
    const [sortAsc, setSortAsc] = useState<boolean>(false);

    const normalizePrice = (val: number): number => {
        if (!val) return 0;
        return val > 100 ? val / 10 : val;
    };

    useEffect(() => {
        const loadData = async () => {
            setIsLoading(true);
            setErrorMessage(null);
            try {
                const data = await fetchVfmPlayers({
                    maxPrice: maxPrice * 10
                });
                setPlayers(data || []);
            } catch (err: unknown) {
                console.error('Failed to load VFM records:', err);
                const msg = err instanceof Error ? err.message : 'Could not connect to backend server';
                setErrorMessage(msg);
                setPlayers([]);
            } finally {
                setIsLoading(false);
            }
        };
        loadData();
    }, [maxPrice]);

    const handleSort = (field: 'vfmScore' | 'projectedXp' | 'currentPrice') => {
        if (sortField === field) {
            setSortAsc(!sortAsc);
        } else {
            setSortField(field);
            setSortAsc(false);
        }
    };

    const filteredPlayers = players
        .filter((p) => {
            const pPrice = normalizePrice(p.currentPrice);
            const matchesSearch =
                p.webName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                p.teamShortName.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesPos = selectedPosition === 'ALL' || p.position === selectedPosition;
            const matchesPrice = pPrice <= maxPrice;
            return matchesSearch && matchesPos && matchesPrice;
        })
        .sort((a, b) => {
            const factor = sortAsc ? 1 : -1;
            const valA = sortField === 'currentPrice' ? normalizePrice(a.currentPrice) : a[sortField];
            const valB = sortField === 'currentPrice' ? normalizePrice(b.currentPrice) : b[sortField];
            return (valA - valB) * factor;
        });

    const getPositionBadge = (pos: string) => {
        switch (pos) {
            case 'GKP':
                return 'bg-amber-400/15 text-amber-400 border-amber-400/30';
            case 'DEF':
                return 'bg-blue-500/15 text-blue-400 border-blue-500/30';
            case 'MID':
                return 'bg-pl-green/15 text-pl-green border-pl-green/30';
            case 'FWD':
                return 'bg-pl-pink/15 text-pl-pink border-pl-pink/30';
            default:
                return 'bg-slate-800 text-slate-300 border-panel-border';
        }
    };

    return (
        <div className="relative overflow-hidden rounded-3xl border border-panel-border bg-gradient-to-b from-[#18082e] via-panel/85 to-[#0b0314] p-5 sm:p-6 shadow-2xl backdrop-blur-md select-none">

            {/* Background Lineart */}
            <div
                className="pointer-events-none absolute inset-0 z-0 opacity-[0.06] overflow-hidden rounded-3xl [mask-image:linear-gradient(to_bottom,black_40%,transparent_100%)]"
                aria-hidden="true"
            >
                <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 800 400" preserveAspectRatio="none">
                    <line x1="50" y1="350" x2="750" y2="350" strokeWidth="1.5" />
                    <line x1="50" y1="50" x2="50" y2="350" strokeWidth="1.5" />
                    <line x1="50" y1="350" x2="750" y2="80" strokeWidth="1.2" strokeDasharray="6 6" />
                    <circle cx="200" cy="270" r="4" fill="white" />
                    <circle cx="320" cy="210" r="4" fill="white" />
                    <circle cx="450" cy="180" r="4" fill="white" />
                    <circle cx="580" cy="130" r="4" fill="white" />
                    <circle cx="680" cy="100" r="5" fill="#00ff87" />
                </svg>
            </div>

            <div className="relative z-10 space-y-4 sm:space-y-5">

                {/* Header Bar */}
                <div className="flex items-center justify-between border-b border-panel-border/70 pb-3.5">
                    <div>
                        <div className="flex items-center gap-2">
                            <Sparkles className="h-4 w-4 text-pl-green" />
                            <h2 className="text-sm sm:text-base font-black tracking-tight text-white uppercase font-mono">
                                Value For Money Market (VFM)
                            </h2>
                        </div>
                        <p className="text-[11px] sm:text-xs text-slate-400 font-mono mt-0.5">
                            Poisson Points Yield Per Million (£m) • Next Fixture Horizon
                        </p>
                    </div>
                </div>

                {/* Filters Controls Grid */}
                <div className="grid grid-cols-1 md:grid-cols-12 gap-3 sm:gap-4 items-center rounded-2xl bg-canvas/50 border border-panel-border p-3 sm:p-3.5">

                    {/* Quick Search */}
                    <div className="md:col-span-4 relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search player or club..."
                            className="w-full rounded-xl border border-panel-border bg-canvas/90 pl-9 pr-3 py-1.5 text-xs font-mono text-white placeholder-slate-500 focus:border-pl-green focus:outline-none focus:ring-1 focus:ring-pl-green transition-all shadow-inner"
                        />
                    </div>

                    {/* Position Pills */}
                    <div className="md:col-span-4 flex items-center gap-1.5 flex-wrap">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono flex items-center gap-1 mr-1">
              <Filter className="h-3 w-3 text-pl-green" /> Pos:
            </span>
                        {POSITION_FILTERS.map((pos) => (
                            <button
                                key={pos}
                                onClick={() => setSelectedPosition(pos)}
                                className={`rounded-lg px-2.5 py-1 text-xs font-mono font-bold transition-all cursor-pointer ${
                                    selectedPosition === pos
                                        ? 'bg-pl-green text-slate-950 shadow-[0_0_10px_rgba(0,255,135,0.4)]'
                                        : 'bg-canvas/80 text-slate-300 border border-panel-border hover:bg-canvas hover:text-white'
                                }`}
                            >
                                {pos}
                            </button>
                        ))}
                    </div>

                    {/* Max Price Slider */}
                    <div className="md:col-span-4 flex flex-col justify-center gap-1">
                        <div className="flex items-center justify-between text-[11px] font-mono">
              <span className="flex items-center gap-1 text-slate-400 font-bold uppercase tracking-tight">
                <DollarSign className="h-3.5 w-3.5 text-pl-pink" />
                Budget Cap:
              </span>
                            <div className="flex items-center gap-2">
                <span className="font-black text-white bg-slate-900 border border-slate-700 px-2 py-0.5 rounded text-xs">
                  ≤ £{maxPrice.toFixed(1)}m
                </span>
                                {maxPrice < 15.0 && (
                                    <button
                                        onClick={() => setMaxPrice(15.0)}
                                        title="Reset Price Filter"
                                        className="text-slate-400 hover:text-pl-green transition-colors cursor-pointer"
                                    >
                                        <RotateCcw className="h-3 w-3" />
                                    </button>
                                )}
                            </div>
                        </div>

                        <input
                            type="range"
                            min="4.0"
                            max="15.0"
                            step="0.5"
                            value={maxPrice}
                            onChange={(e) => setMaxPrice(Number(e.target.value))}
                            className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-[#00ff87]"
                        />
                    </div>

                </div>

                {/* Error Alert Box */}
                {errorMessage && (
                    <div className="flex items-center gap-3 rounded-xl border border-rose-500/40 bg-rose-950/40 p-3 text-xs text-rose-300 shadow-lg font-mono">
                        <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
                        <span>Server Error: {errorMessage}. Check if Spring Boot backend is running.</span>
                    </div>
                )}

                {/* Data Table */}
                <div className="overflow-x-auto rounded-2xl border border-panel-border bg-canvas/60">
                    <table className="w-full text-left font-mono">
                        <thead>
                        <tr className="border-b border-panel-border/80 bg-slate-950/70 text-slate-300 text-[11px] sm:text-xs uppercase tracking-wider font-bold">
                            <th className="py-2.5 sm:py-3 px-3.5 sm:px-4">Player</th>
                            <th className="py-2.5 sm:py-3 px-2.5 sm:px-3">Position</th>
                            <th
                                onClick={() => handleSort('currentPrice')}
                                className="py-2.5 sm:py-3 px-2.5 sm:px-3 cursor-pointer hover:text-white transition-colors"
                            >
                                <div className="flex items-center gap-1">
                                    <span>Price</span>
                                    <ArrowUpDown className="h-3 w-3" />
                                </div>
                            </th>
                            <th
                                onClick={() => handleSort('projectedXp')}
                                className="py-2.5 sm:py-3 px-2.5 sm:px-3 cursor-pointer hover:text-white transition-colors"
                            >
                                <div className="flex items-center gap-1">
                                    <span>Projected xP</span>
                                    <ArrowUpDown className="h-3 w-3" />
                                </div>
                            </th>
                            <th
                                onClick={() => handleSort('vfmScore')}
                                className="py-2.5 sm:py-3 px-3.5 sm:px-4 cursor-pointer hover:text-white transition-colors text-right"
                            >
                                <div className="flex items-center justify-end gap-1">
                                    <span>VFM Index (xP/£m)</span>
                                    <ArrowUpDown className="h-3 w-3 text-pl-green" />
                                </div>
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-panel-border/40 text-xs sm:text-[13px]">
                        {isLoading ? (
                            <tr>
                                <td colSpan={5} className="py-12 text-center text-slate-400">
                                    <Loader2 className="h-6 w-6 animate-spin mx-auto text-pl-green mb-2" />
                                    <span className="text-xs font-bold">Querying Poisson Market Yields for Next GW...</span>
                                </td>
                            </tr>
                        ) : filteredPlayers.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="py-12 text-center text-slate-400">
                                    <p className="font-bold text-white text-sm">No players found matching your criteria</p>
                                    <p className="text-[11px] text-slate-400 mt-1">
                                        Try increasing the Budget Cap (currently ≤ £{maxPrice.toFixed(1)}m).
                                    </p>
                                </td>
                            </tr>
                        ) : (
                            filteredPlayers.map((player, idx) => {
                                const normalizedPrice = normalizePrice(player.currentPrice);
                                const vfmRatio = (player.vfmScore || (player.projectedXp / normalizedPrice)).toFixed(2);

                                return (
                                    <tr
                                        key={player.fplId}
                                        className="group transition-colors hover:bg-panel/70"
                                    >
                                        {/* Name & Club */}
                                        <td className="py-2.5 sm:py-3 px-3.5 sm:px-4">
                                            <div className="flex items-center gap-2.5">
                          <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-slate-900 border border-slate-700 text-[10px] font-black text-slate-300 font-mono">
                            {idx + 1}
                          </span>
                                                <div>
                            <span className="block font-bold text-white text-xs sm:text-[13px] group-hover:text-pl-green transition-colors">
                              {player.webName}
                            </span>
                                                    <span className="block text-[10px] text-slate-400">
                              {player.teamShortName}
                            </span>
                                                </div>
                                            </div>
                                        </td>

                                        {/* Position */}
                                        <td className="py-2.5 sm:py-3 px-2.5 sm:px-3">
                        <span className={`px-2 py-0.5 rounded text-[9.5px] font-black border uppercase tracking-wider ${getPositionBadge(player.position)}`}>
                          {player.position}
                        </span>
                                        </td>

                                        {/* Price */}
                                        <td className="py-2.5 sm:py-3 px-2.5 sm:px-3 text-slate-200 font-medium text-xs sm:text-[13px]">
                                            £{normalizedPrice.toFixed(1)}m
                                        </td>

                                        {/* Projected xP */}
                                        <td className="py-2.5 sm:py-3 px-2.5 sm:px-3 font-bold text-white text-xs sm:text-[13px]">
                                            {player.projectedXp.toFixed(1)}
                                        </td>

                                        {/* VFM Ratio */}
                                        <td className="py-2.5 sm:py-3 px-3.5 sm:px-4 text-right">
                                            <div className="flex flex-col items-end">
                          <span className="font-black text-xs sm:text-sm text-pl-green drop-shadow-[0_0_8px_rgba(0,255,135,0.35)]">
                            {vfmRatio}
                          </span>
                                                <span className="text-[9px] text-slate-400">
                            pts per £1m
                          </span>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                        </tbody>
                    </table>
                </div>

            </div>

            <div className="absolute bottom-0 left-8 right-8 h-[1px] bg-gradient-to-r from-transparent via-pl-green/20 to-transparent" />
        </div>
    );
};