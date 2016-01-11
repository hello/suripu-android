package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.markup.text.MarkupString;

public class InsightInfo extends ApiResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("category")
    private String category;

    @SerializedName("title")
    private String title;

    @SerializedName("text")
    private MarkupString text;

    @SerializedName("image_url")
    private String imageUrl;


    public long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public MarkupString getText() {
        return text;
    }

    /**
     * Prefer the new image API in {@link Insight}.
     * @return The insight info's image URL.
     */
    @Deprecated
    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return "InsightInfo{" +
                "id=" + id +
                ", category=" + category +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
