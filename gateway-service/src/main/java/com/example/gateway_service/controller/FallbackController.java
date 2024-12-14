package com.example.gateway_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

	@GetMapping("/user")
	public Mono<String> fallbackUser() {
		String errorMessage = "유저 서비스의 장애로 접근이 불가능합니다." + "\n이용에 불편을 드려 죄송합니다.";
		return Mono.just(errorMessage);
	}

	// Mono 타입은 "한 번에 하나의 값"만을 비동기적으로 전달하는 개념
	// Mono 타입은 "값을 바로 줄 수는 없지만, 준비되면 알려줄게"라고 말하는 형태의 데이터 제공 방식
	@GetMapping("/catalog")
	public Mono<String> fallbackCatalog() {
		String errorMessage = "상품 서비스의 장애로 접근이 불가능합니다." + "\n이용에 불편을 드려 죄송합니다.";

		// Mono.just() 함수는 하나의 값(또는 객체)을 즉시 Mono로 감싸서 반환함으로써,
		// 해당 값을 비동기 파이프라인에서 처리할 수 있도록 한다.
		return Mono.just(errorMessage);
	}

	@GetMapping("/order")
	public Mono<String> fallbackOrder() {
		String errorMessage = "주문 서비스의 장애로 접근이 불가능합니다." + "\n이용에 불편을 드려 죄송합니다.";
		return Mono.just(errorMessage);
	}
}
