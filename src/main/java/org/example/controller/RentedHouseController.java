package org.example.controller;

import org.example.ExceptionHandler.ExceptionHandler;
import org.example.model.RentedHouse;
import org.example.service.RentedHouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/rented-houses")
@CrossOrigin(origins = "*")
@SuppressWarnings("unused")
public class RentedHouseController {
    @Autowired
    private RentedHouseService rentedHouseService;

    @RequestMapping(value = "/rented", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRentedHousesByPeriod(@RequestParam(value = "period", required = false) String period,
                                                     @RequestParam(value = "start-period-date", required = false) String startPeriodDate) {
        if (!Stream.of("day", "month", "year").anyMatch(period::contains)) {
            return ExceptionHandler.handleUserException("Request must contains 'day', 'month' or 'year'", HttpStatus.BAD_REQUEST);
        }

        return rentedHouseService.getRentedHousesByPeriod(period, startPeriodDate);
    }

    @RequestMapping(value = "/rented-user-info/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserAvgInfo(@PathVariable("id") Long id) {
        return rentedHouseService.getUserAvgInfo(id);
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserHousesInfo(@PathVariable("id") Long id) {
        return rentedHouseService.getUserHousesInfo(id);
    }

    @RequestMapping(value = "/create-deal", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDeal(@RequestBody RentedHouse newDeal) {
        if (Stream.of(newDeal.getIdHouse(), newDeal.getIdClient(), newDeal.getRentalDuration(), newDeal.getTotalAmount())
                .anyMatch(Objects::isNull)) {
            return ExceptionHandler.handleUserException(
                    "Request must contains 'idHouse', 'idClient', 'rentalDuration' and 'totalAmount'",
                    HttpStatus.BAD_REQUEST);
        }

        return rentedHouseService.createDeal(newDeal);
    }

    @RequestMapping(value = "/edit-deal", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editDeal(@RequestBody RentedHouse editedDeal) {
        if (Stream.of(editedDeal.getIdHouse(), editedDeal.getIdClient(), editedDeal.getRentalDuration(),
                        editedDeal.getTotalAmount())
                .anyMatch(Objects::isNull)) {
            return ExceptionHandler.handleUserException(
                    "Request must contains 'idHouse', 'idClient', 'rentalDuration' and 'totalAmount'",
                    HttpStatus.BAD_REQUEST);
        }

        return rentedHouseService.editDeal(editedDeal);
    }

    @RequestMapping(value = "/extend-deal", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> extendDeal(@RequestBody String extendInfoJSON) {
        if (!extendInfoJSON.contains("idRentedHouse") || !extendInfoJSON.contains("additionalDays")
                || !extendInfoJSON.contains("additionalPrice")) {
            return ExceptionHandler.handleUserException(
                    "Request must contains 'idRentedHouse', 'additionalDays', 'additionalPrice'",
                    HttpStatus.BAD_REQUEST);
        }

        return rentedHouseService.extendDeal(extendInfoJSON);
    }
}
