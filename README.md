# Websocket.in

This plugin can be used for websocket connections only.

For HTTP, TCP/UDP notification have a look at the [Notification Plugin](https://plugins.jenkins.io/notification/)

## Description

The Websocket.in plugin sends the build status to a given websocket URL.
It does this twice:

1. once before the build starts 

2. once after the build steps are performed


Even though the plugin is named Websocket.in, it should work with other websocket services just fine as well.

## Configuration

There are 4 parameters which can be used to configure the Websocket.in plugin: 
Url, prevent fails on failed connection, additional HTTP headers and an initial message.

### 1. URL (required)

The service [https://www.websocket.in](https://www.websocket.in/) lets you define an own websocket URL which looks like:
wss://connect.websocket.in/**YOUR\_CHANNEL\_ID**?room\_id=**YOUR\_ROOM\_ID**

### 2. Prevent fails on failed connection (required)

When checked, a failed connection to the given websocket URL will not fail the build.

### 3. Additional Http headers (optional)

This field allows to give additional http headers with the initial websocket request. This could be helpful for authentication.  

The additional http headers must follow a format that is recognized by the class java.util.Properties.

### 4. Initial message (optional)

If a value is given, it will be sent as the first message as soon as the websocket connection has been established. This could be helpful for authentication.

## Job Status

A compatible subset of the Notification Plugin in json format:

``` syntaxhighlighter-pre
{
    "name": "asgard",
    "url": "job/asgard/",
    "build": {
        "full_url": "http://localhost:8080/job/asgard/18/",
        "number": 18,
        "phase": "COMPLETED",
        "status": "SUCCESS",
        "url": "job/asgard/18/"
    }
}
```

  

  
