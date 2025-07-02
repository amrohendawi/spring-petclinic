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
 * Modified to add boundary test for file size exactly at the maximum allowed limit to
 * catch mutations that change the comparison operator for the size check.
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
	void shouldAcceptFileExactlyAtMaxAllowedSize() throws Exception {
		// given
		PetPhotoService service = new PetPhotoService();
		// 5MB file exactly (5 * 1024 * 1024 bytes)
		byte[] boundaryFileContent = new byte[5 * 1024 * 1024];
		MockMultipartFile boundaryFile = new MockMultipartFile("photo", "boundary.jpg", "image/jpeg",
				boundaryFileContent);

		// when
		String result = service.uploadPhoto(boundaryFile);

		// then
		assertThat(result).isNotNull();
		assertThat(result).endsWith(".jpg");
	}

}
