-- Baseline schema for FPL Optimizer (PostgreSQL 16)

CREATE TABLE public.teams (
                              id integer NOT NULL,
                              name character varying(255),
                              short_name character varying(255),
                              strength_overall_away integer,
                              strength_overall_home integer
);

CREATE TABLE public.gameweeks (
                                  id integer NOT NULL,
                                  deadline_time timestamp(6) with time zone,
                                  finished boolean,
                                  is_current boolean,
                                  is_next boolean,
                                  name character varying(255)
);

CREATE TABLE public.fixtures (
                                 id integer NOT NULL,
                                 away_team_difficulty integer,
                                 away_team_score integer,
                                 finished boolean,
                                 gameweek integer,
                                 home_team_difficulty integer,
                                 home_team_score integer,
                                 kickoff_time timestamp(6) without time zone,
                                 started boolean,
                                 away_team_id integer,
                                 home_team_id integer
);

CREATE TABLE public.fixture_projections (
                                            fixture_id integer NOT NULL,
                                            away_attack_degradation double precision,
                                            away_base_attack double precision,
                                            away_base_defense double precision,
                                            away_clean_sheet_prob double precision,
                                            away_cs_hit boolean,
                                            away_defense_degradation double precision,
                                            away_final_attack double precision,
                                            away_final_defense double precision,
                                            away_goals_delta double precision,
                                            away_lambda double precision,
                                            away_motivation double precision,
                                            gameweek integer,
                                            home_attack_degradation double precision,
                                            home_base_attack double precision,
                                            home_base_defense double precision,
                                            home_clean_sheet_prob double precision,
                                            home_cs_hit boolean,
                                            home_defense_degradation double precision,
                                            home_final_attack double precision,
                                            home_final_defense double precision,
                                            home_goals_delta double precision,
                                            home_lambda double precision,
                                            home_motivation double precision,
                                            away_team_id integer,
                                            home_team_id integer
);

CREATE TABLE public.players (
                                code integer NOT NULL,
                                creativity double precision,
                                expected_assists double precision,
                                expected_assists_per90 double precision,
                                expected_goal_involvements double precision,
                                expected_goal_involvements_per90 double precision,
                                expected_goals double precision,
                                expected_goals_conceded double precision,
                                expected_goals_conceded_per90 double precision,
                                expected_goals_per90 double precision,
                                form double precision,
                                ict_index double precision,
                                influence double precision,
                                threat double precision,
                                corners_and_indirect_free_kicks_order integer,
                                direct_free_kicks_order integer,
                                first_name character varying(255),
                                id integer NOT NULL,
                                last_name character varying(255),
                                chance_of_playing_next_round integer,
                                chance_of_playing_this_round integer,
                                current_price double precision,
                                ep_next double precision,
                                ep_this double precision,
                                news character varying(255),
                                selected_by_percent double precision,
                                transfers_in integer,
                                transfers_in_gameweek integer,
                                transfers_out integer,
                                transfers_out_gameweek integer,
                                penalties_order integer,
                                assists integer,
                                bonus integer,
                                bps integer,
                                cbi integer,
                                clean_sheets integer,
                                defensive_contribution integer,
                                defensive_contribution_per90 double precision,
                                goals_conceded integer,
                                goals_conceded_per90 double precision,
                                goals_scored integer,
                                minutes integer,
                                penalties_missed integer,
                                penalties_saved integer,
                                points_per_game double precision,
                                recoveries integer,
                                red_cards integer,
                                saves integer,
                                saves_per90 double precision,
                                starts integer,
                                tackles integer,
                                total_points integer,
                                yellow_cards integer,
                                "position" character varying(255) NOT NULL,
                                status character varying(255),
                                web_name character varying(255),
                                team_id integer,
                                CONSTRAINT players_position_check CHECK ((("position")::text = ANY ((ARRAY['GKP'::character varying, 'DEF'::character varying, 'MID'::character varying, 'FWD'::character varying])::text[])))
);

