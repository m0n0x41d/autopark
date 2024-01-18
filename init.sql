CREATE TABLE vehicle (
	id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	registration_number varchar(9) NOT NULL,
	year_of_production int NOT NULL,
	price double precision NOT NULL,
	mileage double precision NOT NULL
);

INSERT INTO vehicle (registration_number, year_of_production, price, mileage) VALUES ('A125CT163', 2000, 300000, 200);
INSERT INTO vehicle (registration_number, year_of_production, price, mileage) VALUES ('C548MK174', 2015, 400000, 100);
INSERT INTO vehicle (registration_number, year_of_production, price, mileage) VALUES ('A125CT102', 2020, 500000, 50);

SELECT * FROM vehicle;
