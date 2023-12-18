CREATE DATABASE House-Sharing;

CREATE TABLE Client(
	id BIGSERIAL PRIMARY KEY,
	login VARCHAR(50) UNIQUE NOT NULL,
	password VARCHAR(150) NOT NULL,
	status VARCHAR(10) NOT NULL DEFAULT 'user',
	phone_number VARCHAR(15) UNIQUE NOT NULL,
	email VARCHAR(50) UNIQUE,
	balance DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE House(
	id BIGSERIAL PRIMARY KEY,
	photo_link TEXT,
	address VARCHAR(150) UNIQUE NOT NULL,
	parking_spaces_count INTEGER NOT NULL DEFAULT 0,
	price_per_day DECIMAL(8,2) NOT NULL,
	district VARCHAR(100) NOT NULL,
	comfort_class VARCHAR(100) NOT NULL,
	description TEXT,
	discount_price DECIMAL(8,2),
	map_location DECIMAL(10,7)[] NOT NULL,
	addition_date TIMESTAMP(6) DEFAULT NOW(),
	last_change_date TIMESTAMP(6)
);

CREATE TABLE Rented_House(
	id_house BIGSERIAL NOT NULL,
	id_client BIGSERIAL NOT NULL,
	rental_start_date TIMESTAMP(6) NOT NULL DEFAULT NOW(),
	rental_duration INTEGER NOT NULL CHECK (rental_duration BETWEEN 1 AND 31),
	rental_end_date TIMESTAMP(6) NOT NULL DEFAULT (NOW() + INTERVAL '1 day'),
	total_amount DECIMAL(8,2) NOT NULL,
	
	CONSTRAINT PK_RentedHouse_IDHouseIDClient PRIMARY KEY(id_house, id_client),
	CONSTRAINT FK_RentedHouse_IDHouse FOREIGN KEY(id_house)
		REFERENCES House(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
	CONSTRAINT FK_RentedHouse_IDClient FOREIGN KEY(id_client)
		REFERENCES Client(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE VIEW FreeHouse
AS
	SELECT * FROM house
	WHERE id NOT IN (SELECT id_house FROM rented_house)
		OR id IN (SELECT id_house FROM rented_house
				  WHERE rental_end_date < NOW());


CREATE OR REPLACE FUNCTION CalculateRentalEndDate() RETURNS TRIGGER AS $$
BEGIN
    NEW.rental_end_date := NEW.rental_start_date + make_interval(days => NEW.rental_duration);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER CalculateRentalEndDateTrigger
BEFORE INSERT ON Rented_House
FOR EACH ROW EXECUTE FUNCTION CalculateRentalEndDate();



-- Добавление 3 записей в таблицу Client
INSERT INTO Client (login, password, phone_number, email, balance)
VALUES 
('user1', 'password1', '1234567890', 'user1@example.com', 100.00),
('user2', 'password2', '2345678901', 'user2@example.com', 200.00),
('user3', 'password3', '3456789012', 'user3@example.com', 300.00);

-- Добавление 3 записей в таблицу House
INSERT INTO House (photo_link, address, parking_spaces_count, price_per_day, district, comfort_class, description, discount_price, map_location, addition_date)
VALUES 
  ('http://example.com/photo1.jpg', '123 Main St', 2, 150.00, 'Central', 'Luxury', 'Beautiful house with a great view', NULL, '{40.7128,-74.0060}', '2023-01-01 12:00:00'),
  ('http://example.com/photo2.jpg', '456 Oak St', 1, 120.00, 'Suburb', 'Standard', 'Cozy home with a garden', 100.00, '{34.0522,-118.2437}', '2023-02-15 14:30:00'),
  ('http://example.com/photo3.jpg', '789 Pine St', 0, 80.00, 'Downtown', 'Economy', 'Compact apartment in the city center', NULL, '{41.8781,-87.6298}', '2023-03-30 10:45:00');

-- Добавление 3 записей в таблицу Rented_House
INSERT INTO Rented_House (id_house, id_client, rental_duration, total_amount)
VALUES 
(1, 1, 7, 700.00),
(2, 2, 14, 2100.00),
(3, 3, 21, 4200.00);
