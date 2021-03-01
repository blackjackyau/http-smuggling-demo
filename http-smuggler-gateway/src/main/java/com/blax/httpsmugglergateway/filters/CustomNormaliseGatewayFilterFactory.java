package com.blax.httpsmugglergateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomNormaliseGatewayFilterFactory extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {

    public CustomNormaliseGatewayFilterFactory() {
        super(NameConfig.class);
    }

    @Override
    public GatewayFilter apply(NameConfig config) {
        return (exchange, chain) -> {

            System.out.println("config" + config.getName());

            HttpHeaders headers = exchange.getRequest().getHeaders();
            Map<String, String> normalisedMap = new HashMap<>();
            for (Map.Entry<String, List<String>> stringListEntry : headers.entrySet()) {
                String key = stringListEntry.getKey();
                String normalisedKey = key.replace("_", "-");
                if (!key.equals(normalisedKey)) {
                    normalisedMap.put(key, normalisedKey);
                    System.out.println("normalisedKey " + key.toString());
                }
            }

            ServerHttpRequest request = exchange.getRequest().mutate().headers(httpHeaders -> {
                for (Map.Entry<String, String> normalisedMapEntry : normalisedMap.entrySet()) {
                    List<String> values = headers.get(normalisedMapEntry.getKey());
                    httpHeaders.remove(normalisedMapEntry.getKey());
                    httpHeaders.put(normalisedMapEntry.getValue(), values);
                }
            }).build();


            System.out.println("normalised headers " + request.getHeaders().toString());
            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}