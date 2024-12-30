
@Test
void testAddVisitWithNullPetId() throws Exception {
    Owner owner = george();
    Visit visit = new Visit();
    visit.setDescription("Check-up");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        owner.addVisit(null, visit);
    });

    assertThat(exception.getMessage()).contains("Pet identifier must not be null!");
}

@Test
void testAddVisitWithNullVisit() throws Exception {
    Owner owner = george();

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        owner.addVisit(owner.getPets().get(0).getId(), null);
    });

    assertThat(exception.getMessage()).contains("Visit must not be null!");
}

@Test
void testAddVisitWithInvalidPetId() throws Exception {
    Owner owner = george();
    Visit visit = new Visit();
    visit.setDescription("Check-up");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        owner.addVisit(999, visit);
    });

    assertThat(exception.getMessage()).contains("Invalid Pet identifier!");
}

@Test
void testGetPetWithIgnoreNewFlag() throws Exception {
    Owner owner = george();
    Pet newPet = new Pet();
    newPet.setName("NewPet");
    owner.addPet(newPet);

    Pet pet = owner.getPet("NewPet", true);
    assertThat(pet).isNull();
}

@Test
void testToString() {
    Owner owner = george();
    String ownerString = owner.toString();

    assertThat(ownerString).contains("id", "new", "lastName", "firstName", "address", "city", "telephone");
}