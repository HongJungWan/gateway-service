package com.example.gateway_service.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 모든 요청/응답 흐름에 대해 로깅 등의 공통 기능을 처리하는 Global 필터.
 * Spring Cloud Gateway는 요청이 들어올 때 지정한 여러 필터들을 체이닝하여 처리하는데,
 * GlobalTrackerFilter는 서비스 호출 전(Pre) 및 호출 후(Post)에 특정 로직(여기서는 로그 출력)을 수행한다.
 * 
 * 주요 포인트:
 * - AbstractGatewayFilterFactory를 상속받아 필터를 정의
 * - Config 클래스를 통해 필터 설정값(로그 출력 여부, 메시지)을 주입
 * - 필터 체인(chain)을 통해 다음 필터나 대상 서비스로 요청을 전달
 * - Mono.fromRunnable()를 이용해 비동기/논블로킹 방식으로 후처리(Post) 로직 수행
 */
@Component
@Slf4j
public class GlobalTrackerFilter extends AbstractGatewayFilterFactory<GlobalTrackerFilter.Config> {

	public GlobalTrackerFilter() {
		// 부모 클래스의 생성자를 호출
		super(Config.class);
	}

	@Data
	public static class Config {
		private String baseMessage;
		private boolean preLogger;
		private boolean postLogger;
	}

    /**
     * apply 메서드에서는 실제 필터 로직을 정의한다.
     * 요청 전(pre) 로깅: 필터 시작 시점에 로그 출력
     * 요청 후(post) 로깅: 필터 체인의 응답 반환 시점에 로그 출력
     * 이를 통해 모든 요청에 대해 공통적으로 수행하는 크로스컷팅(cross-cutting) 관심사를 처리할 수 있다.
     */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpResponse response = exchange.getResponse();

			// 설정된 기본 메시지 출력
			log.info("Global filter base message: {}", config.getBaseMessage());

			// Pre 로깅
			if (config.isPreLogger()) {
				log.info("Global filter start: request id -> {}", request.getId());
			}

			// 체인 필터 호출 후 Mono를 이용해 Post 로깅 작업 비동기 수행
			return chain.filter(exchange).then(Mono.fromRunnable(() -> {
				// Post 로깅
				if (config.isPostLogger()) {
					log.info("Global filter end: response code -> {}", response.getStatusCode());
				}
			}));
		};
	}
}