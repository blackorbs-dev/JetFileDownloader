package blackorbs.dev.jetfiledownloader.ui.favorite

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import blackorbs.dev.jetfiledownloader.R
import blackorbs.dev.jetfiledownloader.entities.Favorite
import blackorbs.dev.jetfiledownloader.ui.Page
import blackorbs.dev.jetfiledownloader.ui.download.OutlineEditBox
import blackorbs.dev.jetfiledownloader.ui.download.PopupDialog
import blackorbs.dev.jetfiledownloader.ui.download.RowActionButtons
import blackorbs.dev.jetfiledownloader.ui.theme.JetTheme

fun NavGraphBuilder.favoritePage(
    onShouldLoadPage: (String?) -> Unit
){
    composable<Page.Favorites> {
        FavoritePage(onShouldLoadPage)
    }
}

@Composable
internal fun FavoritePage(
    onShouldLoadPage: (String?) -> Unit,
    context: Context = LocalContext.current,
    favViewModel: FavViewModel =
        viewModel(context as ComponentActivity)
){
    val favoriteItems = favViewModel.favoriteItems.value
    var favoriteEdit by remember {
        mutableStateOf<Favorite?>(null)
    }

    LazyColumn(
        if(favoriteItems.isEmpty()) Modifier.fillMaxSize()
        else Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if(favoriteItems.isEmpty())
            Arrangement.Center else Arrangement.Top
    ) {
        items(items = favoriteItems, key = {it.id}){ favorite ->
            FavoriteItem(
                favorite = favorite,
                onShouldLoadPage = {
                    onShouldLoadPage(favorite.url)
                },
                onEditBtnClicked = {
                    favoriteEdit = favorite
                }
            )
        }

        if(favoriteItems.isEmpty()){
            item {
                Text(stringResource(
                    R.string.empty_list_info,
                    stringResource(R.string.favorite_websites)
                ), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    favoriteEdit?.let {
        PopupDialog(
            titleRes = R.string.edit_favorite_dialog_title,
            onDismiss = { favoriteEdit = null }
        ) {
            FavoriteEditLayout(
                favorite = it, onDelete = {
                    favViewModel.delete(it)
                    favoriteEdit = null
                },
                onSave = { favUpdate ->
                    favViewModel.update(favUpdate)
                    favoriteEdit = null
                }
            )
        }
    }

}

@Composable
internal fun FavoriteItem(
    favorite: Favorite?,
    onShouldLoadPage: () -> Unit,
    onEditBtnClicked: () -> Unit
){
    favorite?.let {
        Card(
            onClick = onShouldLoadPage,
            Modifier.padding(5.dp)
        ) {
            Row(
                Modifier.padding(
                    top = 10.dp, bottom = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val c = MaterialTheme.colorScheme.primary
                Text(
                    if(favorite.title.isEmpty())"J" else favorite.title[0].toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp)
                        .drawBehind {
                            drawCircle(
                                color = c,
                                radius = size.maxDimension*1.3.toFloat()/2
                            )
                        }
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        favorite.title, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        favorite.url, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = onEditBtnClicked) {
                    Icon(
                        painterResource(R.drawable.ic_edit_square_24),
                        contentDescription = favorite.title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

    }
}

@Composable
fun FavoriteEditLayout(
    favorite: Favorite,
    onDelete: () -> Unit,
    onSave: (Favorite) -> Unit
){
    val titleText = remember {
        mutableStateOf(favorite.title)
    }
    val urlText = remember {
        mutableStateOf(favorite.url)
    }

    OutlineEditBox(
        text = titleText,
        labelResId = R.string.title
    )

    OutlineEditBox(
        text = urlText, maxLines = 4,
        labelResId = R.string.url
    )

    RowActionButtons(
        Modifier.padding(
            start = 5.dp, end = 5.dp,
            top = 22.dp, bottom = 8.dp
        ),
        leftTitleRes = R.string.delete,
        rightTileRes = R.string.save,
        onLeftClicked = onDelete,
        onRightClicked = {
            onSave(favorite.apply {
                title = titleText.value
                url = urlText.value
            })
        }
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewLightDark @Composable
fun FavEditDialogPreview(){
    JetTheme {
        Scaffold {
            PopupDialog(
                R.string.edit_favorite_dialog_title,
                onDismiss = { /*TODO*/ }) {
                FavoriteEditLayout(
                    favorite = Favorite(
                        "Kefblog Tech Home",
                        "https://kefblog.com.ng/my-first-showtresfgsfrsg-wwegr-gteggs-tgtrgsd-dtg-tgtteg-tgtgtrdtttg-tggrgtttgtsd"
                    ),
                    onDelete = {}, onSave = {}
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewLightDark
@Composable
fun FavoriteItemPreview(){
    JetTheme {
        Scaffold {
            Column {
                FavoriteItem(
                    favorite = Favorite(
                        "Kefblog Tech Home",
                        "https://kefblog.com.ng/my-first_show"
                    ),
                    onShouldLoadPage = {}
                ){}

                FavoriteItem(
                    favorite = Favorite(
                        "Kefblog Tech Home",
                        "https://kefblog.com.ng/my-first_show"
                    ), onShouldLoadPage = {}
                ){}
            }
        }
    }
}