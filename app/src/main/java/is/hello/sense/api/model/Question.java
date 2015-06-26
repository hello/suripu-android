package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.LocalDateTime;

import java.util.List;

public class Question extends ApiResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("account_question_id")
    private long accountId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("ask_local_date")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime askDate;

    @JsonProperty("ask_time")
    private AskTime askTime;

    @JsonProperty("choices")
    @JsonDeserialize(contentAs = Choice.class)
    private List<Choice> choices;


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

    public LocalDateTime getAskDate() {
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
        @JsonProperty("id")
        private long id;

        @JsonProperty("text")
        private String text;

        @JsonProperty("question_id")
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

    public enum Type {
        CHOICE,
        CHECKBOX,
        QUANTITY,
        DURATION,
        TIME,
        TEXT,
        UNKNOWN;

        @JsonCreator
        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }

    public enum AskTime {
        MORNING,
        AFTERNOON,
        EVENING,
        ANYTIME,
        UNKNOWN;

        @JsonCreator
        public static AskTime fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
