package com.maary.shareas.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlurMaskFilter.Blur
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentEditorBinding
import com.maary.shareas.fragment.editor.BlurFragment
import com.maary.shareas.fragment.editor.BrightnessFragment
import com.maary.shareas.fragment.editor.PaintFragment
import com.maary.shareas.fragment.editor.UpscaleFragment
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private var isSaved = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.startEditing()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentBitmapState.collect { state ->
                    if (state == WallpaperViewModel.HOME) {
                        binding.appbarToggleGroup.check(binding.appbarButtonHome.id)
                    }
                    if (state == WallpaperViewModel.LOCK) {
                        binding.appbarToggleGroup.check(binding.appbarButtonLock.id)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.upscaleProgressState.collect { state ->
                    if (state == 100) {
                        binding.editorButtonApply.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.primaryColorState.collect {
                    if (viewModel.primary != null) {
                        val colorValue = viewModel.teriary!!
                        val colorStateList = ColorStateList.valueOf(colorValue)
                        binding.editorButtonBlur.backgroundTintList = colorStateList
                        binding.editorButtonBrightness.backgroundTintList = colorStateList
                        binding.editorButtonFill.backgroundTintList = colorStateList
                        binding.editorButtonUpscale.backgroundTintList = colorStateList
                        binding.appbarButtonCancel.setBackgroundColor(viewModel.secondary!!)
                        binding.appbarButtonConfirm.setBackgroundColor(viewModel.secondary!!)
                        binding.editorButtonApply.setTextColor(viewModel.primary!!)
                        binding.editorButtonAbort.setTextColor(viewModel.primary!!)
                        binding.chipApplyHome.backgroundTintList = colorStateList
                        binding.chipApplyLock.backgroundTintList = colorStateList
                    }
                }
            }
        }

        activity?.onBackPressedDispatcher?.addCallback {
            if (!isSaved) viewModel.restoreChanges()
            isEnabled = false
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.editorButtons.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chipHome = binding.chipApplyHome
        val chipLock = binding.chipApplyLock

        binding.appbarButtonHome.setOnClickListener {
            viewModel.currentBitmap = WallpaperViewModel.HOME
        }

        binding.appbarButtonLock.setOnClickListener {
            viewModel.currentBitmap = WallpaperViewModel.LOCK
        }

        binding.appbarButtonCancel.setOnClickListener {
            viewModel.restoreChanges()
            isSaved = false
            if (binding.editorCard.visibility == View.VISIBLE) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.appbarButtonConfirm.setOnClickListener {
            viewModel.saveEdit()
            isSaved = true
            if (binding.editorCard.visibility == View.VISIBLE) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.editorButtonApply.setOnClickListener {
            if (!chipHome.isChecked) viewModel.abortEditHome()
            if (!chipLock.isChecked) viewModel.abortEditLock()
            viewModel.saveEdit()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.editorButtonAbort.setOnClickListener {
            viewModel.abortEdit()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.editorButtonBlur.setOnClickListener {
            loadFragment(BlurFragment())
        }

        binding.editorButtonBrightness.setOnClickListener {
            loadFragment(BrightnessFragment())
        }

        binding.editorButtonFill.setOnClickListener {
            loadFragment(PaintFragment())
        }

        binding.editorButtonUpscale.setOnClickListener {
            loadFragment(UpscaleFragment())
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.finishEditing()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadFragment(fragment: Fragment) {
        viewModel.abortEdit()
        val fragmentManager: FragmentManager = childFragmentManager
        fragmentManager.commitNow {
            replace(binding.editorFragmentContainer.id, fragment)
        }
        if (fragment is UpscaleFragment) {
            binding.editorButtonApply.visibility = View.INVISIBLE
            binding.chipApplyHome.isClickable = false
            binding.chipApplyLock.isClickable = false
        } else {
            binding.editorButtonApply.visibility = View.VISIBLE
            binding.chipApplyHome.isClickable = true
            binding.chipApplyLock.isClickable = true
        }
        binding.editorCard.visibility = View.VISIBLE
    }
}