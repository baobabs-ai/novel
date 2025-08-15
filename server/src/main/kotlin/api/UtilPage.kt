package api

fun validatePageNumber(page: Int) {
    if (page < 0) {
        throwBadRequest("Page number should not be less than 0")
    }
}

fun validatePageSize(pageSize: Int, max: Int = 100) {
    if (pageSize < 1) {
        throwBadRequest("Page size should not be less than 1")
    }
    if (pageSize > max) {
        throwBadRequest("Page size should not be greater than ${max}")
    }
}
