package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.api.gson.Enums;

public class Question extends ApiResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("account_question_id")
    private long accountId;

    @SerializedName("text")
    private String text;

    @SerializedName("type")
    private Type type;

    @SerializedName("ask_local_date")
    private DateTime askDate;

    @SerializedName("ask_time")
    private AskTime askTime;

    @SerializedName("choices")
    private List<Choice> choices;


    @VisibleForTesting
    public static Question create(long id, long accountId,
                                  String text, Type type,
                                  DateTime askDate, AskTime askTime,
                                  List<Choice> choices) {
        Question question = new Question();

        question.id = id;
        question.accountId = accountId;
        question.text = text;
        question.type = type;
        question.askDate = askDate;
        question.askTime = askTime;
        question.choices = choices;

        return question;
    }


    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getText() {
        return text;
    }

    public Type getType() {
        return type;
    }

    public AskTime getAskTime() {
        return askTime;
    }

    public DateTime getAskDate() {
        return askDate;
    }

    public List<Choice> getChoices() {
        return choices;
    }


    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", text='" + text + '\'' +
                ", type=" + type +
                ", askDate=" + askDate +
                ", choices=" + choices +
                '}';
    }

    public static class Choice {
        @SerializedName("id")
        private long id;

        @SerializedName("text")
        private String text;

        @SerializedName("question_id")
        private Long questionId;


        public long getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public @Nullable Long getQuestionId() {
            return questionId;
        }


        @Override
        public String toString() {
            return "Choice{" +
                    "id=" + id +
                    ", text='" + text + '\'' +
                    ", questionId=" + questionId +
                    '}';
        }
    }

    public enum Type implements Enums.FromString {
        CHOICE,
        CHECKBOX,
        QUANTITY,
        DURATION,
        TIME,
        TEXT,
        UNKNOWN;

        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }

    public enum AskTime implements Enums.FromString {
        MORNING,
        AFTERNOON,
        EVENING,
        ANYTIME,
        UNKNOWN;

        public static AskTime fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
