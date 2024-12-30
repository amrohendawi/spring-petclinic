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
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ClinicServiceTests {

    @Autowired
    protected OwnerRepository owners;

    @Autowired
    protected VetRepository vets;

    Pageable pageable;

    // Existing test methods...

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

        assertThat(pet7.getVisits()) //
            .hasSize(found + 1) //
            .allMatch(value -> value.getId() != null);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenPetIdIsNull() {
        Optional<Owner> optionalOwner = this.owners.findById(6);
        assertThat(optionalOwner).isPresent();
        Owner owner6 = optionalOwner.get();

        Visit visit = new Visit();
        visit.setDescription("test");

        assertThatThrownBy(() -> owner6.addVisit(null, visit))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Pet identifier must not be null!");
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenVisitIsNull() {
        Optional<Owner> optionalOwner = this.owners.findById(6);
        assertThat(optionalOwner).isPresent();
        Owner owner6 = optionalOwner.get();

        Pet pet7 = owner6.getPet(7);

        assertThatThrownBy(() -> owner6.addVisit(pet7.getId(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Visit must not be null!");
    }

    @Test
    void shouldFindPetByNameIgnoringCase() {
        Optional<Owner> optionalOwner = this.owners.findById(1);
        assertThat(optionalOwner).isPresent();
        Owner owner = optionalOwner.get();

        Pet pet = owner.getPet("cat", true);
        assertThat(pet).isNotNull();
        assertThat(pet.getName()).isEqualToIgnoringCase("cat");
    }

    @Test
    @Transactional
    void shouldReturnNullWhenNoPetFoundByName() {
        Optional<Owner> optionalOwner = this.owners.findById(1);
        assertThat(optionalOwner).isPresent();
        Owner owner = optionalOwner.get();

        Pet pet = owner.getPet("nonexistent", true);
        assertThat(pet).isNull();
    }

    @Test
    void shouldReturnNonEmptyStringForOwnerToString() {
        Optional<Owner> optionalOwner = this.owners.findById(1);
        assertThat(optionalOwner).isPresent();
        Owner owner = optionalOwner.get();

        String ownerDescription = owner.toString();
        assertThat(ownerDescription).isNotEmpty();
    }
}