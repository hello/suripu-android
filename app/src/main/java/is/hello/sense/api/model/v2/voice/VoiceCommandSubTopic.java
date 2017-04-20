package is.hello.sense.api.model.v2.voice;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.Constants;

public class VoiceCommandSubTopic extends ApiResponse {

    @SerializedName("command_title")
    private final String commandTitle;

    @SerializedName("commands")
    private final List<String> commands;

    public VoiceCommandSubTopic(@NonNull final String commandTitle,
                                @NonNull final List<String> commands) {
        this.commandTitle = commandTitle;
        this.commands = commands;
    }

    @Override
    public String toString() {
        return "VoiceCommandSubTopic{" +
                "commandTitle='" + this.commandTitle + '\'' +
                ", commands=" + this.commands +
                '}';
    }

    @NonNull
    public String getCommandTitle() {
        return this.commandTitle;
    }

    @NonNull
    public List<String> getCommands() {
        return this.commands;
    }

    @NonNull
    public String getFirstCommand() {
        if (this.commands.isEmpty()) {
            return Constants.EMPTY_STRING;
        }
        return this.commands.get(0);
    }
}
