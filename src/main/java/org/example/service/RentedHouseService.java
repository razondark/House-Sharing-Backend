package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ExceptionHandler.ExceptionHandler;
import org.example.hibernateController.HibernateSessionController;
import org.example.model.House;
import org.example.model.RentedHouse;
import org.example.response.ResponseMessage;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unused")
public class RentedHouseService {
    private record RentedHouses(List<RentedHouse> rentedHouses) {
    }

    @Autowired
    private HibernateSessionController sessionController;

    public ResponseEntity<?> getRentedHousesByPeriod(String period, String startPeriodDate) {
        try (var session = sessionController.openSession()) {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

            if (period == null) {
                List<RentedHouse> houses = session.createQuery("from Rented_House", RentedHouse.class).list();
                return new ResponseEntity<>(new RentedHouses(houses), HttpStatus.OK);
            }
            if (startPeriodDate == null) {
                startPeriodDate = LocalDateTime.now().format(dateTimeFormat);
            }

            // get start datetime
            LocalDateTime startDateTime = LocalDate.parse(startPeriodDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay();

            String endDate;
            switch (period) {
                case "day":
                    endDate = startDateTime.minusDays(1).format(dateTimeFormat);
                    break;
                case "month":
                    endDate = startDateTime.minusMonths(1).format(dateTimeFormat);
                    break;
                case "year":
                    endDate = startDateTime.minusYears(1).format(dateTimeFormat);
                    break;
                default:
                    return ExceptionHandler.handleUserException("Request must contain 'day', 'month', or 'year'",
                            HttpStatus.BAD_REQUEST);
            }
            LocalDateTime endDateTime = LocalDateTime.parse(endDate, dateTimeFormat);

            var query = session.createQuery("FROM Rented_House WHERE rentalStartDate BETWEEN :startDate AND :endDate",
                    RentedHouse.class);
            query.setParameter("endDate", startDateTime);
            query.setParameter("startDate", endDateTime);
            var houses = query.list();

            if (houses.isEmpty()) { // if houses not found
                return ExceptionHandler.handleInfoException(ResponseMessage.HOUSES_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new RentedHouses(houses), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    private <T> T executeUserAvgInfoQuery(String queryString, Long id, Class<T> resultType) {
        try (var session = sessionController.openSession()) {
            var query = session.createQuery(queryString, resultType);
            query.setParameter("id", id);

            return query.uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }

    private Long getUserTransactionsCount(Long id) {
        String transactionsCountQuery = "SELECT COUNT(*) FROM Rented_House WHERE idClient = :id";
        return executeUserAvgInfoQuery(transactionsCountQuery, id, Long.class);
    }

    private BigDecimal getUserAvgMoney(Long id) {
        String avgMoneyQuery = "SELECT AVG(totalAmount) FROM Rented_House WHERE idClient = :id";
        Double avgMoney = executeUserAvgInfoQuery(avgMoneyQuery, id, Double.class);
        return (avgMoney != null) ? new BigDecimal(avgMoney.toString()) : null;
    }

    private RentedHouse getUserLastBiggestDeal(Long id) {
        String lastBiggestDealQuery = "from Rented_House where idClient = :id and totalAmount = (" +
                "select max(totalAmount) from Rented_House where idClient = :id) " +
                "order by totalAmount desc limit 1";
        return executeUserAvgInfoQuery(lastBiggestDealQuery, id, RentedHouse.class);
    }

    private BigDecimal getUserTotalMoney(Long id) {
        String totalMoneyQuery = "SELECT SUM(totalAmount) FROM Rented_House WHERE idClient = :id";
        return executeUserAvgInfoQuery(totalMoneyQuery, id, BigDecimal.class);
    }

    private Long getUserTotalRentalPeriod(Long id) {
        String totalRentalPeriodQuery = "SELECT SUM(rentalDuration) FROM Rented_House WHERE idClient = :id";
        return executeUserAvgInfoQuery(totalRentalPeriodQuery, id, Long.class);
    }

    private Long getUserCurrentTransactionsNumber(Long id) {
        String totalCurrentTransactionsNumberQuery = "SELECT COUNT(*) FROM Rented_House WHERE idClient = :id " +
                "AND rentalEndDate > CURRENT_TIMESTAMP";
        return executeUserAvgInfoQuery(totalCurrentTransactionsNumberQuery, id, Long.class);
    }

    public ResponseEntity<?> getUserAvgInfo(Long id) {
        try (var session = sessionController.openSession()) {
            record UserInfo(Long transactionsCount, Long currentTransactionsNumber, BigDecimal avgMoney,
                            RentedHouse lastBiggestDeal, BigDecimal totalMoney, Long totalRentalPeriod) {
                // кол-во сделок
                // текущее кол-во домов в аренде
                // средняя сумма сделки
                // самая крупная сделка
                // все потрачено денег
                // общий срок аренды
            }

            // кол-во сделок
            var transactionsCount = getUserTransactionsCount(id);
            // текущее кол-во домов в аренде
            var currentTransactionsNumber = getUserCurrentTransactionsNumber(id);
            // средняя сумма сделки
            var avgMoney = getUserAvgMoney(id);
            // самая крупная сделка
            var lastBiggestDeal = getUserLastBiggestDeal(id);
            // всего потрачено денег
            var totalMoney = getUserTotalMoney(id);
            // общий срок аренды
            var totalRentalPeriod = getUserTotalRentalPeriod(id);

            return new ResponseEntity<>(new UserInfo(transactionsCount, currentTransactionsNumber, avgMoney,
                    lastBiggestDeal, totalMoney, totalRentalPeriod), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> getUserHousesInfo(Long id) {
        record UserDeal(Long id, House house, Long idClient, String rentalStartDate,
                        Integer rentalDuration, String rentalEndDate, BigDecimal totalAmount, Boolean isRented) {
        }

        record UserDealList(List<UserDeal> deals) {
        }

        record UserHousesInfo(Long id, String photoName, String address, Integer countParking, BigDecimal price,
                              String place, Timestamp endDate, Timestamp startDate, Boolean rented,
                              String description) {
        }

        try (var session = sessionController.openSession()) {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

            String userHousesInfoQuery = "FROM Rented_House r WHERE r.idClient = :id";
            var queryTotalRental = session.createQuery(userHousesInfoQuery, RentedHouse.class);
            queryTotalRental.setParameter("id", id);

            var dbDeals = queryTotalRental.list();
            if (dbDeals.isEmpty()) {
                return ExceptionHandler.handleInfoException(ResponseMessage.HOUSES_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            List<UserDeal> deals = new ArrayList<>();
            for (var deal : dbDeals) {
                House house = getHouseById(deal.getIdHouse());
                Boolean isRented = deal.getRentalEndDate().after(new Timestamp(System.currentTimeMillis()));

                UserDeal userDeal = new UserDeal(deal.getId(), house, deal.getIdClient(),
                        deal.getRentalStartDate().toString().split(" ")[0], deal.getRentalDuration(),
                        deal.getRentalEndDate().toString().split(" ")[0], deal.getTotalAmount(), isRented);

                deals.add(userDeal);

            }

            return new ResponseEntity<>(new UserDealList(deals), HttpStatus.OK);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    private House getHouseById(Long id) {
        try (var session = sessionController.openSession()) {
            return session.get(House.class, id);
        } catch (Exception e) {
            return null;
        }
    }

    public ResponseEntity<?> createDeal(RentedHouse newDeal) {
        try (var session = sessionController.openSession()) {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            // проверка что дом свободен
            var freeHouses = session.createNativeQuery("select * from FreeHouse where id = :id", House.class)
                    .setParameter("id", newDeal.getIdHouse())
                    .uniqueResult();

            if (freeHouses == null) {
                return ExceptionHandler.handleInfoException(ResponseMessage.DEAL_ALREADY_EXISTS, HttpStatus.CONFLICT);
            }

            // установка дат
            LocalDateTime currentDateTime;
            if (newDeal.getRentalStartDate() == null) {
                currentDateTime = LocalDateTime.now();
                newDeal.setRentalStartDate(Timestamp.valueOf(currentDateTime));
                newDeal.setRentalEndDate(Timestamp.valueOf(currentDateTime.plusDays(newDeal.getRentalDuration())));
            } else {
                currentDateTime = newDeal.getRentalStartDate().toLocalDateTime();
                newDeal.setRentalEndDate(Timestamp.valueOf(currentDateTime.plusDays(newDeal.getRentalDuration())));
            }

            session.beginTransaction();
            session.persist(newDeal);
            session.getTransaction().commit();

            return new ResponseEntity<>(newDeal, HttpStatus.CREATED);

        } catch (ConstraintViolationException e) {
            return ExceptionHandler.handleInfoException(ResponseMessage.DEAL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> editDeal(RentedHouse editedDeal) {
        try (var session = sessionController.openSession()) {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

            LocalDateTime currentDateTime;
            if (editedDeal.getRentalStartDate() == null) {
                currentDateTime = LocalDateTime.now();
                editedDeal.setRentalStartDate(Timestamp.valueOf(currentDateTime));
                editedDeal.setRentalEndDate(Timestamp.valueOf(currentDateTime.plusDays(editedDeal.getRentalDuration())));
            } else {
                currentDateTime = editedDeal.getRentalStartDate().toLocalDateTime();
                editedDeal.setRentalEndDate(Timestamp.valueOf(currentDateTime.plusDays(editedDeal.getRentalDuration())));
            }

            session.beginTransaction();
            session.merge(editedDeal);
            session.getTransaction().commit();

            return new ResponseEntity<>(editedDeal, HttpStatus.CREATED);

        } catch (ConstraintViolationException e) {
            return ExceptionHandler.handleInfoException(ResponseMessage.DEAL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }

    public ResponseEntity<?> extendDeal(String extendInfoJSON) {
        try (var session = sessionController.openSession()) {
            var extendInfoMap = new ObjectMapper().readValue(extendInfoJSON, Map.class);

            Long idRentedHouse = Long.valueOf((String) extendInfoMap.get("idRentedHouse")).longValue();
            Integer additionalDays = Integer.valueOf((String) extendInfoMap.get("additionalDays")).intValue();
            BigDecimal additionalPrice = new BigDecimal((String) extendInfoMap.get("additionalPrice"));

            var deal = session.get(RentedHouse.class, idRentedHouse);
            var newDuration = deal.getRentalDuration() + additionalDays;
            var newPrice = deal.getTotalAmount().add(additionalPrice);

            deal.setRentalDuration(newDuration);
            deal.setTotalAmount(newPrice);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(deal.getRentalEndDate());
            calendar.add(Calendar.DATE, additionalDays);
            deal.setRentalEndDate(new Timestamp(calendar.getTime().getTime()));

            session.beginTransaction();
            session.merge(deal);
            session.getTransaction().commit();

            return new ResponseEntity<>(deal, HttpStatus.CREATED);

        } catch (ConstraintViolationException e) {
            return ExceptionHandler.handleInfoException(ResponseMessage.DEAL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return ExceptionHandler.handleServerException(e);
        }
    }
}
