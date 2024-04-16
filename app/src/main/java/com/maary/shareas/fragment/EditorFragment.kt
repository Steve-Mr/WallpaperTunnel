package com.maary.shareas.fragment

import android.graphics.BlurMaskFilter.Blur
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentEditorBinding
import com.maary.shareas.fragment.editor.BlurFragment
import com.maary.shareas.fragment.editor.BrightnessFragment
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditorFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentBitmapState.collect { state ->
                    if (state == viewModel.HOME) {
                        binding.appbarToggleGroup.check(binding.appbarButtonHome.id)
                    }
                    if (state == viewModel.LOCK) {
                        binding.appbarToggleGroup.check(binding.appbarButtonLock.id)
                    }
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            viewModel.restoreChanges()
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarContainer.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            binding.editorButtons.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chipHome = binding.chipApplyHome
        val chipLock = binding.chipApplyLock

        binding.appbarButtonHome.setOnClickListener {
            viewModel.currentBitmap = viewModel.HOME
        }

        binding.appbarButtonLock.setOnClickListener {
            viewModel.currentBitmap = viewModel.LOCK
        }

        binding.appbarButtonCancel.setOnClickListener {
            viewModel.restoreChanges()
            removeFragment()
            exit()
        }

        binding.appbarButtonConfirm.setOnClickListener {
            viewModel.saveEdit()
            removeFragment()
            exit()
        }

        binding.editorButtonApply.setOnClickListener {
            if (!chipHome.isChecked) viewModel.abortEditHome()
            if (!chipLock.isChecked) viewModel.abortEditLock()
            viewModel.saveEdit()
            removeFragment()
        }

        binding.editorButtonAbort.setOnClickListener {
            viewModel.abortEdit()
            removeFragment()
        }

        binding.editorButtonBlur.setOnClickListener {
            loadFragment(BlurFragment())
        }

        binding.editorButtonBrightness.setOnClickListener {
            loadFragment(BrightnessFragment())
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EditorFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun exit() {
        viewModel.finishEditing()
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.beginTransaction()?.remove(this)?.commit()
        fragmentManager?.popBackStack()
    }

    private fun loadFragment(fragment: Fragment) {
        viewModel.abortEdit()
        removeFragment()
        val fragmentManager: FragmentManager = childFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(binding.editorFragmentContainer.id, fragment)
        transaction.addToBackStack(null) // 可选，用于返回栈管理
        transaction.commit()
        binding.editorCard.visibility = View.VISIBLE
    }

    private fun removeFragment() {
        binding.editorCard.visibility = View.INVISIBLE
        val fragmentManager: FragmentManager = childFragmentManager
        val transaction = fragmentManager.beginTransaction()
        for (f in fragmentManager.fragments) {
            transaction.remove(f)
        }
        transaction.commit()
        fragmentManager.popBackStack()
    }
}