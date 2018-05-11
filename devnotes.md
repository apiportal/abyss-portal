# JDBC URL içinde db şema verilmesi

``` java
JsonObject jdbcClientConfig = new JsonObject()
                .put("url", "jdbc:postgresql://192.168.10.40:5432/abyssportal?currentSchema=portalschema")}
```
You can set the default search_path at the database level:

    ALTER DATABASE <database_name> SET search_path TO schema1,schema2;

Or at the user or role level:

    ALTER ROLE <role_name> SET search_path TO schema1,schema2;
    
Or add parameter to connection URL:

    https://jdbc.postgresql.org/documentation/head/connect.html

#Service Discovery kullanımı
## discovery record yaratılması

``` java
Record record = JDBCDataSource.createRecord(
        Constants.PORTAL_DATA_SOURCE_SERVICE,
        new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
        new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                .put("max_pool_size", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
);
```
## record publish edilmesi

``` java
AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().publish(record, asyncResult -> {
    if (asyncResult.succeeded()) {
        logger.info("serviceDiscovery.publish OK..." + asyncResult.succeeded());
    } else {
        logger.error("serviceDiscovery.publish failed..." + asyncResult.cause());
        start.fail(asyncResult.cause());
    }
});
```
## discovery'den record olarak service lookup yapılması
### yöntem 1
``` java
AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getRecord(
        new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE),
        asyncResult -> {
            if (asyncResult.succeeded() && asyncResult.result() != null) {
                // Retrieve the service reference
                ServiceReference serviceReference = AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getReference(asyncResult.result());

                // Retrieve the service object
                JDBCClient client = serviceReference.getAs(JDBCClient.class);


                //...

                serviceReference.release();
            }
        }
);
````
### yöntem 2
``` java
JDBCDataSource.<JsonObject>getJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(),
        new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE),
        ar -> {
            if (ar.succeeded()) {
                JDBCClient client = ar.result();
                //JDBCClient jdbcClient = JDBCClient.createShared(vertx, new JsonObject(),ar.toString());

                // ...

                // Dont' forget to release the service
                ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), client);

            }
        });
```