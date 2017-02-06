package is.hello.sense.api.model.v2.voice;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;

import static is.hello.sense.util.ValueUtil.getSafeList;
import static is.hello.sense.util.ValueUtil.getSafeString;

public class VoiceCommandResponse extends ApiResponse {

    @NonNull
    @SerializedName("voice_command_topics")
    private final List<VoiceCommandTopic> voiceCommandTopics;

    public VoiceCommandResponse(@Nullable final List<VoiceCommandTopic> voiceSubjects) {
        this.voiceCommandTopics = getSafeList(voiceSubjects);
    }

    @NonNull
    public List<VoiceCommandTopic> getVoiceCommandTopics() {
        return this.voiceCommandTopics;
    }

    private class VoiceCommandTopic {

        @NonNull
        @SerializedName("title")
        private final String title;

        @NonNull
        @SerializedName("description")
        private final String description;

        @NonNull
        @SerializedName("subtopics")
        private final List<VoiceCommandSubTopic> voiceCommandSubTopics;


        public VoiceCommandTopic(@Nullable final String title,
                                 @Nullable final String description,
                                 @Nullable final List<VoiceCommandSubTopic> voiceCommandSubTopics) {
            this.title = getSafeString(title);
            this.description = getSafeString(description);
            this.voiceCommandSubTopics = getSafeList(voiceCommandSubTopics);
        }

        @NonNull
        public String getTitle() {
            return this.title;
        }

        @NonNull
        public String getDescription() {
            return this.description;
        }

        @NonNull
        public List<VoiceCommandSubTopic> getVoiceCommandSubTopics() {
            return this.voiceCommandSubTopics;
        }
    }

    private class VoiceCommandSubTopic {

        @NonNull
        @SerializedName("command_title")
        private final String commandTitle;

        @NonNull
        @SerializedName("commands")
        private final List<String> commands;

        public VoiceCommandSubTopic(@Nullable final String commandTitle,
                                    @Nullable final List<String> commands) {
            this.commandTitle = getSafeString(commandTitle);
            this.commands = getSafeList(commands);
        }

        @NonNull
        public String getCommandTitle() {
            return this.commandTitle;
        }

        @NonNull
        public List<String> getCommands() {
            return this.commands;
        }
    }
}
