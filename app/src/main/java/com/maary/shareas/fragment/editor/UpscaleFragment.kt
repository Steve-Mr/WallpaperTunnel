package com.maary.shareas.fragment.editor

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentUpscaleBinding
import com.maary.shareas.fragment.UpscaleSettingsFragment
import com.maary.shareas.helper.PreferencesHelper
import kotlinx.coroutines.launch

class UpscaleFragment : Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentUpscaleBinding? = null
    private val binding get() = _binding!!

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val preferencesHelper by lazy { PreferencesHelper(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.abortEdit(null)
                requireParentFragment().childFragmentManager.beginTransaction().remove(this@UpscaleFragment).commit()
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
                viewModel.upscaleProgressState.collect { state ->
                    binding.progressUpscale.setProgressCompat(state, true)
                    if (state == 100) {
                        Log.v("WVM", "FROM2 ${viewModel.upscaleToggle}")
                        viewModel.upscaleToggle = !viewModel.upscaleToggle
                        viewModel.clearUpscaleProgress()
                        Log.v("WVM", "TO2 ${viewModel.upscaleToggle}")
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
                            viewModel.upscale(
                                requireContext(), binding.menuChooseModelTextview.text.toString(),
                                preferencesHelper.getTileSize(),
                                preferencesHelper.getFP16(), preferencesHelper.getCPUDisabled())
                        }
                        false -> {
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            binding.buttonUpscaleToggle.setIconResource(R.drawable.ic_play)
                            if (viewModel.getUpscaleProgress() != 100) {
                                binding.progressUpscale.setProgressCompat(0, true)
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.primaryColorState.collect {
                    if (viewModel.primary != null) {
                        val primaryValue = viewModel.getPrimaryColor(requireContext())
                        val primaryStateList = ColorStateList.valueOf(primaryValue)
                        val tertiaryValue = viewModel.getTertiaryColor(requireContext())
                        val tertiaryStateList = ColorStateList.valueOf(tertiaryValue)
                        binding.buttonUpscaleToggle.iconTint = primaryStateList
                        binding.betaIcon.iconTint = ColorStateList.valueOf(
                            viewModel.getPrimaryColorAlt(requireContext()))
                        binding.betaIcon.setTextColor(viewModel.getPrimaryColorAlt(requireContext()))
                    }
                }
            }
        }

    }

    override fun onDetach() {
        super.onDetach()
        onBackPressedCallback.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpscaleBinding.inflate(inflater, container, false)

        binding.menuChooseModelTextview.setText(resources.getStringArray(R.array.model_names)[2], false)

        binding.betaIcon.setOnLongClickListener {
            UpscaleSettingsFragment().show(childFragmentManager, UpscaleSettingsFragment.TAG)
            true
        }

        binding.buttonUpscaleToggle.setOnClickListener {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.v("WVM", "FROM ${viewModel.upscaleToggle}")

            viewModel.upscaleToggle = !viewModel.upscaleToggle
            Log.v("WVM", "TO ${viewModel.upscaleToggle}")

        }

        val preferencesHelper = PreferencesHelper(requireContext())


        return binding.root
    }
}