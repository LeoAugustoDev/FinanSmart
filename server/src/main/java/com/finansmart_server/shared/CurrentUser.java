package com.finansmart_server.shared;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Expõe o identificador do usuário autenticado (claim "sub" do token do Keycloak). */
@Component
public class CurrentUser {

	public String id() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}

}
