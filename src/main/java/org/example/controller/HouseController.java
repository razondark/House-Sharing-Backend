package org.example.controller;

import org.example.ExceptionHandler.ExceptionHandler;
import org.example.model.House;
import org.example.service.HouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*")
@SuppressWarnings("unused")
public class HouseController {
    @Autowired
    private HouseService houseService;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllHouses() {
        return houseService.getAllHouses();
    }

    @RequestMapping(value = "/free", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFreeHousesByParams(@RequestParam(value = "comfort-class", required = false) String comfortClass,
                                                   @RequestParam(value = "districts", required = false) List<String> districts) {
        return houseService.getFreeHousesByParams(comfortClass, districts);
    }

    public ResponseEntity<?> getHouseById(@PathVariable("id") Long id) {
        return houseService.getHouseById(id);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createHouse(@RequestBody House newHouse) {
        if (Stream.of(newHouse.getAddress(), newHouse.getPricePerDay(), newHouse.getDistrict(),
                        newHouse.getComfortClass(), newHouse.getMapLocation())
                .anyMatch(Objects::isNull)) {
            return ExceptionHandler.handleUserException(
                    "Request must contains 'address', 'pricePerDay', 'district', 'comfortClass' and 'mapLocation'",
                    HttpStatus.BAD_REQUEST);
        }

        return houseService.createHouse(newHouse);
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editHouse(@RequestBody House editedHouse) {
        if (Stream.of(editedHouse.getAddress(), editedHouse.getPricePerDay(), editedHouse.getDistrict(),
                        editedHouse.getComfortClass(), editedHouse.getMapLocation())
                .anyMatch(Objects::isNull)) {
            return ExceptionHandler.handleUserException(
                    "Request must contains 'address', 'pricePerDay', 'district', 'comfortClass' and 'mapLocation'",
                    HttpStatus.BAD_REQUEST);
        }

        return houseService.editHouse(editedHouse);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteHouse(@RequestBody String idHouseJSON) {
        if (!idHouseJSON.contains("id")) {
            return ExceptionHandler.handleUserException("Request must contains 'id'", HttpStatus.BAD_REQUEST);
        }

        return houseService.deleteHouse(idHouseJSON);
    }
}
