package com.sertum.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VersionTest {

    @Test
    fun `sertum version constant is the M0 baseline`() {
        assertThat(SERTUM_VERSION).isEqualTo("0.1.0")
    }
}
