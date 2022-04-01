package io.dokar.expandabletext.sample

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.dokar.expandabletext.ExpandableText
import io.dokar.expandabletext.sample.ui.theme.ExpandableTextTheme

class MainActivity : ComponentActivity() {
    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val backgroundColor = MaterialTheme.colors.background
            LaunchedEffect(backgroundColor) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val flags = window.decorView.systemUiVisibility
                    window.decorView.systemUiVisibility = flags or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    window.statusBarColor = backgroundColor.toArgb()
                }
            }
            ExpandableTextTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Example()
                }
            }
        }
    }
}

@Composable
fun Example() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        Text("\uD83D\uDCDC ExpandableText", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        val text = "Very short text"
        Header("Fixed (maxLines = ${text.length})")
        ExpandableText(expanded = false, text = text, maxLines = text.length)

        Spacer(modifier = Modifier.height(16.dp))

        Header("Expandable")
        ShowMoreText()

        Spacer(modifier = Modifier.height(16.dp))

        Header("Expandable")
        ShowMoreText(
            toggleText = { expanded ->
                Text(
                    text = if (expanded) "Show less" else "Show more",
                    color = MaterialTheme.colors.primary,
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Header("Expandable")
        ShowMoreText(
            toggleText = { expanded ->
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                )
            }
        )
    }
}

@Composable
fun Header(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colors.primary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun ShowMoreText(
    modifier: Modifier = Modifier,
    toggleText: @Composable ((expanded: Boolean) -> Unit)? = null,
) {
    val text = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque ullamcorper, 
        ex eu efficitur tincidunt, quam felis ultricies ex, quis consectetur nisl mauris 
        dignissim nisi. Praesent purus turpis, venenatis vitae ornare ac, suscipit quis lorem. 
        Nulla vitae tellus venenatis, pulvinar ipsum nec, tristique nibh. Cras vestibulum faucibus 
        sem, ac imperdiet odio faucibus eu. Donec eget ex ac neque pharetra pellentesque et id 
        massa. Etiam rhoncus tortor sed magna pharetra gravida. Sed aliquam ipsum ac purus blandit 
        hendrerit. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia 
        curae; Duis quis consequat ipsum, id malesuada orci. Praesent ut leo eu ex posuere 
        convallis. Aliquam et dolor nec risus mattis laoreet. Sed volutpat erat ut nibh vulputate, 
        a vulputate augue vestibulum. Integer efficitur, lectus eget bibendum congue, erat orci 
        imperdiet erat, at convallis metus ipsum quis nibh. Sed aliquet neque ullamcorper massa 
        molestie tempus. Suspendisse potenti. Sed gravida rutrum arcu.
    """.trimIndent()
    var expanded by remember { mutableStateOf(false) }
    ExpandableText(
        expanded = expanded,
        text = text,
        modifier = modifier
            .animateContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded }
            ),
        toggleContent = {
            if (toggleText != null) {
                toggleText(expanded)
            }
        },
        maxLines = 4,
    )
}
