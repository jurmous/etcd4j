Etcd4j is a client library for [etcd](https://github.com/coreos/etcd), a highly available keystore. This library is based on 
Netty 4.1 and Java 7. It supports all key based etcd requests, can be secured with SSL and supports
defining multiple connection URLs and custom retry policies. It is completely async and works with promises to
retrieve the results. It also supports the etcd wait functionality to wait for future changes.

# Etcd version support
This project supports any etcd client which supports the etcd v2 api. This is any etcd version up
 from etcd 0.3 to 2.x

# Download

## Maven
```xml
<dependency>
  <groupId>org.mousio</groupId>
  <artifactId>etcd4j</artifactId>
  <version>2.12.0</version>
</dependency>
```

## Gradle
```
compile 'org.mousio:etcd4j:2.12.0'
```

## Manually
Visit [etcd4j Github releases page](https://github.com/jurmous/etcd4j/releases)

# Code examples

## Setup

Setting up a client which connects to default 127.0.0.1:4001 URL. (auto closes Netty eventloop thanks to try-resource block)
```Java
try(EtcdClient etcd = new EtcdClient()){
  // Logs etcd version
  System.out.println(etcd.getVersion());
}
```

Setting up your own URLs
```Java
try(EtcdClient etcd = new EtcdClient(
    URI.create("http://123.45.67.89:8001"),
    URI.create("http://123.45.67.90:8001"))){
  // Logs etcd version
  System.out.println(etcd.getVersion());
}
```

Setting up SSL (You need to set up the server with SSL)
```Java
SslContext sslContext = SslContext.newClientContext();

try(EtcdClient etcd = new EtcdClient(sslContext,
    URI.create("https://123.45.67.89:8001"),
    URI.create("https://123.45.67.90:8001"))){
  // Logs etcd version
  System.out.println(etcd.getVersion());
}
```

## Sending and retrieving

Sending a request and retrieve values from response
```Java
try{
  EtcdKeysResponse response = client.put("foo", "bar").send().get();

  // Prints out: bar
  System.out.println(response.node.value);
}catch(EtcdException e){
  // Do something with the exception returned by etcd
}catch(IOException e){
  // Exception happened in the retrieval. Do something with it.
}catch(TimeoutException e){
  // Timeout happened. Do something
}
```

## Options

You can set multiple options on the requests before sending to the server like ttl and prev exist.
```Java
  EtcdKeysResponsePromise promise = client.put("foo", "bar").ttl(50).prevExist().send();
```

## Promises

All requests return a Promise after sending. You can send multiple requests async before retrieving
their values
```Java
  EtcdKeysResponsePromise promise1 = client.put("foo", "bar").send();
  EtcdKeysResponsePromise promise2 = client.put("foo", "bar").send();

  // Call the promise in a blocking way
  try{
    EtcdKeysResponse response = promise1.get();
    // Do something with response
  }catch(EtcdException e){
    if(e.isErrorCode(EtcdErrorCode.NodeExist)){
        // Do something with error code
    }
    // Do something with the exception returned by etcd
  }catch(IOException | TimeoutException e){
    // Handle other types of exceptions
  }

  // or listen to it async (Java 8 lambda construction)
  promise2.addListener(promise -> {
    Throwable t = promise.getException();
    if(t instanceof EtcdException){
        if(((EtcdException) t).isErrorCode(EtcdErrorCode.NodeExist)){
            // Do something with error code
        }
    }

    // getNow() returns null on exception
    EtcdKeysResponse response = promise.getNow();
    if(response != null){
      System.out.println(response.node.value);
    }
  });
```


## Put examples
You need to read out the returned promises to see the response
```Java
// Simple put
etcd.put("foo","bar").send();

// Put a new dir
etcd.putDir("foo_dir").send();

// Put with ttl and prevexists check
etcd.put("foo","bar2").ttl(20).prevExist().send();

// Put with prevValue check
etcd.put("foo","bar3").prevValue("bar2").send();

// Put with prevIndex check
etcd.put("foo","bar4").prevIndex(2).send();

```

## Get examples
You need to read out the returned promises to see the response.

```Java
// Simple key fetch
etcd.get("foo").send();

// Get all nodes and all nodes below it recursively
etcd.getAll().recursive().send();
// Gets directory foo_dir and all nodes below it recursively
etcd.getDir("foo_dir").recursive().send();

// Wait for next change on foo
EtcdResponsePromise promise = etcd.get("foo").waitForChange().send();
// Java 8 lambda construction
promise.addListener(promise -> {
  // do something with change
});

// Wait for change of foo with index 7
etcd.get("foo").waitForChange(7).send();

// Get all items recursively below queue as a sorted list
etcd.get("queue").sorted().recursive().send();

```

## Delete examples
You need to read out the returned promises to see the response
```Java
// Simple delete
etcd.delete("foo").send();

// Directory and all subcontents delete
etcd.deleteDir("foo_dir").recursive().send();

etcd.delete("foo").prevIndex(3).send();

etcd.delete("foo").prevValue("bar4").send();

```

## Post examples
You need to read out the returned promises to see the response
```Java
// Simple post
etcd.post("queue","Job1").send();

// Post with ttl check
etcd.put("queue","Job2").ttl(20).send();
```

## Set timeout on requests
It is possible to set a timeout on all requests. By default there is no timeout.

```Java

// Timeout of 1 second on a put value
EtcdKeysResponsePromise putPromise = client.put("foo", "bar").timeout(1, TimeUnit.SECONDS).send();

try{
  EtcdKeysResponse r = putPromise.get();
}catch(TimeoutException e){
  //Handle timeout
}catch(Exception e){
  handle other types of exceptions
}


EtcdKeyGetRequest getRequest = client.get("foo").waitForChange().timeout(2, TimeUnit.MINUTES);

try{
  r = getRequest.send().get();
}catch(TimeoutException | IOException e){
  try{
    // Retry again once
    r = getRequest.send().get();
  }catch(TimeoutException | IOException e){
    // Fails again... Maybe wait a bit longer or give up
  }catch(Exception e){
    // Handle other types of exceptions
  }
}catch(Exception e){
  // Handle other types of exceptions
}

```

## Set a Retry Policy
By default etcd4j will retry with an exponential back-off algorithm starting with a 20ms interval and
 will back off to a 10000 ms interval. It will also retry indefinitely.
There are more settings on the exponential backoff algorithm to set a retry limit and there are also
some alternative policies like retries with timeout, with a max retry count or just retry once.
Check the ```mousio.client.retry``` package for more details.

```Java

// Set the retry policy for all requests on a etcd client connection
// Will retry with an interval of 200ms with timeout of a total of 20000ms
etcd.setRetryHandler(new RetryWithTimeout(200, 20000));

// Set the retry policy for only one request
// Will retry 2 times with an interval of 300ms
EtcdKeysResponse response = etcd.get("foo").setRetryPolicy(new RetryNTimes(300, 2)).send().get();
```

# Logging

The framework logs its connects, retries and warnings with slf4j. (Simple Logging Facade for Java)

You need to add a binding to your own logging framework to see the logs.

If you want to see the logs in the console use the simple binding.

Maven
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.7</version>
</dependency>
```

Gradle
```
compile 'org.slf4j:slf4j-simple:1.7.7'
```

# Custom parameters on EtcdNettyClient

It is possible to control some parameters on the Netty client of etcd4j.

You can set the following parameters in a config:

* Netty Event Loop: You can set the Event Loop to use by the client so you can recycle existing
event loop groups or use for example Epoll based event loop groups. Be sure to change the socket
channel class if you not use a NioEventLoopGroup. By default it will create a shared NioEventLoopGroup;
* Socket channel class: You can set the socket channel class here. Default is NioSocketChannel.class
* Connect timeout: The timeout of the Netty client itself. Default is 300ms
* Max Frame size: The max frame size of the packages. Default is 100KiB (100 * 1024)
* Host name: The name which Host header will report. Default is hostname:port of the server which is
connected to.

To create an Etcd client with a custom timeout and Netty event loop:
```Java
    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setHostName("www.example.net")
        .setEventLoopGroup(customEventLoop);
        // .setEventLoopGroup(customEventLoop, false); // don't close event loop group when etcd client close

    nettySslContext
    try(EtcdClient etcd = new EtcdClient(new EtcdNettyClient(config, sslContext, URI.create(uri)))){
      // Use etcd client here
    }
```

# Utils

Etcd contains some utils that can make it easier to set up etcd4j.

* mousio.client.util.SRV2URIs - A utility class to resolve DNS SRV addresses to a list of URIs. SRV addresses are also supported by [etcd itself](https://github.com/coreos/etcd/blob/master/Documentation/clustering.md#dns-discovery). 