CREATE TABLE public.player_gameweek_logs (
                                             id bigint NOT NULL,
                                             corners_and_indirect_free_kicks_order integer,
                                             direct_free_kicks_order integer,
                                             fixture_difficulty integer,
                                             form_at_gw double precision,
                                             is_home boolean,
                                             chance_of_playing_next_round integer,
                                             chance_of_playing_this_round integer,
                                             current_price double precision,
                                             ep_next double precision,
                                             ep_this double precision,
                                             news character varying(255),
                                             selected_by_percent double precision,
                                             transfers_in integer,
                                             transfers_in_gameweek integer,
                                             transfers_out integer,
                                             transfers_out_gameweek integer,
                                             actual_assists integer,
                                             actual_bonus integer,
                                             actual_bps integer,
                                             actual_clean_sheets integer,
                                             actual_defensive_contribution integer,
                                             actual_goals integer,
                                             actual_goals_conceded integer,
                                             actual_minutes integer,
                                             actual_penalties_missed integer,
                                             actual_penalties_saved integer,
                                             actual_red_cards integer,
                                             actual_saves integer,
                                             actual_total_points integer,
                                             actual_xa double precision,
                                             actual_xg double precision,
                                             actual_xgc double precision,
                                             actual_yellow_cards integer,
                                             prediction_delta double precision,
                                             was_starter boolean,
                                             penalties_order integer,
                                             projected_bonus double precision,
                                             projected_clean_sheet_prob double precision,
                                             projected_def_con_prob double precision,
                                             projected_defensive_contribution double precision,
                                             projected_goals_conceded double precision,
                                             projected_minutes double precision,
                                             projected_opponent_goals double precision,
                                             projected_red_cards_prob double precision,
                                             projected_saves double precision,
                                             projected_team_goals double precision,
                                             projected_xa double precision,
                                             projected_xg double precision,
                                             projected_xp double precision,
                                             projected_yellow_cards_prob double precision,
                                             season character varying(255) NOT NULL,
                                             status character varying(255),
                                             fixture_id integer NOT NULL,
                                             gameweek_id integer NOT NULL,
                                             opponent_team_id integer,
                                             player_code integer NOT NULL
);

ALTER TABLE public.player_gameweek_logs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.player_gameweek_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

-- Primary Keys
ALTER TABLE ONLY public.fixture_projections
    ADD CONSTRAINT fixture_projections_pkey PRIMARY KEY (fixture_id);

ALTER TABLE ONLY public.fixtures
    ADD CONSTRAINT fixtures_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.gameweeks
    ADD CONSTRAINT gameweeks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT player_gameweek_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.players
    ADD CONSTRAINT players_pkey PRIMARY KEY (code);

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);

-- Unique Constraints
ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT uk_player_fixture_season UNIQUE (player_code, fixture_id, season);

ALTER TABLE ONLY public.players
    ADD CONSTRAINT ukta0d901emq6dhtk2lul4svdo5 UNIQUE (id);

-- Foreign Keys
ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT fk2jark7xasdabdr3hy4s4h1aov FOREIGN KEY (gameweek_id) REFERENCES public.gameweeks(id);

ALTER TABLE ONLY public.players
    ADD CONSTRAINT fk5nglidr00c4dyybl171v6kask FOREIGN KEY (team_id) REFERENCES public.teams(id);

ALTER TABLE ONLY public.fixture_projections
    ADD CONSTRAINT fk7339hq903vk1s71pd0w01sbur FOREIGN KEY (home_team_id) REFERENCES public.teams(id);

ALTER TABLE ONLY public.fixture_projections
    ADD CONSTRAINT fk82oqva7wu58dyaj213w95mxbx FOREIGN KEY (away_team_id) REFERENCES public.teams(id);

ALTER TABLE ONLY public.fixture_projections
    ADD CONSTRAINT fkbgh1i3897ie7eatrfgcqykdjq FOREIGN KEY (fixture_id) REFERENCES public.fixtures(id);

ALTER TABLE ONLY public.fixtures
    ADD CONSTRAINT fkcpsdg0eggx0klaprs4fuou75w FOREIGN KEY (away_team_id) REFERENCES public.teams(id);

ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT fkeclkjda6feiutjdotjlfelgta FOREIGN KEY (player_code) REFERENCES public.players(code);

ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT fkjpugu2vhmw0iedxr8ovvcolx1 FOREIGN KEY (fixture_id) REFERENCES public.fixtures(id);

ALTER TABLE ONLY public.fixtures
    ADD CONSTRAINT fkk2i8x74ia85mtwrm3t0ha5rme FOREIGN KEY (home_team_id) REFERENCES public.teams(id);

ALTER TABLE ONLY public.player_gameweek_logs
    ADD CONSTRAINT fknhxi5or5yexgksepeejf3hgu2 FOREIGN KEY (opponent_team_id) REFERENCES public.teams(id);