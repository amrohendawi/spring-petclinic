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
 * @author Spring PetClinic Team
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

}
