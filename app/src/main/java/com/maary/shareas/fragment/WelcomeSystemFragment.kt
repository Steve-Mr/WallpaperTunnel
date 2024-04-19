package com.maary.shareas.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maary.shareas.R
import com.maary.shareas.databinding.FragmentWelcomeSystemBinding

class WelcomeSystemFragment : Fragment() {


    private var _binding: FragmentWelcomeSystemBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeSystemBinding.inflate(inflater, container, false)
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.pager)

        // Inflate the layout for this fragment
        val rootView = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(rootView.findViewById(R.id.fab_welcome_system_next)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (bottomMargin < systemBars.bottom){
                    bottomMargin = systemBars.bottom + v.marginBottom
                }
            }
            rootView.findViewById<AppBarLayout>(R.id.container_welcome_system_appbar)
                .setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        rootView.findViewById<FloatingActionButton>(R.id.fab_welcome_system_next).setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }

        binding.topAppBarWelcomeSystem.setNavigationOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }

        binding.buttonWelcomeSystemYes.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                startActivity(intent)

            }
        }
        binding.buttonWelcomeSystemNo.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        if (MediaStore.canManageMedia(requireContext())) {
            binding.buttonWelcomeSystemNo.visibility = View.INVISIBLE
            binding.buttonWelcomeSystemYes.visibility = View.INVISIBLE
            binding.textWelcomeSystemPermission1.helperText = getText(R.string.permission_got)
        }
    }
}