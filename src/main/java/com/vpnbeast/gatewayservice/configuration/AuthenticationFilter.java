package com.vpnbeast.gatewayservice.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpnbeast.gatewayservice.model.ExceptionInfo;
import com.vpnbeast.gatewayservice.service.HttpService;
import com.vpnbeast.gatewayservice.service.JwtService;
import com.vpnbeast.gatewayservice.util.DateUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class AuthenticationFilter implements GatewayFilter {

    private final RouterValidator routerValidator;
    private final JwtService jwtService;
    private final HttpService httpService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (routerValidator.isSecured.test(request)) {
            if (httpService.isAuthMissing(request))
                return httpService.onError(exchange, "Authorization header is missing in request",
                        HttpStatus.UNAUTHORIZED);

            final String token = httpService.getAuthHeader(request).substring(7);

            if (!jwtService.isTokenValid(token)) {
                log.info("token is invalid");
                return httpService.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            // TODO: check if token is expired

            httpService.populateRequestWithHeaders(exchange, token, jwtService.getUsernameFromToken(token));
        }

        return chain.filter(exchange);
    }

}