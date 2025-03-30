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

		// Additional negative assertions to cover branch where pet id does not match any
		// pet
		assertThat(owner.getPet(999)).isNull();
		assertThat(owner.getPet("nonexistent")).isNull();
		// Added negative test to cover an invalid pet id scenario
		assertThat(owner.getPet(-1)).isNull();

		// New assertions to cover both the positive branch and the negative branch for
		// getPet(String)
		Pet existingPet = owner.getPets().get(0);
		// Valid name matching (exact and case-insensitive) should return the pet
		assertThat(owner.getPet(existingPet.getName())).isEqualTo(existingPet);
		assertThat(owner.getPet(existingPet.getName().toLowerCase())).isEqualTo(existingPet);
		assertThat(owner.getPet(existingPet.getName().toUpperCase())).isEqualTo(existingPet);
		// An altered name should not match
		assertThat(owner.getPet(existingPet.getName() + "x")).isNull();

		// Additional test: ensure that an unsaved pet with a duplicate name does not
		// override the persisted pet
		Pet duplicateUnsaved = new Pet();
		duplicateUnsaved.setName(existingPet.getName());
		duplicateUnsaved.setBirthDate(existingPet.getBirthDate());
		duplicateUnsaved.setType(existingPet.getType());
		owner.addPet(duplicateUnsaved);
		// getPet should still return the persisted pet, ignoring the new unsaved one
		assertThat(owner.getPet(existingPet.getName())).isEqualTo(existingPet);
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

		// Additional assertions to cover both branches of getPet
		// Positive scenario: retrieving the pet by its id returns the correct pet
		Pet petById = owner6.getPet(pet.getId());
		assertThat(petById).isNotNull();
		assertThat(petById.getName()).isEqualTo("bowser");

		// Negative tests: retrieving a pet with a non-existent id should return null
		assertThat(owner6.getPet(999)).isNull();
		assertThat(owner6.getPet(0)).isNull();

		// New assertions for case-insensitive matching and non-existent pet name
		Pet petByNameUpperCase = owner6.getPet("BOWSER");
		assertThat(petByNameUpperCase).isNotNull();
		assertThat(petByNameUpperCase.getId()).isEqualTo(pet.getId());

		Pet petByNameMixedCase = owner6.getPet("BowSer");
		assertThat(petByNameMixedCase).isNotNull();
		assertThat(petByNameMixedCase.getId()).isEqualTo(pet.getId());

		assertThat(owner6.getPet("unknown")).isNull();

		// Additional test to cover the new pet conditional branch in getPet method.
		// Create a new pet but do not save it. It remains new (id == null) and should not
		// be returned by getPet(String name).
		Pet unsaved = new Pet();
		unsaved.setName("unsaved");
		unsaved.setBirthDate(LocalDate.now());
		unsaved.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(unsaved);
		// The unsaved pet should have id null
		assertThat(unsaved.getId()).isNull();
		// The lookup for the unsaved pet by name should return null because the pet is
		// new and not yet persisted
		assertThat(owner6.getPet("unsaved")).isNull();

		// Additional assertion: When multiple unsaved pets with the same name exist,
		// getPet should return null
		Pet unsaved2 = new Pet();
		unsaved2.setName("unpersisted");
		unsaved2.setBirthDate(LocalDate.now());
		unsaved2.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(unsaved2);
		assertThat(unsaved2.getId()).isNull();
		assertThat(owner6.getPet("unpersisted")).isNull();

		// Additional assertions to cover multiple pets scenario
		Pet pet2 = new Pet();
		pet2.setName("Buddy");
		pet2.setBirthDate(LocalDate.now());
		pet2.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(pet2);
		// Persist the second pet
		this.owners.save(owner6);
		optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();

		Pet pet2ByName = owner6.getPet("buddy");
		assertThat(pet2ByName).isNotNull();
		assertThat(pet2ByName.getId()).isNotNull();

		Pet pet2ById = owner6.getPet(pet2ByName.getId());
		assertThat(pet2ById).isNotNull();
		assertThat(pet2ById.getName()).isEqualTo("Buddy");

		// Negative test: lookup with a similar but incorrect name
		assertThat(owner6.getPet("BuddYx")).isNull();

		// Additional test for boundary scenario: when duplicate pets exist with the same
		// name, only the persisted pet should be returned
		// Create a duplicate pet with the same name as an existing, persisted pet, but do
		// not persist the duplicate
		Pet duplicate = new Pet();
		duplicate.setName("Buddy");
		duplicate.setBirthDate(LocalDate.now());
		duplicate.setType(EntityUtils.getById(types, PetType.class, 2));
		owner6.addPet(duplicate);
		// Retrieving by name should return the persisted pet (pet2ByName) and ignore the
		// unsaved duplicate
		Pet petRetrieved = owner6.getPet("Buddy");
		assertThat(petRetrieved).isNotNull();
		assertThat(petRetrieved.getId()).isEqualTo(pet2ByName.getId());
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
		// Negative test: getPet with an invalid id should return null
		assertThat(owner6.getPet(-1)).isNull();
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

		// Negative test: invoking addVisit with a null pet ID should throw
		// IllegalArgumentException
		Visit validVisit = new Visit();
		validVisit.setDescription("valid test visit");
		assertThatThrownBy(() -> owner6.addVisit(null, validVisit)).isInstanceOf(IllegalArgumentException.class);

		// Negative test: invoking addVisit with a null visit should throw
		// IllegalArgumentException
		assertThatThrownBy(() -> owner6.addVisit(pet7.getId(), null)).isInstanceOf(IllegalArgumentException.class);

		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.setDescription("test");

		owner6.addVisit(pet7.getId(), visit);
		this.owners.save(owner6);

		assertThat(pet7.getVisits()).hasSize(found + 1).allMatch(value -> value.getId() != null);
	}

	@Test
	void shouldFindVisitsByPetId() {
		Optional<Owner> optionalOwner = this.owners.findById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		Collection<Visit> visits = pet7.getVisits();

		assertThat(visits).hasSize(2).element(0).extracting(Visit::getDate).isNotNull();
		// Negative test: getPet with a non-existent pet id should return null
		assertThat(owner6.getPet(999)).isNull();
		// Additional negative test: retrieving a pet by a non-existent name should return
		// null
		assertThat(owner6.getPet("nonexistent")).isNull();
		// Added negative test for invalid pet id
		assertThat(owner6.getPet(-1)).isNull();
	}

}
