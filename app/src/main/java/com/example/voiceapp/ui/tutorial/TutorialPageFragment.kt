package com.example.voiceapp.ui.tutorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.voiceapp.R
import com.example.voiceapp.databinding.FragmentTutorialPageBinding

class TutorialPageFragment : Fragment() {

    private var _binding: FragmentTutorialPageBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_PAGE = "page"

        fun newInstance(page: Int): TutorialPageFragment {
            val fragment = TutorialPageFragment()
            val args = Bundle()
            args.putInt(ARG_PAGE, page)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val page = arguments?.getInt(ARG_PAGE) ?: 0

        when (page) {
            0 -> {
                binding.tvTutorialTitle.text = getString(R.string.tutorial_page1_title)
                binding.tvTutorialDescription.text = getString(R.string.tutorial_page1_description)
                binding.ivTutorialImage.setImageResource(android.R.drawable.ic_dialog_info)
            }
            1 -> {
                binding.tvTutorialTitle.text = getString(R.string.tutorial_page2_title)
                binding.tvTutorialDescription.text = getString(R.string.tutorial_page2_description)
                binding.ivTutorialImage.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
            2 -> {
                binding.tvTutorialTitle.text = getString(R.string.tutorial_page3_title)
                binding.tvTutorialDescription.text = getString(R.string.tutorial_page3_description)
                binding.ivTutorialImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            3 -> {
                binding.tvTutorialTitle.text = getString(R.string.tutorial_page4_title)
                binding.tvTutorialDescription.text = getString(R.string.tutorial_page4_description)
                binding.ivTutorialImage.setImageResource(android.R.drawable.ic_menu_search)
            }
            4 -> {
                binding.tvTutorialTitle.text = getString(R.string.tutorial_page5_title)
                binding.tvTutorialDescription.text = getString(R.string.tutorial_page5_description)
                binding.ivTutorialImage.setImageResource(android.R.drawable.ic_menu_preferences)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
