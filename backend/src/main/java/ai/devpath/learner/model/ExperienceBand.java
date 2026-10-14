package ai.devpath.learner.model;

public enum ExperienceBand {
    ZERO_TO_ONE(0),
    TWO_TO_THREE(2),
    FOUR_TO_FIVE(4),
    SIX_TO_EIGHT(6),
    NINE_PLUS(9);

    private final int lowerBoundYears;

    ExperienceBand(int lowerBoundYears) {
        this.lowerBoundYears = lowerBoundYears;
    }

    public int lowerBoundYears() {
        return lowerBoundYears;
    }
}
