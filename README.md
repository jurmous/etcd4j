Etcd4j is a client library for [etcd](https://github.com/coreos/etcd), a highly available keystore. This library is based on 
Netty 4.1 and Java 7. It supports all key based etcd requests, can be secured with SSL and supports
defining multiple connection URLs and retries. It is completely async and works with promises to
retrieve the results. It also supports the etcd wait functionality to wait for future changes.

Etcd version support
====================
This project currently supports etcd 0.3 which first supports the v2 api and any above. It supports
all the key space features in 0.4.x.

Download
========

Maven
-----
```xml
<dependency>
  <groupId>org.mousio</groupId>
  <artifactId>etcd4j</artifactId>
  <version>0.4.6</version>
</dependency>
```

Gradle
------
```
compile 'org.mousio:etcd4j:0.4.6'
```

Manually
--------
Visit [etcd4j Github releases page](https://github.com/jurmous/etcd4j/releases)

Code examples
=============

Setup
-----

Setting up a client which connects to default 127.0.0.1:4001 URL. (auto closes Netty eventloop thanks to try-resource block)
```Java
try(EtcdClient etcd = new EtcdClient()){
  // Logs etcd version
  System.out.log(etcd.getVersion());
}
```

Setting up your own URLs
```Java
try(EtcdClient etcd = new EtcdClient(
    URI.create("http://123.45.67.89:8001"),
    URI.create("http://123.45.67.90:8001"))){
  // Logs etcd version
  System.out.log(etcd.getVersion());
}
```

Setting up SSL (You need to set up the server with SSL)
```Java
SslContext sslContext = SslContext.newClientContext();

try(EtcdClient etcd = new EtcdClient(sslContext,
    URI.create("https://123.45.67.89:8001"),
    URI.create("https://123.45.67.90:8001"))){
  // Logs etcd version
  System.out.log(etcd.getVersion());
}
```

Sending and retrieving
----------------------

Sending a request and retrieve values from response
```Java
try{
  EtcdKeysResponse response = client.putValue("foo", "bar").send().get();
  
  // Do something with response
}catch(EtcdException e){
  // Do something with the exception returned by etcd
}catch(IOException e){
  // Exception happened in the retrieval. Do something with it.
}catch(TimeoutException e){
  // Timeout happened. Do something
}
```

Options
-------

You can set multiple options on the requests before sending to the server like ttl and prev exist.
```Java
  EtcdKeysResponsePromise promise = client.putValue("foo", "bar").ttl(50).prevExist().send();
```

Promises
--------

All requests return a Promise after sending. You can send multiple requests async before retrieving 
their values
```Java
  EtcdKeysResponsePromise promise1 = client.putValue("foo", "bar").send();
  EtcdKeysResponsePromise promise2 = client.putValue("foo", "bar").send();
  
  // Call the promise in a blocking way
  try{
    EtcdKeysResponse response = promise1.get();
    // Do something with response
  }catch(EtcdException e){
    // Do something with the exception returned by etcd
  }catch(IOException | TimeoutException e){
    // Handle other types of exceptions
  }
  
  // or listen to it async (Java 8 lambda construction)
  promise2.addListener(promise -> {
    // getNow() returns null on exception
    EtcdKeysResponse response = promise.getNow();
    if(response != null){
      System.out.println(response.node.value);
    }
  });
```

Set timeout on requests
-----------------------
It is possible to set a timeout on all requests. By default there is no timeout.

```Java

// Timeout of 1 second on a put value
EtcdKeysResponsePromise putPromise = client.putValue("foo", "bar").timeout(1, TimeUnit.SECONDS).send();

try{
  EtcdKeysResponse r = putPromise.get();
}catch(TimeoutException e){
  //Handle timeout
}catch(Exception e){
  handle other types of exceptions
}


EtcdKeysGetRequest getRequest = client.get("foo").waitForChange().timeout(2, TimeUnit.MINUTES);

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

Handle disconnects
------------------

If you want to handle disconnects you need to catch the PrematureDisconnectException.
(Issue #4 tracks possible future built in retry handling)

```Java

// Wait for next change on foo
EtcdResponsePromise promise = etcd.get("foo").send();
try {
  EtcdKeysResponse key = response.get();
} catch (PrematureDisconnectException e) {
  // Handle Premature Disconnect exception.
} catch (IOException e) {
  e.printStackTrace();
} catch (EtcdException e) {
  e.printStackTrace();
} catch (TimeoutException e) {
  e.printStackTrace();
}

// Async version with a get with waitForChange:
EtcdResponsePromise promise = etcd.get("foo").waitForChange().send();
// Java 8 lambda construction
promise.addListener(promise -> {
  try {
    EtcdKeysResponse key = response.get();
  } catch (PrematureDisconnectException e) {
    // Handle Premature Disconnect exception.
  } catch (IOException e) {
    e.printStackTrace();
  } catch (EtcdException e) {
    e.printStackTrace();
  } catch (TimeoutException e) {
    e.printStackTrace();
  }
});


```

Put examples
------------
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

Get examples
------------
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

Delete examples
---------------
You need to read out the returned promises to see the response
```Java
// Simple delete
etcd.delete("foo").send();

// Directory and all subcontents delete
etcd.deleteDir("foo_dir").recursive().send();

etcd.delete("foo").prevIndex(3).send();

etcd.delete("foo").prevValue("bar4").send();

```

Post examples
------------
You need to read out the returned promises to see the response
```Java
// Simple post
etcd.post("queue","Job1").send();

// Post with ttl check
etcd.put("queue","Job2").ttl(20).send();
```