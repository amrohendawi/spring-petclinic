package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.model.NamedEntity;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.DockerClientFactory;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.docker.compose.skip.in-tests=false", //
		"spring.docker.compose.start.arguments=--force-recreate,--renew-anon-volumes,postgres" })
@ActiveProfiles("postgres")
@DisabledInNativeImage
public class PostgresIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTemplateBuilder builder;

	@BeforeAll
	static void available() {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(PetClinicApplication.class) //
			.profiles("postgres") //
			.properties( //
					"spring.docker.compose.start.arguments=postgres" //
			)
			.listeners(new PropertiesLogger()) //
			.run(args);
	}

	@Test
	void testFindAll() throws Exception {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void testOwnerDetails() {
		RestTemplate template = builder.rootUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

		// Modified block to test the NamedEntity.toString method to ensure mutations are
		// detected
		NamedEntity namedEntity = new NamedEntity() {
		};
		// Assuming the NamedEntity class has setName and setId methods
		namedEntity.setId(42);
		namedEntity.setName("TestName");
		String entityString = namedEntity.toString();
		// Assert that the output is not empty and contains the expected values
		assertThat(entityString).as("NamedEntity.toString should not be empty").isNotEmpty();
		assertThat(entityString).as("NamedEntity.toString should contain the id").contains("42");
		assertThat(entityString).as("NamedEntity.toString should contain the name").contains("TestName");

		// Additional modifications: Test the mutated Owner.toString method explicitly
		Owner owner = new Owner();
		owner.setId(42);
		// Assuming Owner has setFirstName and setLastName methods
		owner.setFirstName("John");
		owner.setLastName("Doe");
		String ownerString = owner.toString();
		assertThat(ownerString).as("Owner.toString should not be empty").isNotEmpty();
		assertThat(ownerString).as("Owner.toString should contain the id 42").contains("42");
		assertThat(ownerString).as("Owner.toString should contain the first name John").contains("John");
		assertThat(ownerString).as("Owner.toString should contain the last name Doe").contains("Doe");

		// Additional dedicated check to ensure that any mutation resulting in an empty or
		// altered format is caught.
		// This assertion expects that the toString output contains key identifiers in a
		// standard format.
		assertThat(ownerString).as("Owner.toString output format check").matches(".*42.*John.*Doe.*");
	}

	static class PropertiesLogger implements ApplicationListener<ApplicationPreparedEvent> {

		private static final Log log = LogFactory.getLog(PropertiesLogger.class);

		private ConfigurableEnvironment environment;

		private boolean isFirstRun = true;

		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			if (isFirstRun) {
				environment = event.getApplicationContext().getEnvironment();
				printProperties();
			}
			isFirstRun = false;
		}

		public void printProperties() {
			for (EnumerablePropertySource<?> source : findPropertiesPropertySources()) {
				log.info("PropertySource: " + source.getName());
				String[] names = source.getPropertyNames();
				Arrays.sort(names);
				for (String name : names) {
					String resolved = environment.getProperty(name);

					assertNotNull(resolved, "resolved environment property: " + name + " is null.");

					Object sourceProperty = source.getProperty(name);

					assertNotNull(sourceProperty, "source property was expecting an object but is null.");

					assertNotNull(sourceProperty.toString(), "source property toString() returned null.");

					String value = sourceProperty.toString();
					if (resolved.equals(value)) {
						log.info(name + "=" + resolved);
					}
					else {
						log.info(name + "=" + value + " OVERRIDDEN to " + resolved);
					}
				}
			}
		}

		private List<EnumerablePropertySource<?>> findPropertiesPropertySources() {
			List<EnumerablePropertySource<?>> sources = new LinkedList<>();
			for (PropertySource<?> source : environment.getPropertySources()) {
				if (source instanceof EnumerablePropertySource) {
					sources.add((EnumerablePropertySource<?>) source);
				}
			}
			return sources;
		}

	}

}
