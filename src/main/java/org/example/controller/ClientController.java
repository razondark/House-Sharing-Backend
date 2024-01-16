package org.example.controller;

import org.example.ExceptionHandler.ExceptionHandler;
import org.example.model.Client;
import org.example.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
@SuppressWarnings("unused")
public class ClientController {
    @Autowired
    private ClientService clientService;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllClients() {
        return clientService.getAllClients();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClientById(@PathVariable("id") Long id) {
        return clientService.getClientById(id);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClientByLoginAndPassword(@RequestBody String loginDataJSON) {
        if (!loginDataJSON.contains("login") || !loginDataJSON.contains("password")) {
            return ExceptionHandler.handleUserException("Request must contains 'login' and 'password'",
                    HttpStatus.BAD_REQUEST);
        }

        return clientService.getClientByLoginAndPassword(loginDataJSON);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createClient(@RequestBody Client newClient) {
        if (newClient.getLogin() == null || newClient.getPassword() == null || newClient.getPhoneNumber() == null) {
            return ExceptionHandler.handleUserException("Request must contains 'login', 'password' and 'phoneNumber'",
                    HttpStatus.BAD_REQUEST);
        }
        if (newClient.getStatus() == null) {
            newClient.setStatus("user");
        }
        if (!newClient.getStatus().equalsIgnoreCase("user")
                && !newClient.getStatus().equalsIgnoreCase("admin")) {
            return ExceptionHandler.handleUserException("Status must be 'user' or 'admin'",
                    HttpStatus.BAD_REQUEST);
        }

        return clientService.createClient(newClient);
    }

    @RequestMapping(value = "/hesoyam", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMoneyToClient(@RequestBody String idClientJSON) {
        if (!idClientJSON.contains("id")) {
            return ExceptionHandler.handleUserException("Request must contains 'id'",
                    HttpStatus.BAD_REQUEST);
        }

        return clientService.getMoneyToClient(idClientJSON);
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editClient(@RequestBody Client editedClient) {
        if (editedClient.getLogin() == null || editedClient.getPassword() == null || editedClient.getPhoneNumber() == null
                || editedClient.getBalance() == null || editedClient.getStatus() == null) {
            return ExceptionHandler.handleUserException("Request must contains " +
                            "'login', 'password', 'phoneNumber', 'balance' and 'status'",
                    HttpStatus.BAD_REQUEST);
        }

        return clientService.editClient(editedClient);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteClient(@RequestBody String idClientJSON) {
        if (!idClientJSON.contains("id")) {
            return ExceptionHandler.handleUserException("Request must contains 'id'",
                    HttpStatus.BAD_REQUEST);
        }

        return clientService.deleteClient(idClientJSON);
    }
}
