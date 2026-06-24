package com.secretbase.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Screenshot Home Normal 393x852")
@Composable
fun HomeScreenScreenshotNormal393() {
    HomeScreenPreviewContent(HomePreviewScenario.Normal)
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Screenshot Home Normal 360x800")
@Composable
fun HomeScreenScreenshotNormal360() {
    HomeScreenPreviewContent(HomePreviewScenario.Normal)
}

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Screenshot Home Empty 393x852")
@Composable
fun HomeScreenScreenshotEmpty393() {
    HomeScreenPreviewContent(HomePreviewScenario.Empty)
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Screenshot Home Empty 360x800")
@Composable
fun HomeScreenScreenshotEmpty360() {
    HomeScreenPreviewContent(HomePreviewScenario.Empty)
}
