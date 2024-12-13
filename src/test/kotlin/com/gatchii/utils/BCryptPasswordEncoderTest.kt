package com.gatchii.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import shared.common.UnitTest
import kotlin.test.assertTrue

/**
 * Package: com.gatchii.utils
 * Created: Devonshin
 * Date: 14/11/2024
 */

@UnitTest
class BCryptPasswordEncoderTest {

    private val bcncoder = BCryptPasswordEncoder()
    private val password = "<PASSWORD>"

    @Test
    fun `encode test`() {
        //given
        //when
        val encode = bcncoder.encode(password)
        //then
        println("encode: $encode")
        assertTrue(encode.isNotEmpty())
        assertTrue(encode != password)

    }

    @Test
    fun `matches test`() {
        //given
        val encode = bcncoder.encode(password)
        //when
        //then
        val matches = bcncoder.matches(password, encode)
        assertThat(matches).isTrue()
    }

    @Test
    fun `not matches test`() {
        //given
        val encode = bcncoder.encode(password)
        //when
        //then
        val matches = bcncoder.matches("_$password", encode)
        assertThat(matches).isFalse()
    }

}