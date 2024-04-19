package com.maary.shareas.fragment.editor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentBlurBinding
import com.maary.shareas.databinding.FragmentUpscaleBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UpscaleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UpscaleFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentUpscaleBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UpscaleFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UpscaleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}