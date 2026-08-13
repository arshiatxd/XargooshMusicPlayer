package com.example.xargoosh.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.material3.Text
import com.example.xargoosh.core.design.themes.XargooshTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { XargooshTheme { Text("Xargoosh") } }
  }

  @Test
  fun appThemeContent_exists() {
    composeTestRule.onNodeWithText("Xargoosh").assertExists()
  }
}
