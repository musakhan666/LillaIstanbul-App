package com.company.restuarantapp.ui.main

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.company.restuarantapp.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@Composable
fun MealFormScreen() {

    val mealNames = remember { mutableStateListOf("", "", "", "", "", "") }
    val imageUris = remember { mutableStateListOf<Uri?>(null, null, null, null, null, null) }
    var price by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRow by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rowToDelete by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> selectedRow?.let { imageUris[it] = uri } }
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp)) {
        // Price input field
        TextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),  // Fill the available space
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(mealNames) { i, mealName ->
                MealRow(
                    mealName = mealNames[i],
                    onMealNameChange = { mealNames[i] = it },
                    imageUri = imageUris[i],
                    onImageSelect = {
                        selectedRow = i
                        imageLauncher.launch("image/*")
                    },
                    onDeleteClick = {
                        rowToDelete = i
                        showDeleteDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Save button or loading indicator
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = {
                    if (mealNames.any { it.isNotBlank() } || imageUris.any { it != null }) {
                        if (price.isNotBlank() && price.toFloatOrNull() != null) {
                            isLoading = true
                            saveMeals(mealNames, imageUris, price.toFloat(), context) {
                                isLoading = false
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a valid price.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "At least one meal section must be completed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Save")
            }
        }

        // Show delete confirmation dialog
        if (showDeleteDialog && rowToDelete != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    rowToDelete?.let { row ->
                        deleteMeal(row + 1, context) { success ->
                            if (success) {
                                mealNames[row] = ""
                                imageUris[row] = null
                                Toast.makeText(context, "Meal deleted", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                        }
                    }
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    }
}


@Composable
fun MealRow(
    mealName: String,
    onMealNameChange: (String) -> Unit,
    imageUri: Uri?,
    onImageSelect: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = mealName,
            onValueChange = onMealNameChange,
            label = { Text("Meal") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))

        // Button with Upload Icon
        Icon(
            painter = painterResource(id = R.drawable.upload),
            contentDescription = "Upload Image",
            tint = Color.Black,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onImageSelect)
        )

        Spacer(modifier = Modifier.width(8.dp))
        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = "Selected Meal Image",
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Meal")
        }
    }
}


@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Meal") },
        text = { Text("Are you sure you want to delete this meal?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun deleteMeal(rowIndex: Int, context: android.content.Context, onComplete: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    // Use FieldValue.delete() to completely remove the fields
    firestore.collection("meals").document("meal")
        .update(
            mapOf(
                "meal$rowIndex" to FieldValue.delete(),
                "mealImage$rowIndex" to FieldValue.delete()
            )
        )
        .addOnSuccessListener {
            Log.d("Firebase", "Meal $rowIndex deleted")
            Toast.makeText(context, "Meal $rowIndex successfully deleted", Toast.LENGTH_SHORT)
                .show()
            onComplete(true)
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error deleting meal $rowIndex", e)
            Toast.makeText(context, "Failed to delete meal", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
}


// Function to save meals and images to Firebase Storage and Firestore
fun saveMeals(
    mealNames: List<String>,
    imageUris: List<Uri?>,
    price: Float,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val mealData = hashMapOf<String, Any>()

    var completedUploads = 0
    val totalUploads = mealNames.filterIndexed { index, mealName ->
        mealName.isNotBlank() || imageUris[index] != null
    }.size

    if (totalUploads == 0) {
        mealData["price"] = price
        saveMealDataToFirestore(mealData, context, onComplete)
        return
    }

    // Upload each meal with its image to Firebase
    mealNames.forEachIndexed { index, mealName ->
        val imageUri = imageUris[index]
        val mealNameWithIndex = "${index + 1}. $mealName"

        if (mealName.isNotBlank() && imageUri != null) {
            val storageRef = storage.reference.child("meal_images/meal${index + 1}.jpg")
            val uploadTask = storageRef.putFile(imageUri)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    mealData["meal${index + 1}"] = mealNameWithIndex
                    mealData["mealImage${index + 1}"] = uri.toString()

                    completedUploads++
                    if (completedUploads == totalUploads) {
                        mealData["price"] = price
                        saveMealDataToFirestore(mealData, context, onComplete)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error uploading image for meal $index", e)
                onComplete()
            }
        } else if (mealName.isNotBlank() && imageUri == null) {
            // Handle case where there's a meal name but no image
            mealData["meal${index + 1}"] = mealNameWithIndex

            completedUploads++
            if (completedUploads == totalUploads) {
                mealData["price"] = price
                saveMealDataToFirestore(mealData, context, onComplete)
            }
        } else if (mealName.isBlank() && imageUri != null) {
            // Handle case where there's no meal name but an image
            val storageRef = storage.reference.child("meal_images/meal${index + 1}.jpg")
            val uploadTask = storageRef.putFile(imageUri)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    mealData["mealImage${index + 1}"] = uri.toString()

                    completedUploads++
                    if (completedUploads == totalUploads) {
                        mealData["price"] = price
                        saveMealDataToFirestore(mealData, context, onComplete)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error uploading image for meal $index", e)
                onComplete()
            }
        }
    }
}

fun saveMealDataToFirestore(
    mealData: HashMap<String, Any>,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("meals")
        .document("meal")
        .set(mealData, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("Firebase", "Meals successfully updated in Firestore")
            Toast.makeText(context, "Meals successfully saved!", Toast.LENGTH_SHORT).show()
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error updating meals in Firestore", e)
            onComplete()
        }
}
