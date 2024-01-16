package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ExceptionHandler.ExceptionHandler;
import org.example.hibernateController.HibernateSessionController;
import org.example.model.House;
import org.example.response.ResponseMessage;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@SuppressWarnings("unused")
public class HouseService {
    private record Houses(List<House> houses) {
    }

    @Autowired
    private HibernateSessionController sessionController;

    public ResponseEntity<?> getAllHouses() {
        try (var session = sessionController.openSession()) {
            List<House> houses = session.createQuery("from House", House.class).list();
            return new ResponseEntity<>(new Houses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> getHouseById(Long id) {
        try (var session = sessionController.openSession()) {
            var house = session.get(House.class, id);
            if (house == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.HOUSE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(house, HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    private String freeHousesQuery(String comfortClass, List<String> districts) {
        var queryBuilder = new StringBuilder("SELECT * FROM FreeHouse");

        // if comfort class specified
        if (comfortClass != null && !comfortClass.isEmpty()) {
            queryBuilder.append(" AND comfort_class ILIKE '").append(comfortClass).append("'");
        }

        // if districts specified
        if (districts != null && !districts.isEmpty()) {
            queryBuilder.append(" AND (");
            for (int i = 0; i < districts.size(); i++) {
                queryBuilder.append("district ILIKE '").append(districts.get(i)).append("'");
                if (i != districts.size() - 1) {
                    queryBuilder.append(" OR ");
                }
            }
            queryBuilder.append(")");
        }

        return queryBuilder.toString();
    }

    public ResponseEntity<?> getFreeHousesByParams(String comfortClass, List<String> districts) {
        try (var session = sessionController.openSession()) {
            var houses = session.createNativeQuery(freeHousesQuery(comfortClass, districts), House.class).list();

            if (houses.isEmpty()) { // if houses not found
                return ExceptionHandler.handleInfoException(ResponseMessage.HOUSES_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new Houses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> createHouse(House newHouse) {
        try (var session = sessionController.openSession()) {
            if (newHouse.getAdditionDate() == null) {
                newHouse.setAdditionDate(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Europe/Moscow"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))));
            }

            session.beginTransaction();
            session.persist(newHouse);
            session.getTransaction().commit();

            return new ResponseEntity<>(newHouse, HttpStatus.CREATED);
        } catch (ConstraintViolationException e) {
            return ExceptionHandler.handleInfoException(ResponseMessage.HOUSE_ALREADY_EXISTS, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> editHouse(House editHouse) {
        try (var session = sessionController.openSession()) {
            House oldHouse = session.get(House.class, editHouse.getId());
            if (oldHouse.equals(editHouse)) {
                return ExceptionHandler.handleInfoException(ResponseMessage.NO_DIFFERENCE_BETWEEN_DATA, HttpStatus.CONFLICT);
            }

            try {
                // search for the necessary set methods
                var methods = oldHouse.getClass().getMethods();
                for (var method : methods) {
                    if (method.getName().startsWith("get") && method.getParameterTypes().length == 0
                            && !void.class.equals(method.getReturnType())) {
                        if (!Objects.equals(method.invoke(oldHouse), method.invoke(editHouse))) {
                            var setterName = method.getName().replace("get", "set");
                            var setter = House.class.getMethod(setterName, method.getReturnType());
                            setter.invoke(oldHouse, method.invoke(editHouse));
                        }
                    }
                }

                oldHouse.setLastChangeDate(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Europe/Moscow"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))));
            } catch (Exception e) {
                return ExceptionHandler.handleServerException(e);
            }

            session.beginTransaction();
            session.merge(oldHouse);
            session.getTransaction().commit();

            return new ResponseEntity<>(oldHouse, HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> deleteHouse(String idHouseJSON) {
        try (var session = sessionController.openSession()) {
            var idMap = new ObjectMapper().readValue(idHouseJSON, Map.class);
            Long id = new ObjectMapper().convertValue(idMap.get("id"), Long.class);

            House deletedHouse = session.get(House.class, id);
            if (deletedHouse == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.HOUSE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            session.beginTransaction();
            session.remove(deletedHouse);
            session.getTransaction().commit();

            return new ResponseEntity<>(ResponseMessage.DELETED_SUCCESSFULLY.getJSON(), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }
}
