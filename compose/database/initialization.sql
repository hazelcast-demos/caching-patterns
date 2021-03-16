CREATE DATABASE IF NOT EXISTS store;

USE store;

CREATE TABLE IF NOT EXISTS Person
(
    id         BIGINT PRIMARY KEY NOT NULL,
    first_name VARCHAR(80)        NOT NULL,
    last_name  VARCHAR(80)        NOT NULL,
    birthdate  DATE
);

INSERT INTO Person(id, first_name, last_name, birthdate)
VALUES (1, 'Joe', 'Dalton', '1970-01-02'),
       (2, 'Jack', 'Dalton', '1973-04-05'),
       (3, 'William', 'Dalton', '1976-07-08'),
       (4, 'Averell', 'Dalton', '1979-10-11'),
       (5, 'Ma', 'Dalton', NULL);