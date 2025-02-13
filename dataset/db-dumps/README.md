# Database Dumps

This folder contains the database dumps of both **MongoDB** and **Redis**.

These can be loaded into the respective databases using the following commands (Linux):

## MongoDB
```bash
tar -xvzf mongodb-dump.tar.gz
mongorestore --db library /path/to/dump/
```

## Redis
```bash
tar -xvzf redis-dump.tar.gz
cp /path/to/dump.rdb /var/lib/redis/dump.rdb
chown redis:redis /var/lib/redis/dump.rdb
chmod 660 /var/lib/redis/dump.rdb
```
