DROP TABLE track;

CREATE TABLE track (
    id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    vehicle_id int REFERENCES vehicle(id) ON DELETE SET NULL,
    coordinates GEOMETRY(Point, 4326),
    time_point varchar(50)
);

SELECT * FROM track;

DROP TABLE trip;

CREATE TABLE trip (
    id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    vehicle_id int REFERENCES vehicle(id) ON DELETE SET NULL,
    time_start varchar(50),
    time_end varchar(50)
);

SELECT * FROM trip;

