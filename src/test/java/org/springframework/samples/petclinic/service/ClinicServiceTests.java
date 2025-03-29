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

		// Additional assertion to verify that getPet returns the pet with the exact id
		// passed
		Pet existingPet = owner.getPets().get(0);
		int petId = existingPet.getId();
		assertThat(owner.getPet(petId)).isEqualTo(existingPet);

		// Newly added assertion to verify that getPet returns the pet when queried by its
		// exact name
		assertThat(owner.getPet(existingPet.getName())).isEqualTo(existingPet);
		// New assertion to verify case-sensitive pet name lookup
		assertThat(owner.getPet(existingPet.getName().toUpperCase())).isNull();

		// New assertion to test negative branch using pet id:
		assertThat(owner.getPet(999)).isNull();

		// New assertion to test negative branch using pet name:
		assertThat(owner.getPet("NonExistentPet")).isNull();

		// Additional modifications to cover survived mutation in getPet(id) method:
		// Add a second pet to create a multi-pet scenario and ensure that getPet returns
		// the correct pet
		Pet pet2 = new Pet();
		pet2.setName("doggo");
		pet2.setBirthDate(LocalDate.now());
		// Reuse the same pet type as the existing pet
		pet2.setType(existingPet.getType());
		owner.addPet(pet2);
		this.owners.save(owner);

		Optional<Owner> updatedOwnerOptional = this.owners.findById(owner.getId());
		assertThat(updatedOwnerOptional).isPresent();
		Owner updatedOwner = updatedOwnerOptional.get();
		assertThat(updatedOwner.getPets()).hasSize(2);

		// Retrieve the newly added pet by name
		Pet persistedPet2 = updatedOwner.getPets()
			.stream()
			.filter(p -> "doggo".equals(p.getName()))
			.findFirst()
			.orElse(null);
		assertThat(persistedPet2).isNotNull();

		// Verify that getPet(id) returns the correct pet in a multi-pet scenario
		assertThat(updatedOwner.getPet(persistedPet2.getId())).isEqualTo(persistedPet2);

		// Additional negative test: Create an owner with no pets to ensure getPet handles
		// empty pet list
		Owner emptyOwner = new Owner();
		emptyOwner.setFirstName("Test");
		emptyOwner.setLastName("Empty");
		emptyOwner.setAddress("Address");
		emptyOwner.setCity("City");
		emptyOwner.setTelephone("123456");
		this.owners.save(emptyOwner);
		Optional<Owner> retrievedEmptyOwnerOptional = this.owners.findById(emptyOwner.getId());
		assertThat(retrievedEmptyOwnerOptional).isPresent();
		Owner retrievedEmptyOwner = retrievedEmptyOwnerOptional.get();
		assertThat(retrievedEmptyOwner.getPets()).isEmpty();
		assertThat(retrievedEmptyOwner.getPet(1)).isNull();
		assertThat(retrievedEmptyOwner.getPet("TestPet")).isNull();

		// Additional negative assertions to cover boundary conditions of getPet
		assertThat(owner.getPet(0)).isNull();
		assertThat(owner.getPet("")).isNull();
		// New negative test for a negative pet id
		assertThat(owner.getPet(-1)).isNull();
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

		// checks that id has been generated using name lookup
		Pet persistedPet = owner6.getPet("bowser");
		Integer persistedPetId = persistedPet.getId();
		assertThat(persistedPetId).isNotNull();

		// Add an unsaved (new) pet with the same name to verify that getPet returns the
		// persisted one
		Pet unsavedPet = new Pet();
		unsavedPet.setName("bowser");
		unsavedPet.setBirthDate(LocalDate.now());
		unsavedPet.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(unsavedPet);

		// When retrieving by name, the persisted pet should be returned rather than the
		// new unsaved pet
		Pet retrievedPet = owner6.getPet("bowser");
		assertThat(retrievedPet.getId()).isEqualTo(persistedPetId);
		// New assertion to verify case-sensitive pet name lookup for persisted pet
		assertThat(owner6.getPet("BOWSER")).isNull();

		// New assertion to test correct pet retrieval by id in a multi-pet scenario:
		Pet fetchedPet = owner6.getPet(persistedPetId);
		assertThat(fetchedPet).isNotNull();
		assertThat(fetchedPet.getName()).isEqualTo("bowser");

		// Additional modifications to cover survived mutation in getPet(id) method:
		// Add a second persisted pet with a different name to create a multi-pet scenario
		Pet buddy = new Pet();
		buddy.setName("buddy");
		buddy.setBirthDate(LocalDate.now());
		buddy.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(buddy);
		this.owners.save(owner6);

		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();

		// Ensure the new pet "buddy" is persisted and retrievable by name
		Pet persistedBuddy = owner6.getPet("buddy");
		assertThat(persistedBuddy).isNotNull();
		assertThat(persistedBuddy.getId()).isNotNull();

		// Verify that getPet(id) returns the correct pet when multiple pets exist
		Pet fetchedBuddy = owner6.getPet(persistedBuddy.getId());
		assertThat(fetchedBuddy).isNotNull();
		assertThat(fetchedBuddy.getName()).isEqualTo("buddy");

		// Negative test: calling getPet with a non-existent pet id should return null
		assertThat(owner6.getPet(9999)).isNull();

		// Additional negative assertions to cover boundary conditions of getPet
		assertThat(owner6.getPet("")).isNull();
		assertThat(owner6.getPet(0)).isNull();
		// New negative test for a negative pet id
		assertThat(owner6.getPet(-1)).isNull();
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

		// Additional negative branch check: non-existent pet id should return null.
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

		// Additional test to cover the survived mutation in getSpecialties:
		// Create a Vet with specialties added in unsorted order and check if they are
		// returned sorted.
		Vet unsortedVet = new Vet();
		Specialty spec1 = new Specialty();
		spec1.setName("surgery");
		Specialty spec2 = new Specialty();
		spec2.setName("dentistry");
		unsortedVet.getSpecialties().add(spec1);
		unsortedVet.getSpecialties().add(spec2);

		// Expect the specialties to be sorted by name: "dentistry" should come before
		// "surgery"
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

		// Additional tests to enforce null defensive checks for addVisit method
		// Expect an exception when petId is null
		assertThatIllegalArgumentException().isThrownBy(() -> owner6.addVisit(null, new Visit()));
		// Expect an exception when visit is null
		assertThatIllegalArgumentException().isThrownBy(() -> owner6.addVisit(pet7.getId(), null));
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
