
package com.example.qlinic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.qlinic.R

@Composable
fun NameField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, modifier = Modifier.width(100.dp))
        TextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isLetter() || c.isWhitespace() || c == '-' || c == '/' }) },
            singleLine = true,
            placeholder = { Text(text = placeholder) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f) // Use weight to fill available space
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderField(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val genders = listOf("Male", "Female")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Gender", modifier = Modifier.width(100.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f) // Use weight to fill available space
        ) {
            TextField(
                value = selectedGender,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.chevron_down),
                        contentDescription = "Dropdown Arrow",
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                genders.forEach { gender ->
                    DropdownMenuItem(
                        text = { Text(gender) },
                        onClick = {
                            onGenderSelected(gender)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneNumberField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Phone Number", modifier = Modifier.width(100.dp))

        // Phone number input field
        TextField(
            value = phoneNumber,
            onValueChange = { onPhoneNumberChange(it.filter { char -> char.isDigit() }) },
            singleLine = true,
            placeholder = { Text(text = "e.g. 12-345 6789") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            leadingIcon = {
                // Static "+60" prefix
                Text(
                    text = "+60",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            modifier = Modifier.weight(1f) // Fill remaining space
        )
    }
}

// This safer VisualTransformation fixes the crash.
class IcVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.filter { it.isDigit() }.take(12)
        val formattedText = buildString {
            for (i in originalText.indices) {
                append(originalText[i])
                if ((i == 5 || i == 7) && i < originalText.length - 1) {
                    append('-')
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 5 -> offset
                    offset <= 7 -> offset + 1
                    else -> offset + 2
                }.coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 6 -> offset
                    offset <= 9 -> offset - 1
                    else -> offset - 2
                }.coerceAtMost(originalText.length)
            }
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}


@Composable
fun IcField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLength = 12
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "IC", modifier = Modifier.width(100.dp))
        Column(modifier = Modifier.weight(1f)) {
            TextField(
                value = value,
                onValueChange = {
                    if (it.length <= maxLength) {
                        onValueChange(it.filter { char -> char.isDigit() })
                    }
                },
                visualTransformation = IcVisualTransformation(),
                placeholder = { Text(text = "e.g. 010203-07-0001") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                isError = isError,
                modifier = Modifier.fillMaxWidth() // Fill width of the Column
            )
            if (isError && errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
