package com.project.geolocation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneNumberChange,
        label = { Text("Ingrese número de telefono destino") },
        placeholder = { Text("3001234567") },
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun PhoneNumberInputPreview(){
    PhoneNumberInput(
        phoneNumber = "3208657509",
        onPhoneNumberChange = { }
    )
}

@Preview(showBackground = true)
@Composable
fun PhoneNumberInputEmptyPreview(){
    PhoneNumberInput(
        phoneNumber = "",
        onPhoneNumberChange = { }
    )
}
