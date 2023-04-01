CREATE TABLE [IF NOT EXISTS] db_logs (
    [timestamp] long,
    [level] varchar(10),
    [message] varchar(512),
);

(timestamp, level, message)