package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * autowiring <em>by type</em>.
 * <li><strong>Transaction management</strong>, meaning each test method is executed in
 * its own transaction, which is automatically rolled back by default. Thus, even if tests
 * insert or otherwise change database state, there is no need for a teardown or cleanup
 * script.
 * <li>An {@link org.springframework.context.ApplicationContext ApplicationContext} is
 * also inherited and can be used for explicit bean lookup if necessary.</li>
 * </ul>
 *
 * @author Ken Krebs
 * @... (and other authors)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
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

    // Other existing test methods...

    @Test
    @Transactional
    void shouldThrowExceptionWhenAddingVisitWithNullPetId() {
        Optional<Owner> optionalOwner = this.owners.findById(6);
        assertThat(optionalOwner).isPresent();
        Owner owner6 = optionalOwner.get();

        Visit visit = new Visit();
        visit.setDescription("test");

        assertThrows(IllegalArgumentException.class, () -> {
            owner6.addVisit(null, visit);
        });
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenAddingVisitWithNullVisit() {
        Optional<Owner> optionalOwner = this.owners.findById(6);
        assertThat(optionalOwner).isPresent();
        Owner owner6 = optionalOwner.get();

        assertThrows(IllegalArgumentException.class, () -> {
            owner6.addVisit(7, null);
        });
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenAddingVisitWithInvalidPetId() {
        Optional<Owner> optionalOwner = this.owners.findById(6);
        assertThat(optionalOwner).isPresent();
        Owner owner6 = optionalOwner.get();

        Visit visit = new Visit();
        visit.setDescription("test");

        assertThrows(IllegalArgumentException.class, () -> {
            owner6.addVisit(999, visit);
        });
    }

    // Rest of the test methods remain unchanged
}
