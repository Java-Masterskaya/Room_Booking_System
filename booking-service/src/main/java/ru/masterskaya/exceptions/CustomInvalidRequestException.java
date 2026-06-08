package ru.masterskaya.exceptions;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.InvalidRequestException;

import java.util.List;

public class CustomInvalidRequestException extends InvalidRequestException {

    private final List<String> maskedErrors;

    public CustomInvalidRequestException(ValidationReport validationReport, List<String> maskedErrors) {
        super(validationReport);
        this.maskedErrors = maskedErrors;
    }

    @Override
    public String getMessage() {
        return "OpenAPI Validation Failed: " + String.join(", ", maskedErrors);
    }

    public List<String> getMaskedErrors() {
        return maskedErrors;
    }
}
