CREATE TABLE `fcfreddata`.`observations` (
`id` int(11) unsigned NOT NULL AUTO_INCREMENT,
`series` VARCHAR(50) NOT NULL,
`rt_start` Date NOT NULL,
`rt_end` Date NOT NULL,
`date` Date NOT NULL,
`value` Decimal(8,2),
PRIMARY KEY (`id`),
KEY `series` (`series`),
KEY `date` (`date`)
) ENGINE=InnoDB;
