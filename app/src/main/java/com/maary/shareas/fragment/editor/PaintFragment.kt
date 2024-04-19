package com.maary.shareas.fragment.editor

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.palette.graphics.Palette
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentPaintBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PaintFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PaintFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentPaintBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WallpaperViewModel by activityViewModels()

    private var position = 4 //viewModel.CENTER
    private var zoom = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaintBinding.inflate(inflater, container, false)


        binding.buttonAlignCenter.isChecked = true

//        binding.menuChooseScaleTextview.setText(resources.getStringArray(R.array.zoom_scales)[2], false)

        binding.hexEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.length == 6){
                    binding.hexEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                    binding.buttonColorCustom.setBackgroundColor(Color.parseColor("#${binding.hexEditText.text}"))
                }
            }
        })

        binding.buttonsAlignment.addOnButtonCheckedListener { _, checkId, isChecked ->
            if (isChecked) {
                when (checkId) {
                    R.id.button_align_left -> position = viewModel.LEFT
                    R.id.button_align_top -> position = viewModel.TOP
                    R.id.button_align_center -> position = viewModel.CENTER
                    R.id.button_align_bottom -> position = viewModel.BOTTOM
                    R.id.button_align_right -> position = viewModel.RIGHT
                }
            }

        }

        val colors = viewModel.extractTopFiveColors()
        binding.buttonColor1.setBackgroundColor(colors[0])
        binding.buttonColor2.setBackgroundColor(colors[1])
        binding.buttonColor3.setBackgroundColor(colors[2])
        binding.buttonColor4.setBackgroundColor(colors[3])
        binding.buttonColor5.setBackgroundColor(colors[4])

        binding.buttonColorCustom.setOnClickListener {
            if (binding.hexEditText.text.isNullOrEmpty()) {
                binding.hexEditText.requestFocus()
                // 显示键盘
                val imm = getSystemService(requireContext(), InputMethodManager::class.java) as InputMethodManager
                imm.showSoftInput(binding.hexEditText, InputMethodManager.SHOW_IMPLICIT)
            } else {
                paintColor(Color.parseColor("#${binding.hexEditText.text}"))
            }
        }

        binding.buttonColor1.setOnClickListener {
            paintColor(colors[0])
        }
        binding.buttonColor2.setOnClickListener {
            paintColor(colors[1])
        }
        binding.buttonColor3.setOnClickListener {
            paintColor(colors[2])
        }
        binding.buttonColor4.setOnClickListener {
            paintColor(colors[3])
        }
        binding.buttonColor5.setOnClickListener {
            paintColor(colors[4])
        }

        binding.buttonBlur.setOnClickListener {
            paintBlur(requireContext())
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PaintFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PaintFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun paintColor(color: Int) {
        zoom = if (binding.scaleEditText.text.isNullOrEmpty()) {
            binding.scaleEditText.setText(resources.getStringArray(R.array.zoom_scales)[5], false)
            1f
        } else {
            binding.scaleEditText.text.toString().toFloat()
        }
        viewModel.paintColor(
            position,
            color,
            zoom )
    }

    private fun paintBlur(context: Context) {
        zoom = if (binding.scaleEditText.text.isNullOrEmpty()) {
            binding.scaleEditText.setText(resources.getStringArray(R.array.zoom_scales)[3], false)
            0.8f
        } else {
            binding.scaleEditText.text.toString().toFloat()
        }
        viewModel.paintBlur(position, 16, context, zoom)

    }
}