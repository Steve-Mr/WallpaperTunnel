package com.maary.shareas.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WelcomeSystemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WelcomeSystemFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
        val rootView = _binding!!.root
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

        _binding!!.topAppBarWelcomeSystem.setNavigationOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                && Environment.isExternalStorageManager()) {
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                }else{
                    viewPager.setCurrentItem(viewPager.currentItem - 2, true)
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)

                }else{
                    viewPager.setCurrentItem(viewPager.currentItem - 2, true)
                }
            }

        }

        _binding!!.buttonWelcomeSystemYes.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                startActivity(intent)

            }
        }
        _binding!!.buttonWelcomeSystemNo.setOnClickListener {
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
            _binding!!.buttonWelcomeSystemNo.visibility = View.INVISIBLE
            _binding!!.buttonWelcomeSystemYes.visibility = View.INVISIBLE
            _binding!!.textWelcomeSystemPermission1.helperText = getText(R.string.permission_got)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WelcomeSystemFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WelcomeSystemFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}