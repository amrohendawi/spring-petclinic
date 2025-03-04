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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.samples.petclinic.vet.Specialty;
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
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
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

		// Additional test to check negative scenario for getPet method by name
		// Verifies that requesting a pet by a name that does not exist returns null
		assertThat(owner.getPet("nonExistentPet")).isNull();

		// Additional test to check negative scenario for getPet method by id
		// Verifies that requesting a pet by an id that does not exist returns null
		assertThat(owner.getPet(999)).isNull();

		// New negative tests to cover edge cases, ensuring that mutations in conditional
		// logic are caught.
		// For example, passing an empty string should return null because no pet has an
		// empty name.
		assertThat(owner.getPet("")).isNull();

		// Similarly, passing an id of 0 (assuming no pet has id 0) should return null
		assertThat(owner.getPet(0)).isNull();

		// ------------------------------------------------------------------------------
		// Additional modifications to ensure getPet correctly selects the pet by its id
		// when an owner has multiple pets.
		// Create a new owner with multiple pets having distinct IDs
		Owner newOwner = new Owner();
		newOwner.setFirstName("Test");
		newOwner.setLastName("Owner");
		newOwner.setAddress("123 Test St");
		newOwner.setCity("Testville");
		newOwner.setTelephone("1234567890");

		// Create first pet
		Pet pet1 = new Pet();
		pet1.setName("Buddy");
		pet1.setBirthDate(LocalDate.now());
		Collection<PetType> types = this.owners.findPetTypes();
		pet1.setType(EntityUtils.getById(types, PetType.class, 1));
		newOwner.addPet(pet1);

		// Create second pet
		Pet pet2 = new Pet();
		pet2.setName("Charlie");
		pet2.setBirthDate(LocalDate.now());
		pet2.setType(EntityUtils.getById(types, PetType.class, 2));
		newOwner.addPet(pet2);

		// Save the new owner with pets
		newOwner = this.owners.save(newOwner);

		// Retrieve the saved owner
		Optional<Owner> newOwnerOptional = this.owners.findById(newOwner.getId());
		assertThat(newOwnerOptional).isPresent();
		newOwner = newOwnerOptional.get();
		assertThat(newOwner.getPets()).hasSize(2);

		// Assert that getPet returns the correct pet for each valid ID
		Pet foundPet1 = newOwner.getPet(pet1.getId());
		Pet foundPet2 = newOwner.getPet(pet2.getId());
		assertThat(foundPet1).isNotNull();
		assertThat(foundPet2).isNotNull();
		assertThat(foundPet1.getName()).isEqualTo("Buddy");
		assertThat(foundPet2.getName()).isEqualTo("Charlie");

		// Check behavior when a non-existent pet ID is queried
		assertThat(newOwner.getPet(9999)).isNull();
		// ------------------------------------------------------------------------------
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

		// Additional test: verify that getPet returns null for a name that doesn't exist
		assertThat(owner6.getPet("nonexistent")).isNull();

		// Additional test: unsaved/new pet should be ignored by getPet
		Pet unsavedPet = new Pet();
		unsavedPet.setName("unsaved");
		// Directly adding a new pet without saving (id remains null, considered as new)
		owner6.getPets().add(unsavedPet);
		assertThat(owner6.getPet("unsaved")).isNull();
	}

	@Test
	@Transactional
	void shouldUpdatePetName() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		String oldName = pet7.getName();

		String newName = oldName + "X";
		pet7.setName(newName);
		this.owners.save(owner6);

		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		pet7 = owner6.getPet(7);
		assertThat(pet7.getName()).isEqualTo(newName);

		// Additional test: verify that getPet returns null when an invalid pet id is
		// passed
		assertThat(owner6.getPet(9999)).isNull();
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.getLastName()).isEqualTo("Douglas");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
		assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");

		// ------------------------------------------------------------------------------
		// Additional test to verify that getSpecialties() returns a sorted list even if
		// the specialties
		// are added in an unsorted order. This test will fail if the sort operation in
		// getSpecialties()
		// is removed, thus killing the mutation.
		Vet unsortedVet = new Vet();
		Specialty specialty1 = new Specialty();
		specialty1.setName("surgery");
		Specialty specialty2 = new Specialty();
		specialty2.setName("dentistry");
		// Add in unsorted order
		unsortedVet.addSpecialty(specialty1);
		unsortedVet.addSpecialty(specialty2);

		// When getSpecialties is called, it should return the specialties sorted by name
		// (alphabetically)
		// i.e., "dentistry" should come before "surgery"
		assertThat(unsortedVet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
		assertThat(unsortedVet.getSpecialties().get(1).getName()).isEqualTo("surgery");
		// ------------------------------------------------------------------------------
	}

	@Test
	@Transactional
	void shouldAddNewVisitForPet() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.setDescription("test");

		owner6.addVisit(pet7.getId(), visit);
		this.owners.save(owner6);

		assertThat(pet7.getVisits()).hasSize(found + 1).allMatch(value -> value.getId() != null);

		// Additional tests to ensure that null parameters are not accepted in addVisit
		// This will fail if addVisit does not throw an exception for null parameters
		Visit validVisit = new Visit();
		validVisit.setDescription("valid visit");

		// Test passing null as petId
		assertThatThrownBy(() -> owner6.addVisit(null, validVisit)).isInstanceOf(IllegalArgumentException.class);

		// Test passing null as visit
		assertThatThrownBy(() -> owner6.addVisit(pet7.getId(), null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldFindVisitsByPetId() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		Collection<Visit> visits = pet7.getVisits();

		assertThat(visits).hasSize(2).element(0).extracting(Visit::getDate).isNotNull();
	}

}
