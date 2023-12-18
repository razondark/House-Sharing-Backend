package org.example.controller;

import org.example.hibernateConnector.HibernateSessionController;
import org.example.model.RentedHouse;
import org.example.response.ErrorMessageResponse;
import org.example.response.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/rented-houses")
@SuppressWarnings("unused")
public class RentedHouseController {
    private record RentedHouses(List<RentedHouse> rentedHouses) {
    }

    @Autowired
    private HibernateSessionController sessionController;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllRentedHouses() {
        try (var session = HibernateSessionController.openSession()) {
            List<RentedHouse> rentedHouses = session.createQuery("from Rented_House", RentedHouse.class).list();
            return new ResponseEntity<>(new RentedHouseController.RentedHouses(rentedHouses), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/rented", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRentedHousesByParams(@RequestParam(value = "period", required = false) String period) {
        try (var session = HibernateSessionController.openSession()) {
            if (period == null) {
                List<RentedHouse> houses = session.createQuery("from Rented_House", RentedHouse.class).list();
                return new ResponseEntity<>(new RentedHouses(houses), HttpStatus.OK);
            }

            if (!period.contains("day") && !period.contains("month") && !period.contains("year")) {
                return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.BAD_REQUEST, "Request must contains 'day', 'month' or 'year'"), HttpStatus.BAD_REQUEST);
            }

            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String startDate = LocalDateTime.now().format(dateTimeFormat);

            String endDate;
            if (period.contains("day")) {
                endDate = LocalDateTime.now().minusDays(1).format(dateTimeFormat);
            } else if (period.contains("month")) {
                endDate = LocalDateTime.now().minusMonths(1).format(dateTimeFormat);
            } else {
                endDate = LocalDateTime.now().minusYears(1).format(dateTimeFormat);
            }

            LocalDateTime startDateTime = LocalDateTime.parse(startDate, dateTimeFormat);
            LocalDateTime endDateTime = LocalDateTime.parse(endDate, dateTimeFormat);

            var query = session.createQuery("FROM Rented_House WHERE rentalStartDate BETWEEN :startDate AND :endDate", RentedHouse.class);
            query.setParameter("endDate", startDateTime);
            query.setParameter("startDate", endDateTime);

            var houses = query.list();
            if (houses.isEmpty()) { // if houses not found
                return new ResponseEntity<>(ResponseMessage.HOUSES_NOT_FOUND.getJSON(), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new RentedHouseController.RentedHouses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long getUserTransactionsCount(Long id)
    {
        try (var session = HibernateSessionController.openSession()) {
            String transactionsCountQuery = "SELECT COUNT(*) FROM Rented_House WHERE idClient = :id";
            var queryTransactionsCount = session.createQuery(transactionsCountQuery, Long.class);
            queryTransactionsCount.setParameter("id", id);

            return queryTransactionsCount.uniqueResult();
        }
        catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getUserAvgMoney(Long id) {
        try (var session = HibernateSessionController.openSession()) {
            String avgMoneyQuery = "SELECT AVG(totalAmount) FROM Rented_House WHERE idClient = :id";
            var queryAvgMoney = session.createQuery(avgMoneyQuery, Double.class);
            queryAvgMoney.setParameter("id", id);

            return new BigDecimal(queryAvgMoney.uniqueResult().toString());
        }
        catch (Exception e) {
            return null;
        }
    }

    private RentedHouse getUserLastBiggestDeal(Long id) {
        try (var session = HibernateSessionController.openSession()) {
            String lastBiggestDealQuery = "from Rented_House where idClient = :id and totalAmount = (" +
                    "select max(totalAmount) from Rented_House where idClient = :id) " +
                    "order by totalAmount desc";
            var queryLastBiggestDeal = session.createQuery(lastBiggestDealQuery, RentedHouse.class);
            queryLastBiggestDeal.setParameter("id", id);
            queryLastBiggestDeal.setMaxResults(1);

            return queryLastBiggestDeal.uniqueResult();
        }
        catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getUserTotalMoney(Long id) {
        try (var session = HibernateSessionController.openSession()) {
            String totalMoneyQuery = "SELECT SUM(totalAmount) FROM Rented_House WHERE idClient = :id";
            var queryTotalMoney = session.createQuery(totalMoneyQuery, BigDecimal.class);
            queryTotalMoney.setParameter("id", id);

            return queryTotalMoney.uniqueResult();
        }
        catch (Exception e) {
            return null;
        }
    }

    private Long getUserTotalRentalPeriod(Long id) {
        try (var session = HibernateSessionController.openSession()) {
            String totalRentalPeriodQuery = "SELECT SUM(rentalDuration) FROM Rented_House WHERE idClient = :id";
            var queryTotalRental = session.createQuery(totalRentalPeriodQuery, Long.class);
            queryTotalRental.setParameter("id", id);

            return queryTotalRental.uniqueResult();
        }
        catch (Exception e) {
            return null;
        }
    }


    @RequestMapping(value = "/rented-user-info/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserAvgInfo(@PathVariable("id") Long id) {
        try (var session = HibernateSessionController.openSession()) {
            record UserInfo(Long transactionsCount, BigDecimal avgMoney, RentedHouse lastBiggestDeal,
                            BigDecimal totalMoney, Long totalRentalPeriod) {
                // кол-во сделок
                // средняя сумма сделки
                // самая крупная сделка
                // все потрачено денег
                // общий срок аренды
            }

            // кол-во сделок
            var transactionsCount = getUserTransactionsCount(id);
            // средняя сумма сделки
            var avgMoney = getUserAvgMoney(id);
            // самая крупная сделка
            var lastBiggestDeal = getUserLastBiggestDeal(id);
            // всего потрачено денег
            var totalMoney = getUserTotalMoney(id);
            // общий срок аренды
            var totalRentalPeriod = getUserTotalRentalPeriod(id);

            return new ResponseEntity<>(new UserInfo(transactionsCount, avgMoney, lastBiggestDeal, totalMoney, totalRentalPeriod), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(new ErrorMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
