package com.eyalm.adns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.screens.providerLogin.Login
import com.eyalm.adns.ui.screens.providerLogin.ProfileOptionPage
import com.eyalm.adns.ui.screens.providerLogin.SuccessLoginScreen
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.ProviderLoginViewModel
import kotlinx.coroutines.launch

class ProviderLoginActivity : ComponentActivity() {
    enum class Step { LOGIN , LOADING, PROFILE, SUCCESS }



    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {

            val viewModel: ProviderLoginViewModel = viewModel()
            val providerId = intent.getStringExtra("provider")

            val providers = DnsProviders.getAllProviders
            val provider = providers.find { it.id == providerId }!!

            AdnsTheme {

                val step = viewModel.currentStep
                val profiles = viewModel.profiles



                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                        scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = tween(300)
                                        ) +
                                        slideIntoContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                                            initialOffset = { it / 8 }
                                        )) togetherWith
                                        (fadeOut(animationSpec = tween(90)) +
                                                scaleOut(
                                                    targetScale = 1.08f,
                                                    animationSpec = tween(300)
                                                ))
                            } else {
                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                        scaleIn(
                                            initialScale = 1.08f,
                                            animationSpec = tween(300)
                                        )) togetherWith
                                        (fadeOut(animationSpec = tween(90)) +
                                                scaleOut(
                                                    targetScale = 0.92f,
                                                    animationSpec = tween(300)
                                                ) +
                                                slideOutOfContainer(
                                                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                                    animationSpec = tween(
                                                        300,
                                                        easing = FastOutSlowInEasing
                                                    ),
                                                    targetOffset = { it / 8 }
                                                ))
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "onboarding_step_transition"
                    ) { targetStep ->
                        when (targetStep) {
                            Step.LOGIN -> {
                                Login(
                                    provider = provider,
                                    onNextClick = { email, password ->
                                        lifecycleScope.launch {
                                            viewModel.nextStep()
                                            viewModel.providerLogin(email, password, provider.id)
                                        }
                                    },
                                    onBackClick = {
                                        finish()
                                    }
                                )
                            }
                            Step.LOADING -> {
                                BackHandler {  }
                                OnboardingTemplate(
                                    content = { },
                                    bottomBarContent = {
                                        LinearWavyProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        )
                                    }
                                )
                            }
                            Step.PROFILE -> {
                                BackHandler {  }
                                ProfileOptionPage(
                                    profiles = profiles,
                                    onNextClick = { profile ->
                                        viewModel.setProfile(profile)
                                    },
                                    createProfile = { name ->
                                        lifecycleScope.launch {
                                            viewModel.createProfile(name)
                                        }

                                    }
                                )
                            }
                            Step.SUCCESS -> {
                                BackHandler { finish() }
                                SuccessLoginScreen(
                                    onFinishClicked = {
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting3(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    AdnsTheme {
        Greeting3("Android")
    }
}