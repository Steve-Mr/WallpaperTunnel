package com.maary.shareas.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maary.shareas.databinding.FragmentUpscaleSettingsBinding
import com.maary.shareas.helper.PreferencesHelper
import kotlinx.coroutines.launch

class UpscaleSettingsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentUpscaleSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpscaleSettingsBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment

        val preferencesHelper = PreferencesHelper(requireContext())
        binding.chooseTileSize.setText(preferencesHelper.getTileSize().toString(), false)
        binding.switchFp16.isChecked = preferencesHelper.getFP16()
        binding.switchCpuDisabled.isChecked = preferencesHelper.getCPUDisabled()

        binding.chooseTileSize.setOnItemClickListener { adapterView, _, i, _ ->
            preferencesHelper.setTileSize(adapterView.getItemAtPosition(i).toString().toInt())
        }

        binding.switchFp16.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesHelper.setFP16(isChecked)
            }
        }

        binding.switchCpuDisabled.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesHelper.setCPUDisabled(isChecked)
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "UpscaleSettingsDialog"
    }
}