package org.example.controller;

import org.example.hibernateConnector.HibernateSessionController;
import org.example.model.House;
import org.example.response.ErrorMessageResponse;
import org.example.response.ResponseMessage;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/houses")
@SuppressWarnings("unused")
public class HouseController {
    private record Houses(List<House> houses) {
    }

    @Autowired
    private HibernateSessionController sessionController;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllHouses() {
        try (var session = HibernateSessionController.openSession()) {
            List<House> houses = session.createQuery("from House", House.class).list();
            return new ResponseEntity<>(new Houses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/free", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFreeHousesByParams(@RequestParam(value = "comfort-class", required = false) String comfortClass,
                                                   @RequestParam(value = "districts", required = false) List<String> districts) {
        try (var session = HibernateSessionController.openSession()) {
            if (comfortClass == null && districts == null) { // /free/all
                List<House> houses = session.createNativeQuery("select * from FreeHouse", House.class).list();
                return new ResponseEntity<>(new Houses(houses), HttpStatus.OK);
            }

            String query;
            if (comfortClass != null && districts == null) { // /free/{comfortClass}
                query = "select * from FreeHouse WHERE comfort_class ILIKE '" + comfortClass + "'";
            } else if (comfortClass == null && !districts.isEmpty()) { // /free/all/{districts}
                query = getSelectQueryForDistricts(districts);
            } else { // /free/{comfortClass}/{districts}
                query = getSelectQueryForDistricts(districts) + " AND comfort_class ILIKE '" + comfortClass + "'";
            }

            var houses = session.createNativeQuery(query, House.class).list();

            if (houses.isEmpty()) { // if houses not found
                return new ResponseEntity<>(ResponseMessage.HOUSES_NOT_FOUND.getJSON(), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new Houses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createHouse(@RequestBody House newHouse) {
        if (newHouse.getAddress() == null || newHouse.getPricePerDay() == null
                || newHouse.getDistrict() == null || newHouse.getComfortClass() == null
                || newHouse.getMapLocation() == null) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.BAD_REQUEST, "Request must contains 'address', 'price_per_day', 'district', 'comfort_class' and 'map_location'"), HttpStatus.BAD_REQUEST);
        }

        try (var session = HibernateSessionController.openSession()) {
            session.beginTransaction();

            if (newHouse.getAdditionDate() == null) {
                newHouse.setAdditionDate(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))));
            }

            session.persist(newHouse);
            session.getTransaction().commit();
        } catch (ConstraintViolationException e) {
            return new ResponseEntity<>(ResponseMessage.HOUSE_ALREADY_EXISTS.getJSON(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.BAD_REQUEST, e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(newHouse, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editHouse(@RequestBody House editHouse) {
        if (editHouse.getAddress() == null || editHouse.getPricePerDay() == null
                || editHouse.getDistrict() == null || editHouse.getComfortClass() == null
                || editHouse.getMapLocation() == null) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.BAD_REQUEST, "Request must contains 'address', 'price_per_day', 'district', 'comfort_class' and 'map_location'"), HttpStatus.BAD_REQUEST);
        }

        try (var session = HibernateSessionController.openSession()) {
            House oldHouse = session.get(House.class, editHouse.getId());
            if (oldHouse.equals(editHouse)) {
                return new ResponseEntity<>(ResponseMessage.NO_DIFFERENCE_BETWEEN_DATA.getJSON(), HttpStatus.CONFLICT);
            }

            try {
                var methods = oldHouse.getClass().getMethods();
                for (var method : methods) {
                    if (method.getName().startsWith("get") && method.getParameterTypes().length == 0 && !void.class.equals(method.getReturnType())) {
                        if (!Objects.equals(method.invoke(oldHouse), method.invoke(editHouse))) {
                            var setterName = method.getName().replace("get", "set");
                            var setter = House.class.getMethod(setterName, method.getReturnType());
                            setter.invoke(oldHouse, method.invoke(editHouse));
                        }
                    }
                }

                oldHouse.setLastChangeDate(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))));
            } catch (Exception e) {
                return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            session.beginTransaction();
            session.merge(oldHouse);
            session.getTransaction().commit();

            return new ResponseEntity<>(oldHouse, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteHouse(@RequestParam(value = "id") Long id) {
        try (var session = HibernateSessionController.openSession()) {
            House deletedHouse = session.get(House.class, id);
            if (deletedHouse == null) {
                return new ResponseEntity<>(ResponseMessage.HOUSE_NOT_FOUND.getJSON(), HttpStatus.NOT_FOUND);
            }

            session.beginTransaction();
            session.remove(deletedHouse);
            session.getTransaction().commit();

            return new ResponseEntity<>(ResponseMessage.DELETED_SUCCESSFULLY.getJSON(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getSelectQueryForDistricts(List<?> districts) {
        var sb = new StringBuilder();

        sb.append("select * from FreeHouse WHERE (");
        for (int i = 0; i < districts.size(); i++) {
            sb.append("district ILIKE '")
                    .append(districts.get(i))
                    .append("' ");

            if (i != districts.size() - 1) {
                sb.append("OR ");
            }
        }
        sb.append(")");

        return sb.toString();
    }
}
