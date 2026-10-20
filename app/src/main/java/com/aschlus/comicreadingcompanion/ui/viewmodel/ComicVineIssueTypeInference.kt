package com.aschlus.comicreadingcompanion.ui.viewmodel

import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit

object ComicVineIssueTypeInference {

    private val giantSizePattern =
        Regex("""\bgiant[\s-]*size\b""", RegexOption.IGNORE_CASE)

    private val annualPattern =
        Regex("""\bannual\b""", RegexOption.IGNORE_CASE)

    private val oneShotPattern =
        Regex("""\bone[\s-]*shot\b""", RegexOption.IGNORE_CASE)

    private val previewPattern =
        Regex("""\bpreview\b""", RegexOption.IGNORE_CASE)

    private val specialPattern =
        Regex("""\bspecial\b""", RegexOption.IGNORE_CASE)

    fun infer(issue: ComicVineIssueHit): String {
        val label = listOfNotNull(
            issue.issueNumber,
            issue.name
        ).joinToString(" ") { it.trim() }

        return when {
            giantSizePattern.containsMatchIn(label) ->
                "GIANT_SIZE"

            annualPattern.containsMatchIn(label) ->
                "ANNUAL"

            oneShotPattern.containsMatchIn(label) ->
                "ONE_SHOT"

            previewPattern.containsMatchIn(label) ->
                "PREVIEW"

            specialPattern.containsMatchIn(label) ->
                "SPECIAL"

            else -> "REGULAR"
        }
    }
}