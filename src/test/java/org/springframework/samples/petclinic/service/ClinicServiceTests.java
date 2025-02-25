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

package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the Service and the Repository layer.
 * <p>
 * ClinicServiceSpringDataJpaTests subclasses benefit from the following services provided
 * by the Spring TestContext Framework:
 * </p>
 * <ul>
 * <li><strong>Spring IoC container caching</strong> which spares us unnecessary set up
 * time between test execution.</li>
 * <li><strong>Dependency Injection</strong> of test fixture instances, meaning that we
 * don't need to perform application context lookups. See the use of
 * {@link Autowired @Autowired} on the <code> </code> instance variable, which uses
 * autowiring <em>by type</em>.</li>
 * <li><strong>Transaction management</strong>, meaning each test method is executed in
 * its own transaction, which is automatically rolled back by default. Thus, even if tests
 * insert or otherwise change database state, there is no need for a teardown or cleanup
 * script.</li>
 * <li>An {@link org.springframework.context.ApplicationContext ApplicationContext} is
 * also inherited and can be used for explicit bean lookup if necessary.</li>
 * </ul>
 *
 * author Ken Krebs author Rod Johnson author Juergen Hoeller author Sam Brannen author
 * Michael Isvy author Dave Syer
 */
@DataJpaTest
// Ensure that if the mysql profile is active we connect to the real database:
@AutoConfigureTestDatabase(replace = Replace.NONE)
// @TestPropertySource("/application-postgres.properties")
class ClinicServiceTests {

	@Autowired
	protected OwnerRepository owners;

	@Autowired
	protected VetRepository vets;

	Pageable pageable;

	@Test
	void shouldFindOwnersByLastName() {
		Page<Owner> owners = this.owners.findByLastNameStartingWith("Davis", pageable);
		assertThat(owners).hasSize(2);

		owners = this.owners.findByLastNameStartingWith("Daviss", pageable);
		assertThat(owners).isEmpty();
	}

	@Test
	void shouldFindSingleOwnerWithPet() {
		Optional<Owner> optionalOwner = this.owners.findById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();
		assertThat(owner.getLastName()).startsWith("Franklin");
		assertThat(owner.getPets()).hasSize(1);
		assertThat(owner.getPets().get(0).getType()).isNotNull();
		assertThat(owner.getPets().get(0).getType().getName()).isEqualTo("cat");
	}

	@Test
	@Transactional
	void shouldInsertOwner() {
		Page<Owner> owners = this.owners.findByLastNameStartingWith("Schultz", pageable);
		int found = (int) owners.getTotalElements();

		Owner owner = new Owner();
		owner.setFirstName("Sam");
		owner.setLastName("Schultz");
		owner.setAddress("4, Evans Street");
		owner.setCity("Wollongong");
		owner.setTelephone("4444444444");
		this.owners.save(owner);
		assertThat(owner.getId()).isNotZero();

		owners = this.owners.findByLastNameStartingWith("Schultz", pageable);
		assertThat(owners.getTotalElements()).isEqualTo(found + 1);
	}

	@Test
	@Transactional
	void shouldUpdateOwner() {
		Optional<Owner> optionalOwner = this.owners.findById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();
		String oldLastName = owner.getLastName();
		String newLastName = oldLastName + "X";

		owner.setLastName(newLastName);
		this.owners.save(owner);

		// retrieving new name from database
		optionalOwner = this.owners.findById(1);
		assertThat(optionalOwner).isPresent();
		owner = optionalOwner.get();
		assertThat(owner.getLastName()).isEqualTo(newLastName);
	}

	@Test
	void shouldFindAllPetTypes() {
		Collection<PetType> petTypes = this.owners.findPetTypes();

		PetType petType1 = EntityUtils.getById(petTypes, PetType.class, 1);
		assertThat(petType1.getName()).isEqualTo("cat");
		PetType petType4 = EntityUtils.getById(petTypes, PetType.class, 4);
		assertThat(petType4.getName()).isEqualTo("snake");
	}

	@Test
	@Transactional
	void shouldInsertPetIntoDatabaseAndGenerateId() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		int found = owner6.getPets().size();

		Pet pet = new Pet();
		pet.setName("bowser");
		Collection<PetType> types = this.owners.findPetTypes();
		pet.setType(EntityUtils.getById(types, PetType.class, 2));
		pet.setBirthDate(LocalDate.now());
		owner6.addPet(pet);
		assertThat(owner6.getPets()).hasSize(found + 1);

		this.owners.save(owner6);

		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		assertThat(owner6.getPets()).hasSize(found + 1);
		// checks that id has been generated
		pet = owner6.getPet("bowser");
		assertThat(pet.getId()).isNotNull();

		// Additional assertion to ensure that getPet returns null for non-existent pet
		// names
		assertThat(owner6.getPet("nonexistent")).isNull();

		// Additional assertion to ensure that getPet returns null for non-existent pet
		// ids
		assertThat(owner6.getPet(9999)).isNull();

		// New assertion to verify that getPet(int) returns the exact instance when
		// searched by id
		assertThat(owner6.getPet(pet.getId())).isSameAs(pet);
	}

	@Test
	@Transactional
	void shouldUpdatePetName() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		// Added assertion to verify that the correct pet is retrieved by id
		assertThat(pet7.getId()).isEqualTo(7);
		String oldName = pet7.getName();

		String newName = oldName + "X";
		pet7.setName(newName);
		this.owners.save(owner6);

		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		pet7 = owner6.getPet(7);
		// Re-check the pet id to ensure getPet returns the correct pet
		assertThat(pet7.getId()).isEqualTo(7);
		assertThat(pet7.getName()).isEqualTo(newName);

		// Additional assertion to ensure that getPet returns null for non-existent pet
		// ids
		assertThat(owner6.getPet(9999)).isNull();
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.getLastName()).isEqualTo("Douglas");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		// Modified assertions to verify specialties without enforcing order
		assertThat(vet.getSpecialties()).extracting("name").containsExactlyInAnyOrder("dentistry", "surgery");
	}

	@Test
	@Transactional
	void shouldAddNewVisitForPet() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		// Added assertion to verify correct pet retrieval by id
		assertThat(pet7.getId()).isEqualTo(7);
		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.setDescription("test");

		// Normal addition of a visit
		owner6.addVisit(pet7.getId(), visit);
		this.owners.save(owner6);

		assertThat(pet7.getVisits()).hasSize(found + 1).allMatch(value -> value.getId() != null);

		// Additional assertions to ensure that null checks in addVisit are enforced
		// Test 1: Passing a null pet id should throw an IllegalArgumentException
		Visit visitForNullTest = new Visit();
		visitForNullTest.setDescription("null pet id test");
		assertThatIllegalArgumentException().isThrownBy(() -> owner6.addVisit(null, visitForNullTest));

		// Test 2: Passing a null visit should throw an IllegalArgumentException
		assertThatIllegalArgumentException().isThrownBy(() -> owner6.addVisit(pet7.getId(), null));

		// Test 3: Passing a pet id that does not exist should throw an
		// IllegalArgumentException
		assertThatIllegalArgumentException().isThrownBy(() -> owner6.addVisit(9999, visitForNullTest));
	}

	@Test
	void shouldFindVisitsByPetId() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		Collection<Visit> visits = pet7.getVisits();

		assertThat(visits).hasSize(2).element(0).extracting(Visit::getDate).isNotNull();

		// Added assertion to verify that getPet returns null for a non-existent pet id
		assertThat(owner6.getPet(9999)).isNull();
	}

}
