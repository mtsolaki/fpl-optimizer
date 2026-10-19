import React, { useState } from 'react';
import type { LineupPlayer } from '../types/fpl';
import { TrendingUp, DollarSign, Activity, ShieldAlert } from 'lucide-react';

/**
 * Properties for the PlayerCard component.
 */
interface PlayerCardProps {
    /**
     * Lineup player record containing identity, projected xP, status, and role.
     */
    player: LineupPlayer;
    /**
     * Flag indicating whether the card is rendered inside the bench dugout.
     * Influences the orientation of the hover details popover.
     *
     * @defaultValue false
     */
    isBench?: boolean;
}

/**
 * Kit color configuration schema for SVG rendering.
 */
interface KitColorConfig {
    body: string;
    sleeves: string;
    cuff: string;
    trim: string;
    stripes?: string;
}

/**
 * Visual kit styling mapping for Premier League clubs.
 */
const CLUB_KITS: Record<string, KitColorConfig> = {
    ARS: { body: '#d0101b', sleeves: '#ffffff', cuff: '#d0101b', trim: '#ffffff' },
    ARSENAL: { body: '#d0101b', sleeves: '#ffffff', cuff: '#d0101b', trim: '#ffffff' },
    AVL: { body: '#670e36', sleeves: '#95bfe5', cuff: '#670e36', trim: '#95bfe5' },
    'ASTON VILLA': { body: '#670e36', sleeves: '#95bfe5', cuff: '#670e36', trim: '#95bfe5' },
    BOU: { body: '#c8102e', sleeves: '#1a1a1a', cuff: '#c8102e', trim: '#c8102e', stripes: '#1a1a1a' },
    BOURNEMOUTH: { body: '#c8102e', sleeves: '#1a1a1a', cuff: '#c8102e', trim: '#c8102e', stripes: '#1a1a1a' },
    BRE: { body: '#e30613', sleeves: '#ffffff', cuff: '#e30613', trim: '#e30613', stripes: '#ffffff' },
    BRENTFORD: { body: '#e30613', sleeves: '#ffffff', cuff: '#e30613', trim: '#e30613', stripes: '#ffffff' },
    BHA: { body: '#0057b8', sleeves: '#ffffff', cuff: '#ffcd00', trim: '#ffcd00', stripes: '#ffffff' },
    BRIGHTON: { body: '#0057b8', sleeves: '#ffffff', cuff: '#ffcd00', trim: '#ffcd00', stripes: '#ffffff' },
    CHE: { body: '#034694', sleeves: '#034694', cuff: '#ffffff', trim: '#ffffff' },
    CHELSEA: { body: '#034694', sleeves: '#034694', cuff: '#ffffff', trim: '#ffffff' },
    COV: { body: '#00a3e0', sleeves: '#00a3e0', cuff: '#ffffff', trim: '#ffffff' },
    COVENTRY: { body: '#00a3e0', sleeves: '#00a3e0', cuff: '#ffffff', trim: '#ffffff' },
    'COVENTRY CITY': { body: '#00a3e0', sleeves: '#00a3e0', cuff: '#ffffff', trim: '#ffffff' },
    CRY: { body: '#1b458f', sleeves: '#c4122e', cuff: '#ffcd00', trim: '#ffcd00', stripes: '#c4122e' },
    'CRYSTAL PALACE': { body: '#1b458f', sleeves: '#c4122e', cuff: '#ffcd00', trim: '#ffcd00', stripes: '#c4122e' },
    EVE: { body: '#003399', sleeves: '#003399', cuff: '#ffffff', trim: '#ffffff' },
    EVERTON: { body: '#003399', sleeves: '#003399', cuff: '#ffffff', trim: '#ffffff' },
    FUL: { body: '#ffffff', sleeves: '#ffffff', cuff: '#000000', trim: '#000000' },
    FULHAM: { body: '#ffffff', sleeves: '#ffffff', cuff: '#000000', trim: '#000000' },
    HUL: { body: '#f59700', sleeves: '#18181b', cuff: '#f59700', trim: '#18181b', stripes: '#18181b' },
    HULL: { body: '#f59700', sleeves: '#18181b', cuff: '#f59700', trim: '#18181b', stripes: '#18181b' },
    'HULL CITY': { body: '#f59700', sleeves: '#18181b', cuff: '#f59700', trim: '#18181b', stripes: '#18181b' },
    IPS: { body: '#004494', sleeves: '#004494', cuff: '#f15a24', trim: '#f15a24' },
    IPSWICH: { body: '#004494', sleeves: '#004494', cuff: '#f15a24', trim: '#f15a24' },
    'IPSWICH TOWN': { body: '#004494', sleeves: '#004494', cuff: '#f15a24', trim: '#f15a24' },
    LEE: { body: '#ffffff', sleeves: '#ffffff', cuff: '#004494', trim: '#ffcd00' },
    LEEDS: { body: '#ffffff', sleeves: '#ffffff', cuff: '#004494', trim: '#ffcd00' },
    'LEEDS UNITED': { body: '#ffffff', sleeves: '#ffffff', cuff: '#004494', trim: '#ffcd00' },
    LIV: { body: '#c8102e', sleeves: '#c8102e', cuff: '#f6eb61', trim: '#f6eb61' },
    LIVERPOOL: { body: '#c8102e', sleeves: '#c8102e', cuff: '#f6eb61', trim: '#f6eb61' },
    MCI: { body: '#6cabdd', sleeves: '#6cabdd', cuff: '#ffffff', trim: '#ffffff' },
    'MAN CITY': { body: '#6cabdd', sleeves: '#6cabdd', cuff: '#ffffff', trim: '#ffffff' },
    'MANCHESTER CITY': { body: '#6cabdd', sleeves: '#6cabdd', cuff: '#ffffff', trim: '#ffffff' },
    MUN: { body: '#da291c', sleeves: '#da291c', cuff: '#ffffff', trim: '#ffffff' },
    'MAN UTD': { body: '#da291c', sleeves: '#da291c', cuff: '#ffffff', trim: '#ffffff' },
    'MANCHESTER UNITED': { body: '#da291c', sleeves: '#da291c', cuff: '#ffffff', trim: '#ffffff' },
    NEW: { body: '#18181b', sleeves: '#18181b', cuff: '#ffffff', trim: '#ffffff', stripes: '#ffffff' },
    NEWCASTLE: { body: '#18181b', sleeves: '#18181b', cuff: '#ffffff', trim: '#ffffff', stripes: '#ffffff' },
    NFO: { body: '#dd0000', sleeves: '#dd0000', cuff: '#ffffff', trim: '#ffffff' },
    FOREST: { body: '#dd0000', sleeves: '#dd0000', cuff: '#ffffff', trim: '#ffffff' },
    "NOTT'M FOREST": { body: '#dd0000', sleeves: '#dd0000', cuff: '#ffffff', trim: '#ffffff' },
    TOT: { body: '#ffffff', sleeves: '#ffffff', cuff: '#132257', trim: '#132257' },
    SPURS: { body: '#ffffff', sleeves: '#ffffff', cuff: '#132257', trim: '#132257' },
    TOTTENHAM: { body: '#ffffff', sleeves: '#ffffff', cuff: '#132257', trim: '#132257' },
    SUN: { body: '#eb172b', sleeves: '#ffffff', cuff: '#18181b', trim: '#18181b', stripes: '#ffffff' },
    SUNDERLAND: { body: '#eb172b', sleeves: '#ffffff', cuff: '#18181b', trim: '#18181b', stripes: '#ffffff' },
};

