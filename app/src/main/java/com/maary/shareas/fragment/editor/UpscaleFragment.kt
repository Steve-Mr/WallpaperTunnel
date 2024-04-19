package com.maary.shareas.fragment.editor

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentUpscaleBinding
import kotlinx.coroutines.launch

class UpscaleFragment : Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentUpscaleBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback {
            viewModel.abortEdit()
            requireParentFragment().childFragmentManager.beginTransaction().remove(this@UpscaleFragment).commit()
            requireParentFragment().childFragmentManager.popBackStack()
            // 获取包含当前 Fragment 的布局
            val containingLayout = requireView().parent.parent.parent as? View
            // 如果布局不为空，则隐藏布局
            containingLayout?.visibility = View.INVISIBLE
            isEnabled = false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.upscaleProgressState.collect { state ->
                    binding.progressUpscale.setProgressCompat(state, true)
                    if (state == 100) {
                        viewModel.upscaleToggle = !viewModel.upscaleToggle
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.upscaleToggleState.collect { state ->
                    when (state) {
                        true -> {
                            binding.buttonUpscaleToggle.setIconResource(R.drawable.ic_action_close)
                            binding.progressUpscale.isIndeterminate = true
                            viewModel.upscale(requireContext(), binding.menuChooseModelTextview.text.toString())
                        }
                        false -> {
                            binding.buttonUpscaleToggle.setIconResource(R.drawable.ic_play)
                            binding.progressUpscale.setProgressCompat(0, true)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.primaryColorState.collect {
                    if (viewModel.primary != null) {
                        val primaryValue = viewModel.primary!!
                        val primaryStateList = ColorStateList.valueOf(primaryValue)
                        val tertiaryValue = viewModel.teriary!!
                        val tertiaryStateList = ColorStateList.valueOf(tertiaryValue)
                        binding.buttonUpscaleToggle.iconTint = primaryStateList
                        binding.betaIcon.iconTint = tertiaryStateList
                        binding.betaIcon.setTextColor(tertiaryValue)
                    }
                }
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpscaleBinding.inflate(inflater, container, false)

        binding.menuChooseModelTextview.setText(resources.getStringArray(R.array.model_names)[2], false)

        binding.buttonUpscaleToggle.setOnClickListener {
            viewModel.upscaleToggle = !viewModel.upscaleToggle
        }

        return binding.root
    }
}