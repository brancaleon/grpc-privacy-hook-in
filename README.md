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

# How to use the Hook-in on Client-Side

If you are the developer of an application, that should send a gRPC-request to a server, that uses this component for privacy, you need to sign the requests with a private key and the user id, that is stored at the server.
It is recommended to use the clientSide package of this project to do so. It signs the request and puts a purpose in the message metadata.
You need to change the creation of the method stub.

**Example**

Original method stub creation for a order-service:

~~~
OrderServiceGrpc.OrderServiceBlockingStub orderStub = OrderServiceGrpc.newBlockingStub(channel);
~~~

Replace it by this:

~~~
OrderServiceGrpc.OrderServiceBlockingStub orderStub = OrderServiceGrpc
        .newBlockingStub(channel)
        .withCallCredentials(new AccessControlClientCredentials("your client ID", "the request purpose", "path/to/your/private/key"));
~~~

# How to use the Hook-in on Server-side

## Add Access Control to a gRPC Server

**Intercept the server**

You need to add an interceptor to the server creation. If this is the standard server ...

~~~
server = ServerBuilder.forPort(this.port)
        .addService(new OrderImpl())
        .build()
        .start();
~~~

... then you add an access control interceptor like this:

~~~
server = ServerBuilder.forPort(this.port)
        .addService(new OrderImpl())
        .intercept(new AccessControlServerInterceptor("path/to/the/json/config"))
        .build()
        .start();
~~~

**Configure Access Permissions**

The path in the example above must point to a json file, that defines which accesses are allowed. Here is a example of a simple configuration file:

~~~
{
  "key_server": {
    "host": "localhost",
    "port": 50005
  },
  "purposes": {
    "my_purpose": {
      "name": "My Standard purpose",
      "allowed_clients": [
        "client1",
        "client2"
      ],
      "allowed_methods": [
        "proto.OrderService/OrderMeal"
      ]
    }
  }
}
~~~

The configuration consists of 2 parts: First you have to define the host and the port of a third party public key server.
The component will use this server to request public keys, when a request needs to be authorized.

Then the list of purposes is defined. Each purpose can allow clients to send requests for this purpose and allow methods to be requested for a specific purpose.
In this example client1 and client2 are allowed to access this server for the purpose of "my_purpose" and are then allowed to use the "OrderMeal"-method.

## Minimize Messages

**Extend the Configuration**

To enable minimization of message fields, you first have to extend the configuration file to define what minimization functions you want to apply on which field. All these mappings depend on purposes.

~~~
{
  "key_server": {
    "host": "localhost",
    "port": 50005
  },
  "purposes": {
    "my_purpose": {
      "name": "My Standard purpose",
      "allowed_clients": [
        "client1",
        "client2"
      ],
      "allowed_methods": [
        "proto.OrderService/OrderMeal"
      ],
      "minimization": {
        "OrderRequest": {
          "name": [
            {
              "function": "replace",
              "replace": "Mister X"
            }
          ]
        }
      }
    }
  }
}
~~~

A minimization block was added to "my_purpose". This example would apply the replacement function to the field name in an "OrderRequest"-message.
Most of the provided minimization functions have some parameters. You can define parameters directly under the function name.
In this example the parameter "replace" is set to "Mister X", which means that the field name will always be replaces by this String.

You could also configure a list of multiple functions for one field. In this case all functions build a chain, i.e. they would be applied one after another.

**Extend the interception**

To make it working, we need to extend the interception at the server creation as well:

~~~
DataMinimizerInterceptor minimizer = DataMinimizerInterceptor.newBuilder("path/to/the/json/config").build();
server = ServerBuilder.forPort(this.port)
        .addService(new OrderImpl())
        .intercept(new AccessControlServerInterceptor("path/to/the/json/config"))
        .intercept(minimizer)
        .build()
        .start();
~~~

This would intercept messages in all directions (i.e. requests as well as responses).
If you don't want that, you can switch one option off:

