package com.doodle.calendar_challenge.infrastructure.rest.error;

import java.util.Map;

public record ValidationErrorResponse(Map<String, String> errors) {
}
