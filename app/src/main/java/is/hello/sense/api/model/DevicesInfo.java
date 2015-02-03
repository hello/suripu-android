package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DevicesInfo extends ApiResponse {
    @JsonProperty("sense_id")
    private String id;

    @JsonProperty("paired_accounts")
    private int numberPairedAccounts;


    public String getId() {
        return id;
    }

    public int getNumberPairedAccounts() {
        return numberPairedAccounts;
    }


    @Override
    public String toString() {
        return "DevicesInfo{" +
                "id='" + id + '\'' +
                ", numberPairedAccounts=" + numberPairedAccounts +
                '}';
    }
}
