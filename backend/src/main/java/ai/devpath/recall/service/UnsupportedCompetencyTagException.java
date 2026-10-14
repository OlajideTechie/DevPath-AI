package ai.devpath.recall.service;

public class UnsupportedCompetencyTagException extends RuntimeException {
    public UnsupportedCompetencyTagException() {
        super("Unsupported competency tag");
    }
}
