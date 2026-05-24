package com.eyalm.adns

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.ui.screens.onboarding.ShizukuActivationScreen
import com.eyalm.adns.ui.screens.providerLogin.Login
import com.eyalm.adns.ui.screens.providerLogin.ProfileOptionPage
import com.eyalm.adns.ui.screens.providerLogin.SuccessLoginScreen
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.ProviderLoginViewModel
import kotlinx.coroutines.launch

class ProviderLoginActivity : ComponentActivity() {
    enum class Step { LOGIN , SIGNUP, LOADING, PROFILE, SUCCESS }



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
                    when (step) {
                        Step.LOGIN -> Login(
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
                        Step.SIGNUP -> TODO()
                        Step.LOADING -> ShizukuActivationScreen()
                        Step.PROFILE -> ProfileOptionPage(
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
                        Step.SUCCESS -> {
                            Log.d("ProviderLoginActivity", "Success screen")
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