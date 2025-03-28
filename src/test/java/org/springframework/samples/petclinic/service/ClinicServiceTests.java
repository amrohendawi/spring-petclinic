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
 * script.
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
		// Additional assertions to cover negative branch in getPet(String)
		assertThat(owner.getPet("nonExistentPet")).isNull();
		// Additional assertions to cover negative branch in getPet(int)
		assertThat(owner.getPet(9999)).isNull();

		// New assertions to verify correct branch of getPet(int) when pet exists
		Pet existingPet = owner.getPets().get(0);
		assertThat(owner.getPet(existingPet.getId())).isEqualTo(existingPet);

		// Additional test for multiple pets scenario to cover edge conditions in getPet
		Pet newPet = new Pet();
		newPet.setName("doggy");
		newPet.setType(existingPet.getType());
		newPet.setBirthDate(LocalDate.now());
		owner.addPet(newPet);
		this.owners.save(owner);

		Optional<Owner> optionalOwnerUpdated = this.owners.findById(1);
		assertThat(optionalOwnerUpdated).isPresent();
		Owner updatedOwner = optionalOwnerUpdated.get();
		assertThat(updatedOwner.getPets()).hasSize(2);

		Pet doggy = updatedOwner.getPet("doggy");
		assertThat(doggy).isNotNull();

		// Verify that getPet(int) returns the correct pet for each pet in a multiple pet
		// scenario
		Pet petByIdExisting = updatedOwner.getPet(existingPet.getId());
		assertThat(petByIdExisting).isEqualTo(existingPet);

		Pet petByIdDoggy = updatedOwner.getPet(doggy.getId());
		assertThat(petByIdDoggy).isEqualTo(doggy);

		// Negative test: non-existent pet id returns null
		assertThat(updatedOwner.getPet(99999)).isNull();
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

		// Insert first pet "bowser"
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
		// Verify that the pet "bowser" was inserted and has an id
		pet = owner6.getPet("bowser");
		assertThat(pet).isNotNull();
		assertThat(pet.getId()).isNotNull();

		// Additional check for negative scenario using getPet(String): getPet with a
		// non-existing name returns null
		assertThat(owner6.getPet("nonExisting")).isNull();

		// Additional check for negative scenario using getPet(int): getPet with a
		// non-existing id returns null
		assertThat(owner6.getPet(9999)).isNull();

		// Additional test for multiple pets scenario
		int currentCount = owner6.getPets().size();
		Pet petFluffy = new Pet();
		petFluffy.setName("fluffy");
		petFluffy.setType(EntityUtils.getById(types, PetType.class, 2));
		petFluffy.setBirthDate(LocalDate.now());
		owner6.addPet(petFluffy);
		assertThat(owner6.getPets()).hasSize(currentCount + 1);

		this.owners.save(owner6);
		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		// Verify that both pets can be retrieved correctly
		assertThat(owner6.getPet("bowser")).isNotNull();
		assertThat(owner6.getPet("fluffy")).isNotNull();
		assertThat(owner6.getPet("nonExisting")).isNull();

		// New assertions to verify getPet(int id) returns the correct pet in case of
		// multiple pets
		Pet petBowser = owner6.getPet("bowser");
		Pet petFluffyRetrieved = owner6.getPet("fluffy");
		assertThat(owner6.getPet(petBowser.getId())).isEqualTo(petBowser);
		assertThat(owner6.getPet(petFluffyRetrieved.getId())).isEqualTo(petFluffyRetrieved);
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
		// Additional negative test for getPet(int) with non-existent id
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

		// Negative test: Passing null visit should throw IllegalArgumentException
		assertThatThrownBy(() -> owner6.addVisit(pet7.getId(), null)).isInstanceOf(IllegalArgumentException.class);

		// Negative test: Passing null pet id should throw IllegalArgumentException
		Visit anotherVisit = new Visit();
		anotherVisit.setDescription("test null pet id");
		assertThatThrownBy(() -> owner6.addVisit(null, anotherVisit)).isInstanceOf(IllegalArgumentException.class);
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
