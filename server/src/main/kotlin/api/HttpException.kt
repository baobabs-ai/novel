package api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

data class HttpException(
    val status: HttpStatusCode,
    override val message: String,
) : Exception(message)

fun throwBadRequest(message: String): Nothing =
    throw HttpException(HttpStatusCode.BadRequest, message)

fun throwUnauthorized(message: String): Nothing =
    throw HttpException(HttpStatusCode.Unauthorized, message)

fun throwConflict(message: String): Nothing =
    throw HttpException(HttpStatusCode.Conflict, message)

fun throwNotFound(message: String): Nothing =
    throw HttpException(HttpStatusCode.NotFound, message)

fun throwInternalServerError(message: String): Nothing =
    throw HttpException(HttpStatusCode.InternalServerError, message)

suspend inline fun ApplicationCall.doOrRespondError(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        val httpMethod = request.httpMethod.value
        val uri = request.uri
        application.environment.log.warn("Caught exception $httpMethod-$uri:", e.message)

        e.printStackTrace()

        if (e is HttpException) {
            respond(e.status, e.message)
        } else {
            respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
        }
    }
}

suspend inline fun <reified T : Any?> ApplicationCall.tryRespond(block: () -> T) =
    doOrRespondError {
        val message = block()
        if (message is Unit || message == null) {
            response.status(HttpStatusCode.OK)
        } else {
            respond(message)
        }
    }

suspend inline fun ApplicationCall.tryRespondRedirect(block: () -> String) =
    doOrRespondError {
        val url = block()
        respondRedirect(url)
    }
