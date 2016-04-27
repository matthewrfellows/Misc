
# Create Database

CREATE DATABASE `fcfreddata`;

# Create Tables

CREATE TABLE `fcfreddata`.`obs_revisions` (
`id` int(11) unsigned NOT NULL AUTO_INCREMENT,
`series` VARCHAR(50) NOT NULL,
`rt_start` Date NOT NULL,
`rt_end` Date NOT NULL,
`date` Date NOT NULL,
`value` Decimal(8,2),
PRIMARY KEY (`id`),
KEY `series` (`series`),
KEY `date` (`date`),
UNIQUE KEY `series_dates` (`rt_start`,`rt_end`,`series`,`date`)
) ENGINE=InnoDB;

CREATE TABLE `fcfreddata`.`observations` (
`id` int(11) unsigned NOT NULL AUTO_INCREMENT,
`series` VARCHAR(50) NOT NULL,
`date` Date NOT NULL,
`value` Decimal(8,2),
PRIMARY KEY (`id`),
KEY `series` (`series`),
KEY `date` (`date`),
UNIQUE KEY `series_date` (`series`,`date`)
) ENGINE=InnoDB;

