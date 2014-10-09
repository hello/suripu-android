package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public class Question extends ApiResponse {
    @JsonProperty("id")
    private long id;

    @JsonProperty("text")
    private String text;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("choices")
    @JsonDeserialize(contentAs = Choice.class)
    private List<Choice> choices;


    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Type getType() {
        return type;
    }

    public List<Choice> getChoices() {
        return choices;
    }


    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", type=" + type +
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

    public static enum Type {
        CHOICE,
        YES_NO,
        UNKNOWN;

        @JsonCreator
        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
