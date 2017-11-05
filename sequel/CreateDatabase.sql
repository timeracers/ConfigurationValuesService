CREATE SCHEMA configurations;

DROP TABLE IF EXISTS configurations.apps CASCADE;

CREATE TABLE configurations.apps (
   name   text   PRIMARY KEY
);

DROP TABLE IF EXISTS configurations.configProfiles CASCADE;

CREATE TABLE configurations.configProfiles (
   app   text   NOT NULL   REFERENCES configurations.apps (name)  ON DELETE CASCADE,
   config   text   NOT NULL,
   token   uuid   NOT NULL   UNIQUE,
   PRIMARY KEY (app, config)
);

DROP TABLE IF EXISTS configurations.configValues CASCADE;

CREATE TABLE configurations.configValues (
   token   uuid   PRIMARY KEY   REFERENCES configurations.configProfiles (token)  ON DELETE CASCADE,
   values   text   NOT NULL
);