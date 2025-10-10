package com.example.voiceapp.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.voiceapp.BuildConfig
import com.example.voiceapp.api.OpenAIClient
import com.example.voiceapp.databinding.FragmentSettingsBinding
import com.example.voiceapp.ui.tutorial.TutorialActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.navigation.fragment.findNavController
import com.example.voiceapp.R
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "user_settings"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AGENT_NAME = "agent_name"
        private const val KEY_USER_ICON_URI = "user_icon_uri"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"

        // デフォルト値
        const val DEFAULT_USER_NAME = "ユーザー"
        const val DEFAULT_AGENT_NAME = "AIアシスタント"
        const val DEFAULT_PERSONALITY = "kind" // playful | kind | objective

        fun getUserName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        }

        fun getAgentName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_AGENT_NAME, DEFAULT_AGENT_NAME) ?: DEFAULT_AGENT_NAME
        }

        fun getUserIconUri(context: Context): Uri? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getString(KEY_USER_ICON_URI, null)
            return value?.let { Uri.parse(it) }
        }

        fun getPersonality(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_PERSONALITY, DEFAULT_PERSONALITY) ?: DEFAULT_PERSONALITY
        }

        fun isTTSEnabled(context: Context): Boolean {
            val voiceappPrefs = context.getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            return voiceappPrefs.getBoolean("tts_enabled", true)
        }

        fun isEmojiReadingEnabled(context: Context): Boolean {
            val voiceappPrefs = context.getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            return voiceappPrefs.getBoolean("emoji_reading_enabled", true)
        }

        fun isWebSearchEnabled(context: Context): Boolean {
            val voiceappPrefs = context.getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            return voiceappPrefs.getBoolean("web_search_enabled", false)
        }
        
        fun getSpeechRate(context: Context): Float {
            val voiceappPrefs = context.getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            // 0.5〜2.0の範囲、デフォルトは1.0（標準速度）
            return voiceappPrefs.getFloat("speech_rate", 1.0f)
        }

        fun resetToDefaults(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }

    interface OnSettingsSavedListener {
        fun onSettingsSaved()
    }
    private var settingsSavedListener: OnSettingsSavedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSettingsSavedListener) {
            settingsSavedListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        settingsSavedListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadUserSettings()
        setupClickListeners()
        
        // 初回起動チェック
        checkAndShowFirstLaunchDialog()
    }
    
    private fun checkAndShowFirstLaunchDialog() {
        val isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        
        if (isFirstLaunch) {
            // 初回起動フラグをfalseに設定
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
            
            // ダイアログを表示
            showFirstLaunchDialog()
        }
    }
    
    private fun showFirstLaunchDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.first_launch_title)
            .setMessage(R.string.first_launch_message)
            .setPositiveButton(R.string.first_launch_positive) { dialog, _ ->
                // Web検索スイッチにフォーカスするなどの処理
                binding.switchWebSearch.requestFocus()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.first_launch_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    //    private fun setupUI() {
    //        // APIキーの状態を表示
    //        val apiKey = BuildConfig.OPENAI_API_KEY
    //        if (apiKey.isNotEmpty() && apiKey != "your_openai_api_key_here") {
    //            binding.tvApiKeyStatus.text = "APIキー: 設定済み (${apiKey.take(10)}...)"
    //        } else {
    //            binding.tvApiKeyStatus.text = "APIキー: 未設定"
    //        }
    //
    //        // Base URLを表示
    //        binding.tvBaseUrlStatus.text = "Base URL: ${BuildConfig.OPENAI_BASE_URL}"
    //    }

    private fun loadUserSettings() {
        // 保存された設定を読み込み
        val userName = sharedPreferences.getString(KEY_USER_NAME, DEFAULT_USER_NAME)
        val agentName = sharedPreferences.getString(KEY_AGENT_NAME, DEFAULT_AGENT_NAME)
        val iconUriString = sharedPreferences.getString(KEY_USER_ICON_URI, null)
        val personality = sharedPreferences.getString(KEY_PERSONALITY, DEFAULT_PERSONALITY)

        binding.etUserName.setText(userName)
        binding.etAgentName.setText(agentName)
        if (iconUriString != null) {
            val uri = Uri.parse(iconUriString)
            binding.ivUserIcon.setImageURI(uri)
        }
        // 性格ラジオ反映
        when (personality) {
            "playful" -> binding.rgPersonality.check(binding.radioPlayful.id)
            "kind" -> binding.rgPersonality.check(binding.radioKind.id)
            "objective" -> binding.rgPersonality.check(binding.radioObjective.id)
            else -> binding.rgPersonality.check(binding.radioKind.id)
        }
        
        // 音声読み上げ設定を読み込み
        val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
        val isTtsEnabled = voiceappPrefs.getBoolean("tts_enabled", true)
        val isEmojiReadingEnabled = voiceappPrefs.getBoolean("emoji_reading_enabled", false)
        val isWebSearchEnabled = voiceappPrefs.getBoolean("web_search_enabled", false)
        val speechRate = voiceappPrefs.getFloat("speech_rate", 1.0f)
        
        binding.switchTtsEnabled.isChecked = isTtsEnabled
        binding.switchEmojiReading.isChecked = isEmojiReadingEnabled
        binding.switchWebSearch.isChecked = isWebSearchEnabled
        
        // 音声速度のSeekBarを設定（0.5〜2.0を0〜100に変換）
        val seekBarProgress = ((speechRate - 0.5f) / 1.5f * 100).toInt()
        binding.seekBarSpeechRate.progress = seekBarProgress
        updateSpeechRateLabel(speechRate)
    }

    private fun setupClickListeners() {
        binding.btnSaveUserSettings.setOnClickListener {
            saveUserSettings()
        }
        binding.btnPickUserIcon.setOnClickListener {
            pickImage()
        }
        binding.btnOpenDebug.setOnClickListener {
            findNavController().navigate(R.id.nav_debug)
        }
        
        binding.btnShowTutorial.setOnClickListener {
            showTutorial()
        }
        
        // 性格: 選択変更で即時保存
        binding.rgPersonality.setOnCheckedChangeListener { _, checkedId ->
            val selectedPersonality = when (checkedId) {
                binding.radioPlayful.id -> "playful"
                binding.radioKind.id -> "kind"
                binding.radioObjective.id -> "objective"
                else -> DEFAULT_PERSONALITY
            }
            sharedPreferences.edit().putString(KEY_PERSONALITY, selectedPersonality).apply()
            // 必要に応じてナビゲーションヘッダー等を更新
            settingsSavedListener?.onSettingsSaved()
        }
        
        // 音声読み上げメインスイッチ
        binding.switchTtsEnabled.setOnCheckedChangeListener { _, isChecked ->
            val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            voiceappPrefs.edit().putBoolean("tts_enabled", isChecked).apply()
            if (isChecked) {
                Toast.makeText(requireContext(), "音声読み上げをONにしました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "音声読み上げをOFFにしました", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 絵文字読み上げスイッチ
        binding.switchEmojiReading.setOnCheckedChangeListener { _, isChecked ->
            val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            voiceappPrefs.edit().putBoolean("emoji_reading_enabled", isChecked).apply()
            if (isChecked) {
                Toast.makeText(requireContext(), "絵文字読み上げをONにしました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "絵文字読み上げをOFFにしました", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.switchWebSearch.setOnCheckedChangeListener { _, isChecked ->
            val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
            voiceappPrefs.edit().putBoolean("web_search_enabled", isChecked).apply()
            
            val message = if (isChecked) {
                "Web検索モードをONにしました（GPT-4o Search Preview使用）"
            } else {
                "Web検索モードをOFFにしました（gpt-4o-mini使用）"
            }
            
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // 音声速度のSeekBar変更リスナー
        binding.seekBarSpeechRate.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // 0〜100を0.5〜2.0に変換
                val speechRate = 0.5f + (progress / 100f * 1.5f)
                updateSpeechRateLabel(speechRate)
                
                if (fromUser) {
                    val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
                    voiceappPrefs.edit().putFloat("speech_rate", speechRate).apply()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val progress = seekBar?.progress ?: 50
                val speechRate = 0.5f + (progress / 100f * 1.5f)
                Toast.makeText(requireContext(), "音声速度を変更しました（${String.format("%.1f", speechRate)}x）", Toast.LENGTH_SHORT).show()
            }
        })
        
        // テスト音声再生ボタン
        binding.btnTestSpeech.setOnClickListener {
            testSpeechPlayback()
        }
    }
    
    private fun updateSpeechRateLabel(speechRate: Float) {
        val label = when {
            speechRate < 0.8f -> getString(R.string.settings_speech_rate_slow)
            speechRate > 1.2f -> getString(R.string.settings_speech_rate_fast)
            else -> getString(R.string.settings_speech_rate_normal)
        }
        binding.tvSpeechRateValue.text = "$label（${String.format("%.1f", speechRate)}x）"
    }
    
    private fun showTutorial() {
        val intent = Intent(requireContext(), TutorialActivity::class.java)
        startActivity(intent)
    }
    
    private fun testSpeechPlayback() {
        val voiceappPrefs = requireContext().getSharedPreferences("voiceapp_settings", Context.MODE_PRIVATE)
        val isTtsEnabled = voiceappPrefs.getBoolean("tts_enabled", true)
        
        if (!isTtsEnabled) {
            Toast.makeText(requireContext(), "音声読み上げがOFFになっています", Toast.LENGTH_SHORT).show()
            return
        }
        
        val speechRate = voiceappPrefs.getFloat("speech_rate", 1.0f)
        val testText = getString(R.string.settings_test_speech_text)
        
        // ボタンを無効化
        binding.btnTestSpeech.isEnabled = false
        binding.btnTestSpeech.text = "再生中..."
        
        lifecycleScope.launch {
            try {
                val customApiKey = voiceappPrefs.getString("custom_api_key", "")?.trim()
                val apiKey = if (!customApiKey.isNullOrEmpty()) {
                    customApiKey
                } else {
                    BuildConfig.OPENAI_API_KEY
                }
                
                if (apiKey.isEmpty() || apiKey == "your_openai_api_key_here") {
                    Toast.makeText(requireContext(), "APIキーが設定されていません", Toast.LENGTH_SHORT).show()
                    binding.btnTestSpeech.isEnabled = true
                    binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
                    return@launch
                }
                
                val client = OpenAIClient(apiKey, "https://api.openai.com/v1/")
                val result = client.textToSpeech(
                    text = testText,
                    voice = "alloy",
                    speed = speechRate.toDouble()
                )
                
                result.fold(
                    onSuccess = { audioData ->
                        playAudio(audioData)
                    },
                    onFailure = { error ->
                        Toast.makeText(requireContext(), "音声生成エラー: ${error.message}", Toast.LENGTH_SHORT).show()
                        Log.e("SettingsFragment", "TTS error", error)
                        binding.btnTestSpeech.isEnabled = true
                        binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "エラー: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SettingsFragment", "Test speech error", e)
                binding.btnTestSpeech.isEnabled = true
                binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
            }
        }
    }
    
    private fun playAudio(audioData: ByteArray) {
        try {
            // 既存のMediaPlayerを解放
            mediaPlayer?.release()
            
            // 一時ファイルに音声データを保存
            val tempFile = File.createTempFile("tts_test", ".mp3", requireContext().cacheDir)
            tempFile.writeBytes(audioData)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                
                setOnCompletionListener {
                    binding.btnTestSpeech.isEnabled = true
                    binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
                    tempFile.delete()
                    it.release()
                }
                
                setOnErrorListener { mp, what, extra ->
                    Toast.makeText(requireContext(), "再生エラー", Toast.LENGTH_SHORT).show()
                    binding.btnTestSpeech.isEnabled = true
                    binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
                    tempFile.delete()
                    mp.release()
                    true
                }
                
                start()
            }
            
            Toast.makeText(requireContext(), "テスト音声を再生中", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "再生エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SettingsFragment", "Audio playback error", e)
            binding.btnTestSpeech.isEnabled = true
            binding.btnTestSpeech.text = getString(R.string.settings_test_speech)
        }
    }

    private fun saveUserSettings() {
        val userName = binding.etUserName.text.toString().trim()
        val agentName = binding.etAgentName.text.toString().trim()
        val currentIconUri = selectedIconUri

        // 空の場合はデフォルト値を使用
        val finalUserName = if (userName.isEmpty()) DEFAULT_USER_NAME else userName
        val finalAgentName = if (agentName.isEmpty()) DEFAULT_AGENT_NAME else agentName
        val selectedPersonality = when (binding.rgPersonality.checkedRadioButtonId) {
            binding.radioPlayful.id -> "playful"
            binding.radioKind.id -> "kind"
            binding.radioObjective.id -> "objective"
            else -> DEFAULT_PERSONALITY
        }

        // SharedPreferencesに保存
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, finalUserName)
            .putString(KEY_AGENT_NAME, finalAgentName)
            .putString(KEY_PERSONALITY, selectedPersonality)
            .apply {
                if (currentIconUri != null) {
                    putString(KEY_USER_ICON_URI, currentIconUri.toString())
                }
            }
            .apply()

        // UIを更新
        binding.etUserName.setText(finalUserName)
        binding.etAgentName.setText(finalAgentName)

        Toast.makeText(context, "設定を保存しました", Toast.LENGTH_SHORT).show()
        settingsSavedListener?.onSettingsSaved()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // MediaPlayerを解放
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }

    // 画像ピッカー
    private var selectedIconUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // 読み込み & 正方形中央クロップ & 円形加工
            val processed = processSelectedImage(uri)
            if (processed != null) {
                // 内部保存してそのUriを保持
                val savedUri = saveBitmapInternal(processed)
                if (savedUri != null) {
                    selectedIconUri = savedUri
                    binding.ivUserIcon.setImageBitmap(processed)
                } else {
                    binding.ivUserIcon.setImageBitmap(processed)
                }
            } else {
                binding.ivUserIcon.setImageURI(uri) // フォールバック
                selectedIconUri = uri
            }
        }
    }

    private fun processSelectedImage(uri: Uri): Bitmap? {
        return try {
            val input: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(input) ?: return null
            input?.close()

            // 正方形中央クロップ
            val size = minOf(original.width, original.height)
            val x = (original.width - size) / 2
            val y = (original.height - size) / 2
            val square = Bitmap.createBitmap(original, x, y, size, size)

            // 目的サイズ (72dp相当) を端末密度で
            val targetPx = (72 * resources.displayMetrics.density).toInt()
            val scaled = Bitmap.createScaledBitmap(square, targetPx, targetPx, true)

            // 円形マスク
            val output = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val path = Path()
            path.addOval(RectF(0f, 0f, targetPx.toFloat(), targetPx.toFloat()), Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            null
        }
    }

    private fun saveBitmapInternal(bitmap: Bitmap): Uri? {
        return try {
            val dir = File(requireContext().filesDir, "user_icons")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "icon.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun pickImage() {
        imagePickerLauncher.launch("image/*")
    }
}
