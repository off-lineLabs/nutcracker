package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.template.data.AppLanguage
import com.example.template.data.model.Product
import com.example.template.data.model.SearchResponse
import com.example.template.data.model.getLocalizedProductName
import com.example.template.data.service.OpenFoodFactsService
import com.example.template.ui.theme.appSurfaceColor
import com.example.template.ui.theme.appTextPrimaryColor
import com.example.template.ui.theme.appTextSecondaryColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.template.R

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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var wholeFoodsOnly by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Debounced search effect
    LaunchedEffect(searchQuery, wholeFoodsOnly) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            delay(500) // 500ms debounce
            isLoading = true
            errorMessage = null
            
            try {
                val result = openFoodFactsService.searchProducts(
                    searchTerms = searchQuery,
                    pageSize = 20,
                    languageCode = when (currentLanguage) {
                        AppLanguage.SPANISH -> "es"
                        AppLanguage.PORTUGUESE -> "pt"
                        AppLanguage.ENGLISH -> "en"
                    },
                    wholeFoodsOnly = wholeFoodsOnly
                )
                
                result.fold(
                    onSuccess = { response ->
                        searchResults = response.products.filter { it.productName != null }
                        hasSearched = true
                    },
                    onFailure = { error ->
                        errorMessage = "Search failed: ${error.message}"
                        searchResults = emptyList()
                        hasSearched = true
                    }
                )
            } catch (e: Exception) {
                errorMessage = "Search failed: ${e.message}"
                searchResults = emptyList()
                hasSearched = true
            } finally {
                isLoading = false
            }
        } else if (searchQuery.isBlank()) {
            searchResults = emptyList()
            hasSearched = false
            errorMessage = null
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
                            contentDescription = "Close",
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
                            text = when (currentLanguage) {
                                AppLanguage.SPANISH -> "Buscar alimentos..."
                                AppLanguage.PORTUGUESE -> "Buscar alimentos..."
                                AppLanguage.ENGLISH -> "Search for food..."
                            },
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
                                    contentDescription = "Clear",
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
                        text = when (currentLanguage) {
                            AppLanguage.SPANISH -> "Solo alimentos naturales"
                            AppLanguage.PORTUGUESE -> "Apenas alimentos naturais"
                            AppLanguage.ENGLISH -> "Whole foods only"
                        },
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
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF60A5FA)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Searching...",
                                    color = appTextSecondaryColor()
                                )
                            }
                        }
                    }
                    
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = appTextSecondaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    hasSearched && searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = appTextSecondaryColor(),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = when (currentLanguage) {
                                        AppLanguage.SPANISH -> "No se encontraron resultados"
                                        AppLanguage.PORTUGUESE -> "Nenhum resultado encontrado"
                                        AppLanguage.ENGLISH -> "No results found"
                                    },
                                    color = appTextSecondaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when (currentLanguage) {
                                        AppLanguage.SPANISH -> "Intenta con otros tÃ©rminos de bÃºsqueda"
                                        AppLanguage.PORTUGUESE -> "Tente outros termos de busca"
                                        AppLanguage.ENGLISH -> "Try different search terms"
                                    },
                                    color = appTextSecondaryColor(),
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                                text = when (currentLanguage) {
                                                    AppLanguage.SPANISH -> "Mostrando solo alimentos naturales"
                                                    AppLanguage.PORTUGUESE -> "Mostrando apenas alimentos naturais"
                                                    AppLanguage.ENGLISH -> "Showing whole foods only"
                                                },
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
                        }
                    }
                    
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = appTextSecondaryColor(),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = when (currentLanguage) {
                                        AppLanguage.SPANISH -> "Busca alimentos por nombre"
                                        AppLanguage.PORTUGUESE -> "Busque alimentos por nome"
                                        AppLanguage.ENGLISH -> "Search for food by name"
                                    },
                                    color = appTextSecondaryColor(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                text = " • ${proteins.toInt()}g protein",
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


