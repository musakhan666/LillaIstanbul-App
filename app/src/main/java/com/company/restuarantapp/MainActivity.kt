package com.company.restuarantapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
    import com.company.restuarantapp.ui.main.MealFormScreen
import com.company.restuarantapp.ui.theme.RestuarantAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestuarantAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MealFormScreen()
                }
            }
        }
    }
}
