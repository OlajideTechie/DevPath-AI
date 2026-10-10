package ai.devpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class BackendApplicationTests {
	@Autowired
	private WebApplicationContext context;

	@Test
	void actuatorHealthIsPublicAndDoesNotExposeDetails() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String response = mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).isEqualTo("{\"status\":\"UP\"}");
	}

	@Test
	void openApiDocumentsLearnerEndpoints() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String response = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response)
				.contains("\"title\":\"DevPath AI API\"")
				.contains("\"/api/v1/learners\"")
				.contains("\"/api/v1/learners/{learnerId}\"")
				.contains("\"summary\":\"Create a learner profile\"")
				.contains("\"summary\":\"Get a learner profile\"")
				.contains("\"yearsOfExperience\"")
				.contains("\"fieldErrors\"")
				.contains("\"Location\"");
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void actuatorDoesNotExposeEndpointsOtherThanHealth() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isNotFound());
	}

	@Test
	void corsAllowsConfiguredFrontendOriginForLearnerApi() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		for (String origin : new String[] { "http://localhost:5173", "http://127.0.0.1:5173" }) {
			mockMvc.perform(options("/api/v1/learners")
							.header(HttpHeaders.ORIGIN, origin)
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
					.andExpect(status().isOk())
					.andExpect(result -> assertThat(result.getResponse()
							.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
							.isEqualTo(origin));
		}
	}

	@Test
	void corsRejectsUnconfiguredFrontendOriginForLearnerApi() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(options("/api/v1/learners")
						.header(HttpHeaders.ORIGIN, "https://untrusted.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isForbidden());
	}

}
