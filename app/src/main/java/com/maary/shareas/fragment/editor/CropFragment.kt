package com.maary.shareas.fragment.editor

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentBlurBinding
import com.maary.shareas.databinding.FragmentCropBinding
import com.maary.shareas.view.ScrollableImageView
import kotlinx.coroutines.launch

class CropFragment: Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentCropBinding? = null
    private val binding get() = _binding!!
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.abortEdit(null)
                requireParentFragment().childFragmentManager.beginTransaction().remove(this@CropFragment).commit()
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
                        val colorValue = viewModel.getPrimaryColor(requireContext())
                        val colorValueSec = viewModel.getSecondaryColor(requireContext())
                        val colorValueOnPri = viewModel.getPrimaryColorAlt(requireContext())
                        val colorStateList = ColorStateList.valueOf(colorValue)
                        val colorStateListSec = ColorStateList.valueOf(colorValueSec)
                        val colorStateListOnPri = ColorStateList.valueOf(colorValueOnPri)
                        binding.buttonLeftEdge.backgroundTintList = colorStateList
                        binding.buttonRightEdge.backgroundTintList = colorStateList
                        binding.buttonConfirmEdge.backgroundTintList = colorStateListSec
                        binding.buttonLeftEdge.iconTint = colorStateListOnPri
                        binding.buttonRightEdge.iconTint = colorStateListOnPri
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropBinding.inflate(inflater, container, false)
        binding.buttonConfirmEdge.isEnabled = false

        val imageView = requireActivity().findViewById<ScrollableImageView>(R.id.main_view)

        binding.buttonLeftEdge.setOnClickListener {
            binding.buttonConfirmEdge.isEnabled = true
            val param = viewModel.scrollToStart()
            imageView.scrollImageTo(param.first, param.second, param.third)
        }

        binding.buttonRightEdge.setOnClickListener {
            binding.buttonConfirmEdge.isEnabled = true
            val param = viewModel.scrollToEnd()
            imageView.scrollImageTo(param.first, param.second, param.third)
        }

        binding.buttonConfirmEdge.setOnClickListener {
            if (binding.buttonLeftEdge.isChecked) {
                viewModel.crop(start = imageView.getVisibleRectStart())
            }
            if (binding.buttonRightEdge.isChecked) {
                viewModel.crop(end = imageView.getVisibleRectEnd())
            }
            binding.buttonConfirmEdge.isEnabled = false
        }


        return binding.root
    }
}