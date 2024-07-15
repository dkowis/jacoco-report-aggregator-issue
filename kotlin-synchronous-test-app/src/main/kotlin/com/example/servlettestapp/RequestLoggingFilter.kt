package com.example.servlettestapp

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.filter.AbstractRequestLoggingFilter

//TODO: I'm honestly not completely sure this works well with async
@Component
class RequestLoggingFilter() : AbstractRequestLoggingFilter() {
    private val log = KotlinLogging.logger { }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
        log.info { "BEFORE REQUEST" }
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        log.info { "AFTER REQUEST" }
    }

}