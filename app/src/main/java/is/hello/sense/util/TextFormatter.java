package is.hello.sense.util;

public class TextFormatter {

    public static String formatInsightCategory(String category){
        return category.replaceAll("_", " ").toUpperCase();
    }

}
