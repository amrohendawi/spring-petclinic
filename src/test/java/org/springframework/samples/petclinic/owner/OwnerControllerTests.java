
package org.springframework.samples.petclinic.owner;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

    private static final int TEST_OWNER_ID = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerRepository owners;

    private Owner george() {
        Owner george = new Owner();
        george.setId(TEST_OWNER_ID);
        george.setFirstName("George");
        george.setLastName("Franklin");
        george.setAddress("110 W. Liberty St.");
        george.setCity("Madison");
        george.setTelephone("6085551023");
        Pet max = new Pet();
        PetType dog = new PetType();
        dog.setName("dog");
        max.setType(dog);
        max.setName("Max");
        max.setBirthDate(LocalDate.now());
        george.addPet(max);
        max.setId(1);
        return george;
    }

    @BeforeEach
    void setup() {

        Owner george = george();
        given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
                .willReturn(new PageImpl<>(Lists.newArrayList(george)));

        given(this.owners.findAll(any(Pageable.class))).willReturn(new PageImpl<>(Lists.newArrayList(george)));

        given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
        Visit visit = new Visit();
        visit.setDate(LocalDate.now());
        george.getPet("Max").getVisits().add(visit);

    }

    @Test
    void testGetPetWithExistingPetName() {
        Owner owner = george();
        Pet pet = owner.getPet("Max");
        assertNotNull(pet, "Expected to find a pet with name 'Max'");
        assertEquals("Max", pet.getName(), "Pet name should be 'Max'");
    }

    @Test
    void testGetPetWithNonExistingPetName() {
        Owner owner = george();
        Pet pet = owner.getPet("NonExistent");
        assertNull(pet, "Expected not to find a pet with name 'NonExistent'");
    }

    // ... (other tests remain unchanged)
}
