package lk.tmjnr.speakbuddy.util;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import java.util.List;

import lk.tmjnr.speakbuddy.domain.model.GrammarMistake;

/**
 * Utility for building SpannableString with grammar highlighting.
 */
public class SpannableUtils {

    /**
     * Build a formatted correction display:
     * ❌ "wrong text" (red, strikethrough)
     * ✅ "correct text" (green, bold)
     * 💡 explanation (gray, italic)
     */
    public static SpannableStringBuilder buildCorrectionSpannable(List<GrammarMistake> mistakes) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i < mistakes.size(); i++) {
            GrammarMistake m = mistakes.get(i);

            // Wrong text — red with strikethrough
            int start = builder.length();
            builder.append("❌ ").append(m.getWrong()).append("\n");
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#E53935")),
                    start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StrikethroughSpan(),
                    start + 2, builder.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Correct text — green and bold
            start = builder.length();
            builder.append("✅ ").append(m.getCorrect()).append("\n");
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#43A047")),
                    start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start + 2, builder.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Explanation — gray italic
            start = builder.length();
            builder.append("💡 ").append(m.getExplanation());
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#757575")),
                    start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.ITALIC),
                    start + 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Add spacing between mistakes
            if (i < mistakes.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder;
    }
}
