if exists(select * from sys.tables where name = 'PENNYLANE_REFERENTIALS')
    drop table PENNYLANE_REFERENTIALS

create table PENNYLANE_REFERENTIALS
(
    PARAGRAPHE varchar(100) primary key,
    CODE_PENNYLANE varchar(200) not null,
    CODE_ATH varchar(200) not null
)

INSERT INTO PENNYLANE_REFERENTIALS values
('','', '')