import React, { useState, useEffect } from 'react';
import type { PlayerGameweekReport } from '../types/fpl';
import { fetchGameweekAnalytics } from '../services/fplApi';
import {
    BarChart3,
    Search,
    Filter,
    Loader2,
    AlertCircle,
    ArrowUpRight,
    ArrowDownRight,
    Minus,
    Target,
    TrendingUp,
    Clock,
    Calendar,
    RotateCcw,
    ArrowUpDown
} from 'lucide-react';

/**
 * Valid tactical position filter values for the analytics table.
 */
const POSITION_FILTERS = ['ALL', 'GKP', 'DEF', 'MID', 'FWD'] as const;
type PositionFilter = (typeof POSITION_FILTERS)[number];

/**
 * Sortable numeric fields in the gameweek performance report table.
 */
type SortableField =
    | 'projectedXp'
    | 'actualPoints'
    | 'projectedMinutes'
    | 'actualMinutes'
    | 'variance';

/**
 * Analytics and model evaluation view comparing statistical projections against realized match points.
 *
 * Fetches player gameweek reports from the backend analytics endpoint, manages gameweek stepping,
 * supports text searching and position filtering, and computes client-side prediction variance (Delta).
 *
 * @returns The rendered Gameweek Analytics view.
 */
export const GameweekAnalyticsView: React.FC = () => {
    const [reports, setReports] = useState<PlayerGameweekReport[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Gameweek State (null resolves automatically to the upcoming gameweek in the backend)
    const [gameweek, setGameweek] = useState<number | null>(null);

    // Filter state
    const [searchQuery, setSearchQuery] = useState<string>('');
    const [selectedPosition, setSelectedPosition] = useState<PositionFilter>('ALL');

    // Sorting state
    const [sortField, setSortField] = useState<SortableField>('projectedXp');
    const [sortAsc, setSortAsc] = useState<boolean>(false);

    /**
     * Loads gameweek reports whenever the selected gameweek changes.
     */
    useEffect(() => {
        const loadData = async () => {
            setIsLoading(true);
            setErrorMessage(null);
            try {
                const data = await fetchGameweekAnalytics(gameweek ?? undefined);
                setReports(data || []);
            } catch (err: unknown) {
                console.error('Failed to load analytics:', err);
                const msg = err instanceof Error ? err.message : 'Failed to connect to analytics endpoint';
                setErrorMessage(msg);
                setReports([]);
            } finally {
                setIsLoading(false);
            }
        };
        loadData();
    }, [gameweek]);

    /**
     * Toggles sort direction or updates the active sort column.
     *
     * @param field - The column field to sort by.
     */
    const handleSort = (field: SortableField) => {
        if (sortField === field) {
            setSortAsc(!sortAsc);
        } else {
            setSortField(field);
            setSortAsc(false);
        }
    };

    /**
     * Computes the prediction error delta (Actual Points - Projected xP).
     *
     * @param item - The individual player report.
     * @returns The difference value, or null if match outcomes are not yet settled.
     */
    const calculateDelta = (item: PlayerGameweekReport): number | null => {
        if (item.actualPoints === null || item.actualPoints === undefined) {
            return null;
        }
        const xp = item.projectedXp ?? 0;
        return item.actualPoints - xp;
    };

    // Filter and sort reports
    const filteredAndSortedReports = reports
        .filter((p) => {
            const matchesSearch =
                p.webName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                p.teamShortName.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesPos = selectedPosition === 'ALL' || p.position === selectedPosition;
            return matchesSearch && matchesPos;
        })
        .sort((a, b) => {
            const factor = sortAsc ? 1 : -1;

            let valA: number = 0;
            let valB: number = 0;

            switch (sortField) {
                case 'projectedXp':
                    valA = a.projectedXp ?? 0;
                    valB = b.projectedXp ?? 0;
                    break;
                case 'actualPoints':
                    valA = a.actualPoints ?? -999;
                    valB = b.actualPoints ?? -999;
                    break;
                case 'projectedMinutes':
                    valA = a.projectedMinutes ?? 0;
                    valB = b.projectedMinutes ?? 0;
                    break;
                case 'actualMinutes':
                    valA = a.actualMinutes ?? -1;
                    valB = b.actualMinutes ?? -1;
                    break;
                case 'variance':
                    valA = calculateDelta(a) ?? -999;
                    valB = calculateDelta(b) ?? -999;
                    break;
            }

            return (valA - valB) * factor;
        });

    const finishedGamesCount = reports.filter(
        (r) => r.actualPoints !== null && r.actualPoints !== undefined
    ).length;

    const avgProjected = reports.length > 0
        ? (reports.reduce((acc, r) => acc + (r.projectedXp || 0), 0) / reports.length).toFixed(1)
        : '0.0';

    /**
     * Resolves the styling badge classes for a given tactical pitch position.
     *
     * @param pos - Tactical position abbreviation.
     * @returns Tailwind CSS class string.
     */
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
        <div className="space-y-6 pb-12 select-none">

            {/* 1. Header & Metric Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">

                {/* Card 1: Scope */}
                <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pl-purple border border-pl-pink/30 text-pl-green">
                            <Target className="h-5 w-5" />
                        </div>
                        <div>
                            <span className="block text-[10px] font-black uppercase tracking-wider text-slate-400 font-mono">
                                Model Scope
                            </span>
                            <span className="text-base font-black text-white font-mono">
                                {reports.length} Players Evaluated
                            </span>
                        </div>
                    </div>
                </div>

                {/* Card 2: Mean xP */}
                <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-pl-green/10 border border-pl-green/30 text-pl-green">
                            <TrendingUp className="h-5 w-5" />
                        </div>
                        <div>
                            <span className="block text-[10px] font-black uppercase tracking-wider text-slate-400 font-mono">
                                Mean Expected xP
                            </span>
                            <span className="text-base font-black text-pl-green font-mono">
                                {avgProjected} xP / Player
                            </span>
                        </div>
                    </div>
                </div>

                {/* Card 3: Fixture Status */}
                <div className="relative overflow-hidden rounded-2xl border border-panel-border bg-panel/75 p-4 shadow-xl backdrop-blur-md">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 border border-amber-500/30 text-pl-gold">
                            <Clock className="h-5 w-5" />
                        </div>
                        <div>
                            <span className="block text-[10px] font-black uppercase tracking-wider text-slate-400 font-mono">
                                Horizon Status
                            </span>
                            <span className="text-base font-black text-white font-mono">
                                {finishedGamesCount > 0 ? `${finishedGamesCount} Realized` : 'Upcoming Projections'}
                            </span>
                        </div>
                    </div>
                </div>

            </div>

            {/* 2. Main Analytics Panel */}
            <div className="relative overflow-hidden rounded-3xl border border-panel-border bg-gradient-to-b from-[#18082e] via-panel/85 to-[#0b0314] p-5 sm:p-6 shadow-2xl backdrop-blur-md">

                {/* Background Lineart */}
                <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.05] overflow-hidden rounded-3xl" aria-hidden="true">
                    <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 800 300" preserveAspectRatio="none">
                        <line x1="0" y1="50" x2="800" y2="50" strokeDasharray="4 6" />
                        <line x1="0" y1="150" x2="800" y2="150" strokeDasharray="4 6" />
                        <line x1="0" y1="250" x2="800" y2="250" strokeDasharray="4 6" />
                        <circle cx="700" cy="150" r="80" strokeWidth="1.2" strokeDasharray="3 4" />
                    </svg>
                </div>

                <div className="relative z-10 space-y-4">

                    {/* Header Bar with Gameweek Stepper */}
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-panel-border/70 pb-4">
                        <div>
                            <div className="flex items-center gap-2">
                                <BarChart3 className="h-4 w-4 text-pl-green" />
                                <h2 className="text-sm sm:text-base font-black tracking-tight text-white uppercase font-mono">
                                    Predictions vs Realized Outcomes
                                </h2>
                            </div>
                            <p className="text-[11px] sm:text-xs text-slate-400 font-mono mt-0.5">
                                Model Evaluation • Click Column Headers to Sort Metrics
                            </p>
                        </div>

                        {/* Gameweek Stepper */}
                        <div className="flex items-center gap-2 bg-canvas/80 border border-panel-border rounded-xl px-3 py-1.5 self-start sm:self-auto shadow-inner">
                            <Calendar className="h-3.5 w-3.5 text-pl-green" />
                            <span className="text-xs font-mono font-bold text-slate-300">GW:</span>
                            <button
                                onClick={() => setGameweek((prev) => Math.max(1, (prev ?? 1) - 1))}
                                className="h-6 w-6 rounded bg-slate-800 text-xs font-bold text-white hover:bg-slate-700 flex items-center justify-center cursor-pointer transition-colors"
                                title="Previous Gameweek"
                            >
                                -
                            </button>
                            <span className="text-xs sm:text-sm font-mono font-black text-white px-1 text-center min-w-[36px]">
                                {gameweek !== null ? gameweek : 'Next'}
                            </span>
                            <button
                                onClick={() => setGameweek((prev) => Math.min(38, (prev ?? 1) + 1))}
                                className="h-6 w-6 rounded bg-slate-800 text-xs font-bold text-white hover:bg-slate-700 flex items-center justify-center cursor-pointer transition-colors"
                                title="Next Gameweek"
                            >
                                +
                            </button>
                            {gameweek !== null && (
                                <button
                                    onClick={() => setGameweek(null)}
                                    title="Reset to Next Auto GW"
                                    className="ml-1 text-slate-400 hover:text-pl-green transition-colors cursor-pointer"
                                >
                                    <RotateCcw className="h-3.5 w-3.5" />
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Filters Bar: Search & Position Pills */}
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                        {/* Position Pills */}
                        <div className="flex items-center gap-1.5 flex-wrap">
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

                        {/* Quick Search */}
                        <div className="relative w-full sm:w-60">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="Search player or club..."
                                className="w-full rounded-xl border border-panel-border bg-canvas/90 pl-9 pr-3 py-1.5 text-xs font-mono text-white placeholder-slate-500 focus:border-pl-green focus:outline-none focus:ring-1 focus:ring-pl-green transition-all shadow-inner"
                            />
                        </div>
                    </div>

                    {/* Error Message Box */}
                    {errorMessage && (
                        <div className="flex items-center gap-3 rounded-xl border border-rose-500/40 bg-rose-950/40 p-3 text-xs text-rose-300 shadow-lg font-mono">
                            <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
                            <span>Server Error: {errorMessage}</span>
                        </div>
                    )}

                    {/* Report Data Table with Interactive Sorting */}
                    <div className="overflow-x-auto rounded-2xl border border-panel-border bg-canvas/60">
                        <table className="w-full text-left font-mono">
                            <thead>
                            <tr className="border-b border-panel-border/80 bg-slate-950/70 text-slate-300 text-[11px] sm:text-xs uppercase tracking-wider font-bold select-none">
                                <th className="py-2.5 sm:py-3 px-3.5 sm:px-4">Player</th>
                                <th className="py-2.5 sm:py-3 px-2 sm:px-2.5">Pos</th>

                                {/* Proj Mins Sort */}
                                <th
                                    onClick={() => handleSort('projectedMinutes')}
                                    className="py-2.5 sm:py-3 px-2 sm:px-3 text-right cursor-pointer hover:text-white transition-colors"
                                >
                                    <div className="flex items-center justify-end gap-1">
                                        <span>xMins</span>
                                        <ArrowUpDown className={`h-3 w-3 ${sortField === 'projectedMinutes' ? 'text-cyan-400' : 'text-slate-500'}`} />
                                    </div>
                                </th>

                                {/* Actual Mins Sort */}
                                <th
                                    onClick={() => handleSort('actualMinutes')}
                                    className="py-2.5 sm:py-3 px-2 sm:px-3 text-right cursor-pointer hover:text-white transition-colors"
                                >
                                    <div className="flex items-center justify-end gap-1">
                                        <span>Mins</span>
                                        <ArrowUpDown className={`h-3 w-3 ${sortField === 'actualMinutes' ? 'text-pl-green' : 'text-slate-500'}`} />
                                    </div>
                                </th>

                                {/* Projected xP Sort */}
                                <th
                                    onClick={() => handleSort('projectedXp')}
                                    className="py-2.5 sm:py-3 px-2 sm:px-3 text-right cursor-pointer hover:text-white transition-colors"
                                >
                                    <div className="flex items-center justify-end gap-1">
                                        <span>Proj xP</span>
                                        <ArrowUpDown className={`h-3 w-3 ${sortField === 'projectedXp' ? 'text-pl-green' : 'text-slate-500'}`} />
                                    </div>
                                </th>

                                {/* Actual Pts Sort */}
                                <th
                                    onClick={() => handleSort('actualPoints')}
                                    className="py-2.5 sm:py-3 px-2 sm:px-3 text-right cursor-pointer hover:text-white transition-colors"
                                >
                                    <div className="flex items-center justify-end gap-1">
                                        <span>Pts</span>
                                        <ArrowUpDown className={`h-3 w-3 ${sortField === 'actualPoints' ? 'text-pl-green' : 'text-slate-500'}`} />
                                    </div>
                                </th>

                                {/* Variance Delta Sort */}
                                <th
                                    onClick={() => handleSort('variance')}
                                    className="py-2.5 sm:py-3 px-3.5 sm:px-4 text-right cursor-pointer hover:text-white transition-colors"
                                >
                                    <div className="flex items-center justify-end gap-1">
                                        <span>Variance (&Delta;)</span>
                                        <ArrowUpDown className={`h-3 w-3 ${sortField === 'variance' ? 'text-pl-pink' : 'text-slate-500'}`} />
                                    </div>
                                </th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-panel-border/40 text-xs sm:text-[13px]">
                            {isLoading ? (
                                <tr>
                                    <td colSpan={7} className="py-12 text-center text-slate-400">
                                        <Loader2 className="h-6 w-6 animate-spin mx-auto text-pl-green mb-2" />
                                        <span className="text-xs font-bold">Querying Gameweek Performance Reports...</span>
                                    </td>
                                </tr>
                            ) : filteredAndSortedReports.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-12 text-center text-slate-400">
                                        No player reports available for this gameweek horizon.
                                    </td>
                                </tr>
                            ) : (
                                filteredAndSortedReports.map((p, idx) => {
                                    const diff = calculateDelta(p);
                                    const hasActualPts = p.actualPoints !== null && p.actualPoints !== undefined;
                                    const hasActualMins = p.actualMinutes !== null && p.actualMinutes !== undefined;

                                    return (
                                        <tr key={p.fplId} className="group transition-colors hover:bg-panel/70">
                                            {/* Name & Club */}
                                            <td className="py-2.5 sm:py-3 px-3.5 sm:px-4">
                                                <div className="flex items-center gap-2.5">
                                                        <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-slate-900 border border-slate-700 text-[10px] font-black text-slate-300 font-mono">
                                                            {idx + 1}
                                                        </span>
                                                    <div>
                                                            <span className="block font-bold text-white text-xs sm:text-[13px] group-hover:text-pl-green transition-colors">
                                                                {p.webName}
                                                            </span>
                                                        <span className="block text-[10px] text-slate-400">
                                                                {p.teamShortName}
                                                            </span>
                                                    </div>
                                                </div>
                                            </td>

                                            {/* Position */}
                                            <td className="py-2.5 sm:py-3 px-2 sm:px-2.5">
                                                    <span className={`px-2 py-0.5 rounded text-[9.5px] font-black border uppercase tracking-wider ${getPositionBadge(p.position)}`}>
                                                        {p.position}
                                                    </span>
                                            </td>

                                            {/* Projected Minutes */}
                                            <td className="py-2.5 sm:py-3 px-2 sm:px-3 text-right font-medium text-cyan-300">
                                                {p.projectedMinutes !== null && p.projectedMinutes !== undefined
                                                    ? `${Math.round(p.projectedMinutes)}'`
                                                    : '-'}
                                            </td>

                                            {/* Actual Minutes */}
                                            <td className="py-2.5 sm:py-3 px-2 sm:px-3 text-right text-slate-300 font-medium">
                                                {hasActualMins ? `${p.actualMinutes}'` : '-'}
                                            </td>

                                            {/* Projected xP */}
                                            <td className="py-2.5 sm:py-3 px-2 sm:px-3 text-right font-black text-white">
                                                {p.projectedXp?.toFixed(1) ?? '0.0'}
                                            </td>

                                            {/* Actual Points */}
                                            <td className="py-2.5 sm:py-3 px-2 sm:px-3 text-right font-black">
                                                {hasActualPts ? (
                                                    <span className="text-white bg-slate-900 px-2 py-0.5 rounded border border-slate-700">
                                                            {p.actualPoints}
                                                        </span>
                                                ) : (
                                                    <span className="text-slate-500 text-[11px]">-</span>
                                                )}
                                            </td>

                                            {/* Variance Delta (Frontend Calculated) */}
                                            <td className="py-2.5 sm:py-3 px-3.5 sm:px-4 text-right">
                                                {diff !== null ? (
                                                    <div className="inline-flex items-center gap-1 font-black text-xs">
                                                        {diff > 0.5 ? (
                                                            <span className="flex items-center text-pl-green">
                                                                    <ArrowUpRight className="h-3.5 w-3.5" />
                                                                    +{diff.toFixed(1)}
                                                                </span>
                                                        ) : diff < -0.5 ? (
                                                            <span className="flex items-center text-rose-400">
                                                                    <ArrowDownRight className="h-3.5 w-3.5" />
                                                                {diff.toFixed(1)}
                                                                </span>
                                                        ) : (
                                                            <span className="flex items-center text-slate-400">
                                                                    <Minus className="h-3 w-3" />
                                                                {diff.toFixed(1)}
                                                                </span>
                                                        )}
                                                    </div>
                                                ) : (
                                                    <span className="text-slate-600 text-[10px] font-mono">Pending</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                            </tbody>
                        </table>
                    </div>

                </div>
            </div>

        </div>
    );
};