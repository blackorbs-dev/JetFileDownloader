package blackorbs.dev.jetfiledownloader.ui.download


//    val folderText = buildAnnotatedString {
//        appendInlineContent(download.filePath)
//        append(download.filePath)
//    }
//
//    val folderIcon = mapOf(
//        download.filePath to InlineTextContent(
//            Placeholder(width = 1.3.em, height = 1.3.em,
//                placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
//            )
//        ){
//            Icon(painterResource(R.drawable.ic_folder_copy_24),
//                contentDescription = download.filePath)
//        }
//    )


//@Composable
//internal fun DownloadItem(
//    modifier: Modifier = Modifier,
//    download: Download, onClick: (Download) -> Unit)
//{
//    ElevatedCard(
//        modifier = modifier
//            .pointerInput(Unit) {
//                detectTapGestures(
//                    onLongPress = {
//                        download.isSelected.value = true
//                    },
//                    onTap = {
//                        onClick(download.apply {
//                            actionType = ActionType.None
//                        })
//                    }
//                )
//            }.padding(5.dp)
//    ) {
//        ConstraintLayout(Modifier.fillMaxWidth()) {
//
//            val (time, type, title, status, error, pauseResumeBtn,
//                selectBack) = createRefs()
//
//            Text(download.dateTime.toLocalTime().format(
//                DateTimeFormatter.ofPattern("h:mm a")
//            ).uppercase(),
//                Modifier
//                    .background(
//                        color = MaterialTheme.colorScheme.primary,
//                        shape = RoundedCornerShape(bottomEnd = 8.dp)
//                    )
//                    .padding(start = 4.dp, end = 4.dp)
//                    .constrainAs(time) {
//                        top.linkTo(parent.top)
//                        start.linkTo(parent.start)
//                    },
//                color = MaterialTheme.colorScheme.background,
//                fontSize = 12.sp
//            )
//            Text(download.type.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.background, maxLines = 1, modifier =
//                Modifier
//                    .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
//                    .drawFileTypeBack(typeColor(download.type))
//                    .constrainAs(type) {
//                        top.linkTo(time.bottom)
//                        start.linkTo(parent.start)
//                    }
//            )
//            Text(download.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis,
//                modifier = Modifier
//                    .constrainAs(title) {
//                        top.linkTo(parent.top)
//                        start.linkTo(type.end)
//                        if (download.isNotCompleted)
//                            end.linkTo(pauseResumeBtn.start)
//                        else
//                            end.linkTo(parent.end, margin = 10.dp)
//                        width = Dimension.fillToConstraints
//                    }
//                    .padding(top = 20.dp)
//            )
//            val percent = animateIntAsState(download.sizePercent.intValue, label = "percent")
//            Text(
//                buildAnnotatedString {
//                    withStyle(SpanStyle(color = statusColor(download.status.value))){
//                        append(stringResource(download.status.value.titleResID))
//                    }
//                    append(
//                        " (${
//                            if(download.isNotCompleted) "${
//                                percent.value}% — "
//                            else ""
//                        }${download.totalSize.formatAsFileSize()})"
//                    )
//                },
//                fontSize = 14.sp,
//                modifier = Modifier.constrainAs(status){
//                    top.linkTo(title.bottom)
//                    start.linkTo(title.start)
//                }
//            )
//            if(download.status.value == Status.Error){
//                Text(
//                    download.status.value.text, fontSize = 13.sp,
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier.constrainAs(error){
//                        top.linkTo(status.bottom)
//                        start.linkTo(title.start)
//                    }
//                )
//            }
//            if(download.isNotCompleted){
//                FilledIconButton(onClick = {
//                    onClick(download.apply{
//                        actionType = if(download.isPending) ActionType.Pause
//                        else ActionType.Resume
//                    })
//                },
//                    Modifier
//                        .padding(top = 4.dp, end = 4.dp)
//                        .constrainAs(pauseResumeBtn) {
//                            end.linkTo(parent.end)
//                            top.linkTo(parent.top)
//                        }
//                ) {
//                    Icon(painterResource(
//                        if(download.isPending) R.drawable.ic_pause_circle_24
//                        else R.drawable.ic_play_circle_24
//                    ),
//                        modifier = Modifier.sizeIn(minWidth = 35.dp, minHeight = 35.dp),
//                        contentDescription = stringResource(download.status.value.titleResID)
//                    )
//                }
//            }
//            androidx.compose.animation.AnimatedVisibility(
//                download.isSelected.value,
//                modifier =
//                Modifier
//                    .constrainAs(selectBack) {
//                        linkTo(
//                            start = parent.start, end = parent.end,
//                            bottom = parent.bottom, top = parent.top
//                        )
//                        width = Dimension.fillToConstraints
//                        height = Dimension.fillToConstraints
//                    }
//                    .background(color = MaterialTheme.colorScheme.surfaceDim)
//            ){
//                val color = MaterialTheme.colorScheme.primary
//                ConstraintLayout(Modifier.drawBehind {
//                    val h = size.height; val w = size.width
//                    val s = Size(0.12f*w, h*0.49f)
//                    drawRect(color = color,
//                        size = Size(0.12f*w, h)
//                    )
//                    drawRect(color = color,
//                        size = s, topLeft = Offset(w-s.width, 0f)
//                    )
//                    drawRect(color = color,
//                        size = s, topLeft = Offset(w-s.width, h-s.height)
//                    )
//                }) {
//                    val (delete, share, info) = createRefs()
//
////                    Spacer(
////                        Modifier
////                            .constrainAs(delBack) {
////                                linkTo(top = parent.top, bottom = parent.bottom)
////                                height = Dimension.fillToConstraints
////                                width = Dimension.percent(0.125f)
////                            }
////                            .background(color = MaterialTheme.colorScheme.primary)
////                    )
////                    Spacer(
////                        Modifier
////                            .constrainAs(shareBack) {
////                                top.linkTo(parent.top)
////                                end.linkTo(parent.end)
////                                height = Dimension.percent(0.495f)
////                                width = Dimension.percent(0.125f)
////                            }
////                            .background(color = MaterialTheme.colorScheme.primary)
////                    )
////                    Spacer(
////                        Modifier
////                            .constrainAs(infoBack) {
////                                bottom.linkTo(parent.bottom)
////                                end.linkTo(parent.end)
////                                height = Dimension.percent(0.495f)
////                                width = Dimension.percent(0.125f)
////                            }
////                            .background(color = MaterialTheme.colorScheme.primary)
////                    )
//                    ActionIconButton(
//                        iconResId = R.drawable.ic_delete_forever_24,
//                        descResId = R.string.delete,
//                        modifier = Modifier.constrainAs(delete){
//                            linkTo(
//                                top = parent.top, bottom = parent.bottom
//                            )
//                        }
//                    ) { onClick(download.apply{actionType = ActionType.Delete}) }
//                    ActionIconButton(
//                        iconResId = R.drawable.ic_ios_share_24,
//                        descResId = R.string.share,
//                        modifier = Modifier.constrainAs(share){
//                            top.linkTo(parent.top)
//                            end.linkTo(parent.end)
//                        }
//                    ) {}
//                    ActionIconButton(
//                        iconResId = R.drawable.ic_info_outline_24,
//                        descResId = R.string.info,
//                        modifier = Modifier.constrainAs(info){
//                            bottom.linkTo(parent.bottom)
//                            end.linkTo(parent.end)
//                        }
//                    ) {}
//                }
//                val c = MaterialTheme.colorScheme.primary
//                Icon(
//                    painterResource(R.drawable.ic_check_24),
//                    contentDescription = stringResource(R.string.selected),
//                    modifier = Modifier
//                        .wrapContentSize()
//                        .size(50.dp)
//                        .drawBehind { drawCircle(color = c) }
//                        .padding(5.dp),
//                    tint = MaterialTheme.colorScheme.onPrimary
//                )
//            }
//
//        }
//    }
//}