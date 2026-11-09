package com.finansmart_server.me;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

	@GetMapping
	MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("name"), jwt.getClaimAsString("email"));
	}

	record MeResponse(String id, String username, String name, String email) {
	}

}
