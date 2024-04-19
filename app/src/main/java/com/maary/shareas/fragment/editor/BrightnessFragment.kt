package com.maary.shareas.fragment.editor

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentBrightnessBinding
import kotlinx.coroutines.launch

class BrightnessFragment : Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentBrightnessBinding? = null
    private val binding get() = _binding!!
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.abortEdit()
                requireParentFragment().childFragmentManager.beginTransaction().remove(this@BrightnessFragment).commit()
                requireParentFragment().childFragmentManager.popBackStack()
                // 获取包含当前 Fragment 的布局
                val containingLayout = requireView().parent.parent.parent as? View
                // 如果布局不为空，则隐藏布局
                containingLayout?.visibility = View.INVISIBLE
                isEnabled = false
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.primaryColorState.collect {
                    if (viewModel.primary != null) {
                        val primaryValue = viewModel.primary!!
                        val primaryStateList = ColorStateList.valueOf(primaryValue)
                        val secondaryValue = viewModel.secondary!!
                        val secondaryStateList = ColorStateList.valueOf(secondaryValue)
                        binding.adjustmentSlider.thumbTintList = primaryStateList
                        binding.adjustmentSlider.trackActiveTintList = secondaryStateList
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrightnessBinding.inflate(inflater, container, false)

        val sliderBrightness = binding.adjustmentSlider
        sliderBrightness.valueFrom = -50f
        sliderBrightness.valueTo = 50f
        sliderBrightness.stepSize = 1f
        sliderBrightness.value = 0f

        sliderBrightness.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            viewModel.editBrightness(value)
        })

        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        onBackPressedCallback.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}