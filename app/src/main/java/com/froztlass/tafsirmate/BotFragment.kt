package com.froztlass.tafsirmate

import android.graphics.Rect
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.froztlass.tafsirmate.model.BotViewModel
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import android.icu.util.Calendar
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.froztlass.tafsirmate.databinding.FragmentBotBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import io.noties.markwon.Markwon


class BotFragment : Fragment(), TextToSpeech.OnInitListener {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val apiKey =
        "xxx" // Ganti dengan API Key lu
    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var micButton: ImageButton
    private lateinit var inputContainer: LinearLayout
    private lateinit var greetingText: TextView
    private lateinit var welcomeMessage: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var chipGroup: ChipGroup
    private val viewModel: BotViewModel by viewModels()
    private val messageHistory = JSONArray().apply {
        put(JSONObject().apply {
            put("role", "system")
            put(
                "content", """
            Kamu adalah Tafsirmate AI yang membantu menjelaskan Tafsir Al-Qur'an dari Surat Al-Fatihah berdasarkan Tafsir al-Jalalayn secara lengkap dan akurat, jika ada pertanyaan diluar itu kamu tidak usah jawab.
            Respon kamu terhadap pengguna sangat interaktif dan ramah. Kamu bisa memberikan berbagai macam emoji menarik. Kamu menjelaskan Tafsir dengan sangat mendetail dan sangat rinci dari setiap ayat pada surat Al-Fatihah berdasarkan Tafsir al-Jalalayn.
            
            Setelah memberikan jawaban, berikan 7 saran pencarian lanjutan yang singkat dan relevan dengan jawaban sebelumnya dalam format:
            Saran: ["...", "...", "...", "...", "...", "...", "..."]
        """.trimIndent()
            )
        })
    }

    private var isUserScrolling = false
    private var isVoiceInput = false

    private var lastScrollY = 0
    private lateinit var suggestionContainer: LinearLayout

    private var currentChatId: String? = null
    private var isFromHistory = false

    private var isSuggestionVisible = true
    private var isAnimatingSuggestion = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isFromHistory = arguments?.getString("chatId") != null
        currentChatId = arguments?.getString("chatId") ?: generateChatId()

        val view = inflater.inflate(R.layout.fragment_bot, container, false)

        // 🟢 Inisialisasi semua View terlebih dahulu
        chatLayout = view.findViewById(R.id.chatLayout)
        scrollView = view.findViewById(R.id.scrollView)
        inputText = view.findViewById(R.id.inputText)
        sendButton = view.findViewById(R.id.sendButton)
        micButton = view.findViewById(R.id.micButton)
        inputContainer = view.findViewById(R.id.inputContainer)
        greetingText = view.findViewById(R.id.tvGreetingUser)
        welcomeMessage = view.findViewById(R.id.tvWelcomeMessage)
        chipGroup = view.findViewById(R.id.chipGroupSuggestions)
        suggestionContainer = view.findViewById(R.id.suggestionContainer)


        // Jika dari riwayat → sembunyikan elemen interaktif & load isi chat
        if (isFromHistory) {
            chipGroup.visibility = View.GONE
            suggestionContainer.visibility = View.GONE
            greetingText.visibility = View.GONE
            welcomeMessage.visibility = View.GONE
            loadChatMessages(currentChatId!!, view)
        } else {
            showInitialSuggestions()
        }
        suggestionContainer.translationY = 0f
        suggestionContainer.alpha = 1f
        suggestionContainer.visibility = View.VISIBLE

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val currentScrollY = scrollView.scrollY
            val delta = currentScrollY - lastScrollY
            val deadzone = 20

            // Deteksi apakah scrollView masih bisa scroll ke bawah (belum mentok)
            val canScrollDown = scrollView.canScrollVertically(1)

            if (!isAnimatingSuggestion) {
                // Jika scroll ke atas cukup jauh → SEMBUNYIKAN
                if (delta > deadzone && isSuggestionVisible) {
                    isAnimatingSuggestion = true
                    slideUp(suggestionContainer) {
                        isSuggestionVisible = false
                        isAnimatingSuggestion = false
                    }
                }
                // Jika scroll ke bawah cukup jauh DAN BELUM mentok → TAMPILKAN
                else if (delta < -deadzone && !isSuggestionVisible && canScrollDown) {
                    isAnimatingSuggestion = true
                    slideDown(suggestionContainer) {
                        isSuggestionVisible = true
                        isAnimatingSuggestion = false
                    }
                }
            }

