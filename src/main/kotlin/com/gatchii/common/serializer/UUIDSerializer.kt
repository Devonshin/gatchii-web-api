package com.gatchii.common.serializer

import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 16/09/2024
 */

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = UUID::class)
class UUIDSerializer: KSerializer<UUID> {

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UuidCreator.fromString(decoder.decodeString())
    }

}