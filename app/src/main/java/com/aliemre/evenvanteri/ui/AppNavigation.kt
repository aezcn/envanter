package com.aliemre.evenvanteri.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aliemre.evenvanteri.data.InventoryRepository
import com.aliemre.evenvanteri.ui.home.HomeScreen
import com.aliemre.evenvanteri.ui.home.HomeViewModel
import com.aliemre.evenvanteri.ui.item.ItemEditScreen
import com.aliemre.evenvanteri.ui.item.ItemEditViewModel
import com.aliemre.evenvanteri.ui.location.LocationScreen
import com.aliemre.evenvanteri.ui.location.LocationViewModel

private object Routes {
    const val HOME = "home"
    const val LOCATION = "location/{locationId}"
    /**
     * Hem ekleme hem düzenleme aynı rota. itemId boşsa yeni ürün, doluysa
     * mevcut ürün; locationId ise "bu dolaptan geldim" bilgisini taşır ve
     * yeni üründe konumu hazır seçer.
     */
    const val ITEM_EDIT = "item?itemId={itemId}&locationId={locationId}"

    fun location(id: String) = "location/$id"
    fun itemEdit(itemId: String? = null, locationId: String? = null) =
        "item?itemId=${itemId.orEmpty()}&locationId=${locationId.orEmpty()}"
}

@Composable
fun AppNavigation(
    repository: InventoryRepository,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = factory { HomeViewModel(repository) },
            )
            HomeScreen(
                viewModel = viewModel,
                onOpenLocation = { navController.navigate(Routes.location(it)) },
                onAddItem = { navController.navigate(Routes.itemEdit()) },
                onEditItem = { navController.navigate(Routes.itemEdit(itemId = it)) },
            )
        }

        composable(
            route = Routes.LOCATION,
            arguments = listOf(navArgument("locationId") { type = NavType.StringType }),
        ) { entry ->
            val locationId = entry.arguments?.getString("locationId").orEmpty()
            val viewModel: LocationViewModel = viewModel(
                factory = factory { LocationViewModel(repository, locationId) },
            )
            LocationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddItem = {
                    navController.navigate(Routes.itemEdit(locationId = locationId))
                },
                onEditItem = { navController.navigate(Routes.itemEdit(itemId = it)) },
            )
        }

        composable(
            route = Routes.ITEM_EDIT,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType; defaultValue = "" },
                navArgument("locationId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val itemId = entry.arguments?.getString("itemId")?.takeIf { it.isNotBlank() }
            val locationId = entry.arguments?.getString("locationId")?.takeIf { it.isNotBlank() }
            val viewModel: ItemEditViewModel = viewModel(
                factory = factory { ItemEditViewModel(repository, itemId, locationId) },
            )
            ItemEditScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Elle DI kullandığımız için ViewModel'ler constructor parametresi alıyor;
 * bu küçük fabrika onları Compose'un viewModel() çağrısına bağlar.
 */
private inline fun <reified T : ViewModel> factory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
