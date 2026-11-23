-- Performance indexes aligned with repository query patterns

CREATE INDEX IF NOT EXISTS idx_gameweeks_finished
    ON public.gameweeks (finished);

CREATE INDEX IF NOT EXISTS idx_fixtures_gameweek_finished
    ON public.fixtures (gameweek, finished);

CREATE INDEX IF NOT EXISTS idx_players_team_id
    ON public.players (team_id);

CREATE INDEX IF NOT EXISTS idx_pgl_gameweek_season
    ON public.player_gameweek_logs (gameweek_id, season);

CREATE INDEX IF NOT EXISTS idx_pgl_player_gameweek
    ON public.player_gameweek_logs (player_code, gameweek_id);

CREATE INDEX IF NOT EXISTS idx_pgl_gameweek_settled
    ON public.player_gameweek_logs (gameweek_id)
    WHERE actual_total_points IS NOT NULL;