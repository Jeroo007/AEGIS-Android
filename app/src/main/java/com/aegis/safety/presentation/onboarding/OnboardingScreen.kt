package com.aegis.safety.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.safety.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage("Your Safety, Always On",
            "AEGIS monitors your environment locally — audio, motion, and location — to detect potential danger and get you help fast."),
        OnboardingPage("Hold to Alert",
            "Press and hold the SOS button for immediate emergency escalation. Manual SOS is never blocked by AI confidence."),
        OnboardingPage("You Are In Control",
            "Full privacy controls. Nothing is uploaded without your consent. Enable only what you need.")
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (i == 0) {
                    Image(
                        painter = painterResource(R.drawable.ic_aegis_logo),
                        contentDescription = "AEGIS Logo",
                        modifier = Modifier.height(100.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Text(p.title, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(p.body, style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Surface(
                    modifier = Modifier.padding(4.dp).size(if (i == pager.currentPage) 10.dp else 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ) {}
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < pages.size - 1) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    vm.complete(onFinished)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (pager.currentPage == pages.size - 1) "Get Started" else "Next")
        }
        TextButton(onClick = { vm.complete(onFinished) }, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
    }
}

private data class OnboardingPage(val title: String, val body: String)