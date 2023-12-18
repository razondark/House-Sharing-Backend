package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity(name = "Rented_House")
@Table(name = "Rented_House")
@SuppressWarnings("unused")
public class RentedHouse {
    @Id
    @Column(name = "id_house", nullable = false)
    private Long idHouse;

    @Id
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @Column(name = "rental_start_date", nullable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Europe/Moscow")
    private Timestamp rentalStartDate;

    @Column(name = "rental_duration", nullable = false)
    private Integer rentalDuration;

    @Column(name = "rental_end_date", nullable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Europe/Moscow")
    private Timestamp rentalEndDate;

    @Column(name = "total_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public Long getHouse() {
        return idHouse;
    }

    public void setHouse(Long house) {
        this.idHouse = house;
    }

    public Long getClient() {
        return idClient;
    }

    public void setClient(Long client) {
        this.idClient = client;
    }

    public Timestamp getRentalStartDate() {
        return rentalStartDate;
    }

    public void setRentalStartDate(Timestamp rentalStartDate) {
        this.rentalStartDate = rentalStartDate;
    }

    public Integer getRentalDuration() {
        return rentalDuration;
    }

    public void setRentalDuration(Integer rentalDuration) {
        this.rentalDuration = rentalDuration;
    }

    public Timestamp getRentalEndDate() {
        return rentalEndDate;
    }

    public void setRentalEndDate(Timestamp rentalEndDate) {
        this.rentalEndDate = rentalEndDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
