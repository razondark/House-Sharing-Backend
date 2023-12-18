package org.example.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

@SuppressWarnings("FieldMayBeFinal")
public class ErrorMessageResponse {
    @JsonProperty("status")
    private HttpStatus status;
    @JsonProperty("code")
    private int code;
    @JsonProperty("message")
    private String message;

    public ErrorMessageResponse(HttpStatus status, String message) {
        this.code = status.value();
        this.status = status;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Error " + code + ": " + status + "\nMessage: " + message;
    }
}
