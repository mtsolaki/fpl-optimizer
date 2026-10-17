import React, { useEffect } from 'react';
import type {LineupPlayer} from '../types/fpl';
import { X, TrendingUp, DollarSign, ShieldAlert, Award, Activity } from 'lucide-react';

interface PlayerModalProps {
    player: LineupPlayer;
    onClose: () => void;
}

export const PlayerModal: React.FC<PlayerModalProps> = ({ player, onClose }) => {
    // Close with Escape key
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [onClose]);

    const normalizedPrice = player.currentPrice > 100
        ? (player.currentPrice / 10).toFixed(1)
        : player.currentPrice.toFixed(1);

    const isAvailable = player.status === 'a';

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 select-none">

            {/* 1. Backdrop Overlay with Blur */}
            <div
                className="fixed inset-0 bg-canvas/80 backdrop-blur-md transition-opacity animate-in fade-in duration-200"
                onClick={onClose}
            />

            {/* 2. Tactical Modal Container */}
            <div className="relative w-full max-w-md overflow-hidden rounded-3xl border border-panel-border bg-gradient-to-b from-[#18082e] via-panel to-[#0d021a] p-6 shadow-2xl z-10 animate-in zoom-in-95 duration-200">

                {/* Background Lineart Watermark */}
                <div className="pointer-events-none absolute inset-0 z-0 opacity-10 [mask-image:linear-gradient(to_bottom,black_40%,transparent_100%)]">
                    <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 300 200">
                        <circle cx="150" cy="100" r="70" strokeWidth="1.2" strokeDasharray="4 4" />
                        <path d="M50 30 L250 170 M50 170 L250 30" strokeWidth="0.8" strokeDasharray="3 5" />
                    </svg>
                </div>

                {/* Close Button */}
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 z-20 flex h-8 w-8 items-center justify-center rounded-full bg-canvas/80 border border-panel-border text-slate-400 hover:text-white hover:bg-canvas transition-colors"
                >
                    <X className="h-4 w-4" />
                </button>

                {/* Modal Content */}
                <div className="relative z-10 space-y-5">

                    {/* Header Strip: Name, Club & Position */}
                    <div className="flex items-center gap-3.5 border-b border-panel-border/70 pb-4">
                        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-900 border border-slate-700 text-pl-green font-black font-mono shadow-inner text-sm">
                            {player.teamShortName}
                        </div>

                        <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                                <h3 className="text-lg font-black text-white tracking-tight truncate">
                                    {player.webName}
                                </h3>
                                {player.isCaptain && (
                                    <span className="flex h-4.5 w-4.5 items-center justify-center rounded-full bg-pl-gold text-[9px] font-black text-slate-950 shadow-md">
                    C
                  </span>
                                )}
                                {player.isViceCaptain && (
                                    <span className="flex h-4.5 w-4.5 items-center justify-center rounded-full bg-slate-300 text-[9px] font-black text-slate-950 shadow-md">
                    V
                  </span>
                                )}
                            </div>
                            <div className="flex items-center gap-2 text-xs text-slate-400 font-mono mt-0.5">
                                <span className="font-bold text-slate-300">{player.position}</span>
                                <span>•</span>
                                <span>FPL ID #{player.fplId}</span>
                            </div>
                        </div>
                    </div>

                    {/* Core Metrics Grid */}
                    <div className="grid grid-cols-2 gap-3">

                        {/* Projected xP Card */}
                        <div className="rounded-2xl bg-canvas/70 border border-panel-border p-3.5">
                            <div className="flex items-center gap-1.5 text-slate-400 text-[10px] font-bold uppercase font-mono mb-1">
                                <TrendingUp className="h-3.5 w-3.5 text-pl-green" />
                                <span>Projected xP</span>
                            </div>
                            <span className="text-2xl font-black text-pl-green font-mono drop-shadow-[0_0_10px_rgba(0,255,135,0.35)]">
                {player.projectedXp.toFixed(2)}
              </span>
                            <span className="block text-[10px] text-slate-500 font-mono mt-0.5">
                Poisson Expected Points
              </span>
                        </div>

                        {/* Current Price Card */}
                        <div className="rounded-2xl bg-canvas/70 border border-panel-border p-3.5">
                            <div className="flex items-center gap-1.5 text-slate-400 text-[10px] font-bold uppercase font-mono mb-1">
                                <DollarSign className="h-3.5 w-3.5 text-slate-300" />
                                <span>Market Price</span>
                            </div>
                            <span className="text-2xl font-black text-white font-mono">
                £{normalizedPrice}m
              </span>
                            <span className="block text-[10px] text-slate-500 font-mono mt-0.5">
                Official FPL Value
              </span>
                        </div>

                    </div>

                    {/* Tactical Role & Fitness Status */}
                    <div className="space-y-2.5 rounded-2xl bg-canvas/50 border border-panel-border p-3.5 text-xs font-mono">
                        <div className="flex items-center justify-between">
              <span className="text-slate-400 flex items-center gap-1.5">
                <Award className="h-3.5 w-3.5 text-pl-gold" />
                Squad Priority:
              </span>
                            <span className="font-bold text-white">
                {player.benchOrder === 0 ? 'Starting XI Lineup' : `Bench Priority ${player.benchOrder}`}
              </span>
                        </div>

                        <div className="flex items-center justify-between">
              <span className="text-slate-400 flex items-center gap-1.5">
                <Activity className="h-3.5 w-3.5 text-cyan-400" />
                Player Availability:
              </span>
                            <span className={`font-bold ${isAvailable ? 'text-pl-green' : 'text-amber-400'}`}>
                {isAvailable ? 'Fully Available (100%)' : 'Fitness Concern / Flagged'}
              </span>
                        </div>

                        {player.news && (
                            <div className="flex items-start gap-2 rounded-xl bg-amber-500/10 border border-amber-500/20 p-2.5 text-[11px] text-amber-300 font-sans mt-2">
                                <ShieldAlert className="h-4 w-4 shrink-0 text-amber-400 mt-0.5" />
                                <span>{player.news}</span>
                            </div>
                        )}
                    </div>

                    {/* Action Button */}
                    <button
                        onClick={onClose}
                        className="w-full rounded-xl bg-panel-border hover:bg-slate-800 text-white font-mono text-xs font-bold py-2.5 transition-colors"
                    >
                        Dismiss Details
                    </button>

                </div>
            </div>
        </div>
    );
};