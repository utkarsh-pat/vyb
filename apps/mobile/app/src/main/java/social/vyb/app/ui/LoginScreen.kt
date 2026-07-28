package social.vyb.app.ui

import android.content.Context
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.R
import social.vyb.app.data.VybUiState

private val AuthBackground = Color(0xFF0B1220)
private val AuthPanel = Color(0xE61B263B)
private val AuthPanelTop = Color(0xFF263249)
private val AuthField = Color(0xFF263047)
private val AuthText = Color(0xFFF8FAFC)
private val AuthMuted = Color(0xFF9CA9B9)
private val AuthBorder = Color(0x2EFFFFFF)
private val AuthIndigo = Color(0xFF5B5BF7)
private val AuthPurple = Color(0xFF673BEE)
private val AuthTeal = Color(0xFF14B8A6)
private val AuthLink = Color(0xFFC7D2FE)

@Composable
fun LoginScreen(
    state: VybUiState,
    onEmailSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onPasswordReset: (String) -> Unit,
    onGoogleSignIn: (Context) -> Unit,
    onClearError: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var createMode by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val normalizedEmail = email.trim()
    val emailIsValid = Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()

    LaunchedEffect(email, password, confirmPassword, createMode) {
        localError = null
        if (state.authError != null || state.authNotice != null) onClearError()
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            focusManager.clearFocus(force = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1120), Color(0xFF10182C))
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 70.dp)
                .size(230.dp)
                .blur(72.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(AuthIndigo.copy(alpha = .22f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 38.dp)
                .size(175.dp)
                .blur(64.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(AuthTeal.copy(alpha = .16f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 20.dp, end = 12.dp, bottom = 70.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.CenterHorizontally),
                color = AuthPanel,
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, AuthBorder),
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(AuthPanelTop.copy(alpha = .58f), AuthPanel.copy(alpha = .88f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    VybBrandLockup()

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = AuthIndigo.copy(alpha = .18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, AuthIndigo.copy(alpha = .15f))
                    ) {
                        Text(
                            text = if (createMode) "REGISTER" else "LOG IN",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFFE0E7FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = .45.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (createMode) "Register" else "Log in",
                        color = AuthText,
                        fontSize = 36.sp,
                        lineHeight = 37.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.4).sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = if (createMode) {
                            "Create your account with your college email."
                        } else {
                            "Use your college email to continue."
                        },
                        color = AuthMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.widthIn(max = 210.dp)
                    )

                    state.authError?.let {
                        AuthMessage(text = it, isError = true)
                    }
                    localError?.let {
                        AuthMessage(text = it, isError = true)
                    }
                    state.authNotice?.let {
                        AuthMessage(text = it, isError = false)
                    }

                    Spacer(Modifier.height(6.dp))
                    GoogleAuthButton(
                        label = if (createMode) "Register with Google" else "Continue with Google",
                        enabled = !state.isLoading,
                        onClick = { onGoogleSignIn(context) }
                    )

                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = .08f))
                        Text(
                            "or",
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = AuthMuted,
                            fontSize = 13.sp
                        )
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = .08f))
                    }

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "College email",
                        placeholder = "you@college.edu",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = if (createMode) "Create a password" else "Enter your password",
                        keyboardType = KeyboardType.Password,
                        imeAction = if (createMode) ImeAction.Next else ImeAction.Done,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingContent = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                    tint = Color(0xFF8390A5)
                                )
                            }
                        }
                    )

                    if (createMode) {
                        Spacer(Modifier.height(12.dp))
                        AuthTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm password",
                            placeholder = "Confirm your password",
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                        )
                    } else {
                        Text(
                            text = "Forgot your password?",
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(vertical = 6.dp)
                                .clickable(
                                    enabled = !state.isLoading,
                                    role = Role.Button,
                                    onClick = {
                                        if (emailIsValid) {
                                            onPasswordReset(normalizedEmail)
                                        } else {
                                            localError = "Enter a valid college email address."
                                        }
                                    }
                                ),
                            color = AuthLink.copy(alpha = if (state.isLoading) .55f else 1f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(if (createMode) Modifier.height(14.dp) else Modifier.height(0.dp))
                    GradientAuthButton(
                        label = when {
                            state.isLoading -> "Please wait..."
                            createMode -> "Create account"
                            else -> "Sign in"
                        },
                        enabled = !state.isLoading,
                        loading = state.isLoading,
                        onClick = {
                            if (!emailIsValid) {
                                localError = "Enter a valid college email address."
                            } else if (password.length < 6) {
                                localError = "Use a password with at least 6 characters."
                            } else if (createMode && password != confirmPassword) {
                                localError = "Your password confirmation does not match."
                            } else if (createMode) {
                                onCreateAccount(normalizedEmail, password)
                            } else {
                                onEmailSignIn(normalizedEmail, password)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (createMode) "Already have an account?" else "If you do not have an account,",
                            color = AuthMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (createMode) "Sign in" else "please register first",
                            modifier = Modifier
                                .padding(start = 5.dp, top = 3.dp, bottom = 3.dp)
                                .clickable(
                                    enabled = !state.isLoading,
                                    role = Role.Button,
                                    onClick = { createMode = !createMode }
                                ),
                            color = AuthLink.copy(alpha = if (state.isLoading) .55f else 1f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = AuthText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            trailingIcon = trailingContent,
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AuthText,
                unfocusedTextColor = AuthText,
                focusedContainerColor = AuthField,
                unfocusedContainerColor = AuthField,
                disabledContainerColor = AuthField.copy(alpha = .72f),
                focusedBorderColor = AuthIndigo.copy(alpha = .5f),
                unfocusedBorderColor = AuthBorder,
                focusedPlaceholderColor = AuthMuted.copy(alpha = .58f),
                unfocusedPlaceholderColor = AuthMuted.copy(alpha = .58f),
                cursorColor = AuthIndigo
            )
        )
    }
}

@Composable
private fun GoogleAuthButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            ),
        color = if (enabled) Color.White else Color.White.copy(alpha = .65f),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(11.dp))
            Text(
                label,
                color = Color(0xFF0A0A0A),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun GradientAuthButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = MutableInteractionSource()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(AuthIndigo, AuthPurple))
                } else {
                    Brush.horizontalGradient(
                        listOf(AuthIndigo.copy(alpha = .42f), AuthPurple.copy(alpha = .42f))
                    )
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(21.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                label,
                color = Color.White.copy(alpha = if (enabled) 1f else .7f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun AuthMessage(text: String, isError: Boolean) {
    val color = if (isError) Color(0xFFFF6384) else Color(0xFF5CFF9A)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        color = color.copy(alpha = .1f),
        border = BorderStroke(1.dp, color.copy(alpha = .2f)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = AuthText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Start
        )
    }
}