~~~
DataMinimizerInterceptor requestMinimizer = DataMinimizerInterceptor.newBuilder("path/to/the/json/config").withoutResponseIntercepting().build();
DataMinimizerInterceptor responseMinimizer = DataMinimizerInterceptor.newBuilder("path/to/the/json/config").withoutRequestIntercepting().build();
~~~

## Overview of the predefined Minimization Functions

There are some minimization functions predefined, that cover the main use cases. After the function name is stated, to which gRPC-field-types this functions can be applied.

**erasure**
(all types)

Simply deletes all values in the field. No configuration parameters.

**replace**
(String, Int, Long, Boolean)

Replaces all values in the field by a configured default.

|Parameter|Description|Default|
|---|---|---|
|replace|The value by which any input will be replaced|"" for String field, 0 for numeric field, true for boolean field|

'***' (String)

Replaces each char of a string by a *, i.e. the length of the string remains the same.

| Parameter   | Description                                                               | Default |
|-------------|---------------------------------------------------------------------------|---------|
| readableEnd | Number of chars at the end of the string, that should remain in cleartext | 0       |

**generalize** (Int, Long, Double)

Divides the space of numbers into different bins. A incoming value is replaced by a value, that represents its bin, i.e. all values of one bin are not distinguishable any more.

| Parameter         | Description                                                                                                                   | Default |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------|---------|
| binSize           | size of the bin                                                                                                               | 10      |
| offset            | offset of the starting value of each bin (e.g. binSize=10, offset=5 -> first bin is 5-14)                                     | 0       |
| representerOffset | value by which each bin is represented and by which field values are replaced (e.g. 3 -> values in bin 0-9 are replaced by 3) | 0       |

**gaussianNoise**
(Double)

Adds randomly generated gaussian noise to the field value (can be positive or negative).

| Parameter | Description                           | Default |
|-----------|---------------------------------------|---------|
| variance  | Variance of the gaussian distribution | 1.0     |

**hashing**
(String)

Replaces a string by a hash-value (SHA-256 is used). No configuration parameters.

## Define custom Minimization Functions

It is possible to define custom minimization functions and use them in the configuration file.
First you need to know, that every minimization function is basically a collection of different operators for different gRPC field types.
You don't have to define operators for all gRPC types, but you can define operators for multiple types.
However, in the configuration you can only map functions to fields types, for which the function has a defined operator, otherwise erasure will be applied instead.

Let's implement as an example a function that adds a value to integer- or long-fields.
First you create a new MinimizationFunction-Object and define the operator for integers.
The operator is a generic interface, that includes only the apply-function:

~~~
MinimizationFunction addFunction = new MinimizationFunction();
addFunction.addIntOperator(new ConfigurableOperator<Integer>() {
    @Override
    public Integer apply(Integer value, Map<String, String> parameters) {
        return value+Integer.parseInt(parameters.getOrDefault("addValue", "1"));
    }
});
~~~

The 'value' is the value that is originally set in the gRPC-message, i.e. that should be privacy minimized.
'parameters' is a map, that includes all parameters that are set in the json-configuration.
The returned value will be set in the new message. You can also return null to erase this field.

Since the interface ConfigurableOperator<T.> simply extends the functional interface BiFunction<T, Map<String, String>, T>, you can also define operators as lambda-expression.
We will do this for the long-operator:

~~~
addFunction.addLongOperator(
        (value, parameters) -> value+Integer.parseInt(parameters.getOrDefault("addValue", "1")));
~~~

Finally, you have to pass the new custom minimization function to the minimizer. At this step you can choose a function name (e.g. "add"), by which you can call this function in the json configuration.

~~~
DataMinimizerInterceptor minimizer = DataMinimizerInterceptor.newBuilder("path/to/the/json/config")
        .defineMinimizationFunction("add", addFunction)
        .build();
~~~