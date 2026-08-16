package com.aliemre.evenvanteri

import android.app.Application
import android.content.Context
import com.aliemre.evenvanteri.data.InventoryRepository
import com.aliemre.evenvanteri.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Elle kurulan bağımlılık kabı.
 *
 * Hilt yerine bu tercih edildi: uygulama tek modüllü ve bağımlılık sayısı bir
 * avuç. Hilt'in getireceği ek KSP adımı, bulutta derlendiği için zaten yavaş
 * olan turu daha da uzatırdı.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.build(context)

    val inventoryRepository = InventoryRepository(
        locationDao = database.locationDao(),
        itemDao = database.itemDao(),
    )
}

class EvEnvanteriApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.inventoryRepository.seedDefaultLocationsIfEmpty()
        }
    }
}
