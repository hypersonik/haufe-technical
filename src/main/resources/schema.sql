-- DDL
CREATE TABLE MANUFACTURER (
     ID BIGINT NOT NULL AUTO_INCREMENT,
     NAME CHARACTER VARYING(30) NOT NULL,
     COUNTRY CHARACTER VARYING(30),
     CREATED_AT TIMESTAMP WITH TIME ZONE,
     UPDATED_AT TIMESTAMP WITH TIME ZONE,
     CONSTRAINT MANUFACTURER_PK PRIMARY KEY (ID),
     CONSTRAINT MANUFACTURER_UNIQUE UNIQUE (NAME)
);

CREATE TABLE BEER (
     ID BIGINT NOT NULL AUTO_INCREMENT,
     NAME CHARACTER VARYING(30) NOT NULL,
     ABV REAL,
     "STYLE" CHARACTER VARYING(25),
     DESCRIPTION CHARACTER VARYING(200),
     CREATED_AT TIMESTAMP WITH TIME ZONE,
     UPDATED_AT TIMESTAMP WITH TIME ZONE,
     CONSTRAINT BEER_PK PRIMARY KEY (ID)
);

CREATE INDEX BEER_NAME_IDX ON BEER (NAME);

-- Data
INSERT INTO MANUFACTURER (NAME, COUNTRY, CREATED_AT, UPDATED_AT)
VALUES('Lo Vilot', 'ES', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Brewdog', 'UK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Mikkeller', 'DK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Stone Brewing', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Sierra Nevada', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Brooklyn Brewery', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Lagunitas Brewing Company', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Trillium Brewing Company', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Cantillon Brewery', 'BE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('De Ranke Brewery', 'BE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO BEER (NAME, ABV, "STYLE", DESCRIPTION, CREATED_AT, UPDATED_AT)
VALUES('Apricot Dispersion', 6, 'Mixed fermentation', 'Cervesa de fermentació mixta macerada amb albercoc local.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Brett Saison', 5.5, 'Saison', 'A dry and fruity beer with a complex aroma of spices and citrus.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Citra Pale Ale', 5.2, 'Pale Ale', 'A hoppy and refreshing beer with a citrusy aroma.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Double IPA', 8, 'IPA', 'A strong and hoppy beer with a high alcohol content.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Elderflower Saison', 4.5, 'Saison', 'A light and floral beer with a hint of elderflower.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Flanders Red Ale', 6, 'Flanders Red Ale', 'A sour and fruity beer with a deep red color.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Gose', 4.2, 'Gose', 'A salty and sour beer with a hint of coriander.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Hazy IPA', 6.5, 'IPA', 'A hazy and juicy beer with a tropical fruit aroma.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Imperial Stout', 10, 'Stout', 'A rich and dark beer with a high alcohol content.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Jolly Pumpkin Oro de Calabaza', 8, 'Belgian Strong Ale', 'A complex and spicy beer with a hint of oak.',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
