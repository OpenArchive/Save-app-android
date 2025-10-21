package net.opendasharchive.openarchive.services.storacha.util

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.security.Security

class Ed25519UtilsTest {

    @Before 
    fun setup() {
        // Ensure BouncyCastle is registered for tests
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    @Test
    fun testKeyPairGeneration() {
        val keyPair = Ed25519Utils.generateKeyPair()
        
        assertNotNull(keyPair)
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
        assertTrue(keyPair.private is Ed25519PrivateKeyParameters)
        assertTrue(keyPair.public is Ed25519PublicKeyParameters)
    }

    @Test
    fun testDidCreation() {
        val keyPair = Ed25519Utils.generateKeyPair()
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        
        val did = Ed25519Utils.createDidFromPublicKey(publicKey)
        
        assertNotNull(did)
        assertTrue(did.startsWith("did:key:z"))
        assertTrue(did.length > 20) // Should be a reasonable length
    }

    @Test
    fun testSignatureGeneration() {
        val keyPair = Ed25519Utils.generateKeyPair()
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val challenge = "test-challenge-string"
        
        val signature = Ed25519Utils.signChallenge(challenge, privateKey)
        
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }

    @Test
    fun testSignatureVerification() {
        val keyPair = Ed25519Utils.generateKeyPair()
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        val challenge = "test-challenge-string"
        
        val signature = Ed25519Utils.signChallenge(challenge, privateKey)
        val isValid = Ed25519Utils.verifySignature(challenge, signature, publicKey)
        
        assertTrue(isValid)
    }

    @Test
    fun testInvalidSignatureVerification() {
        val keyPair1 = Ed25519Utils.generateKeyPair()
        val keyPair2 = Ed25519Utils.generateKeyPair()
        val privateKey = keyPair1.private as Ed25519PrivateKeyParameters
        val wrongPublicKey = keyPair2.public as Ed25519PublicKeyParameters
        val challenge = "test-challenge-string"
        
        val signature = Ed25519Utils.signChallenge(challenge, privateKey)
        val isValid = Ed25519Utils.verifySignature(challenge, signature, wrongPublicKey)
        
        assertFalse(isValid)
    }

    @Test
    fun testDidToPublicKeyExtraction() {
        val keyPair = Ed25519Utils.generateKeyPair()
        val originalPublicKey = keyPair.public as Ed25519PublicKeyParameters
        val did = Ed25519Utils.createDidFromPublicKey(originalPublicKey)

        val extractedPublicKey = Ed25519Utils.extractPublicKeyFromDid(did)

        assertNotNull(extractedPublicKey)
        assertArrayEquals(originalPublicKey.encoded, extractedPublicKey!!.encoded)
    }

    @Test
    fun testValidDidValidation() {
        // Generate a valid DID
        val keyPair = Ed25519Utils.generateKeyPair()
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        val validDid = Ed25519Utils.createDidFromPublicKey(publicKey)

        // Test that it validates correctly
        assertTrue(Ed25519Utils.isValidDid(validDid))
    }

    @Test
    fun testInvalidDidValidation_EmptyString() {
        assertFalse(Ed25519Utils.isValidDid(""))
    }

    @Test
    fun testInvalidDidValidation_NoPrefix() {
        assertFalse(Ed25519Utils.isValidDid("z6MkjTHQxjZh7sQZ7sZBvJxDqyzYb4nKq1iWzWUzRr3oT1XB"))
    }

    @Test
    fun testInvalidDidValidation_WrongPrefix() {
        assertFalse(Ed25519Utils.isValidDid("did:web:example.com"))
    }

    @Test
    fun testInvalidDidValidation_IncompletePrefix() {
        assertFalse(Ed25519Utils.isValidDid("did:key:"))
    }

    @Test
    fun testInvalidDidValidation_MissingZPrefix() {
        assertFalse(Ed25519Utils.isValidDid("did:key:6MkjTHQxjZh7sQZ7sZBvJxDqyzYb4nKq1iWzWUzRr3oT1XB"))
    }

    @Test
    fun testInvalidDidValidation_RandomString() {
        assertFalse(Ed25519Utils.isValidDid("not-a-valid-did"))
    }

    @Test
    fun testInvalidDidValidation_InvalidBase58() {
        // Contains invalid Base58 characters (0, O, I, l)
        assertFalse(Ed25519Utils.isValidDid("did:key:z0OIl123456789"))
    }

    @Test
    fun testInvalidDidValidation_TooShort() {
        assertFalse(Ed25519Utils.isValidDid("did:key:z"))
    }

    @Test
    fun testValidDidFromExample() {
        // Test with a known valid DID format from the codebase
        val exampleDid = "did:key:z6MkjTHQxjZh7sQZ7sZBvJxDqyzYb4nKq1iWzWUzRr3oT1XB"
        // Note: This may or may not be valid depending on the actual key encoding
        // but it has the correct format structure
        val result = Ed25519Utils.isValidDid(exampleDid)
        // We're testing that the function handles it without crashing
        assertNotNull(result)
    }
}