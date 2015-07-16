package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.util.markup.text.MarkupString;

public class InsightInfo extends ApiResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("category")
    private InsightCategory category;

    @SerializedName("title")
    private String title;

    @SerializedName("text")
    private MarkupString text;

    @SerializedName("image_url")
    private String imageUrl;


    public long getId() {
        return id;
    }

    public InsightCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public MarkupString getText() {
        return text;
    }

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
