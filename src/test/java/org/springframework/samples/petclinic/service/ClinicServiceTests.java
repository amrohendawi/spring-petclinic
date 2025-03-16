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

		// Additional negative assertions to cover cases in getPet for non-existing pet
		// ids
		assertThat(owner6.getPet(-1)).isNull();
		assertThat(owner6.getPet(0)).isNull();

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

		// Additional assertions to trigger the specific branch in getPet method and kill
		// the mutation:
		// 1. Ensure getPet returns null when a pet with the specified name does not exist
		assertThat(owner6.getPet("nonexistent")).isNull();
		assertThat(owner6.getPet(9999)).isNull();
		assertThat(owner6.getPet("")).isNull();
		assertThat(owner6.getPet((String) null)).isNull();

		// 2. Test with multiple pets where one pet's name closely resembles the search
		// string
		// Add a pet with name "Fluffy"
		Pet petA = new Pet();
		petA.setName("Fluffy");
		petA.setBirthDate(LocalDate.now());
		petA.setType(EntityUtils.getById(types, PetType.class, 2));
		int sizeBefore = owner6.getPets().size();
		owner6.addPet(petA);
		this.owners.save(owner6);
		Owner ownerReloaded = this.owners.findById(6).get();
		Pet retrievedFluffy = ownerReloaded.getPet("Fluffy");
		assertThat(retrievedFluffy).isNotNull();
		assertThat(retrievedFluffy.getName()).isEqualTo("Fluffy");
		assertThat(ownerReloaded.getPets()).hasSize(sizeBefore + 1);

		// Add another pet with a similar but different name "Fluff"
		Pet petB = new Pet();
		petB.setName("Fluff");
		petB.setBirthDate(LocalDate.now());
		petB.setType(EntityUtils.getById(types, PetType.class, 2));
		ownerReloaded.addPet(petB);
		this.owners.save(ownerReloaded);
		ownerReloaded = this.owners.findById(6).get();
		Pet retrievedFluff = ownerReloaded.getPet("Fluff");
		assertThat(retrievedFluff).isNotNull();
		assertThat(retrievedFluff.getName()).isEqualTo("Fluff");

		// 3. Test edge case with a new pet instance (unsaved) to verify conditional
		// branch
		Pet petC = new Pet();
		petC.setName("Buddy");
		petC.setBirthDate(LocalDate.now());
		petC.setType(EntityUtils.getById(types, PetType.class, 2));
		// Add petC to the owner's pet collection without saving it to simulate a 'new'
		// pet
		ownerReloaded.addPet(petC);
		// Since petC is new (id is null), getPet should still be able to retrieve it
		// based on name match
		Pet retrievedBuddy = ownerReloaded.getPet("Buddy");
		assertThat(retrievedBuddy).isNotNull();
		assertThat(retrievedBuddy.getName()).isEqualTo("Buddy");

		// Additional assertions to more thoroughly test the getPet(int) method behavior:
		// Verify that for every pet in the owner's collection, getPet(id) returns the
		// correct pet
		Owner ownerFinal = this.owners.findById(6).get();
		for (Pet existingPet : ownerFinal.getPets()) {
			Pet petById = ownerFinal.getPet(existingPet.getId());
			assertThat(petById).isNotNull();
			assertThat(petById.getId()).isEqualTo(existingPet.getId());
		}
		// Also check that a non-existent pet id returns null
		assertThat(ownerFinal.getPet(9999)).isNull();
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
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.getLastName()).isEqualTo("Douglas");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
		assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");

		// Additional test to verify that getSpecialties sorts unsorted lists.
		// Create a new Vet instance with specialties added in unsorted order.
		Vet unsortedVet = new Vet();
		// Clear any default specialties if present
		unsortedVet.getSpecialties().clear();

		Specialty specialty1 = new Specialty();
		specialty1.setName("surgery");
		Specialty specialty2 = new Specialty();
		specialty2.setName("dentistry");

		unsortedVet.getSpecialties().add(specialty1);
		unsortedVet.getSpecialties().add(specialty2);

		// Calling getSpecialties() should return a sorted list (i.e., dentistry before
		// surgery)
		assertThat(unsortedVet.getSpecialties()).hasSize(2);
		assertThat(unsortedVet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
		assertThat(unsortedVet.getSpecialties().get(1).getName()).isEqualTo("surgery");
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

		// Additional assertions to test null parameter handling in addVisit
		// When a null petId is provided, an IllegalArgumentException is expected
		Visit validVisit = new Visit();
		validVisit.setDescription("null test");
		assertThatThrownBy(() -> owner6.addVisit(null, validVisit)).isInstanceOf(IllegalArgumentException.class);

		// When a null visit is provided, an IllegalArgumentException is expected
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
