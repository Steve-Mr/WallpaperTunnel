package com.maary.shareas.fragment.editor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.slider.Slider
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentBlurBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlurFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlurFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val viewModel: WallpaperViewModel by activityViewModels()
    private var _binding: FragmentBlurBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            viewModel.abortEdit()
            requireParentFragment().childFragmentManager.beginTransaction().remove(this@BlurFragment).commit()
            requireParentFragment().childFragmentManager.popBackStack()
            // 获取包含当前 Fragment 的布局
            val containingLayout = requireView().parent.parent.parent as? View
            // 如果布局不为空，则隐藏布局
            containingLayout?.visibility = View.INVISIBLE
            isEnabled = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        _binding = FragmentBlurBinding.inflate(inflater, container, false)
        val sliderBlur = binding.adjustmentSlider

        sliderBlur.valueFrom = 0f
        sliderBlur.valueTo = 25f
        sliderBlur.stepSize = 1f
        sliderBlur.addOnChangeListener(Slider.OnChangeListener { _: Slider?, value: Float, _: Boolean ->
            viewModel.editBlur(requireActivity(), value)
        })
        return binding.root
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
         * @return A new instance of fragment BlurFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
                BlurFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}