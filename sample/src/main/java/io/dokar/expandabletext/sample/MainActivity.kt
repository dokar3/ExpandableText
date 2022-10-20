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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.LayoutDirection
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
        Header("Not expandable (maxLines = ${text.length})")
        ExpandableText(expanded = false, text = text, collapsedMaxLines = text.length)

        Spacer(modifier = Modifier.height(16.dp))

        Header("No toggle")
        ShowMoreText()

        Spacer(modifier = Modifier.height(16.dp))

        Header("Text() toggle")
        ShowMoreText(
            toggle = { expanded ->
                Text(
                    text = if (expanded) "Show less" else "Show more",
                    color = MaterialTheme.colors.primary,
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Header("Icon() toggle")
        ShowMoreText(
            toggle = { expanded ->
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

        Spacer(modifier = Modifier.height(16.dp))

        Header("Right-to-left")
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ShowMoreText(
                text = LoremIpsumArabic,
                toggle = { expanded ->
                    Text(
                        text = if (expanded) "Show less" else "Show more",
                        color = MaterialTheme.colors.primary,
                    )
                }
            )
        }
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
    text: String = LoremIpsum(words = 100).values.first(),
    toggle: @Composable ((expanded: Boolean) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExpandableText(
        expanded = expanded,
        text = text,
        collapsedMaxLines = 3,
        modifier = modifier
            .animateContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded }
            ),
        toggle = {
            if (toggle != null) {
                toggle(expanded)
            }
        },
    )
}

// Text copied from https://istizada.com/arabic-lorem-ipsum/
private const val LoremIpsumArabic =
    """لكن لا بد أن أوضح لك أن كل هذه الأفكار المغلوطة حول استنكار  النشوة وتمجيد الألم نشأت بالفعل، وسأعرض لك التفاصيل لتكتشف حقيقة وأساس تلك السعادة البشرية، فلا أحد يرفض أو يكره أو يتجنب الشعور بالسعادة، ولكن بفضل هؤلاء الأشخاص الذين لا يدركون بأن السعادة لا بد أن نستشعرها بصورة أكثر عقلانية ومنطقية فيعرضهم هذا لمواجهة الظروف الأليمة، وأكرر بأنه لا يوجد من يرغب في الحب ونيل المنال ويتلذذ بالآلام، الألم هو الألم ولكن نتيجة لظروف ما قد تكمن السعاده فيما نتحمله من كد وأسي.
و سأعرض مثال حي لهذا، من منا لم يتحمل جهد بدني شاق إلا من أجل الحصول على ميزة أو فائدة؟ ولكن من لديه الحق أن ينتقد شخص ما أراد أن يشعر بالسعادة التي لا تشوبها عواقب أليمة أو آخر أراد أن يتجنب الألم الذي ربما تنجم عنه بعض المتعة ؟ 
علي الجانب الآخر نشجب ونستنكر هؤلاء الرجال المفتونون بنشوة اللحظة الهائمون في رغباتهم فلا يدركون ما يعقبها من الألم والأسي المحتم، واللوم كذلك يشمل هؤلاء الذين أخفقوا في واجباتهم نتيجة لضعف إرادتهم فيتساوي مع هؤلاء الذين يتجنبون وينأون عن تحمل الكدح والألم ."""
