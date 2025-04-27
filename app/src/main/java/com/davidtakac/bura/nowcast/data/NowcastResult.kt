package com.davidtakac.bura.nowcast.data

import com.davidtakac.bura.nowcast.data.remote.NowcastResponse

/**
 * Sealed class representing the result of fetching nowcast data.
 * Can be either a successful result containing [NowcastResponse] or an error.
 */
sealed class NowcastResult {
    /**
     * Represents a successful result with the fetched nowcast data.
     * @param data The [NowcastResponse] containing the nowcast information.
     */
    data class Success(val data: NowcastResponse) : NowcastResult()

    /**
     * Represents an error that occurred while fetching nowcast data.
     * @param message A descriptive message about the error.
     */
    data class Error(val message: String) : NowcastResult()
}
