package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.api.validation.ValidKey
import com.fsg.cacheservice.api.validation.ValidRankingMember
import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.NotFoundException
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@Validated
class CacheController(
    private val cacheRepository: CacheRepository
) {
    companion object {
        private const val MAX_RANGE_PAGE_SIZE = 100
    }

    @GetMapping("/{key}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getValue(
        @ValidKey @PathVariable key: String
    ): String {
        return cacheRepository.get(key) ?: throw NotFoundException("Key $key not found")
    }

    @PutMapping("/{key}", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun setValue(
        @ValidKey @PathVariable key: String,
        @RequestParam(required = false) @Min(value = 1, message = "Time-to-live must be greater than zero") ttl: Int?,
        @RequestBody value: String
    ): String {
        if (ttl != null) {
            cacheRepository.set(key, value, Duration.ofSeconds(ttl.toLong()))
        } else {
            cacheRepository.set(key, value)
        }

        return "OK"
    }

    @DeleteMapping("/{key}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun deleteKey(
        @ValidKey @PathVariable key: String
    ): String {
        if (cacheRepository.delete(key)) {
            return "OK"
        }

        throw NotFoundException("Key $key not found")
    }

    @PutMapping("/{key}/increment", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun incrementCounter(
        @ValidKey @PathVariable key: String
    ): String = cacheRepository.increment(key).toString()

    @PostMapping(
        "/{key}/ranking",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun setRankingMember(
        @ValidKey @PathVariable key: String,
        @Valid @RequestBody dto: RankingMemberDto
    ): ResponseEntity<String> {
        val httpStatus = when (cacheRepository.setRankedElement(key, dto.score, dto.member)) {
            true -> HttpStatus.CREATED
            false -> HttpStatus.OK
        }

        return ResponseEntity.status(httpStatus.value()).body("OK")
    }

    @GetMapping("/{key}/ranking/{member}/rank", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getMemberRank(
        @ValidKey @PathVariable key: String,
        @ValidRankingMember @PathVariable member: String
    ): String {
        // HINT: Be careful. This method could fail in a high concurrency environment.
        return cacheRepository.getRankedElementPosition(key, member)?.toString()
            ?: if (cacheRepository.getRankedElementCount(key) == 0L) {
                throw NotFoundException("Key $key not found")
            } else {
                throw NotFoundException("Member $member not found in ranking")
            }
    }

    @GetMapping("/{key}/ranking/range", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRankingRange(
        @ValidKey @PathVariable key: String,
        @RequestParam @NotNull(message = "start cannot be null") start: Int?,
        @RequestParam @NotNull(message = "stop cannot be null") stop: Int?
    ): List<String> {
        if (stop!! - start!! >= MAX_RANGE_PAGE_SIZE) {
            throw BadRequestException("Range too largo. Maximum $MAX_RANGE_PAGE_SIZE elements allowed")
        }

        return cacheRepository.getRankedElementRange(key, start.toLong(), stop.toLong())
    }

    @GetMapping("/{key}/ranking/count", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getRankingCount(
        @ValidKey @PathVariable key: String
    ): String = cacheRepository.getRankedElementCount(key).toString()

    @GetMapping("/keys/count", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getKeyCount(): String = cacheRepository.getCacheKeyCount().toString()
}
