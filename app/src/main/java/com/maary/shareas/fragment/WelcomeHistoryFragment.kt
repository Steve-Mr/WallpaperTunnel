package com.maary.shareas.fragment

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maary.shareas.helper.PreferencesHelper
import com.maary.shareas.QSTileService
import com.maary.shareas.R
import com.maary.shareas.databinding.FragmentWelcomeHistoryBinding
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class WelcomeHistoryFragment : Fragment() {

    private var _binding: FragmentWelcomeHistoryBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeHistoryBinding.inflate(inflater, container, false)
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.pager)

        // Inflate the layout for this fragment
        val rootView = binding.root
        rootView.findViewById<FloatingActionButton>(R.id.fab_welcome_history_next).setOnClickListener {
            if (checkPermission()){
                lifecycleScope.launch {
                    PreferencesHelper(requireContext()).setSettingsHistory(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                }else{
                    viewPager.setCurrentItem(viewPager.currentItem + 2, true)
                }
            } else{
                lifecycleScope.launch {
                    PreferencesHelper(requireContext()).setSettingsHistory(false)
                }
                viewPager.setCurrentItem(viewPager.currentItem + 2, true)

            }
        }
        binding.topAppBarWelcomeHistory.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            binding.textWelcomeHistoryPermission2.visibility = View.GONE
            binding.textWelcomeHistoryPermission1Edit.setText(R.string.read_external_storage)
        }

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!Environment.isExternalStorageManager()) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:com.maary.shareas")
                                )
                            )
                        }
                    }
                }
            }

        binding.buttonWelcomeHistoryYes.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        binding.buttonWelcomeHistoryNo.setOnClickListener {
            lifecycleScope.launch {
                PreferencesHelper(requireContext()).setSettingsHistory(false)
            }
            viewPager.setCurrentItem(viewPager.currentItem + 2, true)
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.fab_welcome_history_next)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (bottomMargin < systemBars.bottom){
                    bottomMargin = systemBars.bottom + v.marginBottom
                }
            }
            view.findViewById<AppBarLayout>(R.id.container_welcome_history_appbar)
                .setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        if (checkPermission()){
            binding.textWelcomeHistoryPermission1.helperText = getText(R.string.permission_got)
            binding.textWelcomeHistoryPermission2.helperText = getText(R.string.permission_got)
            binding.buttonWelcomeHistoryNo.visibility = View.INVISIBLE

            binding.buttonWelcomeHistoryYes.text = getText(R.string.add_quick_tile)
            binding.buttonWelcomeHistoryYes.setOnClickListener {
                val statusBarManager = requireActivity().getSystemService(StatusBarManager::class.java)
                val resultSuccessExecutor = Executor {
                    binding.buttonWelcomeHistoryYes.visibility = View.INVISIBLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    statusBarManager.requestAddTileService(
                        ComponentName(
                            requireContext(),
                            QSTileService::class.java
                        ),
                        getString(R.string.history_activity),
                        Icon.createWithResource(
                            requireContext(),
                            R.drawable.ic_history
                        ),
                        resultSuccessExecutor
                    ) {
                    }
                }
            }
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasStoragePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val hasExternalStorageManagerPermission = Environment.isExternalStorageManager()

                if (hasExternalStorageManagerPermission) {
                    binding.textWelcomeHistoryPermission1.helperText = getText(R.string.permission_got)
                }

                if (hasStoragePermission) {
                    binding.textWelcomeHistoryPermission2.helperText = getText(R.string.permission_got)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermission(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasStoragePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasExternalStorageManagerPermission = Environment.isExternalStorageManager()
            hasStoragePermission && hasExternalStorageManagerPermission
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}