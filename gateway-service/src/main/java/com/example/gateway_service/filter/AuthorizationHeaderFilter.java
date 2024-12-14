package com.example.gateway_service.filter;

import java.util.Base64;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * AuthorizationHeaderFilter는 API Gateway 레벨에서 JWT 토큰을 검증하여 권한이 없는 요청을 사전에 차단하는
 * 역할을 한다. 즉, 인증/인가 로직이 서비스 진입 전 단계에서 처리되는 것이 특징이다.
 */
@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

	// Environment 객체를 통해 application.yml 등의 환경 설정 파일에서 jwt.secret 값을 로딩한다.
	Environment environment;

	public AuthorizationHeaderFilter(Environment environment) {
		super(Config.class);
		this.environment = environment;
	}

	public static class Config {
		// Config 클래스는 Spring Cloud Gateway에서 필수적으로 요구하는 형식이며,
		// 필터 설정시 필요한 파라미터를 담을 수 있다.
		// AuthorizationHeaderFilter는 별도의 파라미터가 없어 빈 클래스다.
	}

	/**
	 * apply 메소드는 필터 로직을 정의한다. 요청 헤더에 Authorization이 존재하는지 확인하고, JWT 토큰
	 * 검증(isJwtValid)을 수행한 뒤 유효하지 않다면 요청을 거부한다.
	 */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();

			// Authorization 헤더가 없는 경우 인증 에러 반환
			if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				return onError(exchange, "No Authorization Header!");
			}

			// Bearer 토큰에서 "Bearer " 부분 제거
			String authorizationHeader = request.getHeaders()
					.get(HttpHeaders.AUTHORIZATION)
					.get(0);
			String accessToken = authorizationHeader.replace("Bearer", "");

			// JWT 토큰이 유효하지 않다면 인증 에러 반환
			if (!isJwtValid(accessToken)) {
				return onError(exchange, "Token is not Valid!");
			}

			// 유효하면 체인에 연결하여 다음 필터나 최종 서비스로 요청을 전달
			return chain.filter(exchange);
		};
	}

	/**
	 * isJwtValid 메소드는 JWT 토큰 유효성을 확인한다. Base64로 인코딩된 jwt.secret을 디코딩하여 키를 만들고, 해당
	 * 키로 토큰을 파싱한 뒤 subject(토큰 주체)가 존재하는지 확인한다. 예외 발생 또는 subject가 없으면 유효하지 않은 토큰으로
	 * 간주한다.
	 */
	private boolean isJwtValid(String accessToken) {
		byte[] keyBytes = Base64.getDecoder()
				.decode(environment.getProperty("jwt.secret"));
		boolean returnValue = true;

		String subject = null;

		try {
			subject = Jwts.parserBuilder()
					.setSigningKey(keyBytes) // 시크릿 키 설정
					.build()
					.parseClaimsJws(accessToken) // 토큰 파싱
					.getBody()
					.getSubject(); // 토큰에서 subject(사용자 구분값) 추출
		} catch (Exception e) {
			returnValue = false; // 파싱 에러 → 토큰 무효
		}

		if (subject == null || subject.isEmpty()) {
			returnValue = false; // subject가 없으면 토큰 무효
		}

		return returnValue;
	}

	/**
	 * onError 메소드는 토큰 검증 실패시 즉시 응답을 반환하고 필터 체인을 종료힌다. HTTP 상태코드는 401(UNAUTHORIZED)로
	 * 설정한다. Reactor의 Mono<Void>는 비동기 처리를 위한 리액티브 타입이며 setComplete()를 통해 응답 작성이
	 * 완료되었음을 알린다.
	 */
	private Mono<Void> onError(ServerWebExchange exchange, String error) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);

		log.error(error);
		return response.setComplete();
	}
}