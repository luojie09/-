package com.secretbase.app.data.supabase

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val systemZoneId: ZoneId = ZoneId.systemDefault()

fun millisToIsoInstant(value: Long): String = Instant.ofEpochMilli(value).toString()

fun isoInstantToMillis(value: String): Long = Instant.parse(value).toEpochMilli()

fun millisToIsoDate(value: Long): String =
    Instant.ofEpochMilli(value)
        .atZone(systemZoneId)
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)

fun millisToNullableIsoDate(value: Long?): String? = value?.let(::millisToIsoDate)

fun isoDateToMillis(value: String): Long =
    LocalDate.parse(value)
        .atStartOfDay(systemZoneId)
        .toInstant()
        .toEpochMilli()

fun nullableIsoDateToMillis(value: String?): Long? = value?.let(::isoDateToMillis)
