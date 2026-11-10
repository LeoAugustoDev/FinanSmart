package com.finansmart_server.config;

import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/** Documentação transversal, mantendo os schemas inferidos dos DTOs e suas validações. */
@Configuration(proxyBeanMethods = false)
class ApiDocumentation {

	@Bean
	OpenApiCustomizer documentApi() {
		Map<String, String> resources = Map.of("accounts", "Contas", "categories", "Categorias",
				"transactions", "Lançamentos", "budgets", "Orçamentos", "dashboard", "Painel",
				"reports", "Relatórios", "leaks", "Vazamentos", "me", "Usuário autenticado");
		return api -> api.getPaths().forEach((path, item) -> {
			if (!path.startsWith("/api/v1/")) return;
			String resource = path.split("/")[3];
			item.readOperationsMap().forEach((method, operation) -> {
				String tag = resources.getOrDefault(resource, resource);
				operation.setTags(java.util.List.of(tag));
				String action = switch (method) {
					case GET -> "Consultar";
					case POST -> "Criar";
					case PUT -> "Atualizar";
					case DELETE -> "Excluir";
					default -> path.endsWith("/unarchive") ? "Desarquivar" : "Arquivar";
				};
				operation.setSummary(action + " — " + tag);
				operation.setDescription("Operação restrita ao usuário identificado pelo sub do token Keycloak. "
						+ "Valores monetários em BRL, com até duas casas decimais. "
						+ (path.contains("{id}") ? "Recurso inexistente ou pertencente a outro usuário retorna 404." : ""));
				operation.getResponses().addApiResponse("401", new ApiResponse().description("Token ausente, inválido ou expirado; resposta da camada de segurança, sem corpo obrigatório."));
				if (operation.getParameters() != null || operation.getRequestBody() != null)
					operation.getResponses().addApiResponse("400", error(400, "Requisição inválida", "Verifique os parâmetros e os campos enviados."));
				if (path.contains("{id}"))
					operation.getResponses().addApiResponse("404", error(404, "Não encontrado", "Recurso não encontrado."));
				if (method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET) {
					operation.getResponses().addApiResponse("409", error(409, "Conflito", "Conflito de unicidade, vínculo ou edição concorrente."));
					if (resource.equals("categories"))
						operation.getResponses().addApiResponse("403", error(403, "Operação proibida", "Categorias do sistema não podem ser alteradas."));
				}
				if (operation.getParameters() != null) operation.getParameters().forEach(parameter -> {
					if ("month".equals(parameter.getName())) {
						parameter.setDescription("Mês no formato YYYY-MM; quando omitido, usa o mês atual do servidor.");
						parameter.setExample("2026-11");
					}
					if ("includeArchived".equals(parameter.getName()))
						parameter.setDescription("Inclui contas arquivadas; padrão false.");
				});
			});
		});
	}

	private static ApiResponse error(int status, String title, String detail) {
		Schema<?> schema = new Schema<>().type("object")
				.addProperty("type", new Schema<>().type("string"))
				.addProperty("title", new Schema<>().type("string"))
				.addProperty("status", new Schema<>().type("integer"))
				.addProperty("detail", new Schema<>().type("string"))
				.addProperty("instance", new Schema<>().type("string"))
				.addProperty("errors", new Schema<>().type("object").description("Erros por campo, quando aplicável."));
		return new ApiResponse().description(detail).content(new Content().addMediaType("application/problem+json",
				new MediaType().schema(schema).example(Map.of("type", "about:blank", "title", title,
						"status", status, "detail", detail))));
	}
}
