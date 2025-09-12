package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.api.validation.ValidKey
import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.exception.CacheException
import com.fsg.cacheservice.core.exception.NotFoundException
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
    @GetMapping("/{key}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getValue(
        @ValidKey @PathVariable key: String
    ): String {
        return cacheRepository.get(key) ?: throw NotFoundException("Key $key not found")
    }

    @PutMapping("/{key}", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun setValue(
        @ValidKey @PathVariable key: String,
        @RequestParam(required = false) ttl: Int?,
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
        @RequestBody dto: RankingMemberDto
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
        @PathVariable member: String
    ): String {
        return cacheRepository.getRankedElementPosition(key, member)?.toString()
            ?: throw NotFoundException("Key $key not found")
    }

    @GetMapping("/{key}/ranking/range", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRankingRange(
        @ValidKey @PathVariable key: String,
        @RequestParam start: Int,
        @RequestParam stop: Int
    ): List<String> {
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
