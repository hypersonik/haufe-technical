-- DDL
CREATE TABLE MANUFACTURER (
    ID BIGINT NOT NULL AUTO_INCREMENT,
    NAME CHARACTER VARYING(30) NOT NULL,
    COUNTRY CHARACTER VARYING(30),
    CREATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT MANUFACTURER_PK PRIMARY KEY (ID),
    CONSTRAINT MANUFACTURER_UNIQUE UNIQUE (NAME)
);

CREATE TABLE BEER (
    ID BIGINT NOT NULL AUTO_INCREMENT,
    NAME CHARACTER VARYING(30) NOT NULL,
    ABV REAL,
    "STYLE" CHARACTER VARYING(25),
    DESCRIPTION CHARACTER VARYING(200),
    MANUFACTURER_ID BIGINT NOT NULL,
    CREATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT BEER_PK PRIMARY KEY (ID),
    CONSTRAINT BEER_MANUFACTURER_FK FOREIGN KEY (MANUFACTURER_ID) REFERENCES MANUFACTURER(ID) ON DELETE CASCADE ON UPDATE RESTRICT
);
CREATE INDEX BEER_NAME_IDX ON BEER (NAME);

CREATE TABLE "USER" (
    ID BIGINT NOT NULL AUTO_INCREMENT,
    NAME CHARACTER VARYING(30) NOT NULL,
    PASSWORD CHARACTER VARYING(200) NOT NULL,
    ROLES CHARACTER VARYING(60) NOT NULL,
    ENABLED BOOLEAN NOT NULL DEFAULT TRUE,
-- The user can be a manufacturer, so we add this field to link the user to the manufacturer
-- If the user is not a manufacturer, this field will be NULL
    MANUFACTURER_ID BIGINT,
    CREATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT USER_PK PRIMARY KEY (ID),
    CONSTRAINT USER_UNIQUE UNIQUE (NAME)
);

-- Data
INSERT INTO MANUFACTURER (NAME, COUNTRY)
VALUES
    ('Lo Vilot', 'ES'),
    ('Brewdog', 'UK'),
    ('Mikkeller', 'DK'),
    ('Stone Brewing', 'US'),
    ('Sierra Nevada', 'US'),
    ('Brooklyn Brewery', 'US'),
    ('Lagunitas Brewing Company', 'US'),
    ('Trillium Brewing Company', 'US'),
    ('Cantillon Brewery', 'BE'),
    ('De Ranke Brewery', 'BE');

INSERT INTO "USER" (NAME, PASSWORD, ROLES, MANUFACTURER_ID)
VALUES
    ('admin', '{bcrypt}$2a$12$RDgzTiZFtNEaFuzXz19H3OufGKEruJHhxDRNAiepxcynkK4LXinF6', 'ADMIN', NULL),
    ('Lo Vilot', '{bcrypt}$2a$12$YZqpg8oKM.VDJ0uSmN0I4ufKbtpybP9jKE7.AScIz0CZCmTydFr7m', 'MANUFACTURER',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Lo Vilot'));

INSERT INTO BEER (NAME, ABV, "STYLE", DESCRIPTION, MANUFACTURER_ID)
VALUES
    ('Apricot Dispersion', 6, 'Mixed fermentation', 'Cervesa de fermentació mixta macerada amb albercoc local.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Lo Vilot')),
    ('Brett Saison', 5.5, 'Saison', 'A dry and fruity beer with a complex aroma of spices and citrus.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Brewdog')),
    ('Citra Pale Ale', 5.2, 'Pale Ale', 'A hoppy and refreshing beer with a citrusy aroma.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Mikkeller')),
    ('Double IPA', 8, 'IPA', 'A strong and hoppy beer with a high alcohol content.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Stone Brewing')),
    ('Elderflower Saison', 4.5, 'Saison', 'A light and floral beer with a hint of elderflower.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Sierra Nevada')),
    ('Flanders Red Ale', 6, 'Flanders Red Ale', 'A sour and fruity beer with a deep red color.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Brooklyn Brewery')),
    ('Gose', 4.2, 'Gose', 'A salty and sour beer with a hint of coriander.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Lagunitas Brewing Company')),
    ('Hazy IPA', 6.5, 'IPA', 'A hazy and juicy beer with a tropical fruit aroma.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Trillium Brewing Company')),
    ('Imperial Stout', 10, 'Stout', 'A rich and dark beer with a high alcohol content.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'Cantillon Brewery')),
    ('Jolly Pumpkin Oro de Calabaza', 8, 'Belgian Strong Ale', 'A complex and spicy beer with a hint of oak.',
        (SELECT ID FROM MANUFACTURER WHERE NAME = 'De Ranke Brewery'));
