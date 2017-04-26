package is.hello.sense.api.model.v2.voice;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class VoiceCommandResponse extends ApiResponse {

    @SerializedName("voice_command_topics")
    private final List<VoiceCommandTopic> voiceCommandTopics;

    public VoiceCommandResponse(@NonNull final List<VoiceCommandTopic> voiceCommandTopics) {
        this.voiceCommandTopics = voiceCommandTopics;
    }

    public List<VoiceCommandTopic> getVoiceCommandTopics() {
        return this.voiceCommandTopics;
    }

    @Override
    public String toString() {
        return "VoiceCommandResponse{" +
                "voiceCommandTopics=" + this.voiceCommandTopics +
                '}';
    }
}
