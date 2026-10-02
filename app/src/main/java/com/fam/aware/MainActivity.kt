package com.fam.aware

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam.aware.ui.AppRoot
import com.fam.aware.ui.rememberViewModelFactory
import com.fam.aware.ui.theme.FamilyAwareTheme
import com.fam.aware.viewmodel.AppearanceViewModel

/**
 * Единственная Activity приложения.
 *
 * Edge-to-edge включается явно: на Android 15+ (API 35) и Android 16 (API 36)
 * это обязательное поведение для targetSdk 35+, и приложение сразу рисует
 * контент под системными панелями, корректно обрабатывая insets.
 *
 * Предиктивная анимация возврата (predictive back) включена в манифесте
 * через `android:enableOnBackInvokedCallback` и работает автоматически,
 * так как в приложении нет перехватов кнопки «Назад».
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val viewModelFactory = rememberViewModelFactory()
            val appearanceViewModel: AppearanceViewModel = viewModel(factory = viewModelFactory)
            val dynamicColor by appearanceViewModel.dynamicColor.collectAsStateWithLifecycle(false)

            FamilyAwareTheme(dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot(
                        appearanceViewModel = appearanceViewModel,
                        viewModelFactory = viewModelFactory,
                    )
                }
            }
        }
    }
}
