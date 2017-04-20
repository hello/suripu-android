package is.hello.sense.api.model.v2.voice;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.util.Constants;

public class VoiceCommandTopic extends ApiResponse {

    @SerializedName("title")
    private final String title;

    @SerializedName("description")
    private final String description;

    @SerializedName("subtopics")
    private final List<VoiceCommandSubTopic> voiceCommandSubTopics;

    @SerializedName("icon_urls")
    private final MultiDensityImage multiDensityImage;

    public VoiceCommandTopic(@NonNull final String title,
                             @NonNull final String description,
                             @NonNull final List<VoiceCommandSubTopic> voiceCommandSubTopics,
                             @NonNull final MultiDensityImage multiDensityImage) {
        this.title = title;
        this.description = description;
        this.voiceCommandSubTopics = voiceCommandSubTopics;
        this.multiDensityImage = multiDensityImage;
    }

    @Override
    public String toString() {
        return "VoiceCommandTopic{" +
                "title='" + this.title + '\'' +
                ", description='" + this.description + '\'' +
                ", voiceCommandSubTopics=" + this.voiceCommandSubTopics +
                ", multiDensityImage=" + this.multiDensityImage +
                '}';
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public List<VoiceCommandSubTopic> getVoiceCommandSubTopics() {
        return this.voiceCommandSubTopics;
    }

    public MultiDensityImage getMultiDensityImage() {
        return this.multiDensityImage;
    }


    @NonNull
    public String getFirstCommand() {
        if (this.voiceCommandSubTopics.isEmpty()) {
            return Constants.EMPTY_STRING;
        }
        return this.voiceCommandSubTopics.get(0).getFirstCommand();
    }
}
