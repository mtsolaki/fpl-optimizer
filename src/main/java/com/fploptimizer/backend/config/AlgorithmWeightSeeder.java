package com.fploptimizer.backend.config;

import com.fploptimizer.backend.entity.AlgorithmWeight;
import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.repository.AlgorithmWeightRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlgorithmWeightSeeder implements CommandLineRunner {

    private final AlgorithmWeightRepository algorithmWeightRepository;
    private final GameweekRepository gameweekRepository;

    @Value("${fpl.season}")
    private String season;

    @Override
    public void run(String... args) throws Exception {
        // 1. Βρίσκουμε την πρώτη αγωνιστική για αρχικοποίηση
        Optional<Gameweek> gwOpt = gameweekRepository.findById(1);
        if (gwOpt.isEmpty()) {
            log.warn("Δεν βρέθηκε Gameweek 1 στη βάση. Βεβαιώσου ότι έχει τρέξει το sync δεδομένων.");
            return;
        }

        Gameweek gw1 = gwOpt.get();

        // 2. Έλεγχος αν υπάρχουν ήδη βάρη
        if (algorithmWeightRepository.existsByGameweekAndSeason(gw1, season)) {
            log.info("Τα αρχικά AlgorithmWeights υπάρχουν ήδη για τη Gameweek {} ({}). Skipping seed.", gw1.getId(), season);
            return;
        }

        log.info("Αρχικοποίηση baseline AlgorithmWeights για τη Gameweek {} ({})...", gw1.getId(), season);

        // 3. Δημιουργία των 4 baseline sets βάσει FPL rules
        AlgorithmWeight gkp = createWeight(Position.GKP, gw1, season, 0.022, 0.0, 3.0, 4.0, 0.20, -0.25, 0.01);
        AlgorithmWeight def = createWeight(Position.DEF, gw1, season, 0.022, 6.0, 3.0, 4.0, 0.20, -0.30, 0.01);
        AlgorithmWeight mid = createWeight(Position.MID, gw1, season, 0.022, 5.0, 3.0, 1.0, 0.25, -0.20, 0.01);
        AlgorithmWeight fwd = createWeight(Position.FWD, gw1, season, 0.022, 4.0, 3.0, 0.0, 0.25, -0.15, 0.01);

        algorithmWeightRepository.saveAll(List.of(gkp, def, mid, fwd));
        log.info("Τα baseline AlgorithmWeights αποθηκεύτηκαν επιτυχώς!");
    }

    private AlgorithmWeight createWeight(Position position, Gameweek gameweek, String season,
                                         Double baseMin, Double xg, Double xa, Double cs,
                                         Double form, Double fdr, Double lr) {
        AlgorithmWeight weight = new AlgorithmWeight();
        weight.setPosition(position);
        weight.setGameweek(gameweek);
        weight.setSeason(season);
        weight.setBaseMinutesWeight(baseMin);
        weight.setXgWeight(xg);
        weight.setXaWeight(xa);
        weight.setCleanSheetWeight(cs);
        weight.setFormWeight(form);
        weight.setFdrWeight(fdr);
        weight.setLearningRate(lr);
        return weight;
    }
}
