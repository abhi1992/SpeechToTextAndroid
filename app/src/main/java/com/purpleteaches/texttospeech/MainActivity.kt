package com.purpleteaches.texttospeech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.purpleteaches.texttospeech.ui.theme.TextToSpeechTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.startActivity

class MainActivity : ComponentActivity() {
    val voiceToTextParser by lazy {
        AudioRecorder(application)
    }
    lateinit var state: State<TextToVoiceParser>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var canRecord by remember {
                mutableStateOf(false)
            }
            var text by remember  { mutableStateOf("") }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    canRecord = isGranted
                }
            )
            LaunchedEffect(key1 = recordAudioLauncher) {
                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            state = voiceToTextParser.state.collectAsState()
            TextToSpeechTheme {
                Scaffold (
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (!state.value.isSpeaking) {
                                    voiceToTextParser.startListening()
                                } else {
                                    voiceToTextParser.stopListening()
                                }
                            }
                        ) {
                            AnimatedContent(targetState = state.value.isSpeaking, label = "fab") { isSpeaking ->
                                if(isSpeaking) {
                                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                                } else {
                                    Icon(imageVector = Icons.Rounded.Mic, contentDescription = "Start")
                                }
                            }
                        }
                    },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            navigationIcon = {

                            },
                            title = {
                                Text("Text to Speech Hindi")
                            },
                            actions = {
                                Share(state.value.spokenText.toString(), LocalContext.current)
                            }
                        )
                    }

                ) { padding ->
                    Column (
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        AnimatedContent(
                            targetState = state.value, label = "Alert"
                        ) { value ->
                            if(value.isSpeaking) {
                                Text("Speaking...")
                            } else {
                                if (value.spokenText.isNotEmpty()) {
                                    // Add Full stop after every successful write
                                    val onConfirm: (String) -> Unit = { t -> text = "$t। " }
                                    NAlertDialog(
                                        onDismissRequest = ::onDismiss,
                                        onConfirmation = onConfirm,
                                        options = state.value.spokenText,
                                        text = text,
                                    )
                                }

                                TextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.shapes.small,
                                        )
                                        .padding(25.dp),
                                    value = text,
                                    onValueChange = {
                                        text = it
                                    },
                                    label = {  }
                                )
                            }
                        }

                    }

                }
            }
        }


    }
}

fun onDismiss() {}

// Our custom sharing component
@Composable
fun Share(text: String, context: Context) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)

    FloatingActionButton (
        containerColor = Color.White,
        contentColor = Color.Black,
        onClick = {
            startActivity(context, shareIntent, null)
        }
    ) {
        Icon(imageVector = Icons.Default.Share, contentDescription = null)
    }
}

@Composable
fun NAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (text: String) -> Unit,
    options: ArrayList<String>,
    text: String
) {

    val openAlertDialog = remember { mutableStateOf(true) }
    val onConfirm: (String) -> Unit = {
        text -> onConfirmation(text)
        openAlertDialog.value = false
    }

    val onDismiss: () -> Unit = {
        onDismissRequest()
        openAlertDialog.value = false
    }

    if(!openAlertDialog.value) {
        return;
    }

    Dialog(
        onDismissRequest = { openAlertDialog.value = false },
        properties = DialogProperties(),
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(375.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                RadioButtons(
                    options,
                    onDismissRequest = onDismiss,
                    text = text.toString(),
                    onConfirmation = onConfirm
                )
            }
        }
    )
}

@Composable
fun RadioButtons(options: ArrayList<String>, onDismissRequest: () -> Unit, text: String, onConfirmation: (text: String) -> Unit) {
    val selectedValue = remember { mutableStateOf("") }

    val isSelectedItem: (String) -> Boolean = { selectedValue.value == it }
    val onChangeState: (String) -> Unit = { selectedValue.value = it }

    val items = options
    Column(Modifier.padding(8.dp)) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = isSelectedItem(item),
                        onClick = { onChangeState(item) },
                        role = Role.RadioButton
                    )
                    .padding(8.dp)
            ) {
                RadioButton(
                    selected = isSelectedItem(item),
                    onClick = null
                )
                Text(
                    text = item,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Dismiss")
                }
                TextButton(
                    onClick = { onConfirmation(text + selectedValue.value) },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}