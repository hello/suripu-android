package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DevicesInfo extends ApiResponse {
    @JsonProperty("sense_id")
    private String senseId;

    @JsonProperty("paired_accounts")
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
