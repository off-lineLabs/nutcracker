package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.offlinelabs.nutcracker.data.AppLanguage
import com.offlinelabs.nutcracker.data.model.Product
import com.offlinelabs.nutcracker.data.model.getLocalizedProductName
import com.offlinelabs.nutcracker.data.service.OpenFoodFactsService
import com.offlinelabs.nutcracker.ui.theme.appSurfaceColor
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun FoodSearchDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product) -> Unit,
    openFoodFactsService: OpenFoodFactsService,
    currentLanguage: AppLanguage
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var technicalErrorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var wholeFoodsOnly by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMorePages by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current
    
    // Debounced search effect - loads first page immediately
    LaunchedEffect(searchQuery, wholeFoodsOnly) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            delay(500) // 500ms debounce
            isLoading = true
            isLoadingMore = false
            technicalErrorMessage = null
            currentPage = 1
            hasMorePages = false
            searchResults = emptyList()
            
            try {
                // Load first page with smaller size for faster initial results
                val result = openFoodFactsService.searchProducts(
                    searchTerms = searchQuery,
                    pageSize = 10, // Smaller initial page for faster response
                    languageCode = when (currentLanguage) {
                        AppLanguage.SPANISH -> "es"
                        AppLanguage.PORTUGUESE -> "pt"
                        AppLanguage.ENGLISH -> "en"
                    },
                    wholeFoodsOnly = wholeFoodsOnly,
                    requireCompleteNutrition = true
                )
                
                result.fold(
                    onSuccess = { response ->
                        val firstPageProducts = response.products.filter { it.productName != null }
                        searchResults = firstPageProducts
                        hasSearched = true
                        hasMorePages = firstPageProducts.size >= 10 // Assume more pages if we got full page
                        
                        // Load additional pages in background
                        if (hasMorePages) {
                            isLoadingMore = true
                            coroutineScope.launch {
                                try {
                                    // Load additional pages with larger page size
                                    for (page in 2..3) {
                                        val additionalResult = openFoodFactsService.searchProducts(
                                            searchTerms = searchQuery,
                                            pageSize = 20,
                                            languageCode = when (currentLanguage) {
                                                AppLanguage.SPANISH -> "es"
                                                AppLanguage.PORTUGUESE -> "pt"
                                                AppLanguage.ENGLISH -> "en"
                                            },
                                            wholeFoodsOnly = wholeFoodsOnly,
                                            requireCompleteNutrition = true
                                        )
                                        
                                        additionalResult.fold(
                                            onSuccess = { response ->
                                                val newProducts = response.products.filter { it.productName != null }
                                                if (newProducts.isNotEmpty()) {
                                                    // Append new products, avoiding duplicates
                                                    val currentResults = searchResults
                                                    val existingIds = currentResults.mapNotNull { it.productName }.toSet()
                                                    val uniqueNewProducts = newProducts.filter { 
                                                        it.productName != null && !existingIds.contains(it.productName)
                                                    }
                                                    searchResults = currentResults + uniqueNewProducts
                                                    hasMorePages = newProducts.size >= 20
                                                } else {
                                                    hasMorePages = false
                                                }
                                            },
                                            onFailure = {
                                                // Silently fail for additional pages - we already have results
                                                hasMorePages = false
                                            }
                                        )
                                        
                                        // Small delay between pages to avoid overwhelming the API
                                        delay(300)
                                    }
                                } catch (e: Exception) {
                                    // Silently fail for additional pages
                                    hasMorePages = false
                                } finally {
                                    isLoadingMore = false
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        technicalErrorMessage = error.message ?: "Unknown error"
                        searchResults = emptyList()
                        hasSearched = true
                    }
                )
            } catch (e: Exception) {
                technicalErrorMessage = e.message ?: "Unknown error"
                searchResults = emptyList()
                hasSearched = true
            } finally {
                isLoading = false
            }
        } else if (searchQuery.isBlank()) {
            searchResults = emptyList()
            hasSearched = false
            technicalErrorMessage = null
            currentPage = 1
            hasMorePages = false
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_food),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextPrimaryColor()
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = appTextSecondaryColor()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_food_placeholder),
                            color = appTextSecondaryColor()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = appTextSecondaryColor()
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.clear),
                                    tint = appTextSecondaryColor()
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF60A5FA),
                        unfocusedBorderColor = appTextSecondaryColor().copy(alpha = 0.3f),
                        focusedTextColor = appTextPrimaryColor(),
                        unfocusedTextColor = appTextPrimaryColor()
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Whole Foods Filter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.whole_foods_only),
                        color = appTextPrimaryColor(),
                        fontSize = 14.sp
                    )
                    
                    Switch(
                        checked = wholeFoodsOnly,
                        onCheckedChange = { wholeFoodsOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF60A5FA),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = appTextSecondaryColor().copy(alpha = 0.3f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content area
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF60A5FA)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.search_loading),
                                    color = appTextSecondaryColor()
                                )
                            }
                        }
                    }
                    
                    technicalErrorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.search_error_title),
                                    color = appTextPrimaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.search_error_message, technicalErrorMessage!!),
                                    color = appTextSecondaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    hasSearched && searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = appTextSecondaryColor(),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                val noResultsText = stringResource(R.string.search_no_results)
                                val openFoodFactsText = stringResource(R.string.open_food_facts)
                                val clickableText = buildAnnotatedString {
                                    val openFoodFactsUrl = "https://world.openfoodfacts.org/"
                                    val openFoodFactsIndex = noResultsText.indexOf(openFoodFactsText)
                                    
                                    if (openFoodFactsIndex != -1) {
                                        // Add text before the link
                                        append(noResultsText.substring(0, openFoodFactsIndex))
                                        
                                        // Add clickable link
                                        pushStringAnnotation("URL", openFoodFactsUrl)
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color(0xFF60A5FA),
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append(openFoodFactsText)
                                        }
                                        pop()
                                        
                                        // Add text after the link
                                        append(noResultsText.substring(openFoodFactsIndex + openFoodFactsText.length))
                                    } else {
                                        append(noResultsText)
                                    }
                                }
                                
                                ClickableText(
                                    text = clickableText,
                                    onClick = { offset ->
                                        clickableText.getStringAnnotations(
                                            tag = "URL",
                                            start = offset,
                                            end = offset
                                        ).firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                    },
                                    style = TextStyle(
                                        color = appTextSecondaryColor(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Filter indicator
                            if (wholeFoodsOnly) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 0.dp, vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF60A5FA).copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FilterList,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.showing_whole_foods_only),
                                                color = Color(0xFF60A5FA),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            items(searchResults) { product ->
                                ProductSearchResultItem(
                                    product = product,
                                    currentLanguage = currentLanguage,
                                    onClick = {
                                        onProductSelected(product)
                                        onDismiss()
                                    }
                                )
                            }
                            
                            // Loading indicator for additional pages
                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFF60A5FA),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.loading_more_results),
                                                color = appTextSecondaryColor(),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = appTextSecondaryColor(),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.search_food_by_name),
                                    color = appTextPrimaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.search_food_tip),
                                    color = appTextSecondaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 12.sp
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
private fun ProductSearchResultItem(
    product: Product,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            AsyncImage(
                model = product.imageFrontUrl ?: product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(appTextSecondaryColor().copy(alpha = 0.1f)),
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Product info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.getLocalizedProductName(currentLanguage),
                    color = appTextPrimaryColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!product.brands.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.brands,
                        color = appTextSecondaryColor(),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Quick nutrition info
                product.nutriments?.let { nutriments ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        nutriments.energyKcal100g?.let { calories ->
                            Text(
                                text = "${calories.toInt()} kcal/100g",
                                color = appTextSecondaryColor(),
                                fontSize = 12.sp
                            )
                        }
                        nutriments.proteins100g?.let { proteins ->
                            Text(
                                text = " • ${stringResource(R.string.protein_format, proteins.toInt())}",
                                color = appTextSecondaryColor(),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            // Arrow icon
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = appTextSecondaryColor(),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


