// Minimal reference worker-kmp Web Push server — Ktor variant.
//
// Added in worker-kmp v3.0.0-alpha06.X (Phase 9 alpha06.X).
//
// Endpoints:
//   POST /push/subscribe   — accepts {endpoint, p256dh, auth} JSON; stores in SQLite
//   GET  /push/subscribers — admin: count current subscriptions
//
// Background loop: every hour, sends a worker-kmp trigger push to all subscriptions.
//
// VAPID keys: read from env VAPID_PUBLIC_KEY + VAPID_PRIVATE_KEY (BASE64URL-encoded).
// Generate via: npx web-push generate-vapid-keys (or OpenSSL — see README).
//
// Per SECURITY.md T7-T15 (worker-kmp v3.0.0 epic Phase 10):
//   * MUST NOT log raw endpoint URLs (hashed below).
//   * SHOULD encrypt subscription rows at rest (NOT done in this minimal sample).
//   * SHOULD rate-limit /push/subscribe per IP (NOT done — add for production).

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Subscription
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.security.MessageDigest
import java.security.Security

@Serializable
data class SubscribeRequest(val endpoint: String, val p256dh: String, val auth: String)

@Serializable
data class SubscribeResponse(val ok: Boolean)

@Serializable
data class SubscribersCount(val count: Long)

object Subs : Table("subs") {
    val endpoint = text("endpoint")
    val p256dh = text("p256dh")
    val auth = text("auth")
    val created = long("created")
    override val primaryKey = PrimaryKey(endpoint)
}

private fun hashEndpoint(endpoint: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(endpoint.toByteArray())
    return "sha256:" + bytes.joinToString("") { "%02x".format(it) }.substring(0, 8)
}

fun main() {
    val vapidPublic = System.getenv("VAPID_PUBLIC_KEY")
    val vapidPrivate = System.getenv("VAPID_PRIVATE_KEY")
    val vapidSubject = System.getenv("VAPID_SUBJECT") ?: "mailto:admin@example.com"
    if (vapidPublic.isNullOrBlank() || vapidPrivate.isNullOrBlank()) {
        System.err.println("Set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY env vars.")
        System.err.println("Generate via: npx web-push generate-vapid-keys")
        kotlin.system.exitProcess(1)
    }
    if (Security.getProvider("BC") == null) {
        Security.addProvider(BouncyCastleProvider())
    }

    Database.connect("jdbc:sqlite:subscriptions.db", driver = "org.sqlite.JDBC")
    transaction { SchemaUtils.create(Subs) }

    val pushService = PushService(vapidPublic, vapidPrivate, vapidSubject)
    val cronScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cron — every hour, send worker-kmp trigger to every subscription.
    cronScope.launch {
        while (true) {
            delay(60 * 60 * 1000L) // 1 hour
            val rows = transaction {
                Subs.selectAll().map {
                    Triple(it[Subs.endpoint], it[Subs.p256dh], it[Subs.auth])
                }
            }
            for ((endpoint, p256dh, auth) in rows) {
                runCatching {
                    val sub = Subscription(endpoint, Subscription.Keys(p256dh, auth))
                    val payload = """{"type":"WORKER_KMP_TRIGGER","scope":"cron"}"""
                    val notif = Notification(sub, payload)
                    val resp = pushService.send(notif)
                    val code = resp.statusLine.statusCode
                    println("Pushed to ${hashEndpoint(endpoint)} → $code")
                    if (code == 410 || code == 404) {
                        // expired — drop
                        transaction { Subs.deleteWhere { Subs.endpoint eq endpoint } }
                    }
                }.onFailure { e ->
                    println("Push failed for ${hashEndpoint(endpoint)}: ${e.message}")
                }
            }
        }
    }

    val port = (System.getenv("PORT") ?: "8787").toInt()
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) { json() }
        routing {
            post("/push/subscribe") {
                val req = call.receive<SubscribeRequest>()
                transaction {
                    // INSERT OR REPLACE by deleting + inserting (Exposed sqlite quirk).
                    Subs.deleteWhere { Subs.endpoint eq req.endpoint }
                    Subs.insert {
                        it[endpoint] = req.endpoint
                        it[p256dh] = req.p256dh
                        it[auth] = req.auth
                        it[created] = System.currentTimeMillis()
                    }
                }
                println("Subscribed: ${hashEndpoint(req.endpoint)}")
                call.respond(SubscribeResponse(ok = true))
            }
            get("/push/subscribers") {
                val n = transaction { Subs.selectAll().count() }
                call.respond(SubscribersCount(count = n))
            }
            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }.start(wait = true)
}
