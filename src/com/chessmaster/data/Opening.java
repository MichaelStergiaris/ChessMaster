package com.chessmaster.data;

import com.chessmaster.model.Color;
import java.util.List;

public final class Opening {
    public final String id;
    public final String eco;
    public final String name;
    public final String variation;
    public final String category;
    public final Color trainSide;
    public final String difficulty;
    public final String popularity;
    public final String summary;
    public final List<String> movesSan;
    public final List<String> explanations;

    public Opening(String id, String eco, String name, String variation, String category,
                   Color trainSide, String difficulty, String popularity, String summary,
                   List<String> movesSan, List<String> explanations) {
        this.id = id;
        this.eco = eco;
        this.name = name;
        this.variation = variation;
        this.category = category;
        this.trainSide = trainSide;
        this.difficulty = difficulty;
        this.popularity = popularity;
        this.summary = summary;
        this.movesSan = List.copyOf(movesSan);
        this.explanations = List.copyOf(explanations);
    }

    public String fullTitle() {
        return variation.isEmpty() ? name : name + ": " + variation;
    }

    public String getExplanation(int plyIndex) {
        if (plyIndex >= 0 && plyIndex < explanations.size()) {
            return explanations.get(plyIndex);
        }
        return "Develop smoothly, contest the center, and keep your king safe.";
    }
}