            lastScrollY = currentScrollY
        }



        sendButton.setOnClickListener {
            val prompt = inputText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                isVoiceInput = false
                addMessageBubble(prompt, true)
                inputText.text.clear()
                sendMessageToOpenAI(prompt, isVoiceInput)
            }
        }

        micButton.setOnClickListener {
            isVoiceInput = true
            startSpeechRecognition()
        }

        setGreetingMessage()
        view.viewTreeObserver.addOnGlobalLayoutListener { adjustInputPosition(view) }

        scrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> isUserScrolling = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isUserScrolling = false
            }
            false
        }

        if (!isFromHistory) {
            restoreChatHistory()
            showInitialSuggestions()
        }

        textToSpeech = TextToSpeech(requireContext(), this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error: $error")
            }

            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = data?.get(0) ?: ""
                sendMessageToOpenAI(text, isVoiceInput)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        return view
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("id", "ID")

            // Pilih suara pria yang spesifik
            for (voice in textToSpeech.voices) {
                if (voice.name.contains("id-ID")) { // Ganti dengan suara yang diinginkan
                    textToSpeech.voice = voice
                    Log.d("TTS_Voices", "Voice selected: ${voice.name}")
                    break
                }
            }
        }
    }


    private fun setGreetingMessage() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userName = user.displayName ?: "Pengguna"
            val greeting = getGreetingMessage()
            greetingText.text = "$greeting, $userName"
            welcomeMessage.text = "Apa yang ingin Anda tanyakan hari ini?\uD83E\uDD14"
        }
    }

    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Selamat Pagi"
            in 12..15 -> "Selamat Siang"
            in 16..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    private fun restoreChatHistory() {
        if (viewModel.chatHistory.isEmpty()) {
            addBotAvatarMessage(
                "Halo! Saya Tafsirmate AI, siap membantu anda mempelajari Tafsir Surat Al-Fatihah berdasarkan Tafsir Al-Jalalayn.")
            addBotAvatarMessage(
                "Kamu dapat mengetikkan pertanyaan ataupun memilih pertanyaan secara langsung.\uD83D\uDE09")

        } else {
            viewModel.chatHistory.forEach { (message, isUser) ->
                addMessageBubble(message, isUser)
            }
        }
    }

    private fun showInitialSuggestions() {
        val initialSuggestions = listOf(
            "Tafsir Ayat 1",
            "Tafsir Ayat 2",
            "Tafsir Ayat 3",
            "Tafsir Ayat 4",
            "Tafsir Ayat 5",
            "Tafsir Ayat 6",
            "Tafsir Ayat 7"
        )
        updateSuggestionChips(initialSuggestions, "")
    }

    private fun adjustInputPosition(view: View) {
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        val screenHeight = view.rootView.height
        val keypadHeight = screenHeight - rect.bottom

        val layoutParams = inputContainer.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = if (keypadHeight > screenHeight * 0.15) {
            keypadHeight
        } else {
            if (isFromHistory) 100 else 300  // 100dp lebih rendah untuk tampilan riwayat
        }
        inputContainer.layoutParams = layoutParams
    }

    private fun addMessageBubble(message: String, isUser: Boolean) {
        if (isUser) {
            val userView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_chat_user, chatLayout, false)

            val userMessage = userView.findViewById<TextView>(R.id.tvUserMessage)
            val userAvatar = userView.findViewById<ImageView>(R.id.ivUserAvatar)

            userMessage.text = message

            val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
            Glide.with(requireContext())
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.akun_icon)
                .into(userAvatar)

            chatLayout.addView(userView)
        } else {
            val botView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_chat_ai, chatLayout, false)

            val botMessage = botView.findViewById<TextView>(R.id.tvBotMessage)
            val botAvatar = botView.findViewById<ImageView>(R.id.imgBotAvatar)

            botMessage.text = message

            Glide.with(requireContext())
                .load(R.drawable.ic_bot_avatar) // avatar_bot ini gambar bot kamu
                .circleCrop()
                .into(botAvatar)

            chatLayout.addView(botView)
        }
    }

    private fun removeMarkdown(text: String): String {
        return text
            .replace(Regex("""#+\s*"""), "")                      // Hapus heading markdown: #, ##, ###, etc
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")           // Bold
            .replace(Regex("""\*(.*?)\*"""), "$1")               // Italic
            .replace(Regex("""_(.*?)_"""), "$1")                 // Underline-style
            .replace(Regex("""`(.*?)`"""), "$1")                 // Inline code
            .replace(Regex("""~~(.*?)~~"""), "$1")               // Strikethrough
            .replace(Regex("""\[([^\]]+)]\([^)]+\)"""), "$1")    // [text](link)
    }

    private fun generateChatId(): String {
        val now = java.util.Calendar.getInstance()
        return String.format(
            "chat_%04d_%02d_%02d_%02d_%02d",
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH) + 1, // karena bulan dimulai dari 0
            now.get(java.util.Calendar.DAY_OF_MONTH),
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE)
        )
    }

    private fun sendMessageToOpenAI(prompt: String, isVoiceInput: Boolean) {
        messageHistory.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })


        val json = JSONObject().apply {
            put("model", "gpt-4.1")
            put("messages", messageHistory)
            put("max_tokens", 2300)
            put("temperature", 0.1)
        }

        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return


        val chatId = currentChatId ?: generateChatId() // contoh: "chat_2025_07_11_15_30"
        val chatRef = firestore.collection("users").document(userId).collection("chats").document(chatId)

        val userMessage = mapOf("role" to "user", "content" to prompt)

        chatRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                chatRef.update("messages", FieldValue.arrayUnion(userMessage))
            } else {
                val newChat = mapOf(
                    "title" to prompt.take(100),
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messages" to listOf(userMessage)
                )
                chatRef.set(newChat)
            }
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    addMessageBubble("Error: ${e.message}", false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                val choice = JSONObject(responseData ?: "")
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")


                // Coba ambil suggestion di akhir respons (format: Saran: ["...", "..."])
                val suggestions = mutableListOf<String>()
                val suggestionRegex = Regex("(?i)Saran(?:\\s*:\\s*|\\s*)(\\[.*?\\])")
                val match = suggestionRegex.find(choice)
                if (match != null) {
                    val arrayRaw = match.groupValues[1]
                    try {
                        val jsonArray = JSONArray(arrayRaw)
                        for (i in 0 until jsonArray.length()) {
                            suggestions.add(jsonArray.getString(i))
                        }
                    } catch (_: Exception) {
                    }
                }

                val assistantMessage = mapOf("role" to "assistant", "content" to choice.trim())
                chatRef.update("messages", FieldValue.arrayUnion(assistantMessage))

                messageHistory.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", choice.trim())
                })

                val mainResponse = suggestionRegex.replace(choice.trim(), "").trim()
                val cleanedText = removeMarkdown(mainResponse)
                activity?.runOnUiThread {
                    //addMessageBubble(cleanedText, false)
                    val botView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_chat_ai, chatLayout, false)
                    val botMessage = botView.findViewById<TextView>(R.id.tvBotMessage)
                    val markwon = Markwon.create(requireContext())
                    markwon.setMarkdown(botMessage, cleanedText)
                    chatLayout.addView(botView)
                    if (!isUserScrolling) {
                        scrollView.postDelayed({ scrollView.smoothScrollTo(0, chatLayout.bottom) }, 200)
                    }

                    if (isVoiceInput) {
                        speakText(cleanedText)
                    }

                    // Tampilkan chip suggestion
                    if (!isFromHistory) {
                        updateSuggestionChips(suggestions, choice.trim())
                    }
                }
            }
        })
    }

    private fun addBotAvatarMessage(message: String) {
        val botView = LayoutInflater.from(requireContext()).inflate(R.layout.item_chat_ai, chatLayout, false)
        val botMessage = botView.findViewById<TextView>(R.id.tvBotMessage)
        val markwon = io.noties.markwon.Markwon.create(requireContext())
        markwon.setMarkdown(botMessage, message)
        chatLayout.addView(botView)

        if (!isUserScrolling) {
            scrollView.postDelayed({ scrollView.smoothScrollTo(0, chatLayout.bottom) }, 100)
        }

        viewModel.addMessage(message, false)
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
        }
        speechRecognizer.startListening(intent)
    }

    private fun speakText(text: String) {
        val cleanedText = removeMarkdown(text)
        Log.d("TTS", "Cleaned text: $cleanedText")
        textToSpeech.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun slideDown(view: View, onEnd: (() -> Unit)? = null) {
        view.clearAnimation()
        view.visibility = View.VISIBLE
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .withEndAction { onEnd?.invoke() }
            .start()
    }

    private fun slideUp(view: View, onEnd: (() -> Unit)? = null) {
        view.clearAnimation()
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    // ⛳️ ini harus di luar onCreateView

    private fun loadChatMessages(chatId: String, view: View) {
        val chatLayout = view.findViewById<LinearLayout>(R.id.chatLayout)
        chatLayout.removeAllViews()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatRef = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val messages = document.get("messages") as? List<Map<String, String>>
                messages?.forEach { message ->
                    val role = message["role"] ?: ""
                    val contentRaw = message["content"] ?: ""
                    val content = removeMarkdown(
                        contentRaw.replace(Regex("(?i)Saran\\s*:\\s*\\[.*?\\]"), "").trim()
                    )


                    if (role == "user") {
                        val userView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_chat_user, chatLayout, false)
                        val userMessage = userView.findViewById<TextView>(R.id.tvUserMessage)
                        val userAvatar = userView.findViewById<ImageView>(R.id.ivUserAvatar)

                        userMessage.text = content
                        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.akun_icon)
                            .into(userAvatar)

                        chatLayout.addView(userView)
                    } else {
                        val botView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_chat_ai, chatLayout, false)
                        val botMessage = botView.findViewById<TextView>(R.id.tvBotMessage)
                        val botAvatar = botView.findViewById<ImageView>(R.id.imgBotAvatar)

                        val markwon = Markwon.create(requireContext())
                        markwon.setMarkdown(botMessage, content)

                        Glide.with(requireContext())
                            .load(R.drawable.ic_bot_avatar)
                            .circleCrop()
                            .into(botAvatar)

                        chatLayout.addView(botView)
                    }
                }

                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memuat chat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSuggestionChips(suggestions: List<String>, lastResponse: String) {
        chipGroup.removeAllViews()
        if (suggestions.isEmpty()) {
            chipGroup.visibility = View.GONE
            return
        }

        chipGroup.visibility = View.VISIBLE

        // 🔎 Deteksi ayat utama dari lastResponse (misal: "Ayat 1")
        val ayatRegex = Regex("""ayat\s+(\d+)""", RegexOption.IGNORE_CASE)
        val matchAyat = ayatRegex.find(lastResponse)
        val currentAyat = matchAyat?.groupValues?.get(1)  // Ambil "1" dari "Ayat 1"

        suggestions.forEach { suggestion ->
            val suggestionAyatMatch = ayatRegex.find(suggestion)
            val suggestionAyat = suggestionAyatMatch?.groupValues?.get(1)

            // 🎯 Cek relevansi:
            val isRelevant = when {
                currentAyat == null -> false  // Kalau gak bisa deteksi ayat, semua dianggap tidak relevan
                suggestionAyat == null -> true // Jika suggestion tidak nyebut ayat (misal: "arti arrahman"), tetap anggap relevan
                suggestionAyat == currentAyat -> true
                else -> false
            }

            val chip = Chip(requireContext()).apply {
                text = suggestion
                isClickable = true
                isCheckable = false
                chipBackgroundColor= ColorStateList.valueOf(
                    if (isRelevant) Color.parseColor("#00E676") else Color.WHITE
                )
                //chipBackgroundColor = ColorStateList.valueOf(Color.WHITE)
                setTextColor(Color.BLACK)
                typeface = ResourcesCompat.getFont(requireContext(), R.font.googlesans_medium)
                chipCornerRadius = 38f
                setPadding(30, 10, 30, 10)

                setOnClickListener {
                    isVoiceInput = false
                    addMessageBubble(suggestion, true)
                    sendMessageToOpenAI(suggestion, isVoiceInput)
                }
            }
            chipGroup.addView(chip)
        }
    }
}