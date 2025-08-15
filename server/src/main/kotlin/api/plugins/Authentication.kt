package api.plugins

import api.throwUnauthorized
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import infra.user.UserRepository
import infra.user.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.datetime.Clock
import org.koin.ktor.ext.get
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Instant

data class User(
    val id: String,
    val username: String,
    val role: UserRole,
    val createdAt: Instant,
)

fun User.shouldBeAtLeast(role: UserRole) {
    if (!(this.role atLeast role)) {
        throwUnauthorized("Only users with ${role.name} or higher permission can perform this action")
    }
}

fun User.isOldAss(): Boolean {
    return Clock.System.now() - createdAt >= 30.days
}

fun User.shouldBeOldAss() {
    if (!isOldAss()) {
        throwUnauthorized("You are too young")
    }
}

fun Application.authentication(secret: String) = install(Authentication) {
    jwt {
        verifier(
            JWT.require(Algorithm.HMAC256(secret)).build()
        )
        validate { credential ->
            if (credential.subject != null) {
                JWTPrincipal(credential.payload)
            } else {
                null
            }
        }
        challenge { _, _ ->
            call.respond(HttpStatusCode.Unauthorized, "Token is illegal or expired")
        }
    }
}

fun ApplicationCall.user(): User =
    attributes[AuthenticatedUserKey]

fun ApplicationCall.userOrNull(): User? =
    attributes.getOrNull(AuthenticatedUserKey)

fun Route.authenticateDb(
    optional: Boolean = false,
    build: Route.() -> Unit,
): Route {
    return authenticate(
        strategy = if (optional) AuthenticationStrategy.Optional else AuthenticationStrategy.FirstSuccessful,
        build = build,
    ).apply {
        install(PostAuthenticationInterceptors)
    }
}

private val AuthenticatedUserKey = AttributeKey<User>("AuthenticatedUserKey")

private val PostAuthenticationInterceptors = createRouteScopedPlugin(name = "User Validator") {
    val userRepo = application.get<UserRepository>()

    on(AuthenticationChecked) { call ->
        call.principal<JWTPrincipal>()?.let { principal ->
            val user = User(
                id = userRepo.getId(principal.subject!!),
                username = principal.subject!!,
                role = when (principal["role"]) {
                    "admin" -> UserRole.Admin
                    "trusted" -> UserRole.Trusted
                    "member" -> UserRole.Member
                    "restricted" -> UserRole.Restricted
                    else -> UserRole.Banned
                },
                createdAt = Instant.fromEpochSeconds(
                    principal.getClaim("crat", Long::class)!!
                ),
            )
            if (user.role === UserRole.Banned) {
                call.respond(HttpStatusCode.Unauthorized, "User has been banned")
            } else {
                call.attributes.put(AuthenticatedUserKey, user)
            }
        }
    }
}
