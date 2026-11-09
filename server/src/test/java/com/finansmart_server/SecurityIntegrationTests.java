package com.finansmart_server;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void healthIsPublic() throws Exception {
		this.mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void openApiDocumentIsPublic() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("FinanSmart API"));
	}

	@Test
	void apiRejectsRequestsWithoutToken() throws Exception {
		this.mvc.perform(get("/api/v1/anything")).andExpect(status().isUnauthorized());
	}

	@Test
	void apiAcceptsRequestsWithToken() throws Exception {
		this.mvc.perform(get("/api/v1/anything").with(jwt())).andExpect(status().isNotFound());
	}

	@Test
	void corsAllowsTheConfiguredOrigin() throws Exception {
		this.mvc.perform(options("/api/v1/anything")
				.header(HttpHeaders.ORIGIN, "http://localhost:4200")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
	}

	@Test
	void corsRejectsOtherOrigins() throws Exception {
		this.mvc.perform(options("/api/v1/anything")
				.header(HttpHeaders.ORIGIN, "https://site-qualquer.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden());
	}

}
