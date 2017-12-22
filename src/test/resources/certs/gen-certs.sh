#!/bin/bash

openssl genrsa 1024 > host.key
chmod 400 host.key

openssl req -new -x509 -nodes -sha1 -days 365 -key host.key -out host.cert
openssl req -new -x509 -nodes -sha1 -days 365 -key host.key -out client.cert

openssl pkcs12 -export -inkey host.key -in host.cert -name test -out host.p12
keytool -importkeystore -srckeystore host.p12 -srcstoretype pkcs12 -destkeystore truststore.jks

openssl pkcs12 -export -inkey host.key -in client.cert -name test -out client.p12
keytool -importkeystore -srckeystore client.p12 -srcstoretype pkcs12 -destkeystore keystore.jks