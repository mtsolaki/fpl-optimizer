import React, { useState } from 'react';
import { Play, Sparkles, User, Loader2 } from 'lucide-react';

/**
 * Properties for the LineupControls component.
 */
interface LineupControlsProps {
    /**
     * Callback invoked with the parsed team ID when optimization is submitted.
     */
    onOptimize: (teamId: number) => void;
    /**
     * Flag indicating whether optimization is currently processing.
     */
    isLoading: boolean;
}

/**
 * Input toolbar component allowing managers to specify their FPL Team ID and trigger lineup optimization.
 *
 * @param props - The component properties.
 * @returns The rendered input controls panel.
 */
export const LineupControls: React.FC<LineupControlsProps> = ({ onOptimize, isLoading }) => {
    const [teamIdInput, setTeamIdInput] = useState<string>('');

    /**
     * Handles form submission, parses the integer ID, and dispatches the optimization callback.
     *
     * @param e - The form submission event.
     */
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const id = parseInt(teamIdInput.trim(), 10);
        if (!isNaN(id) && id > 0) {
            onOptimize(id);
        }
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="relative overflow-hidden rounded-2xl border border-panel-border bg-gradient-to-r from-panel/90 via-panel/75 to-panel/90 p-3.5 sm:p-4 shadow-xl backdrop-blur-md"
        >
            {/* Background Lineart Accent */}
            <div className="pointer-events-none absolute inset-0 z-0 opacity-[0.04]">
                <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 600 80">
                    <line x1="0" y1="40" x2="600" y2="40" strokeDasharray="4 6" />
                    <circle cx="300" cy="40" r="30" strokeDasharray="3 3" />
                </svg>
            </div>

            <div className="relative z-10 flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4">

                {/* Title & Status */}
                <div className="flex items-center gap-2.5 self-start sm:self-auto">
                    <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-pl-purple border border-pl-pink/30 text-pl-green shadow-inner">
                        <Sparkles className="h-4 w-4" />
                    </div>
                    <div>
                        <h2 className="text-xs sm:text-sm font-black uppercase tracking-wider text-white font-mono">
                            Poisson Squad Optimizer
                        </h2>
                        <p className="text-[10px] sm:text-[11px] text-slate-400 font-mono">
                            Auto-resolves Next Fixture Gameweek
                        </p>
                    </div>
                </div>

                {/* Action Controls */}
                <div className="flex w-full sm:w-auto items-center gap-2">
                    {/* Manager ID Input */}
                    <div className="relative flex-1 sm:w-48">
                        <User className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                        <input
                            type="number"
                            placeholder="FPL Manager ID"
                            value={teamIdInput}
                            onChange={(e) => setTeamIdInput(e.target.value)}
                            disabled={isLoading}
                            className="w-full rounded-xl border border-panel-border bg-canvas/90 pl-9 pr-3 py-1.5 text-xs font-mono text-white placeholder-slate-500 focus:border-pl-green focus:outline-none focus:ring-1 focus:ring-pl-green transition-all shadow-inner"
                            required
                        />
                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        disabled={isLoading || !teamIdInput}
                        className="flex items-center gap-1.5 rounded-xl bg-pl-green hover:bg-[#00e67a] active:scale-95 text-slate-950 font-mono font-black text-xs px-4 py-2 transition-all shadow-[0_0_15px_rgba(0,255,135,0.35)] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0"
                    >
                        {isLoading ? (
                            <>
                                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                <span>Optimizing...</span>
                            </>
                        ) : (
                            <>
                                <Play className="h-3.5 w-3.5 fill-current" />
                                <span>Optimize Team</span>
                            </>
                        )}
                    </button>
                </div>

            </div>
        </form>
    );
};