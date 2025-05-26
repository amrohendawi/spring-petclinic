/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetRepository;
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
	}

	@Test
	void testPropertiesLoggerWithValidProperties() {
		// Create a StandardEnvironment and clear the default property sources
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

		env.getPropertySources().clear();

		// Add a dummy valid EnumerablePropertySource
		EnumerablePropertySource<Object> dummySource = new EnumerablePropertySource<Object>("dummyValid") {
			@Override
			public String[] getPropertyNames() {
				return new String[] { "key1", "key2" };
			}

			@Override
			public Object getProperty(String name) {
				if ("key1".equals(name)) {
					return "value1";
				}
				else if ("key2".equals(name)) {
					return "value2";
				}
				return null;
			}
		};
		env.getPropertySources().addFirst(dummySource);

		// Create a dummy ApplicationContext with the custom environment
		GenericApplicationContext context = new GenericApplicationContext();
		context.setEnvironment(env);
		SpringApplication dummyApp = new SpringApplication();
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(dummyApp, new String[] {}, context);

		// Instantiate PropertiesLogger and trigger onApplicationEvent
		PropertiesLogger logger = new PropertiesLogger();
		logger.onApplicationEvent(event);

		// Call printProperties explicitly to ensure validations pass
		logger.printProperties();
	}

	@Test
	void testPropertiesLoggerWithInvalidProperties() {
		// Create a StandardEnvironment and clear the default property sources
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		env.getPropertySources().clear();

		// Add a dummy invalid EnumerablePropertySource that returns null for its property
		// value
		EnumerablePropertySource<Object> invalidSource = new EnumerablePropertySource<Object>("dummyInvalid") {
			@Override
			public String[] getPropertyNames() {
				return new String[] { "invalidKey" };
			}

			@Override
			public Object getProperty(String name) {
				return null;
			}
		};
		env.getPropertySources().addFirst(invalidSource);

		// Create a dummy ApplicationContext with the custom invalid environment
		GenericApplicationContext context = new GenericApplicationContext();
		context.setEnvironment(env);
		SpringApplication dummyApp = new SpringApplication();
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(dummyApp, new String[] {}, context);

		// Instantiate PropertiesLogger and trigger onApplicationEvent
		PropertiesLogger logger = new PropertiesLogger();
		try {
			logger.onApplicationEvent(event);
			fail("Expected AssertionError for null property value");
		}
		catch (AssertionError e) {
			assertThat(e.getMessage()).contains("resolved environment property: invalidKey is null.");
		}
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
				if (source instanceof EnumerablePropertySource enumerable) {
					sources.add(enumerable);
				}
			}
			return sources;
		}

	}

}
