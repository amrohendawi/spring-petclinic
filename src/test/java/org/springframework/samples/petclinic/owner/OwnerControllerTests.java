package org.springframework.samples.petclinic.service;

// ... (other imports)

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
// @AutoConfigureTestDatabase(replace = Replace.NONE)
class ClinicServiceTests {

    // ... (other fields)

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

        // Adding assertions to ensure null checks are enforced
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> owner6.addVisit(null, visit))
            .withMessage("Pet identifier must not be null!");

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> owner6.addVisit(pet7.getId(), null))
            .withMessage("Visit must not be null!");
    }

    @Test
    void testShowOwner() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
            .andExpect(status().isOk())
            // ... (other assertions)
            .andExpect(view().name("owners/ownerDetails"));

        // Adding assertion to verify toString output
        Owner owner = george();
        String expectedToString = "Owner[id=1, new=false, lastName=Franklin, firstName=George, address=110 W. Liberty St., city=Madison, telephone=6085551023]";
        assertThat(owner.toString()).isEqualTo(expectedToString);
    }
}