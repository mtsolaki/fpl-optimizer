import { useState } from 'react';
import type {UserLineupResponse} from './types/fpl';
import { fetchLineup } from './services/fplApi';
import { Sidebar } from './components/Sidebar';
import { TopHeader } from './components/TopHeader';
import { LineupControls } from './components/LineupControls';
import { HeaderStats } from './components/HeaderStats';
import { Pitch } from './components/Pitch';
import { Bench } from './components/Bench';
import { SquadAnalyticsPanel } from './components/SquadAnalyticsPanel';
import { VfmTable } from './components/VfmTable';
import { GameweekAnalyticsView } from './components/GameweekAnalyticsView';
import { AlertCircle, ShieldAlert } from 'lucide-react';

export default function App() {
  const [activeFeature, setActiveFeature] = useState<string>('optimizer');
  const [lineupData, setLineupData] = useState<UserLineupResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleOptimizeLineup = async (teamId: number) => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const data = await fetchLineup(teamId);
      setLineupData(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to optimize lineup';
      setErrorMessage(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
      <div className="flex flex-col h-screen w-screen bg-canvas text-slate-100 overflow-hidden font-sans">

        {/* 1. Full-Width Top Header */}
        <TopHeader />

        {/* 2. Main Body Container */}
        <div className="flex-1 flex w-full overflow-hidden">

          {/* Navigation Sidebar */}
          <Sidebar
              activeFeatureId={activeFeature}
              onSelectFeature={(id) => setActiveFeature(id)}
          />

          {/* Scrollable Center Canvas */}
          <main className="flex-1 h-full overflow-y-auto px-4 py-6 sm:px-6 sm:py-6">

            {/* Feature 1: Lineup Optimizer */}
            {activeFeature === 'optimizer' && (
                <div className="max-w-7xl mx-auto space-y-6 pb-12">

                  {/* Controls Bar: Manager ID & Optimize */}
                  <LineupControls
                      onOptimize={handleOptimizeLineup}
                      isLoading={isLoading}
                  />

                  {/* Error Alert Box */}
                  {errorMessage && (
                      <div className="flex items-center gap-3 rounded-xl border border-rose-500/40 bg-rose-950/40 p-3.5 text-xs text-rose-300 shadow-lg">
                        <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
                        <span>{errorMessage}</span>
                      </div>
                  )}

                  {/* Split 2-Column Workspace */}
                  {lineupData ? (
                      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">

                        {/* Left Column: Sticky Pitch + Dugout Bench */}
                        <div className="lg:col-span-7 xl:col-span-8 space-y-4 lg:sticky lg:top-2">
                          <Pitch starters={lineupData.starters} />
                          <Bench bench={lineupData.bench} />
                        </div>

                        {/* Right Column: Tactical Command Center */}
                        <div className="lg:col-span-5 xl:col-span-4 space-y-4">
                          <HeaderStats lineup={lineupData} />
                          <SquadAnalyticsPanel lineup={lineupData} />
                        </div>

                      </div>
                  ) : (
                      !isLoading && (
                          <div className="rounded-2xl border border-dashed border-panel-border bg-panel/30 py-20 px-4 text-center">
                            <ShieldAlert className="h-10 w-10 text-slate-600 mx-auto mb-3" />
                            <h3 className="text-sm font-bold text-slate-300">
                              No Lineup Generated Yet
                            </h3>
                            <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                              Enter your Manager ID above, then click Optimize Team to run the model for the next gameweek.
                            </p>
                          </div>
                      )
                  )}
                </div>
            )}

            {/* Feature 2: VFM Market Analytics */}
            {activeFeature === 'vfm' && (
                <div className="max-w-5xl mx-auto pb-12">
                  <VfmTable />
                </div>
            )}
            {/* Feature 3: Gameweek Analytics */}
            {activeFeature === 'analytics' && (
                <div className="max-w-6xl mx-auto">
                  <GameweekAnalyticsView />
                </div>
            )}

          </main>

        </div>

      </div>
  );
}