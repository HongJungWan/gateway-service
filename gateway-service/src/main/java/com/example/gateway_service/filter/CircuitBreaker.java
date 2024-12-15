package com.example.gateway_service.filter;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

/**
 * Resilience4J 기반 서킷 브레이커(Spring Cloud CircuitBreaker) 설정을 제공한다. 서킷 브레이커는 외부
 * 서비스 호출 실패율이 일정 수준 이상일 때 추가적인 실패 요청을 사전에 차단한다.
 */
@Component
public class CircuitBreaker {

	// 기본 서킷 브레이커 설정을 정의한다.
	// CircuitBreakerConfig.ofDefaults(): 기본적으로 설정된 서킷 브레이커 동작 규칙 사용.

	// TimeLimiterConfig: 특정 요청에 대한 타임아웃 시간 설정 (여기서는 4초) 이 Bean은 ReactiveResilience4JCircuitBreakerFactory에 대한 기본 커스터마이저로 등록되어, 
	// 추후 @CircuitBreaker 어노테이션 사용시 기본 세팅이 적용된다.
	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
		// 요청 처리 최대 시간을 4초로 제한하는 TimeLimiter 설정
		TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
				.timeoutDuration(Duration.ofSeconds(4))
				.build();

		return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults()) // 기본 CircuitBreaker 설정(예: 실패율 임계값)
				.timeLimiterConfig(timeLimiterConfig) // 타임아웃 설정 적용
				.build());
	}
}