import React from 'react';
import {
    Shield,
    Sparkles,
    BarChart3,
    CalendarRange,
    Activity,
    type LucideIcon,
    Cpu
} from 'lucide-react';

/**
 * Model describing a primary navigation feature module in the application sidebar.
 */
export interface NavFeature {
    /**
     * Unique module identifier key.
     */
    id: string;
    /**
     * Display label for the navigation item.
     */
    label: string;
    /**
     * Subtext describing module capabilities.
     */
    description: string;
    /**
     * Lucide icon component reference.
     */
    icon: LucideIcon;
    /**
     * Optional status pill badge (e.g. 'Core', 'Live', 'Soon').
     */
    badge?: string;
    /**
     * Availability flag determining if module is active and clickable.
     */
    isAvailable: boolean;
}

/**
 * Static registry of application navigation modules.
 */
export const NAVIGATION_FEATURES: NavFeature[] = [
    {
        id: 'optimizer',
        label: 'Lineup Optimizer',
        description: 'Poisson & Expected Points Solver',
        icon: Shield,
        badge: 'Core',
        isAvailable: true,
    },
    {
        id: 'vfm',
        label: 'VFM Market',
        description: 'Value-for-money transfer picks',
        icon: Sparkles,
        badge: 'Live',
        isAvailable: true,
    },
    {
        id: 'analytics',
        label: 'GW Analytics',
        description: 'Predictions vs Realized Outcomes',
        icon: BarChart3,
        badge: 'Live',
        isAvailable: true,
    },
    {
        id: 'fixtures',
        label: 'Fixture Projections',
        description: 'Strength & difficulty matrix',
        icon: CalendarRange,
        badge: 'Soon',
        isAvailable: false,
    },
    {
        id: 'live',
        label: 'Live Gameweek',
        description: 'Real-time points & rank sync',
        icon: Activity,
        badge: 'Soon',
        isAvailable: false,
    },
];

/**
 * Properties for the Sidebar navigation component.
 */
interface SidebarProps {
    /**
     * Identifier of the currently active navigation view.
     */
    activeFeatureId: string;
    /**
     * Callback dispatched when a module item is selected.
     */
    onSelectFeature: (id: string) => void;
}

/**
 * Application sidebar navigation bar providing module switching and engine status indicators.
 *
 * @param props - The component properties.
 * @returns The rendered sidebar navigation rail.
 */
export const Sidebar: React.FC<SidebarProps> = ({
                                                    activeFeatureId,
                                                    onSelectFeature,
                                                }) => {
    return (
        <aside className="relative h-full w-20 lg:w-72 bg-panel/85 border-r border-panel-border flex flex-col justify-between p-3.5 transition-all duration-300 shrink-0 select-none backdrop-blur-xl overflow-hidden">

            {/* Background Tactical Grid Lineart */}
            <div
                className="pointer-events-none absolute inset-0 z-0 opacity-[0.04] overflow-hidden"
                aria-hidden="true"
            >
                <svg className="h-full w-full stroke-white fill-none" viewBox="0 0 250 800" preserveAspectRatio="none">
                    <line x1="20" y1="0" x2="20" y2="800" strokeDasharray="3 4" strokeWidth="0.8" />
                    <line x1="120" y1="0" x2="120" y2="800" strokeDasharray="3 4" strokeWidth="0.8" />
                    <circle cx="125" cy="400" r="90" strokeWidth="1" strokeDasharray="4 6" />
                    <circle cx="125" cy="400" r="50" strokeWidth="0.8" strokeDasharray="2 4" />
                </svg>
            </div>

            {/* Navigation Feature Tiles */}
            <div className="relative z-10 space-y-1.5 pt-1">

                {/* Section Header */}
                <div className="hidden lg:flex items-center justify-between px-2 mb-2.5">
                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 font-mono">
                        Tactical Modules
                    </span>
                    <span className="h-1.5 w-1.5 rounded-full bg-pl-green shadow-[0_0_6px_#00ff87]" />
                </div>

                {NAVIGATION_FEATURES.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeFeatureId === item.id;

                    return (
                        <button
                            key={item.id}
                            onClick={() => item.isAvailable && onSelectFeature(item.id)}
                            disabled={!item.isAvailable}
                            title={item.label}
                            className={`w-full flex items-center gap-3.5 px-3 py-2.5 rounded-2xl text-left transition-all duration-200 group relative ${
                                isActive
                                    ? 'bg-pl-green/10 border border-pl-green/40 text-pl-green shadow-[0_0_20px_rgba(0,255,135,0.12)]'
                                    : item.isAvailable
                                        ? 'text-slate-300 hover:text-white hover:bg-canvas/70 border border-transparent'
                                        : 'text-slate-500 opacity-40 cursor-not-allowed border border-transparent'
                            }`}
                        >
                            {/* Active Neon Accent Pill */}
                            {isActive && (
                                <span className="absolute left-0 top-2.5 bottom-2.5 w-1 rounded-r-full bg-pl-green shadow-[0_0_8px_#00ff87]" />
                            )}

                            {/* Tile Icon Container */}
                            <div
                                className={`h-9 w-9 shrink-0 rounded-xl flex items-center justify-center transition-all ${
                                    isActive
                                        ? 'bg-pl-green text-slate-950 font-black shadow-md shadow-pl-green/30'
                                        : 'bg-canvas/90 group-hover:bg-slate-800 text-slate-300 border border-panel-border/80'
                                }`}
                            >
                                <Icon className="h-4 w-4" />
                            </div>

                            {/* Tile Label & Meta */}
                            <div className="hidden lg:flex flex-col min-w-0 flex-1 font-mono">
                                <div className="flex items-center justify-between gap-1.5">
                                    <span className={`text-xs font-bold truncate ${isActive ? 'text-white' : 'text-slate-200'}`}>
                                        {item.label}
                                    </span>

                                    {item.badge && (
                                        <span
                                            className={`text-[9px] font-black px-1.5 py-0.2 rounded-md uppercase tracking-wider ${
                                                item.badge === 'Core'
                                                    ? 'bg-pl-green/15 text-pl-green border border-pl-green/30'
                                                    : item.badge === 'Live'
                                                        ? 'bg-pl-pink/15 text-pl-pink border border-pl-pink/30'
                                                        : 'bg-slate-900 text-slate-500 border border-panel-border'
                                            }`}
                                        >
                                            {item.badge}
                                        </span>
                                    )}
                                </div>

                                <span className="text-[10px] text-slate-400 truncate mt-0.5">
                                    {item.description}
                                </span>
                            </div>
                        </button>
                    );
                })}
            </div>

            {/* Engine Status Footer */}
            <div className="relative z-10 p-2.5 rounded-2xl bg-canvas/80 border border-panel-border flex items-center justify-center lg:justify-between shadow-inner">
                <div className="flex items-center gap-2.5 min-w-0">
                    <span className="relative flex h-2 w-2 shrink-0">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-pl-green opacity-75" />
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-pl-green" />
                    </span>

                    <div className="hidden lg:flex flex-col min-w-0 font-mono">
                        <span className="text-[10px] font-black text-slate-200 uppercase tracking-tight">
                            Poisson Engine
                        </span>
                        <span className="text-[9px] text-pl-green truncate">
                            localhost:8080
                        </span>
                    </div>
                </div>

                <div className="hidden lg:flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-slate-900 border border-slate-800 text-slate-400">
                    <Cpu className="h-3.5 w-3.5 text-slate-400" />
                </div>
            </div>

        </aside>
    );
};