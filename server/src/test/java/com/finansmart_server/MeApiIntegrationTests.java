package com.finansmart_server;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeApiIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void returnsTheUserFromTheToken() throws Exception {
		this.mvc.perform(get("/api/v1/me").with(jwt().jwt((token) -> token.subject("user-123")
				.claim("preferred_username", "maria")
				.claim("name", "Maria Silva")
				.claim("email", "maria@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value("user-123"))
			.andExpect(jsonPath("$.username").value("maria"))
			.andExpect(jsonPath("$.name").value("Maria Silva"))
			.andExpect(jsonPath("$.email").value("maria@example.com"));
	}

	@Test
	void rejectsRequestsWithoutToken() throws Exception {
		this.mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
	}

}
