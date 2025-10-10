package com.example.voiceapp.ui.tutorial

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.voiceapp.MainActivity
import com.example.voiceapp.R
import com.example.voiceapp.databinding.ActivityTutorialBinding
import com.google.android.material.tabs.TabLayoutMediator

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding
    private val totalPages = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        val adapter = TutorialPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // TabLayoutとViewPagerを連携
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ ->
            // 各タブにはドットのみ表示
        }.attach()
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < totalPages - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                finishTutorial()
            }
        }

        binding.btnSkip.setOnClickListener {
            finishTutorial()
        }

        // ページ変更時にボタンのテキストを更新
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == totalPages - 1) {
                    binding.btnNext.text = getString(R.string.tutorial_start)
                } else {
                    binding.btnNext.text = getString(R.string.tutorial_next)
                }
            }
        })
    }

    private fun finishTutorial() {
        // チュートリアル完了フラグを保存
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_completed", true).apply()

        // MainActivityに遷移
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private inner class TutorialPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = totalPages

        override fun createFragment(position: Int): Fragment {
            return TutorialPageFragment.newInstance(position)
        }
    }
}
