package lk.tmjnr.speakbuddy.domain.model;

/**
 * Represents a grammar mistake detected by the AI.
 */
public class GrammarMistake {
    private String wrong;
    private String correct;
    private String explanation;

    public GrammarMistake() {
    }

    public GrammarMistake(String wrong, String correct, String explanation) {
        this.wrong = wrong;
        this.correct = correct;
        this.explanation = explanation;
    }

    public String getWrong() {
        return wrong;
    }

    public void setWrong(String wrong) {
        this.wrong = wrong;
    }

    public String getCorrect() {
        return correct;
    }

    public void setCorrect(String correct) {
        this.correct = correct;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