/**
 * Individual player pitch card rendering customized team jerseys, captaincy badges, and hover analytics.
 *
 * Renders an interactive jersey SVG using club color themes, displays captain/vice-captain armbands,
 * and reveals an analytical popover on hover displaying projected xP, cost, and injury status.
 *
 * @param props - The component properties.
 * @returns The rendered player card component.
 */
export const PlayerCard: React.FC<PlayerCardProps> = ({ player, isBench = false }) => {
    const [isHovered, setIsHovered] = useState(false);

    const teamCode = player.teamShortName?.trim().toUpperCase() || '';
    const kit = CLUB_KITS[teamCode] || {
        body: '#334155',
        sleeves: '#475569',
        cuff: '#ffffff',
        trim: '#ffffff',
    };

    const normalizedPrice = player.currentPrice > 100
        ? (player.currentPrice / 10).toFixed(1)
        : player.currentPrice.toFixed(1);
    const formattedPrice = `£${normalizedPrice}m`;

    const isAvailable = player.status === 'a';
    const isDoubtful = player.status === 'd';

    // Starting goalkeeper popover opens downwards to stay within pitch boundaries; all others open upwards
    const opensDownwards = player.position === 'GKP' && !isBench;
    const popoverPositionClass = opensDownwards ? 'top-full mt-2.5' : 'bottom-full mb-3';

    return (
        <div
            className={`relative flex flex-col items-center select-none w-24 sm:w-28 transition-all ${
                isHovered ? 'z-50' : 'z-10'
            }`}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            {/* 1. Tactical Hover Details Card */}
            {isHovered && (
                <div
                    className={`absolute left-1/2 -translate-x-1/2 ${popoverPositionClass} z-[100] w-60 rounded-2xl border border-slate-700/90 bg-slate-950/98 p-3.5 shadow-[0_25px_60px_rgba(0,0,0,0.95)] backdrop-blur-2xl animate-in fade-in zoom-in-95 duration-150 pointer-events-none ring-1 ring-white/15`}
                >
                    {/* Header Strip inside Popover */}
                    <div className="flex items-center gap-2 border-b border-panel-border/70 pb-2 mb-2.5">
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-canvas border border-slate-700 text-[10px] font-black font-mono text-pl-green">
                            {player.teamShortName}
                        </span>
                        <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-1.5">
                                <span className="truncate text-xs font-black text-white">
                                    {player.webName}
                                </span>
                                {player.isCaptain && (
                                    <span className="shrink-0 px-1 py-0.2 rounded bg-pl-gold text-slate-950 font-black text-[8.5px]">
                                        CAP
                                    </span>
                                )}
                                {player.isViceCaptain && (
                                    <span className="shrink-0 px-1 py-0.2 rounded bg-slate-200 text-slate-950 font-black text-[8.5px]">
                                        VC
                                    </span>
                                )}
                            </div>
                            <span className="block text-[9px] font-mono text-slate-400">
                                {player.position} • ID #{player.fplId}
                            </span>
                        </div>
                    </div>

                    {/* Quick Metrics Grid */}
                    <div className="grid grid-cols-2 gap-2 mb-2.5">
                        <div className="rounded-xl bg-canvas border border-panel-border p-2 text-center">
                            <div className="flex items-center justify-center gap-1 text-[8.5px] font-bold text-slate-400 uppercase font-mono mb-0.5">
                                <TrendingUp className="h-3 w-3 text-pl-green" />
                                <span>xP</span>
                            </div>
                            <span className="text-sm font-black text-pl-green font-mono">
                                {player.projectedXp.toFixed(2)}
                            </span>
                        </div>

                        <div className="rounded-xl bg-canvas border border-panel-border p-2 text-center">
                            <div className="flex items-center justify-center gap-1 text-[8.5px] font-bold text-slate-400 uppercase font-mono mb-0.5">
                                <DollarSign className="h-3 w-3 text-slate-300" />
                                <span>Cost</span>
                            </div>
                            <span className="text-sm font-black text-white font-mono">
                                £{normalizedPrice}m
                            </span>
                        </div>
                    </div>

                    {/* Status Details */}
                    <div className="space-y-1.5 text-[10px] font-mono">
                        <div className="flex items-center justify-between text-slate-300">
                            <span className="flex items-center gap-1 text-slate-400">
                                <Activity className="h-3 w-3 text-cyan-400" />
                                Status:
                            </span>
                            <span className={`font-bold ${isAvailable ? 'text-pl-green' : 'text-amber-400'}`}>
                                {isAvailable ? 'Available' : 'Flagged'}
                            </span>
                        </div>

                        {player.news && (
                            <div className="mt-1 flex items-start gap-1.5 rounded-lg bg-amber-500/10 border border-amber-500/20 p-1.5 text-[9px] text-amber-300 font-sans leading-tight">
                                <ShieldAlert className="h-3 w-3 shrink-0 text-amber-400 mt-0.5" />
                                <span>{player.news}</span>
                            </div>
                        )}
                    </div>

                    {/* Popover Arrow Indicator */}
                    {opensDownwards ? (
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 border-solid border-b-slate-950 border-b-8 border-x-transparent border-x-6 border-t-0" />
                    ) : (
                        <div className="absolute top-full left-1/2 -translate-x-1/2 border-solid border-t-slate-950 border-t-8 border-x-transparent border-x-6 border-b-0" />
                    )}
                </div>
            )}

            {/* 2. Injury Alert Dot */}
            {!isAvailable && (
                <span
                    title={player.news || 'Unavailable / Doubtful'}
                    className={`absolute -top-1 -right-1 z-20 h-3 w-3 rounded-full ring-2 ring-slate-950 shadow-md ${
                        isDoubtful ? 'bg-amber-400 animate-pulse' : 'bg-rose-500'
                    }`}
                />
            )}

            {/* 3. Wider Shirt SVG */}
            <div className="relative mb-1.5 flex items-center justify-center filter drop-shadow-[0_8px_12px_rgba(0,0,0,0.65)] transition-transform duration-200 group-hover:scale-105">
                <svg
                    viewBox="0 0 114 100"
                    className="h-16 w-18 sm:h-18 sm:w-20"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                >
                    <defs>
                        <linearGradient id={`kit-light-${player.fplId}`} x1="0" y1="0" x2="1" y2="1">
                            <stop offset="0%" stopColor="#ffffff" stopOpacity="0.22" />
                            <stop offset="50%" stopColor="#000000" stopOpacity="0" />
                            <stop offset="100%" stopColor="#000000" stopOpacity="0.25" />
                        </linearGradient>

                        <clipPath id={`torso-clip-${player.fplId}`}>
                            <path d="M30 18 C30 18 41 12 57 12 C73 12 84 18 84 18 L80 98 C80 99 78 100 76 100 L38 100 C36 100 34 99 34 98 Z" />
                        </clipPath>
                    </defs>

                    {/* Left Sleeve */}
                    <path
                        d="M31 19 L4 37 C2 39 2 42 4 45 L12 55 C14 57 18 57 20 54 L30 43 Z"
                        fill={kit.sleeves}
                        stroke="#111827"
                        strokeWidth="1.5"
                        strokeLinejoin="round"
                    />
                    <path d="M4 45 L12 55" stroke={kit.cuff} strokeWidth="3.2" strokeLinecap="round" />

                    {/* Right Sleeve */}
                    <path
                        d="M83 19 L110 37 C112 39 112 42 110 45 L102 55 C100 57 96 57 94 54 L84 43 Z"
                        fill={kit.sleeves}
                        stroke="#111827"
                        strokeWidth="1.5"
                        strokeLinejoin="round"
                    />
                    <path d="M110 45 L102 55" stroke={kit.cuff} strokeWidth="3.2" strokeLinecap="round" />

                    {/* Torso */}
                    <path
                        d="M30 18 C30 18 41 12 57 12 C73 12 84 18 84 18 L80 98 C80 99 78 100 76 100 L38 100 C36 100 34 99 34 98 Z"
                        fill={kit.body}
                        stroke="#111827"
                        strokeWidth="1.6"
                        strokeLinejoin="round"
                    />

                    {kit.stripes && (
                        <g clipPath={`url(#torso-clip-${player.fplId})`} opacity="0.95">
                            <rect x="42" y="10" width="8" height="92" fill={kit.stripes} />
                            <rect x="64" y="10" width="8" height="92" fill={kit.stripes} />
                        </g>
                    )}

                    <path
                        d="M30 18 C30 18 41 12 57 12 C73 12 84 18 84 18 L80 98 C80 99 78 100 76 100 L38 100 C36 100 34 99 34 98 Z"
                        fill={`url(#kit-light-${player.fplId})`}
                    />

                    <path d="M31 20 L43 31" stroke="#ffffff" strokeWidth="0.8" opacity="0.4" />
                    <path d="M83 20 L71 31" stroke="#ffffff" strokeWidth="0.8" opacity="0.4" />
                    <path d="M36 42 L37 94" stroke="#ffffff" strokeWidth="0.7" opacity="0.22" strokeDasharray="3 3" />
                    <path d="M78 42 L77 94" stroke="#ffffff" strokeWidth="0.7" opacity="0.22" strokeDasharray="3 3" />
                    <line x1="36" y1="97" x2="78" y2="97" stroke="#ffffff" strokeWidth="0.8" opacity="0.25" />

                    <path d="M45 13 C49 22 65 22 69 13" stroke={kit.trim} strokeWidth="3.2" strokeLinecap="round" fill="none" />
                </svg>

                <span className="absolute bottom-1 text-[8.5px] font-black uppercase tracking-wider text-white drop-shadow-[0_1px_3px_rgba(0,0,0,0.9)] opacity-85 font-mono">
                    {player.teamShortName}
                </span>
            </div>

            {/* 4. Capsule with Bold Visible Captaincy */}
            <div className="w-full rounded-xl bg-gradient-to-b from-slate-900/95 via-zinc-900/90 to-black/95 border border-slate-700/60 shadow-xl backdrop-blur-md p-1 sm:p-1.5 flex flex-col items-center">

                {/* Name Row */}
                <div className="w-full flex items-center justify-between gap-1 px-1">
                    <span className="truncate text-left font-extrabold tracking-tight text-white text-[11px] sm:text-xs flex-1">
                        {player.webName}
                    </span>

                    {player.isCaptain && (
                        <span
                            title="Team Captain (2x Points)"
                            className="shrink-0 flex h-4.5 w-4.5 items-center justify-center rounded-md bg-pl-gold text-slate-950 font-black text-[10px] shadow-[0_0_8px_rgba(234,179,8,0.8)] border border-amber-200"
                        >
                            C
                        </span>
                    )}

                    {player.isViceCaptain && (
                        <span
                            title="Vice-Captain"
                            className="shrink-0 flex h-4.5 w-4.5 items-center justify-center rounded-md bg-slate-200 text-slate-950 font-black text-[10px] shadow-sm border border-white"
                        >
                            V
                        </span>
                    )}
                </div>

                <div className="w-full h-[1px] bg-gradient-to-r from-transparent via-pl-green/40 to-transparent my-1" />

                {/* Metrics Row: xP & Price */}
                <div className="w-full flex items-center justify-between px-1 font-mono">
                    <div className="flex items-baseline gap-1">
                        <span className="text-[8.5px] uppercase tracking-wider text-slate-400 font-bold font-sans">
                            xP
                        </span>
                        <span className="font-black text-pl-green text-[11px] sm:text-[12px] drop-shadow-[0_0_8px_rgba(0,255,135,0.45)]">
                            {player.projectedXp.toFixed(1)}
                        </span>
                    </div>

                    <span className="text-[10px] sm:text-[10.5px] font-semibold text-slate-300">
                        {formattedPrice}
                    </span>
                </div>
            </div>

        </div>
    );
};