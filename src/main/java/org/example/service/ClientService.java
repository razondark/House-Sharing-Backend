package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ExceptionHandler.ExceptionHandler;
import org.example.hibernateController.HibernateSessionController;
import org.example.model.Client;
import org.example.response.ResponseMessage;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
@SuppressWarnings("unused")
public class ClientService {
    private record Clients(List<Client> clients) {
    }

    @Autowired
    private HibernateSessionController sessionController;

    public ResponseEntity<?> getAllClients() {
        try (var session = sessionController.openSession()) {
            var clients = session.createQuery("from Client", Client.class).list();
            if (clients == null || clients.isEmpty()) {
                return ExceptionHandler.handleInfoException(ResponseMessage.CLIENTS_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new Clients(clients), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> getClientById(Long id) {
        try (var session = sessionController.openSession()) {
            var client = session.get(Client.class, id);
            if (client == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.CLIENT_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(client, HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> getClientByLoginAndPassword(String loginDataJSON) {
        try (var session = sessionController.openSession()) {
            var loginDataMap = new ObjectMapper().readValue(loginDataJSON, Map.class);

            String login = (String) loginDataMap.get("login");
            String password = (String) loginDataMap.get("password");

            var query = session.createQuery("from Client where login = :login and password = :password", Client.class);
            query.setParameter("login", login);
            query.setParameter("password", hashPassword(password));
            var client = query.uniqueResult();

            if (client == null) { // if client not found
                query = session.createQuery("from Client where login = :login", Client.class);
                query.setParameter("login", login);
                client = query.uniqueResult();
                if (client != null) { // if client found
                    return ExceptionHandler.handleInfoException(ResponseMessage.INCORRECT_PASSWORD, HttpStatus.UNAUTHORIZED);
                }
                return ExceptionHandler.handleInfoException(ResponseMessage.CLIENT_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(client, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            return ExceptionHandler.handleServerException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> createClient(Client newClient) {
        try (var session = sessionController.openSession()) {
            session.beginTransaction();
            newClient.setPassword(hashPassword(newClient.getPassword())); // hash password
            session.persist(newClient);
            session.getTransaction().commit();

            return new ResponseEntity<>(newClient, HttpStatus.CREATED);
        } catch (ConstraintViolationException e) {
            return ExceptionHandler.handleInfoException(ResponseMessage.LOGIN_OR_PHONE_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> getMoneyToClient(String idClientJSON) {
        try (var session = sessionController.openSession()) {
            var idMap = new ObjectMapper().readValue(idClientJSON, Map.class);
            Long id = new ObjectMapper().convertValue(idMap.get("id"), Long.class);

            var random = new Random();

            var client = session.get(Client.class, id);
            if (client == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.CLIENT_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            session.beginTransaction();
            var newBalance = BigDecimal.valueOf(random.nextDouble() * 999.99 + 0.01); // [0.01; 1000.0]
            client.setBalance(client.getBalance().add(newBalance));
            session.merge(client);
            session.getTransaction().commit();

            return new ResponseEntity<>(client, HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> editClient(Client editedClient) {
        try (var session = sessionController.openSession()) {
            Client oldClient = session.get(Client.class, editedClient.getId());
            if (oldClient.equals(editedClient)) {
                return ExceptionHandler.handleInfoException(ResponseMessage.NO_DIFFERENCE_BETWEEN_DATA, HttpStatus.CONFLICT);
            }

            try {
                // search for the necessary set methods
                var methods = oldClient.getClass().getMethods();
                for (var method : methods) {
                    if (method.getName().startsWith("get") && method.getParameterTypes().length == 0
                            && !void.class.equals(method.getReturnType())) {
                        if (!Objects.equals(method.invoke(oldClient), method.invoke(editedClient))) {
                            var setterName = method.getName().replace("get", "set");
                            var setter = Client.class.getMethod(setterName, method.getReturnType());
                            setter.invoke(oldClient, method.invoke(editedClient));
                        }
                    }
                }

            } catch (Exception e) {
                return ExceptionHandler.handleServerException(e);
            }

            session.beginTransaction();
            session.merge(oldClient);
            session.getTransaction().commit();

            return new ResponseEntity<>(oldClient, HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> deleteClient(String idClientJSON) {
        try (var session = sessionController.openSession()) {
            var idMap = new ObjectMapper().readValue(idClientJSON, Map.class);
            Long id = new ObjectMapper().convertValue(idMap.get("id"), Long.class);

            Client deletedClient = session.get(Client.class, id);
            if (deletedClient == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.CLIENT_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            session.beginTransaction();
            session.remove(deletedClient);
            session.getTransaction().commit();

            return new ResponseEntity<>(ResponseMessage.DELETED_SUCCESSFULLY.getJSON(), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    private String hashPassword(String password) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
