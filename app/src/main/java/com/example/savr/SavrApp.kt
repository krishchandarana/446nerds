package com.savr.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import com.example.savr.data.UserProfile
import com.example.savr.data.deserializePlannedMeals
import com.example.savr.data.deserializeRecipes
import com.example.savr.data.getCurrentWeekDays
import com.example.savr.data.getCurrentWeekKey
import com.example.savr.data.serializePlannedMeals
import com.example.savr.data.setUserProfile
import com.example.savr.data.userProfileFlow
import com.savr.app.ui.components.SavrBottomNav
import com.savr.app.ui.screens.CurrentInventory.CurrentInventoryScreen
import com.savr.app.ui.screens.grocery.GroceryScreen
import com.savr.app.ui.screens.login.CreateAccountScreen
import com.savr.app.ui.screens.login.LoginScreen
import com.savr.app.ui.screens.meals.MealsScreen
import com.savr.app.ui.screens.plan.PlanScreen
import com.savr.app.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.flow.collect

@Composable
fun SavrApp(modifier: Modifier = Modifier) {
    val isLoggedInState = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
    var isLoggedIn by isLoggedInState

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        fun verifyAndSetLoggedIn() {
            val user = auth.currentUser
            if (user == null) {
                isLoggedInState.value = false
                return
            }
            // Force token refresh: if user was deleted (or revoked) on server, this fails and we sign out
            user.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isLoggedInState.value = true
                } else {
                    auth.signOut()
                }
            }
        }
        verifyAndSetLoggedIn()
        val listener = FirebaseAuth.AuthStateListener { verifyAndSetLoggedIn() }
        auth.addAuthStateListener(listener)
        try {
            awaitCancellation()
        } finally {
            auth.removeAuthStateListener(listener)
        }
    }

    if (!isLoggedIn) {
        var showCreateAccount by remember { mutableStateOf(false) }
        if (showCreateAccount) {
            CreateAccountScreen(
                onSuccess = {
                    isLoggedIn = true
                    showCreateAccount = false
                },
                onBack = { showCreateAccount = false }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { isLoggedIn = true },
                onNavigateToCreateAccount = { showCreateAccount = true }
            )
        }
        return
    }

    var currentTab by remember { mutableStateOf(NavTab.PLAN) }
    var plannedMealsByDay by remember {
        mutableStateOf(mapOf<Int, Set<String>>()) 
    }
    
    // Get current week days and today's index
    val (_, currentDayIndex) = remember { getCurrentWeekDays() }
    val currentWeekKey = remember { getCurrentWeekKey() }
    var activeDayIndex by remember { mutableStateOf(currentDayIndex) }
    var matchedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isComingFromPlan by remember { mutableStateOf(false) }
    // Use a coroutine scope that persists across composition changes
    val saveScope = rememberCoroutineScope()
    
    // Load generated meals and planned meals from user profile on startup
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    
    LaunchedEffect(Unit) {
        userProfileFlow().collect { p ->
            profile = p
            if (p != null) {
                // Keep generated meals in sync with backend state.
                val savedRecipes = deserializeRecipes(p.generatedMeals)
                if (savedRecipes.isNotEmpty() || matchedRecipes.isEmpty()) {
                    matchedRecipes = savedRecipes
                }

                // Clear planned meals automatically when entering a new week.
                val storedWeekKey = p.plannedMealsWeekKey
                if (storedWeekKey.isNotBlank() && storedWeekKey != currentWeekKey) {
                    plannedMealsByDay = emptyMap()
                    android.util.Log.d("SavrApp", "New week detected ($storedWeekKey -> $currentWeekKey). Clearing planned meals.")
                    saveScope.launch {
                        try {
                            val clearedProfile = p.copy(
                                plannedMeals = emptyList(),
                                plannedMealsWeekKey = currentWeekKey
                            )
                            setUserProfile(clearedProfile)
                            android.util.Log.d("SavrApp", "Cleared last week's planned meals in Firestore")
                        } catch (e: Exception) {
                            android.util.Log.e("SavrApp", "Failed to clear last week's planned meals", e)
                        }
                    }
                } else {
                    // Week is current (or missing key for legacy docs), load saved meals.
                    plannedMealsByDay = deserializePlannedMeals(p.plannedMeals)
                    android.util.Log.d("SavrApp", "Loaded planned meals for week $currentWeekKey: $plannedMealsByDay")
                }
            } else {
                matchedRecipes = emptyList()
                plannedMealsByDay = emptyMap()
                android.util.Log.d("SavrApp", "No profile available; cleared local meal state")
            }
        }
    }
    
    Column(modifier = modifier) {

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState   = currentTab,
                animationSpec = tween(durationMillis = 180),
                label         = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    NavTab.CURRENTINVENTORY  -> CurrentInventoryScreen(
                        onNavigateToMeals = { recipes ->
                            matchedRecipes = recipes
                            activeDayIndex = 1 // Default to Tuesday or some logic
                            isComingFromPlan = false // View-only when coming from inventory
                            currentTab = NavTab.MEALS 
                        }
                    )
                    NavTab.MEALS -> {
                        if (isComingFromPlan) {
                            // Coming from Plan - allow selection
                            var tempSelectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
                            
                            // Initialize temp selections with existing planned meals for this day
                            LaunchedEffect(activeDayIndex) {
                                tempSelectedIds = plannedMealsByDay[activeDayIndex] ?: emptySet()
                            }
                            
                            MealsScreen(
                                recipes = matchedRecipes,
                                selectedIds = tempSelectedIds,
                                allowSelection = true,
                                onToggleRecipe = { id ->
                                    // Update temporary selections only
                                    tempSelectedIds = if (id in tempSelectedIds) {
                                        tempSelectedIds - id
                                    } else {
                                        tempSelectedIds + id
                                    }
                                },
                                onAddToPlan = {
                                    // Save temporary selections to the plan for the active day
                                    val updatedPlannedMeals = plannedMealsByDay.toMutableMap()
                                    if (tempSelectedIds.isEmpty()) {
                                        // Remove the day if no meals selected
                                        updatedPlannedMeals.remove(activeDayIndex)
                                    } else {
                                        // Update the day with selected meals
                                        updatedPlannedMeals[activeDayIndex] = tempSelectedIds
                                    }
                                    plannedMealsByDay = updatedPlannedMeals
                                    android.util.Log.d("SavrApp", "Updated planned meals for day $activeDayIndex: $tempSelectedIds")
                                    
                                    // Save directly to database to avoid cancellation issues
                                    val currentProfile = profile
                                    if (currentProfile != null) {
                                        saveScope.launch {
                                            try {
                                                val serialized = serializePlannedMeals(updatedPlannedMeals)
                                                android.util.Log.d("SavrApp", "Serialized planned meals: $serialized")
                                                val updatedProfile = currentProfile.copy(
                                                    plannedMeals = serialized,
                                                    plannedMealsWeekKey = currentWeekKey
                                                )
                                                setUserProfile(updatedProfile)
                                                android.util.Log.d("SavrApp", "Successfully saved planned meals after adding: ${updatedPlannedMeals.size} days")
                                            } catch (e: Exception) {
                                                android.util.Log.e("SavrApp", "Failed to save planned meals after adding", e)
                                                e.printStackTrace()
                                            }
                                        }
                                    } else {
                                        android.util.Log.w("SavrApp", "Cannot save planned meals: profile is null")
                                    }
                                    
                                    isComingFromPlan = false
                                    currentTab = NavTab.PLAN
                                }
                            )
                        } else {
                            // Coming from Inventory - view only, no selection
                            MealsScreen(
                                recipes = matchedRecipes,
                                allowSelection = false
                            )
                        }
                    }
                    NavTab.PLAN    -> PlanScreen(
                        recipes = matchedRecipes,
                        plannedMealsByDay = plannedMealsByDay,
                        activeDayIndex = activeDayIndex,
                        currentDayIndex = currentDayIndex,
                        onDaySelected = { activeDayIndex = it },
                        onNavigateToMeals   = { 
                            isComingFromPlan = true // Enable selection when coming from plan
                            currentTab = NavTab.MEALS 
                        },
                        onNavigateToGrocery = { currentTab = NavTab.GROCERY }
                    )
                    NavTab.GROCERY -> GroceryScreen()
                    NavTab.PROFILE -> ProfileScreen()
                }
            }
        }
        SavrBottomNav(
            currentTab    = currentTab,
            onTabSelected = { 
                // Clear the "coming from plan" flag when manually switching tabs
                // This ensures Meals tab is view-only unless coming from Plan
                isComingFromPlan = false
                currentTab = it 
            }
        )
    }
}
