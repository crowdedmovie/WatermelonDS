package me.magnum.melonds.github

import me.magnum.melonds.domain.model.Version
import me.magnum.melonds.github.dtos.AssetDto
import me.magnum.melonds.github.dtos.ReleaseDto
import me.magnum.melonds.github.services.UpdateApkMetadata
import me.magnum.melonds.github.services.UpdateApkRejectionReason
import me.magnum.melonds.github.services.UpdateApkValidationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubUpdatePolicyTest {
    @Test
    fun selectNightlyMatchesForkNightlyAssetAndRejectsUpstreamUrl() {
        val upstreamRelease = ReleaseDto(
            tagName = "0.8.0.rc4",
            name = "Upstream 0.8.0.rc4",
            publishedAt = "2026-11-26T12:00:00Z",
            htmlUrl = "https://github.com/SapphireRhodonite/melonDS-android/releases/tag/0.8.0.rc4",
            assets = listOf(
                AssetDto(
                    id = 10L,
                    url = "https://github.com/SapphireRhodonite/melonDS-android/releases/download/0.8.0.rc4/WatermelonDS-0.8.0.rc4-nightly.apk",
                    name = "WatermelonDS-0.8.0.rc4-nightly.apk",
                    size = 28_000_000L,
                    contentType = APK_CONTENT_TYPE,
                ),
            ),
        )
        val forkRelease = ReleaseDto(
            tagName = "v0.8.0-rc4-fork1",
            name = "WatermelonDS Fork v0.8.0-rc4-fork1",
            publishedAt = "2026-11-26T11:00:00Z",
            htmlUrl = "https://github.com/crowdedmovie/WatermelonDS/releases/tag/v0.8.0-rc4-fork1",
            assets = listOf(
                AssetDto(
                    id = 20L,
                    url = "https://github.com/crowdedmovie/WatermelonDS/releases/download/v0.8.0-rc4-fork1/WatermelonDS-Fork-v0.8.0-rc4-fork1-nightly.apk",
                    name = "WatermelonDS-Fork-v0.8.0-rc4-fork1-nightly.apk",
                    size = 28_115_000L,
                    contentType = APK_CONTENT_TYPE,
                ),
            ),
        )

        val candidate = GitHubReleaseSelector.selectNightly(listOf(upstreamRelease, forkRelease))
        assertNotNull(candidate)
        assertEquals("v0.8.0-rc4-fork1", candidate!!.release.tagName)
        assertEquals("WatermelonDS-Fork-v0.8.0-rc4-fork1-nightly.apk", candidate.asset.name)
    }

    @Test
    fun updateValidationAllowsMatchingNightlyPackageAndRejectsCrossPackageOrSignatureMismatch() {
        val installed = UpdateApkMetadata(
            packageName = "me.magnum.melondualds.nightly",
            versionCode = 43L,
            versionName = "v0.8.0-rc4-fork1",
            signerDigests = setOf("abc123"),
        )
        val validCandidate = UpdateApkMetadata(
            packageName = "me.magnum.melondualds.nightly",
            versionCode = 44L,
            versionName = "v0.8.0-rc4-fork2",
            signerDigests = setOf("abc123"),
        )
        val wrongPackageCandidate = validCandidate.copy(packageName = "me.magnum.melondualds")
        val wrongSignatureCandidate = validCandidate.copy(signerDigests = setOf("def456"))

        assertNull(
            UpdateApkValidationPolicy.validate(
                candidate = validCandidate,
                installed = installed,
                expectedVersion = Version.Nightly,
                requireSemanticVersionMatch = false,
                expectedSize = 28_000_000L,
                actualSize = 28_000_000L,
            ),
        )
        assertEquals(
            UpdateApkRejectionReason.PACKAGE_MISMATCH,
            UpdateApkValidationPolicy.validate(
                candidate = wrongPackageCandidate,
                installed = installed,
                expectedVersion = Version.Nightly,
                requireSemanticVersionMatch = false,
                expectedSize = 28_000_000L,
                actualSize = 28_000_000L,
            ),
        )
        assertEquals(
            UpdateApkRejectionReason.SIGNATURE_MISMATCH,
            UpdateApkValidationPolicy.validate(
                candidate = wrongSignatureCandidate,
                installed = installed,
                expectedVersion = Version.Nightly,
                requireSemanticVersionMatch = false,
                expectedSize = 28_000_000L,
                actualSize = 28_000_000L,
            ),
        )
    }
}
