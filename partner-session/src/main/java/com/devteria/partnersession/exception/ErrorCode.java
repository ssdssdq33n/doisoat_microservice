package com.devteria.partnersession.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1004, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1006, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_SALEPARTNER(1007, "Add sale partner failed", HttpStatus.BAD_REQUEST),
    SALEPARTNER_EXISTED(1008, "Sale partner existed", HttpStatus.BAD_REQUEST),
    SALEPARTNER_NOT_EXISTED(1009, "Sale partner not existed", HttpStatus.NOT_FOUND),
    INVALID_SUPPLYPARTNER(1010, "Add Supply partner failed", HttpStatus.BAD_REQUEST),
    SUPPLYPARTNER_EXISTED(1011, "Supply partner existed", HttpStatus.BAD_REQUEST),
    SUPPLYPARTNER_NOT_EXISTED(1012, "Supply partner not existed", HttpStatus.NOT_FOUND),
    INVALID_SESSION(1013, "Create session failed", HttpStatus.BAD_REQUEST),
    SESSION_EXISTED(1014, "Session existed", HttpStatus.BAD_REQUEST),
    SESSION_NOT_EXISTED(1015, "Session not existed", HttpStatus.NOT_FOUND),
    MAXLENGTH_NAME(1016, "Exceeds 100 characters", HttpStatus.NOT_FOUND),
    MAXLENGTH_DESCRIPTION(1017, "Exceeds 255 characters", HttpStatus.NOT_FOUND),
    CANT_DELETE(1018, "Can't delete partner", HttpStatus.NOT_FOUND),
    SESSION_DAY_ERROR(1019, "Cannot create session with date greater than today", HttpStatus.BAD_REQUEST),
    DELETE_ERROR(1020, "Cannot delete session have status is not success and failed", HttpStatus.BAD_REQUEST),
    SALE_SUPPLIER_PARTNER_NOT_MATCH(1021, "Sales partner and supplier partner do not match", HttpStatus.BAD_REQUEST),
    CONTROL_SESSION_DAY_EXISTED(1022, "Control session day existed", HttpStatus.BAD_REQUEST),
    DAY_ERROR(1023, "Invalid date", HttpStatus.BAD_REQUEST),

    LIMIT_TIMEOUT(1024, "Limit timeout", HttpStatus.BAD_REQUEST),
    PROTON_FILE_NOT_FOUND(1025, "proton find not found", HttpStatus.BAD_REQUEST),
    IRIS_FILE_NOT_FOUND(1026, "iris file not found", HttpStatus.BAD_REQUEST),
    SACOMBANK_FILE_NOT_FOUND(1027, "sacombank file not found", HttpStatus.BAD_REQUEST),

    ERROR_STATUS(1028, "Error status", HttpStatus.BAD_REQUEST),
    MONTH_ERROR(1029, "Invalid month", HttpStatus.BAD_REQUEST),
    CONTROL_SESSION_MONTH_EXISTED(1030, "Control session month existed", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT(1031, "invalid date format", HttpStatus.BAD_REQUEST),
    NOT_FILE(1034, "Not file", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1035, "Email not valid", HttpStatus.BAD_REQUEST),
    SEND_EMAIL_ERROR(1036, "Send email error", HttpStatus.BAD_REQUEST),
    PAYMENT_ERROR(1037, "get file Proton Payment error", HttpStatus.BAD_REQUEST),
    ERROR_PASSWORD(1038, "password is not valid", HttpStatus.BAD_REQUEST),

    IMAGE_ERROR(1039, "Counld not determine file type", HttpStatus.BAD_REQUEST),
    MISSING_DATE(1040, "Missing date file", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
