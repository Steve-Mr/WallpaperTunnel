package com.maary.shareas.fragment.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentBrightnessBinding

class BrightnessFragment : Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentBrightnessBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}