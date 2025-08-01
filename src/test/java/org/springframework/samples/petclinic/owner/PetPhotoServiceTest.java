package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for {@link PetPhotoService}
 *
 * Modified to add boundary tests for file size verification. The tests now explicitly
 * create files at one byte below the maximum allowed size, exactly at the maximum allowed
 * size, and one byte over this limit. This ensures that any mutation modifying the
 * relational operator in the file size check is detected.
 */
class PetPhotoServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldReturnNullForEmptyFile() throws Exception {
		// given
		PetPhotoService service = new PetPhotoService();
		MockMultipartFile emptyFile = new MockMultipartFile("photo", "", "image/jpeg", new byte[0]);

		// when
		String result = service.uploadPhoto(emptyFile);

		// then
		assertThat(result).isNull();
	}

	@Test
	void shouldRejectOversizedFile() {
		// given
		PetPhotoService service = new PetPhotoService();
		byte[] largeFileContent = new byte[6 * 1024 * 1024]; // 6MB file
		MockMultipartFile largeFile = new MockMultipartFile("photo", "large.jpg", "image/jpeg", largeFileContent);

		// when & then
		assertThatThrownBy(() -> service.uploadPhoto(largeFile)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("File size exceeds maximum limit");
	}

	@Test
	void shouldRejectInvalidFileType() {
		// given
		PetPhotoService service = new PetPhotoService();
		MockMultipartFile textFile = new MockMultipartFile("photo", "document.txt", "text/plain",
				"test content".getBytes());

		// when & then
		assertThatThrownBy(() -> service.uploadPhoto(textFile)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("File type not supported");
	}

	@Test
	void shouldAcceptValidImageFile() throws Exception {
		// given
		PetPhotoService service = new PetPhotoService();
		MockMultipartFile validImage = new MockMultipartFile("photo", "pet.jpg", "image/jpeg",
				"fake image content".getBytes());

		// when
		String result = service.uploadPhoto(validImage);

		// then
		assertThat(result).isNotNull();
		assertThat(result).endsWith(".jpg");
	}

	@Test
	void shouldAcceptFileAtBoundarySizes() throws Exception {
		// given
		PetPhotoService service = new PetPhotoService();
		final int MAX_BYTES = 5 * 1024 * 1024; // Maximum allowed file size (5MB)

		// Test file one byte below the maximum allowed size
		byte[] oneByteBelow = new byte[MAX_BYTES - 1];
		MockMultipartFile belowBoundaryFile = new MockMultipartFile("photo", "belowBoundary.jpg", "image/jpeg",
				oneByteBelow);

		// when
		String resultBelow = service.uploadPhoto(belowBoundaryFile);

		// then
		assertThat(resultBelow).isNotNull();
		assertThat(resultBelow).endsWith(".jpg");

		// Test file exactly at the maximum allowed size
		byte[] atBoundary = new byte[MAX_BYTES];
		MockMultipartFile boundaryFile = new MockMultipartFile("photo", "boundary.jpg", "image/jpeg", atBoundary);

		String resultAt = service.uploadPhoto(boundaryFile);

		assertThat(resultAt).isNotNull();
		assertThat(resultAt).endsWith(".jpg");
	}

	@Test
	void shouldRejectFileOneByteOverMaxAllowedSize() {
		// given
		PetPhotoService service = new PetPhotoService();
		final int MAX_BYTES = 5 * 1024 * 1024; // Maximum allowed file size (5MB)
		// Create a file that is exactly 5MB + 1 byte
		byte[] overBoundaryContent = new byte[MAX_BYTES + 1];
		MockMultipartFile overBoundaryFile = new MockMultipartFile("photo", "overboundary.jpg", "image/jpeg",
				overBoundaryContent);

		// when & then
		// This test ensures that even a single byte over the maximum limit is rejected
		assertThatThrownBy(() -> service.uploadPhoto(overBoundaryFile)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("File size exceeds maximum limit");
	}

	@Test
	void shouldAcceptValidPngImageFile() throws Exception {
		// given
		PetPhotoService service = new PetPhotoService();
		// Create a valid PNG file with a size well below the maximum allowed size
		MockMultipartFile validPng = new MockMultipartFile("photo", "pet.png", "image/png",
				"fake png content".getBytes());

		// when
		String result = service.uploadPhoto(validPng);

		// then
		assertThat(result).isNotNull();
		assertThat(result).endsWith(".png");
	}

}