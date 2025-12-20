package com.example.qlinic.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qlinic.R


@Composable
fun FirstNameField(
    firstName: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = firstName,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next),
        placeholder = {
            Text(text = "First Name", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun LastNameField(
    lastName: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = lastName,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next),
        placeholder = {
            Text(text = "First Name", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun EmailField(
    email: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = email,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next),
        placeholder = {
            Text(text = "xxx@gmail.com", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.LightGray,
            disabledPlaceholderColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = if (enabled) Color.Black else Color.LightGray, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcNumberField(
    nric: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = nric,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        placeholder = {
            Text(
                text = "e.g., 001014-00-1234",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberField(
    phoneNumber: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = phoneNumber,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        placeholder = {
            Text(
                text = "1999999999",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        },
        leadingIcon = {
            Text(text = "+60 ",
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp))
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun PasswordField(
    password: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    TextField(
        value = password,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(text = "Your password", color = Color.LightGray)
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Toggle password visibility"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderField(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val genders = listOf("Male", "Female")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
    ) {
        TextField(
            value = selectedGender,
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            singleLine = true,
            placeholder = {
                Text(text = stringResource(id = R.string.select_gender), color = Color.LightGray, fontSize = 16.sp)
            },
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.chevron_down),
                    contentDescription = "Dropdown Arrow",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable (enabled = enabled) {expanded = !expanded }
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.LightGray,
                disabledPlaceholderColor = Color.LightGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(color = if (enabled) Color.Black else Color.LightGray, fontSize = 16.sp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(12.dp)
                )
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

@Composable
fun DescriptionField(
    description: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = description,
        onValueChange = onValueChange,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next),
        placeholder = {
            Text(text = "Description...", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.LightGray,
            disabledPlaceholderColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = if (enabled) Color.Black else Color.LightGray, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun SpecializationField(
    specialization: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = specialization,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next),
        placeholder = {
            Text(text = "E.g Gastroenterology", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.LightGray,
            disabledPlaceholderColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = if (enabled) Color.Black else Color.LightGray, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun YearOfExpField(
    yearsOfExp: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = yearsOfExp,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done),
        placeholder = {
            Text(text = "Years of experience", color = Color.LightGray)
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.LightGray,
            disabledPlaceholderColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = if (enabled) Color.Black else Color.LightGray, fontSize = 16.sp),
        modifier = modifier
            .width(350.dp)
            .padding(top = 5.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun ProfileMenuItem(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.Black
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "›",
                fontSize = 24.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun PatientLoginFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    identifierError: String? = null,
    passwordError: String? = null,
    enabledEmail: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Email field for patient
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Email", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            TextField(
                value = email,
                onValueChange = onEmailChange,
                enabled = enabledEmail,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                placeholder = { Text(text = "patient@example.com", color = Color.LightGray) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.LightGray,
                    disabledPlaceholderColor = Color.LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = if (enabledEmail) Color.Black else Color.LightGray, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp).border(width = 1.dp, color = Color.LightGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            )
            identifierError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Password", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            TextField(
                value = password,
                onValueChange = onPasswordChange,
                singleLine = true,
                placeholder = { Text(text = "Your password", color = Color.LightGray) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle password visibility")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp).border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(12.dp))
            )
            passwordError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StaffLoginFields(
    staffId: String,
    onStaffIdChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    identifierError: String? = null,
    passwordError: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Staff ID field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Staff ID", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            TextField(
                value = staffId,
                onValueChange = onStaffIdChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                placeholder = { Text(text = "Enter your staff ID", color = Color.LightGray) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp).border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(12.dp))
            )
            identifierError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field for staff
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Password", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            TextField(
                value = password,
                onValueChange = onPasswordChange,
                singleLine = true,
                placeholder = { Text(text = "Your password", color = Color.LightGray) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle password visibility")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp).border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(12.dp))
            )
            passwordError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

