import React from 'react';

export const TopHeader: React.FC = () => {
    return (
        <header className="relative w-full h-18 bg-gradient-to-b from-panel via-panel/85 to-transparent backdrop-blur-md flex items-center justify-center shrink-0 select-none z-30 overflow-hidden">

            {/* Tactical Pitch & Ball White Lineart Background with Ombre Edge Fading */}
            <div
                className="pointer-events-none absolute inset-0 z-0 opacity-20 [mask-image:linear-gradient(to_bottom,black_20%,transparent_100%)]"
                aria-hidden="true"
            >
                <svg
                    className="w-full h-full stroke-white fill-none"
                    xmlns="http://www.w3.org/2000/svg"
                    preserveAspectRatio="xMidYMid slice"
                    viewBox="0 0 1200 80"
                >
                    {/* Left Tactical Zone: Goalpost & Net Mesh */}
                    <g strokeWidth="1" opacity="0.8">
                        {/* Goal Frame */}
                        <rect x="40" y="10" width="85" height="58" rx="2" strokeWidth="1.5" />
                        <rect x="52" y="16" width="73" height="46" opacity="0.5" />
                        {/* Goal Net Cross-hatching */}
                        <path d="M40 22h85 M40 34h85 M40 46h85 M40 58h85" strokeDasharray="2 3" />
                        <path d="M55 10v58 M70 10v58 M85 10v58 M100 10v58 M115 10v58" strokeDasharray="2 3" />
                        {/* Penalty Arc */}
                        <path d="M125 24 a25 25 0 0 1 0 32" strokeWidth="1.2" strokeDasharray="4 3" />
                    </g>

                    {/* Left-Center Floating Geometric Ball */}
                    <g transform="translate(260, 40)" strokeWidth="1.1">
                        <circle r="22" strokeWidth="1.4" />
                        {/* Classic Telstar Pentagonal Panels */}
                        <polygon points="0,-7 6,-2 4,5 -4,5 -6,-2" fill="white" fillOpacity="0.08" />
                        <line x1="0" y1="-7" x2="0" y2="-22" />
                        <line x1="6" y1="-2" x2="19" y2="-9" />
                        <line x1="4" y1="5" x2="14" y2="17" />
                        <line x1="-4" y1="5" x2="-14" y2="17" />
                        <line x1="-6" y1="-2" x2="-19" y2="-9" />
                        {/* Orbit / Motion Arc */}
                        <path d="M-30 8 A35 15 -25 0 1 25 -20" strokeDasharray="3 3" opacity="0.6" />
                    </g>

                    {/* Center Pitch Markings (Behind the Logo) */}
                    <g strokeWidth="1.2" opacity="0.7">
                        {/* Halfway Line */}
                        <line x1="600" y1="0" x2="600" y2="80" strokeDasharray="6 4" />
                        {/* Center Circle & Spot */}
                        <circle cx="600" cy="40" r="32" strokeWidth="1.2" />
                        <circle cx="600" cy="40" r="2.5" fill="white" />
                    </g>

                    {/* Right-Center Floating Geometric Ball */}
                    <g transform="translate(940, 38)" strokeWidth="1.1">
                        <circle r="20" strokeWidth="1.4" />
                        <polygon points="0,-6 5,-2 3,5 -3,5 -5,-2" fill="white" fillOpacity="0.08" />
                        <line x1="0" y1="-6" x2="0" y2="-20" />
                        <line x1="5" y1="-2" x2="17" y2="-8" />
                        <line x1="3" y1="5" x2="12" y2="16" />
                        <line x1="-3" y1="5" x2="-12" y2="16" />
                        <line x1="-5" y1="-2" x2="-17" y2="-8" />
                        <path d="M-26 -12 A30 14 20 0 1 22 18" strokeDasharray="3 3" opacity="0.6" />
                    </g>

                    {/* Right Tactical Zone: Penalty Box & Attack Arrows */}
                    <g strokeWidth="1" opacity="0.8">
                        <rect x="1075" y="10" width="85" height="58" rx="2" strokeWidth="1.5" />
                        {/* 18-yard Box Internal Markings */}
                        <path d="M1075 22h-35 v36 h35" strokeWidth="1.2" />
                        <circle cx="1030" cy="40" r="1.8" fill="white" />
                        <path d="M1040 28 a18 18 0 0 0 0 24" strokeDasharray="3 3" />
                        {/* Tactical Movement Arrows */}
                        <path d="M1010 18 L990 28 M990 28 L998 27 M990 28 L994 20" strokeWidth="1.2" />
                    </g>
                </svg>
            </div>

            {/* Foreground Content: Title & Lab Badge */}
            <div className="relative z-10 flex items-center gap-3 px-4">

                {/* Hybrid Icon: Chemistry Flask + Football Core */}
                <div className="relative flex items-center justify-center h-9 w-9 rounded-xl bg-canvas/80 border border-panel-border shadow-[0_0_15px_rgba(0,255,135,0.15)]">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.75"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        className="h-5 w-5 text-pl-green"
                    >
                        <path d="M10 2v4.5L5.2 16.8A3 3 0 0 0 7.8 21h8.4a3 3 0 0 0 2.6-4.2L14 6.5V2h-4z" />
                        <path d="M8.5 2h7" />
                        <path
                            d="M12 12.5l2 1.5l-.8 2.5h-2.4l-.8-2.5z"
                            className="fill-pl-pink/20 stroke-pl-pink"
                            strokeWidth="1.2"
                        />
                        <path d="M12 12.5v-2" strokeWidth="1.2" />
                        <path d="M14 14l1.8.8" strokeWidth="1.2" />
                        <path d="M13.2 16.5l1.2 1.5" strokeWidth="1.2" />
                        <path d="M10.8 16.5l-1.2 1.5" strokeWidth="1.2" />
                        <path d="M10 14l-1.8.8" strokeWidth="1.2" />
                    </svg>
                    <span className="absolute -top-1 -right-1 h-2 w-2 rounded-full bg-pl-pink animate-pulse" />
                </div>

                {/* Main Title */}
                <span className="text-base sm:text-lg font-black tracking-[0.2em] text-white uppercase font-mono drop-shadow-[0_2px_12px_rgba(0,0,0,0.8)]">
          FPL OPTIMIZER LAB
        </span>

                {/* Tactical Badge */}
                <span className="ml-1 text-[10px] font-extrabold font-mono uppercase tracking-widest px-2 py-0.5 rounded-md bg-pl-green/10 text-pl-green border border-pl-green/30">
          PROJECTION ENGINE
        </span>
            </div>

            {/* Glowing Ombre Divider at bottom edge */}
            <div className="absolute bottom-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-pl-green/35 to-transparent" />
        </header>
    );
};