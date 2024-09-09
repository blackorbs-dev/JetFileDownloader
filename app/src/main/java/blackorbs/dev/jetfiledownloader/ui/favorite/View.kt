package blackorbs.dev.jetfiledownloader.ui.favorite

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import blackorbs.dev.jetfiledownloader.ui.Page

fun NavGraphBuilder.favoritePage(){
    composable(Page.Favorite.name) { FavoritePage() }
}

@Composable
internal fun FavoritePage(){

}