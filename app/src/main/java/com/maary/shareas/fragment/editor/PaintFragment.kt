package com.maary.shareas.fragment.editor

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.FragmentPaintBinding
import kotlinx.coroutines.launch

class PaintFragment : Fragment() {

    private var _binding: FragmentPaintBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WallpaperViewModel by activityViewModels()

    private var position = 4 //viewModel.CENTER
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.abortEdit()
                requireParentFragment().childFragmentManager.beginTransaction().remove(this@PaintFragment).commit()
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
                        val colorStateList = ColorStateList.valueOf(colorValue)
                        binding.buttonPaint.iconTint = colorStateList
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaintBinding.inflate(inflater, container, false)

        var paintType = 0

        binding.buttonAlignCenter.isChecked = true

        binding.hexEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.length == 6){
                    binding.hexEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                    paintType = binding.buttonColorCustom.id
                    checkButton(paintType)
                    binding.buttonColorCustom.setBackgroundColor(Color.parseColor("#${binding.hexEditText.text}"))
                }
            }
        })

        binding.buttonsAlignment.addOnButtonCheckedListener { _, checkId, isChecked ->
            if (isChecked) {
                when (checkId) {
                    R.id.button_align_left -> position = WallpaperViewModel.LEFT
                    R.id.button_align_top -> position = WallpaperViewModel.TOP
                    R.id.button_align_center -> position = WallpaperViewModel.CENTER
                    R.id.button_align_bottom -> position = WallpaperViewModel.BOTTOM
                    R.id.button_align_right -> position = WallpaperViewModel.RIGHT
                }
            }

        }

        val colors = viewModel.extractTopColorsFromBitmap()
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
                paintType = it.id
                setZoom()
            }
        }

        binding.buttonColor1.setOnClickListener {
            paintType = it.id
            setZoom()
            checkButton(it.id)
        }
        binding.buttonColor2.setOnClickListener {
            paintType = it.id
            setZoom()
            checkButton(it.id)
        }
        binding.buttonColor3.setOnClickListener {
            paintType = it.id
            setZoom()
            checkButton(it.id)
        }
        binding.buttonColor4.setOnClickListener {
            paintType = it.id
            setZoom()
            checkButton(it.id)
        }
        binding.buttonColor5.setOnClickListener {
            paintType = it.id
            setZoom()
            checkButton(it.id)
        }

        binding.buttonBlur.setOnClickListener {
            paintType = it.id
            if (binding.scaleEditText.text.isNullOrEmpty()) {
                binding.scaleEditText.setText(
                    resources.getStringArray(R.array.zoom_scales)[3],
                    false
                )
            }
            checkButton(it.id)
        }

        binding.buttonPaint.setOnClickListener {
            val zoom = binding.scaleEditText.text.toString().toFloat()
            when (paintType) {
                R.id.button_color1 -> viewModel.paintColor(position, colors[0], zoom)
                R.id.button_color2 -> viewModel.paintColor(position, colors[1], zoom)
                R.id.button_color3 -> viewModel.paintColor(position, colors[2], zoom)
                R.id.button_color4 -> viewModel.paintColor(position, colors[3], zoom)
                R.id.button_color5 -> viewModel.paintColor(position, colors[4], zoom)
                R.id.button_color_custom ->
                    viewModel.paintColor(position, Color.parseColor("#${binding.hexEditText.text}"), zoom)
                R.id.button_blur -> viewModel.paintBlur(position, 16, requireContext(), zoom)

            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun checkButton(buttonId: Int) {
        val buttons = mutableListOf(
            R.id.button_blur,
            R.id.button_color1,
            R.id.button_color2,
            R.id.button_color3,
            R.id.button_color4,
            R.id.button_color5,
            R.id.button_color_custom)

        for (button in buttons) {
            if (button == buttonId) {
                binding.root.findViewById<MaterialButton>(button).setIconResource(R.drawable.ic_done)
            } else {
                if (button == R.id.button_blur) {
                    binding.root.findViewById<MaterialButton>(button).setIconResource(R.drawable.ic_blur_16)
                } else {
                    binding.root.findViewById<MaterialButton>(button).icon = null
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

    private fun setZoom() {
        if (binding.scaleEditText.text.isNullOrEmpty()) {
            binding.scaleEditText.setText(resources.getStringArray(R.array.zoom_scales)[5], false)
        }
    }
}