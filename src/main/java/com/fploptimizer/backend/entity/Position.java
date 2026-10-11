package com.fploptimizer.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Position {
    GKP(1, "Goalkeeper"),
    DEF(2, "Defender"),
    MID(3, "Midfielder"),
    FWD(4, "Forward");

    private final int elementTypeId;
    private final String description;

    Position(int elementTypeId, String description) {
        this.elementTypeId = elementTypeId;
        this.description = description;
    }

    /**
     * Conversion from FPL element_type ID (1, 2, 3, 4) to GKP, DEF, MID, FOR.
     */
    @JsonCreator
    public static Position fromElementTypeId(int elementTypeId) {
        for (Position position : values()) {
            if (position.getElementTypeId() == elementTypeId) {
                return position;
            }
        }
        throw new IllegalArgumentException("Unknown FPL element_type: " + elementTypeId);
    }

    /**
     * Επιστρέφει το όνομα της θέσης για serialization (π.χ. "MID", "FWD").
     */
    @JsonValue
    public String getCode() {
        return this.name();
    }
}