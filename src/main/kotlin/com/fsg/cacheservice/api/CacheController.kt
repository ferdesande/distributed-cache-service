package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.api.validation.ValidKey
import com.fsg.cacheservice.api.validation.ValidRankingMember
import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.CacheException
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
    ): String {
        return cacheRepository.increment(key)?.toString() ?: throw CacheException("Unexpected error")
    }

    @PostMapping(
        "/{key}/ranking",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun setRankingMember(
        @ValidKey @PathVariable key: String,
        @Valid @RequestBody dto: RankingMemberDto
    ): ResponseEntity<String> {
        return when (cacheRepository.setRankedElement(key, dto.score, dto.member)) {
            null -> throw CacheException("Unexpected error")
            true -> ResponseEntity.status(HttpStatus.CREATED.value()).body("OK")
            false -> ResponseEntity.ok("OK")
        }
    }

    @GetMapping("/{key}/ranking/{member}/rank", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getMemberRank(
        @ValidKey @PathVariable key: String,
        @ValidRankingMember @PathVariable member: String
    ): String {
        return cacheRepository.getRankedElementPosition(key, member)?.toString()
            ?: if ((cacheRepository.getRankedElementCount(key) ?: 0L) == 0L) {
                throw NotFoundException("Key $key not found")
            } else {
                throw NotFoundException("Member $member not found in ranking")
            }
    }

    @GetMapping("/{key}/ranking/range", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRankingRange(
        @ValidKey @PathVariable key: String,
        @RequestParam
        @NotNull(message = "start cannot be null")
        @Min(value = 0, message = "start must be greater than or equal to 0")
        start: Int?,
        @RequestParam
        @NotNull(message = "stop cannot be null")
        @Min(value = 0, message = "stop must be greater than or equal to 0")
        stop: Int?
    ): List<String> {
        if (start!! > stop!!) {
            throw BadRequestException("Start must be smaller than or equal to stop")
        }

        if (stop - start >= MAX_RANGE_PAGE_SIZE) {
            throw BadRequestException("Range too largo. Maximum $MAX_RANGE_PAGE_SIZE elements allowed")
        }

        return cacheRepository.getRankedElementRange(key, start.toLong(), stop.toLong())
    }

    @GetMapping("/{key}/ranking/count", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getRankingCount(
        @ValidKey @PathVariable key: String
    ): String {
        return cacheRepository.getRankedElementCount(key)?.toString() ?: throw CacheException("Unexpected error")
    }

    @GetMapping("/keys/count", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getKeyCount(): String {
        return cacheRepository.getCacheKeyCount().toString()
    }
}
