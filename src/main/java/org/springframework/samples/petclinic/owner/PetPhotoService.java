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

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling pet photo uploads and management.
 *
 * @author Spring PetClinic Team
 */
@Service
public class PetPhotoService {

	private static final String UPLOAD_DIR = "src/main/resources/static/resources/images/pets/";

	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

	private static final String[] ALLOWED_EXTENSIONS = { "jpg", "jpeg", "png", "gif" };

	/**
	 * Upload a pet photo and return the generated filename.
	 * @param file the multipart file to upload
	 * @return the generated filename, or null if file is empty
	 * @throws IOException if file upload fails
	 * @throws IllegalArgumentException if file validation fails
	 */
	public String uploadPhoto(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			return null;
		}

		validateFile(file);

		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		String fileExtension = getFileExtension(originalFilename);
		String newFilename = UUID.randomUUID().toString() + "." + fileExtension;

		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		Path filePath = uploadPath.resolve(newFilename);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		return newFilename;
	}

	/**
	 * Delete a pet photo file.
	 * @param filename the filename to delete
	 */
	public void deletePhoto(String filename) {
		if (filename != null && !filename.equals("default-pet.svg")) {
			try {
				Path filePath = Paths.get(UPLOAD_DIR + filename);
				Files.deleteIfExists(filePath);
			}
			catch (IOException e) {
				// Log error but don't fail the operation
				System.err.println("Failed to delete photo file: " + filename + " - " + e.getMessage());
			}
		}
	}

	/**
	 * Validate uploaded file for size and type restrictions.
	 * @param file the file to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateFile(MultipartFile file) {
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException("Invalid filename");
		}

		String extension = getFileExtension(filename).toLowerCase();

		boolean isValidExtension = false;
		for (String allowedExt : ALLOWED_EXTENSIONS) {
			if (allowedExt.equals(extension)) {
				isValidExtension = true;
				break;
			}
		}

		if (!isValidExtension) {
			throw new IllegalArgumentException("File type not supported. Please upload JPG, PNG, or GIF files only.");
		}
	}

	/**
	 * Extract file extension from filename.
	 * @param filename the filename
	 * @return the file extension (without dot)
	 */
	private String getFileExtension(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
			return "";
		}
		return filename.substring(lastDotIndex + 1);
	}

}
