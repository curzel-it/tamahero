package it.curzel.tamahero.auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object EmailConfig {
    val apiKey: String = System.getenv("SMTP2GO_API_KEY") ?: ""
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

object EmailService {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)
    private const val SMTP2GO_ENDPOINT = "https://api.smtp2go.com/v3/email/send"
    private const val SENDER = "TamaHero <noreply@tama.curzel.it>"
    private val httpClient = HttpClient(CIO) { engine { requestTimeout = 10_000 } }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendPasswordResetEmail(email: String, token: String) {
        val link = "https://tama.curzel.it/reset-password?token=$token"
        val html = emailTemplate(
            heading = "Reset your password",
            body = "Click the button below to reset your TamaHero account password.",
            buttonText = "Reset Password",
            buttonLink = link,
            footer = "This link expires in 1 hour. If you didn't request this, you can ignore this email.",
        )
        sendEmail(email, "Reset your TamaHero password", html)
    }

    private fun emailTemplate(heading: String, body: String, buttonText: String, buttonLink: String, footer: String): String {
        val bg = "#111111"
        val surface = "#1A1A2E"
        val surfaceElevated = "#2A2A3E"
        val text = "#E8E8F0"
        val textMuted = "#9A97B0"
        val accent = "#4CAF50"
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body style="margin: 0; padding: 0; background-color: $bg;">
                <div style="background-color: $bg; padding: 48px 16px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                    <div style="max-width: 440px; margin: 0 auto;">
                        <div style="text-align: center; margin-bottom: 32px;">
                            <span style="font-size: 28px; font-weight: 700; color: $accent; letter-spacing: 4px;">TAMAHERO</span>
                        </div>
                        <div style="background-color: $surface; border-radius: 12px; padding: 40px 32px; border: 1px solid $surfaceElevated;">
                            <h2 style="color: $text; margin: 0 0 12px 0; font-size: 22px; font-weight: 700; text-align: center;">$heading</h2>
                            <p style="color: $textMuted; font-size: 14px; line-height: 1.6; margin: 0 0 32px 0; text-align: center;">$body</p>
                            <div style="text-align: center; margin: 0 0 32px 0;">
                                <a href="$buttonLink" style="display: inline-block; padding: 14px 40px; background-color: $accent; color: #fff; text-decoration: none; border-radius: 8px; font-size: 14px; font-weight: 700; letter-spacing: 1px;">$buttonText</a>
                            </div>
                            <div style="border-top: 1px solid $surfaceElevated; padding-top: 20px;">
                                <p style="color: $textMuted; font-size: 11px; line-height: 1.5; margin: 0; text-align: center;">$footer</p>
                            </div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private suspend fun sendEmail(to: String, subject: String, htmlBody: String) {
        if (!EmailConfig.isConfigured) {
            logger.warn("SMTP2GO_API_KEY not configured, skipping email to {}", to)
            return
        }
        try {
            val payload = EmailPayload(api_key = EmailConfig.apiKey, to = listOf(to), sender = SENDER, subject = subject, html_body = htmlBody)
            val response = httpClient.post(SMTP2GO_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(payload))
            }
            if (response.status.isSuccess()) {
                logger.info("Email sent to {}: {}", to, subject)
            } else {
                logger.error("Failed to send email to {}: {} {}", to, response.status, response.bodyAsText())
            }
        } catch (e: Exception) {
            logger.error("Error sending email to {}: {}", to, e.message, e)
        }
    }

    @Serializable
    private data class EmailPayload(val api_key: String, val to: List<String>, val sender: String, val subject: String, val html_body: String)
}
