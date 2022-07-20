gRPC Hook-In
==============================================

The project is organized in two main modules:
- *delivery*, the use case application, made of different gRPC services (Order, Driver,...)
- *hook-in*, our gRPC privacy hook-in component

In order to generate the keys for the KeyServer, it's possibile to launch
the file /hook-in/src/main/resources/privateKeys/keys_generator.sh

Then the gRPC services KeyServer, Routing, Restaurant, Order, Driver shall be started.

The use case can be tested by launching the Client application, that generates an OrderRequest to send to the OrderService.

The configuration of the Access Control and Data Minimization is saved in 
/delivery/src/main/resources/config.json