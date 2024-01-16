package org.example.ExceptionHandler;

import org.example.response.ErrorMessageResponse;
import org.example.response.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ExceptionHandler {
    public static ResponseEntity<?> handleUserException(String message, HttpStatus status) {
        return new ResponseEntity<>(new ErrorMessageResponse(status, message), status);
    }

    public static ResponseEntity<?> handleInfoException(ResponseMessage message, HttpStatus status) {
        return new ResponseEntity<>(message.getJSON(), status);
    }

    public static ResponseEntity<?> handleServerException(Exception e) {
        return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<?> handleServerException(Exception e, HttpStatus status) {
        return new ResponseEntity<>(new ErrorMessageResponse(status, e.getMessage()), status);
    }
}
