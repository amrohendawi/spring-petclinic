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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
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
	void testPrintProperties() {
		// Create a controlled StandardEnvironment
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().clear();

		// Add a high precedence property source so that for key "overrideKey", the
		// environment returns "overridden"
		env.getPropertySources().addFirst(new EnumerablePropertySource<Object>("overridden") {
			@Override
			public Object getProperty(String name) {
				if ("overrideKey".equals(name)) {
					return "overridden";
				}
				return null;
			}

			@Override
			public String[] getPropertyNames() {
				return new String[] { "overrideKey" };
			}
		});

		// Add property source with matching value
		env.getPropertySources().addLast(new EnumerablePropertySource<Object>("matchPS") {
			@Override
			public Object getProperty(String name) {
				if ("matchKey".equals(name)) {
					return "value1";
				}
				return null;
			}

			@Override
			public String[] getPropertyNames() {
				return new String[] { "matchKey" };
			}
		});

		// Add property source that will be overridden by the high precedence source
		env.getPropertySources().addLast(new EnumerablePropertySource<Object>("overridePS") {
			@Override
			public Object getProperty(String name) {
				if ("overrideKey".equals(name)) {
					return "original";
				}
				return null;
			}

			@Override
			public String[] getPropertyNames() {
				return new String[] { "overrideKey" };
			}
		});

		// Create a dummy ApplicationContext to supply our custom environment
		ApplicationContext dummyContext = new ApplicationContext() {
			@Override
			public String getId() {
				return "dummy";
			}

			@Override
			public String getApplicationName() {
				return "dummy";
			}

			@Override
			public String getDisplayName() {
				return "dummy";
			}

			@Override
			public long getStartupDate() {
				return 0;
			}

			@Override
			public ApplicationContext getParent() {
				return null;
			}

			@Override
			public org.springframework.beans.factory.ListableBeanFactory getBeanFactory() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean containsBeanDefinition(String beanName) {
				return false;
			}

			@Override
			public int getBeanDefinitionCount() {
				return 0;
			}

			@Override
			public String[] getBeanDefinitionNames() {
				return new String[0];
			}

			@Override
			public String[] getBeanNamesForType(Class<?> type) {
				return new String[0];
			}

			@Override
			public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
				return new String[0];
			}

			@Override
			public <T> java.util.Map<String, T> getBeansOfType(Class<T> type) {
				return java.util.Collections.emptyMap();
			}

			@Override
			public <T> java.util.Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons,
					boolean allowEagerInit) {
				return java.util.Collections.emptyMap();
			}

			@Override
			public String[] getBeanNamesForAnnotation(Class annotationType) {
				return new String[0];
			}

			@Override
			public java.util.Map<String, Object> getBeansWithAnnotation(Class annotationType) {
				return java.util.Collections.emptyMap();
			}

			@Override
			public <A extends java.lang.annotation.Annotation> A findAnnotationOnBean(String beanName,
					Class<A> annotationType) {
				return null;
			}

			@Override
			public Object getBean(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T getBean(String name, Class<T> requiredType) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getBean(String name, Object... args) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T getBean(Class<T> requiredType) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T getBean(Class<T> requiredType, Object... args) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean containsBean(String name) {
				return false;
			}

			@Override
			public boolean isSingleton(String name) {
				return false;
			}

			@Override
			public boolean isPrototype(String name) {
				return false;
			}

			@Override
			public boolean isTypeMatch(String name, Class<?> targetType) {
				return false;
			}

			@Override
			public Class<?> getType(String name) {
				return null;
			}

			@Override
			public org.springframework.beans.factory.config.AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T getBeanProvider(Class<T> requiredType) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
				throw new UnsupportedOperationException();
			}
		};

		// Instantiate the PropertiesLogger and inject our custom environment
		PropertiesLogger logger = new PropertiesLogger();
		logger.setEnvironment(env);
		// Simulate the logger being initialized via an ApplicationPreparedEvent
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(dummyContext, new String[0]);
		logger.onApplicationEvent(event);

		// Now call printProperties() to execute both branches:
		// One property with matching values (matchKey) and one with overridden values
		// (overrideKey)
		logger.printProperties();
	}

	static class PropertiesLogger implements ApplicationListener<ApplicationPreparedEvent> {

		private static final Log log = LogFactory.getLog(PropertiesLogger.class);

		private ConfigurableEnvironment environment;

		private boolean isFirstRun = true;

		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			if (isFirstRun) {
				// Set the environment from the event
				this.environment = event.getApplicationContext().getEnvironment();
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

		// Added setter to enable injecting a controlled environment during tests
		public void setEnvironment(ConfigurableEnvironment environment) {
			this.environment = environment;
		}

	}

}
