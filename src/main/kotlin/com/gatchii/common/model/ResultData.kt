package com.gatchii.common.model

import java.util.*

/**
 * Package: com.gatchii.shared.model
 * Created: Devonshin
 * Date: 27/01/2025
 */

data class ResultData<T : BaseModel<UUID>>(
  val datas: List<T>,
  val hasMoreData: Boolean,
) {
  fun lastId(): UUID? {
    return if (hasMoreData) datas[(datas.size - 1).coerceAtLeast(0)].id else null
  }
}
