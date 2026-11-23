package com.example.vista.data.repository

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Safe, actionable errors; never expose request URLs, credentials or response bodies to the UI. */
class CatalogLoadException private constructor(message: String, cause: Throwable) : IOException(message, cause) {
    companion object {
        fun from(cause: Exception): CatalogLoadException {
            if (cause is CatalogLoadException) return cause
            val message = when (cause) {
                is HttpException -> when (cause.code()) {
                    401, 403 -> "The catalog service rejected this app's credentials or access. Please check for an app update."
                    429 -> "The catalog service is busy. Please wait a moment and retry."
                    in 500..599 -> "The catalog service is temporarily unavailable. Please try again."
                    else -> "The catalog request failed. Please try again."
                }
                is UnknownHostException -> "The catalog server could not be reached. Check your connection or DNS settings and retry."
                is SocketTimeoutException -> "The catalog request timed out. Please check your connection and retry."
                is IOException -> "The catalog service could not be reached. Check your connection and retry."
                else -> "The catalog response could not be read. Please check for an app update and retry."
            }
            return CatalogLoadException(message, cause)
        }
    }
}
