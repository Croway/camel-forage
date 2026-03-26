# JDBC

Forage creates pooled datasources with connection management, optional XA transactions, and auxiliary repositories.

## Quick Start

```properties
forage.myDb.jdbc.db.kind=postgresql
forage.myDb.jdbc.url=jdbc:postgresql://localhost:5432/mydb
forage.myDb.jdbc.username=admin
forage.myDb.jdbc.password=secret
```

```yaml
- to:
    uri: sql
    parameters:
      query: select * from orders
      dataSource: "#myDb"
```

## Supported Databases

{{ forage_beans_table("DataSource", "javax.sql.DataSource") }}

## Properties

{{ forage_properties("DataSource") }}

## Multiple Datasources

Use different names to configure multiple databases:

```properties
forage.ordersDb.jdbc.db.kind=postgresql
forage.ordersDb.jdbc.url=jdbc:postgresql://db1:5432/orders

forage.analyticsDb.jdbc.db.kind=mysql
forage.analyticsDb.jdbc.url=jdbc:mysql://db2:3306/analytics
```
