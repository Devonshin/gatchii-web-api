package com.gatchii.common.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 16/09/2024
 */

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = OffsetDateTime::class)
class OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {

  private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  override fun serialize(encoder: Encoder, value: OffsetDateTime) {
    encoder.encodeString(formatter.format(value) ?: value.toString())
  }

  override fun deserialize(decoder: Decoder): OffsetDateTime {
    return OffsetDateTime.parse(decoder.decodeString(), formatter)
  }

}