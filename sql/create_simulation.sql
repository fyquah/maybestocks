DROP TABLE IF EXISTS simulation;
CREATE TABLE simulation (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    symbol VARCHAR(10) DEFAULT NULL,
    date_ex date DEFAULT NULL,
    open_p decimal(10,4) default NULL,
    close_p decimal(10,4) DEFAULT NULL,
    action VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    INDEX(symbol)
);
