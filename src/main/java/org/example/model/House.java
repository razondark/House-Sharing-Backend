package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "House")
@SuppressWarnings("unused")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_link")
    private String photoLink;

    @Column(name = "address", nullable = false, length = 150)
    private String address;

    @Column(name = "parking_spaces_count", nullable = false)
    private Integer parkingSpacesCount = 0;

    @Column(name = "price_per_day", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "comfort_class", nullable = false, length = 100)
    private String comfortClass;

    @Column(name = "description")
    private String description;

    @Column(name = "discount_price", precision = 8, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "map_location", nullable = false, columnDefinition = "NUMERIC(10,7)[]")
    private BigDecimal[] mapLocation;

    @Column(name = "addition_date", nullable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Europe/Moscow")
    private Timestamp additionDate;

    @Column(name = "last_change_date", columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Europe/Moscow")
    private Timestamp lastChangeDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhotoLink() {
        return photoLink;
    }

    public void setPhotoLink(String photoLink) {
        this.photoLink = photoLink;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getParkingSpacesCount() {
        return parkingSpacesCount;
    }

    public void setParkingSpacesCount(Integer parkingSpacesCount) {
        this.parkingSpacesCount = parkingSpacesCount;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getComfortClass() {
        return comfortClass;
    }

    public void setComfortClass(String comfortClass) {
        this.comfortClass = comfortClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public BigDecimal[] getMapLocation() {
        return mapLocation;
    }

    public void setMapLocation(BigDecimal[] mapLocation) {
        this.mapLocation = mapLocation;
    }

    public Timestamp getAdditionDate() {
        return additionDate;
    }

    public void setAdditionDate(Timestamp additionDate) {
        this.additionDate = additionDate;
    }

    public Timestamp getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(Timestamp lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, parkingSpacesCount, pricePerDay, district, comfortClass, Arrays.hashCode(mapLocation));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        House house = (House) obj;
        return Objects.equals(photoLink, house.photoLink)
                && Objects.equals(address, house.address)
                && Objects.equals(parkingSpacesCount, house.parkingSpacesCount)
                && Objects.equals(pricePerDay, house.pricePerDay)
                && Objects.equals(district, house.district)
                && Objects.equals(comfortClass, house.comfortClass)
                && Objects.equals(description, house.description)
                && Objects.equals(discountPrice, house.discountPrice)
                && Arrays.equals(mapLocation, house.mapLocation);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }
}