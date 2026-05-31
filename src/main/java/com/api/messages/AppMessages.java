package com.api.messages;

public final class AppMessages {

	private AppMessages() {
	}

	public static final String INVALID_JWT =
			"JWT token is InValid";
	public static final String JWT_EXPIRED =
			"JWT token has been expired, Kindly Re-generate and Process";

	public static final String FILE_UPLOAD_SUCCESS =
			"File uploaded successfully.";

	public static final String DUPLICATE_ROWS_FOUND =
			"Duplicate file rows detected. No new records inserted.";

	public static final String PARTIAL_DUPLICATE_ROWS =
			"Some rows inserted successfully and some duplicate rows skipped.";

	public static final String AUTHENTICATION_FAILED =
			"Authentication failed.";

	public static final String INVALID_FILE_FORMAT =
			"Only .xlsx, .xls and .csv files are accepted.";

	public static final String EMPTY_FILE =
			"Cannot upload empty file.";

	public static final String EXISTING_ROWS =
			"All rows already exist. Skipping upload metadata insert";

	public static final String PARSING_ERROR =
			"Failed to upload/parse Excel file";

	public static final String SKIP_INVALIDATE=
			"Skipping invalid row at rowIndex";

	public static final String DUPLICATE_ROW=
			"Duplicate rows already exist";
}

