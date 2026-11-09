package com.finansmart_server.shared;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
		.setControllerAdvice(new GlobalExceptionHandler())
		.build();

	@Test
	void validationErrorsListTheInvalidFields() throws Exception {
		this.mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void dataIntegrityViolationIsConflict() throws Exception {
		this.mvc.perform(get("/integrity")).andExpect(status().isConflict());
	}

	@Test
	void optimisticLockFailureIsConflict() throws Exception {
		this.mvc.perform(get("/lock")).andExpect(status().isConflict());
	}

	@Test
	void unexpectedErrorDoesNotLeakDetails() throws Exception {
		this.mvc.perform(get("/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().string(not(containsString("segredo interno"))));
	}

	record Payload(@NotBlank String name) {
	}

	@RestController
	static class FailingController {

		@PostMapping("/validate")
		void validate(@Valid @RequestBody Payload payload) {
		}

		@GetMapping("/integrity")
		void integrity() {
			throw new DataIntegrityViolationException("duplicado");
		}

		@GetMapping("/lock")
		void lock() {
			throw new OptimisticLockingFailureException("versão antiga");
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("segredo interno");
		}

	}

}
