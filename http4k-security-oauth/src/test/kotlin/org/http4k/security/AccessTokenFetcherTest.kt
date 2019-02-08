package org.http4k.security

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.Validator.Ignore
import org.http4k.lens.Validator.Strict
import org.http4k.lens.WebForm
import org.http4k.lens.string
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenFetcher.Companion.Forms.clientId
import org.http4k.security.AccessTokenFetcher.Companion.Forms.code
import org.http4k.security.AccessTokenFetcher.Companion.Forms.grantType
import org.http4k.security.AccessTokenFetcher.Companion.Forms.redirectUri
import org.http4k.security.AccessTokenFetcher.Companion.Forms.requestForm
import org.http4k.security.AccessTokenFetcher.Companion.Forms.responseForm
import org.http4k.security.oauth.server.refreshtoken.RefreshToken
import org.http4k.security.openid.IdToken
import org.junit.jupiter.api.Test

internal class AccessTokenFetcherTest {
    private val config = OAuthProviderConfig(Uri.of("irrelevant"), "/", "/path", Credentials("", ""))
    private val accessTokenFetcherAuthenticator = ClientSecretAccessTokenFetcherAuthenticator(config)

    @Test
    fun `can get access token from plain text body`() {
        val api = HttpHandler { Response(OK).body("some-access-token") }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }

    @Test
    fun `can get access token from json body`() {
        //see https://tools.ietf.org/html/rfc6749#section-4.1.4
        val api = HttpHandler { Response(OK).with(accessTokenResponseBody of AccessTokenResponse("some-access-token")) }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }

    @Test
    fun `can get access token from form encoded body`() {
        val accessTokenDetails = AccessTokenDetails(AccessToken("some-access-token", "doo", 123, "scope", RefreshToken("bob")), IdToken("id"))
        val api = { _: Request ->
            Response(OK)
                .with(responseForm of accessTokenDetails)
        }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(accessTokenDetails))
    }

    @Test
    fun `can get access token from json body for content-type without directive`() {
        val api = { _: Request ->
            Response(OK)
                .header("Content-Type", "application/json")
                .body("{\"access_token\": \"some-access-token\"}")
        }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }

    @Test
    fun `handle non-successful response`() {
        val api = HttpHandler { Response(BAD_REQUEST) }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), absent())
    }
}
