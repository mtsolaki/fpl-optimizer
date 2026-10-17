import React from 'react';
import type {LineupPlayer} from '../types/fpl';
import { PlayerCard } from './PlayerCard';

interface PitchProps {
    starters: LineupPlayer[];
}

export const Pitch: React.FC<PitchProps> = ({ starters }) => {
    const goalkeepers = starters.filter((p) => p.position === 'GKP');
    const defenders = starters.filter((p) => p.position === 'DEF');
    const midfielders = starters.filter((p) => p.position === 'MID');
    const forwards = starters.filter((p) => p.position === 'FWD');

    return (
        // relative z-10: Stays behind Bench z-30
        <div className="relative z-10 mx-auto w-full rounded-3xl border-4 border-[#18532c] bg-gradient-to-b from-[#0a863b] via-[#097b36] to-[#07682d] shadow-[0_20px_50px_rgba(0,0,0,0.85)] select-none">

            {/* 1. Lawn Stripes */}
            <div className="pointer-events-none absolute inset-0 flex opacity-20 overflow-hidden rounded-3xl">
                <div className="flex-1 bg-white" />
                <div className="flex-1 bg-transparent" />
                <div className="flex-1 bg-white" />
                <div className="flex-1 bg-transparent" />
                <div className="flex-1 bg-white" />
                <div className="flex-1 bg-transparent" />
            </div>

            {/* 2. Tactical Markings Lineart */}
            <div className="pointer-events-none absolute inset-0 z-0 opacity-45 overflow-hidden rounded-3xl" aria-hidden="true">
                <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 600 800" preserveAspectRatio="none">
                    <rect x="25" y="25" width="550" height="750" rx="4" strokeWidth="2.2" />
                    <line x1="25" y1="400" x2="575" y2="400" strokeWidth="2" />
                    <circle cx="300" cy="400" r="65" strokeWidth="2" />
                    <circle cx="300" cy="400" r="3.5" fill="white" />
                    <rect x="160" y="25" width="280" height="120" strokeWidth="1.8" />
                    <rect x="220" y="25" width="160" height="45" strokeWidth="1.5" />
                    <circle cx="300" cy="105" r="2.5" fill="white" />
                    <path d="M245 145 a55 55 0 0 0 110 0" strokeWidth="1.5" />
                    <rect x="160" y="655" width="280" height="120" strokeWidth="1.8" />
                    <rect x="220" y="730" width="160" height="45" strokeWidth="1.5" />
                    <circle cx="300" cy="695" r="2.5" fill="white" />
                    <path d="M245 655 a55 55 0 0 1 110 0" strokeWidth="1.5" />
                    <line x1="160" y1="25" x2="160" y2="775" strokeWidth="1" strokeDasharray="5 7" opacity="0.5" />
                    <line x1="440" y1="25" x2="440" y2="775" strokeWidth="1" strokeDasharray="5 7" opacity="0.5" />
                </svg>
            </div>

            {/* 3. Starters Grid */}
            <div className="relative z-10 flex min-h-[580px] flex-col justify-between py-6 px-2 sm:px-4">
                {/* Goalkeeper */}
                <div className="flex justify-center">
                    {goalkeepers.map((player) => (
                        <PlayerCard key={player.fplId} player={player} isBench={false} />
                    ))}
                </div>

                {/* Defenders */}
                <div className="flex justify-around gap-1 sm:gap-2">
                    {defenders.map((player) => (
                        <PlayerCard key={player.fplId} player={player} isBench={false} />
                    ))}
                </div>

                {/* Midfielders */}
                <div className="flex justify-around gap-1 sm:gap-2">
                    {midfielders.map((player) => (
                        <PlayerCard key={player.fplId} player={player} isBench={false} />
                    ))}
                </div>

                {/* Forwards */}
                <div className="flex justify-around gap-1 sm:gap-2">
                    {forwards.map((player) => (
                        <PlayerCard key={player.fplId} player={player} isBench={false} />
                    ))}
                </div>
            </div>

        </div>
    );
};