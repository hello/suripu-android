package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

public class DevicesInfo extends ApiResponse {
    @SerializedName("sense_id")
    private String senseId;

    @SerializedName("paired_accounts")
    private int numberPairedAccounts;


    public String getSenseId() {
        return senseId;
    }

    public int getNumberPairedAccounts() {
        return numberPairedAccounts;
    }


    @Override
    public String toString() {
        return "DevicesInfo{" +
                "senseId='" + senseId + '\'' +
                ", numberPairedAccounts=" + numberPairedAccounts +
                '}';
    }
}
