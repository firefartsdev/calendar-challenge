package com.doodle.calendar_challenge.domain.shared;

import java.util.List;

public record PagedResult<T>(List<T> content, long totalElements, int totalPages, int currentPage, int pageSize) {
}
