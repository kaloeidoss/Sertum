package com.sertum.player.data.covers

enum class CoverOrigin { USER, EMBEDDED, FOLDER, PLACEHOLDER }

data class CoverChoice(val reference: String?, val origin: CoverOrigin)

/**
 * PRD 7.7.6 priority chain:
 * user-added cover > embedded art > folder image > placeholder.
 */
object CoverResolver {

    const val PLACEHOLDER_REF = "__placeholder__"

    fun resolve(user: String?, embedded: String?, folder: String?): CoverChoice = when {
        !user.isNullOrBlank() -> CoverChoice(user, CoverOrigin.USER)
        !embedded.isNullOrBlank() -> CoverChoice(embedded, CoverOrigin.EMBEDDED)
        !folder.isNullOrBlank() -> CoverChoice(folder, CoverOrigin.FOLDER)
        else -> CoverChoice(PLACEHOLDER_REF, CoverOrigin.PLACEHOLDER)
    }

    /** After the user removes their cover, the chain resumes below it. */
    fun resolveAfterUserRemoval(embedded: String?, folder: String?): CoverChoice =
        resolve(user = null, embedded = embedded, folder = folder)
}
