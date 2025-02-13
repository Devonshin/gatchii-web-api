package com.gatchii.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Package: com.gatchii.config
 * Created: Devonshin
 * Date: 13/02/2025
 */

class GlobalConfig {
    val config: Config = ConfigFactory.load()

    companion object {

        private val instance = GlobalConfig()

        fun getInstance(): GlobalConfig = instance

        fun getConfigedValue(key: String): String = getInstance().config.getString(key)
    }
}