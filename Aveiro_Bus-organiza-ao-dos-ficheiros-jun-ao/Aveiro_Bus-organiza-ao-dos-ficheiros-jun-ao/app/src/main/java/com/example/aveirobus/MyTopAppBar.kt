package com.example.aveirobus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack // Import the back arrow icon
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text // Import Text for the title
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController // For Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(
    isLoggedIn: Boolean,
    onUserButtonClick: () -> Unit,
    currentRoute: String?, // Parameter to know the current route (useful for title logic later)
    canNavigateBack: Boolean, // Parameter to know if back is possible
    navigateUp: () -> Unit, // Function to call when back is clicked (corrected return type to Unit)
) {
    CenterAlignedTopAppBar(
        title = {
            // You can add a Text composable here to show the title of the current screen
            // based on the currentRoute, for example.
            Text("Aveiro Bus") // Placeholder title
        },
        navigationIcon = {
            // Conditionally show the back arrow icon if navigation back is possible
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack, // Use the back arrow icon
                        contentDescription = "Back" // Accessibility description
                    )
                }
            }
            // If not navigating back, you could optionally show a different icon here,
            // or leave it empty to align the title to the start if no back button.
            // Since this is CenterAlignedTopAppBar, the title is centered regardless.
        },
        actions = {
            // Move the user login/profile button to the actions slot (right side)
            IconButton(onClick = onUserButtonClick) {
                if (isLoggedIn) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle, // Logged-in icon
                        contentDescription = "User Profile" // Accessibility description
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person, // Logged-out icon
                        contentDescription = "Login" // Accessibility description
                    )
                }
            }
            // You can add more action icons here (e.g., search, settings)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            // The navigationIconContentColor will now apply to the back arrow
            navigationIconContentColor = Color.Black,
            // The actionIconContentColor will apply to the user icon
            actionIconContentColor = Color.Black
        )
    )
}

@Preview
@Composable
fun MyTopAppBarPreview() {
    // For preview purposes, you need to pass valid parameters
    // currentRoute and canNavigateBack should be example values, not code references
    MyTopAppBar(
        isLoggedIn = false,
        onUserButtonClick = {},
        currentRoute = "autocarros", // Example: a screen where back is not possible
        canNavigateBack = false, // Hide back button in this preview
        navigateUp = {} // Provide an empty lambda for the action
    )
}

@Preview
@Composable
fun MyTopAppBarPreviewWithBack() {
    // Preview showing the back button
    MyTopAppBar(
        isLoggedIn = true,
        onUserButtonClick = {},
        currentRoute = "login", // Example: a screen where back is possible
        canNavigateBack = true, // Show back button in this preview
        navigateUp = {} // Provide an empty lambda for the action
    )
}