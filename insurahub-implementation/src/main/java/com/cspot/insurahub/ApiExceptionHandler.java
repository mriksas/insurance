package com.cspot.insurahub;

import com.cspot.insurahub.claim.exception.ClaimNotPendingException;
import com.cspot.insurahub.claim.exception.InvalidReceiptException;
import com.cspot.insurahub.claim.exception.ClaimUpdateNotAllowedException;
import com.cspot.insurahub.common.exception.DomainValidationException;
import com.cspot.insurahub.common.exception.InvalidPageRequestException;
import com.cspot.insurahub.common.exception.ResourceNotFoundException;
import com.cspot.insurahub.consumer.exception.EmailAlreadyInUseException;
import com.cspot.insurahub.consumer.exception.ConsumerNotFoundException;
import com.cspot.insurahub.consumer.exception.UserCreationException;
import com.cspot.insurahub.enrollment.exception.EnrollmentDeniedException;
import com.cspot.insurahub.insurancepackage.exception.PackageNotFoundException;
import com.cspot.insurahub.insurancepackage.exception.PackageUpdateNotAllowedException;
import com.cspot.insurahub.model.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApiExceptionHandler {

    private final Clock clock;

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleUserCreationException(UserCreationException e, HttpServletRequest request) {
        logError(e);
        ErrorDto errorDto = new ErrorDto()
                .error("USER_CREATION_FAILED")
                .status(500)
                .message("Failed to create user. Please try again later.")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public ErrorDto handleEmailAlreadyInUseException(EmailAlreadyInUseException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("EMAIL_ALREADY_IN_USE")
                .status(409)
                .message("The specified email is already in use.")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorDto handleConsumerNotFoundException(ConsumerNotFoundException e, HttpServletRequest request) {
        logWarn(e);
        return new ErrorDto()
                .error("CONSUMER_NOT_FOUND")
                .status(404)
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleOptimisticLockingFailureException(OptimisticLockingFailureException e,
                                                            HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("CONCURRENT_MODIFICATION")
                .status(409)
                .message("The resource was modified by another user before your changes could be saved.")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        logWarn(e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField())
                .collect(Collectors.joining("; "));
        ErrorDto errorDto = new ErrorDto()
                .error("VALIDATION_FAILED")
                .status(400)
                .message(message)
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleConstraintViolationException(ConstraintViolationException e,
                                                       HttpServletRequest request) {
        logWarn(e);
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        ErrorDto errorDto = new ErrorDto()
                .error("VALIDATION_FAILED")
                .status(400)
                .message(message)
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleInvalidPageRequestException(InvalidPageRequestException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("VALIDATION_FAILED")
                .status(400)
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorDto handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("RESOURCE_NOT_FOUND")
                .status(404)
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleInvalidReceiptException(InvalidReceiptException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("INVALID_RECEIPT")
                .status(400)
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        logWarn(e);
        return new ErrorDto()
                .error("VALIDATION_FAILED")
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Invalid request parameter format")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("MALFORMED_REQUEST_BODY")
                .status(400)
                .message("Missing or invalid request body")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handlePackageUpdateNotAllowedException(PackageUpdateNotAllowedException e,
                                                           HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("PACKAGE_UPDATE_NOT_ALLOWED")
                .status(400)
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public ErrorDto handleAccessDeniedException(AuthorizationDeniedException e, HttpServletRequest request) {
        logWarn(e);
        ErrorDto errorDto = new ErrorDto()
                .error("ACCESS_DENIED")
                .status(403)
                .message("You do not have permissions to perform this operation.")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return errorDto;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto defaultHandler(Exception e, HttpServletRequest request) {
        logError(e);
        return new ErrorDto()
                .error("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal server error")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }


    @ExceptionHandler(DomainValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorDto handleDomainValidationException(
            DomainValidationException e,
            HttpServletRequest request
    ) {
        logWarn(e);

        return new ErrorDto()
                .error(e.getCode())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler(PackageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto handlePackageNotFoundException(
            PackageNotFoundException e,
            HttpServletRequest request
    ) {
        logWarn(e);

        return new ErrorDto()
                .error("PACKAGE_NOT_FOUND")
                .status(HttpStatus.NOT_FOUND.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler(ClaimUpdateNotAllowedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorDto handleClaimUpdateNotAllowedException(
            ClaimUpdateNotAllowedException e,
            HttpServletRequest request
    ) {
        logWarn(e);

        return new ErrorDto()
                .error("CLAIM_UPDATE_NOT_ALLOWED")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler(EnrollmentDeniedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorDto handleEnrollmentDeniedException(EnrollmentDeniedException e, HttpServletRequest request) {
        logWarn(e);
        return new ErrorDto()
                .error("ENROLLMENT_DENIED")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler(ClaimNotPendingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorDto handleClaimNotPendingException(ClaimNotPendingException e, HttpServletRequest request) {
        logWarn(e);
        return new ErrorDto()
                .error("CLAIM_NOT_PENDING")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorDto handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        logWarn(e);

        return new ErrorDto()
                .error("UNSUPPORTED_MEDIA_TYPE")
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .message(e.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
    }

    private void logError(Exception e) {
        log.error("Exception encountered: ", e);
    }

    private void logWarn(Exception e) {
        log.warn("Exception encountered: ", e);
    }
}
