package lk.tmjnr.speakbuddy.data.remote.dto;

import java.util.List;

import lk.tmjnr.speakbuddy.domain.model.GrammarMistake;

/**
 * Response body from the AI backend.
 */
public class ChatResponse {
    private List<GrammarMistake> mistakes;
    private String correctedSentence;
    private String reply;
    private String followUpSuggestion;

    public List<GrammarMistake> getMistakes() { return mistakes; }
    public void setMistakes(List<GrammarMistake> mistakes) { this.mistakes = mistakes; }

    public String getCorrectedSentence() { return correctedSentence; }
    public void setCorrectedSentence(String correctedSentence) { this.correctedSentence = correctedSentence; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getFollowUpSuggestion() { return followUpSuggestion; }
    public void setFollowUpSuggestion(String followUpSuggestion) { this.followUpSuggestion = followUpSuggestion; }
}
